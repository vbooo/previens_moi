import {
  evidenceWindow,
  rankSources,
  searchLocale,
  sourceText,
} from "./source-content.ts";
import { Firecrawl, ProviderFault } from "./firecrawl.ts";
import {
  type Document,
  type Draft,
  type Intent,
  publicUrl,
} from "./semantic.ts";
export interface RetrievedSource extends Document {
  label: string;
  retrieval: "supplied_page" | "search_scrape";
}
export interface DiscoveryBudget {
  reserve(label: string, upperCredits: number): Promise<boolean>;
  settle(label: string): Promise<void>;
  unknown(label: string): Promise<void>;
}
/** Retrieval proves public access, not source authority, relevance or complete coverage. */
function document(
  item: any,
  kind: RetrievedSource["retrieval"],
  intent: Intent,
): RetrievedSource | null {
  const url = item?.metadata?.sourceURL ?? item?.url;
  const code = item?.metadata?.statusCode;
  if (
    typeof url !== "string" || !publicUrl(url) ||
    (code !== undefined && code !== 200) ||
    typeof item?.markdown !== "string" || item.markdown.trim().length < 40
  ) return null;
  const visible = sourceText(item.markdown);
  if (visible.trim().length < 40) return null;
  const raw = item.metadata?.publishedTime ??
    item.metadata?.["article:published_time"];
  const publication = typeof raw === "string" &&
      /^\d{4}-\d\d-\d\dT\d\d:\d\d:\d\d(?:\.\d+)?(?:Z|[+-]\d\d:\d\d)$/.test(
        raw,
      ) && Number.isFinite(Date.parse(raw))
    ? new Date(raw).toISOString()
    : null;
  return {
    url,
    label: typeof item.metadata?.title === "string"
      ? item.metadata.title.slice(0, 300)
      : new URL(url).hostname,
    text: evidenceWindow(visible, intent, 8000),
    publication,
    changedText: null,
    retrieval: kind,
  };
}
export async function discoverSources(
  client: Firecrawl,
  draft: Draft,
  intent: Intent,
  budget: DiscoveryBudget,
  includeWebSearch = true,
  bounds = { queries: 2, pages: 2, offset: 0 },
): Promise<
  {
    state: "retrieved" | "no_source" | "inaccessible";
    sources: RetrievedSource[];
    limited: true;
    failures: { url: string; state: string; httpStatus: number | null }[];
  }
> {
  if (
    !Number.isInteger(bounds.queries) || bounds.queries < 1 ||
    bounds.queries > 2 || !Number.isInteger(bounds.pages) || bounds.pages < 1 ||
    bounds.pages > 2 || !Number.isSafeInteger(bounds.offset) ||
    bounds.offset < 0
  ) throw new Error("Invalid discovery bounds");
  if (intent.status !== "understood") {
    throw new Error("Unresolved criteria cannot trigger discovery");
  }
  const supplied = [
    ...new Set(draft.text.match(/https:\/\/[^\s<>"\]]+/g) ?? []),
  ].filter(publicUrl);
  if (supplied.length > 2) {
    throw new Error("Too many supplied sources for bounded discovery");
  }
  const candidates = supplied.map((url) => ({
    url,
    kind: "supplied_page" as RetrievedSource["retrieval"],
  }));
  const call = async (
    label: string,
    path: string,
    body: unknown,
    upper: number,
  ) => {
    if (!await budget.reserve(label, upper)) {
      throw new Error("Discovery credit envelope exhausted");
    }
    try {
      const value = await client.call("POST", path, body);
      await budget.settle(label);
      return value;
    } catch (e) {
      await budget.unknown(label);
      throw e;
    }
  };
  // A supplied URL does not silently replace wider discovery requested by the user.
  if (includeWebSearch) {
    for (const query of [...new Set(intent.queries)].slice(0, bounds.queries)) {
      const result = await call("search:" + query, "/v2/search", {
        query,
        ...searchLocale(draft),
        limit: 3,
        sources: ["web"],
        timeout: 10000,
      }, 2);
      if (!Array.isArray(result.web)) {
        throw new Error("Unrecognized discovery response");
      }
      for (
        const item of rankSources(
          result.web.filter((v: any) =>
            typeof v?.url === "string" && publicUrl(v.url)
          ),
          draft,
          intent,
        )
      ) {
        if (
          typeof item?.url === "string" && publicUrl(item.url) &&
          !candidates.some((c) => c.url === item.url)
        ) candidates.push({ url: item.url, kind: "search_scrape" });
      }
    }
  }
  const sources: RetrievedSource[] = [];
  let inaccessible = false;
  const failures: { url: string; state: string; httpStatus: number | null }[] =
    [];
  const selected = candidates.slice(0, 3);
  const offset = selected.length ? bounds.offset % selected.length : 0;
  for (
    const candidate of [...selected.slice(offset), ...selected.slice(0, offset)]
      .slice(0, bounds.pages)
  ) {
    try {
      const value = await call("read:" + candidate.url, "/v2/scrape", {
        url: candidate.url,
        formats: ["markdown"],
        onlyMainContent: true,
        maxAge: 0,
        timeout: 25000,
      }, 1);
      const d = document(
        { ...value, url: candidate.url },
        candidate.kind,
        intent,
      );
      if (d) sources.push(d);
      else {
        inaccessible = true;
        failures.push({
          url: candidate.url,
          state: "unreadable_document",
          httpStatus: null,
        });
      }
    } catch (e) {
      if (!(e instanceof ProviderFault)) throw e;
      // A different candidate is independent work, never a retry of a lost POST.
      inaccessible = true;
      failures.push({
        url: candidate.url,
        state: e.state,
        httpStatus: e.httpStatus,
      });
    }
  }
  return {
    state: sources.length
      ? "retrieved"
      : inaccessible
      ? "inaccessible"
      : "no_source",
    sources,
    limited: true,
    failures,
  };
}

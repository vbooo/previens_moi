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
import type { DiscoveryBudget, RetrievedSource } from "./source-discovery.ts";
/** One search (six candidates), then at most three new bodies. Existing retained
 * pages are ALL checked on each cycle; search is repeated only every 24 cycles.
 * Snippets only order candidates. Coverage must still prove their suitability. */
export async function collectStableSources(
  client: Firecrawl,
  draft: Draft,
  intent: Intent,
  retained: { url: string; label: string }[],
  rediscover: boolean,
  budget: DiscoveryBudget,
) {
  if (
    intent.status !== "understood" || retained.length > 3 ||
    retained.some((s) => !publicUrl(s.url))
  ) throw new Error("Invalid source policy");
  const read = async (
    label: string,
    path: string,
    body: unknown,
    upper: number,
  ) => {
    if (!await budget.reserve(label, upper)) {
      throw new Error("Credit envelope exhausted");
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
  let candidates = retained.map((s) => ({ ...s, retained: true, score: 100 }));
  const answers = draft.answers as { questionId: string; value: string }[];
  const region = answers.find((a) => a.questionId === "region")?.value ?? "";
  if (rediscover || retained.length === 0) {
    const query = intent.queries[0] +
      (region && !intent.queries[0].toLowerCase().includes(region.toLowerCase())
        ? " " + region
        : "");
    const result = await read("discovery", "/v2/search", {
      query,
      ...searchLocale(draft),
      limit: 6,
      sources: ["web"],
      timeout: 10000,
    }, 2);
    if (!Array.isArray(result.web)) throw new Error("Unknown search response");
    const supplied = [
      answers.find((a) => a.questionId === "preferred_site")?.value,
      ...draft.text.match(/https:\/\/[^\s<>"\]]+/g) ?? [],
    ].filter((s): s is string => typeof s === "string" && publicUrl(s));
    const scored = rankSources(
      [
        ...supplied.map((url) => ({
          url,
          title: "",
          description: "",
          preferred: true,
        })),
        ...result.web.filter((v: any) =>
          typeof v?.url === "string" && publicUrl(v.url)
        ).map((v: any) => ({ ...v, preferred: false })),
      ].filter((v) => !retained.some((s) => s.url === v.url)),
      draft,
      intent,
    ).map((v) => ({
      url: v.url,
      label: typeof v.title === "string" && v.title.trim()
        ? v.title.slice(0, 300)
        : new URL(v.url).hostname,
      retained: false,
      score: 0,
    }));
    candidates = [
      ...candidates,
      ...scored.filter((v: any, i: number, all: any[]) =>
        all.findIndex((x) => x.url === v.url) === i
      ).slice(0, 3),
    ];
  }
  const documents: RetrievedSource[] = [];
  const failures: string[] = [];
  // Parallel independent pages keep the phase below its lease. No POST retries.
  await Promise.all(candidates.map(async (s) => {
    try {
      const value = await read("read:" + s.url, "/v2/scrape", {
        url: s.url,
        formats: ["markdown"],
        parsers: [],
        onlyMainContent: true,
        maxAge: 0,
        timeout: 25000,
      }, 1);
      const metadata = value.metadata as any;
      const visible = typeof value.markdown === "string"
        ? sourceText(value.markdown)
        : "";
      if (
        typeof value.markdown !== "string" ||
        visible.trim().length < 40 ||
        metadata?.statusCode && metadata.statusCode !== 200
      ) {
        failures.push(s.url);
        return;
      }
      const returned = metadata?.sourceURL ?? s.url;
      if (
        !publicUrl(returned) ||
        new URL(returned).origin !== new URL(s.url).origin
      ) {
        failures.push(s.url);
        return;
      }
      const raw = metadata?.publishedTime ??
        metadata?.["article:published_time"];
      const publication =
        typeof raw === "string" && /^\d{4}-\d\d-\d\dT/.test(raw) &&
          Number.isFinite(Date.parse(raw))
          ? new Date(raw).toISOString()
          : null;
      documents.push({
        url: s.url,
        label: s.label,
        text: evidenceWindow(
          visible,
          intent,
          Math.floor(7800 / Math.max(3, candidates.length)),
        ),
        publication,
        changedText: null,
        retrieval: "search_scrape",
      });
    } catch (e) {
      if (!(e instanceof ProviderFault)) throw e;
      failures.push(s.url);
    }
  }));
  documents.sort((a, b) =>
    candidates.findIndex((s) => s.url === a.url) -
    candidates.findIndex((s) => s.url === b.url)
  );
  return {
    documents,
    healthy: failures.length === 0,
    verified: retained.length
      ? documents.filter((d) => retained.some((s) => s.url === d.url)).length
      : documents.length,
    total: retained.length || candidates.length,
  };
}

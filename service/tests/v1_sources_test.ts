import { collectStableSources } from "../supabase/functions/_shared/stable-sources.ts";
import { Firecrawl } from "../supabase/functions/_shared/firecrawl.ts";
import {
  generalOne,
  type GeneralWork,
} from "../supabase/functions/_shared/general-worker.ts";
import {
  type Draft,
  type Intent,
  MODEL,
} from "../supabase/functions/_shared/semantic.ts";
const assert = (v: unknown) => {
  if (!v) throw new Error("assertion");
};
const draft: Draft = {
  text: "Préviens-moi des concerts de Mira à Tours.",
  language: "fr",
  revision: 1,
  mode: "once",
  answers: [{ questionId: "region", value: "France" }],
};
const intent: Intent = {
  status: "understood",
  title: "Concerts de Mira à Tours",
  criteria: [{
    id: "a",
    text: "Concert de Mira à Tours",
    quote: "concerts de Mira à Tours",
  }],
  questions: [],
  queries: ["Mira Tours concerts"],
  explanation: "Sources publiques",
};
const u = (n: number) => "https://source.example/" + n;
const body =
  "Mira annonce un concert à Tours le 2 mai 2027. Concert confirmé en France.";
const budget = () => ({
  held: 0,
  async reserve(_: string, n: number) {
    this.held += n;
    return this.held <= 8;
  },
  async settle() {},
  async unknown() {},
});
Deno.test("preparation compares search candidates, excludes wrong store geography and never uses snippets as proof", async () => {
  const calls: any[] = [];
  const b = budget();
  const client = new Firecrawl(
    "test",
    true,
    (async (url, init) => {
      const payload = JSON.parse(String(init?.body));
      calls.push(payload);
      return new Response(JSON.stringify({
        success: true,
        data: String(url).endsWith("/search")
          ? {
            web: [
              {
                url: "https://shop.example/en-us/mira",
                title: "Mira concerts US dollars",
              },
              { url: "https://concerts.fr/mira", title: "Mira à Tours France" },
              { url: u(1), title: "Mira Tours" },
              { url: u(2), title: "Mira Tours" },
            ],
          }
          : { markdown: body, metadata: { statusCode: 200 } },
      }));
    }) as typeof fetch,
  );
  const r = await collectStableSources(client, draft, intent, [], false, b);
  assert(
    calls[0].limit === 6 && calls[0].query.endsWith("France") &&
      calls.length === 4 && b.held === 5,
  );
  assert(
    r.documents.length === 3 &&
      r.documents[0].url === "https://concerts.fr/mira" &&
      !r.documents.some((d) => d.url.includes("en-us")),
  );
  assert(
    calls.slice(1).every((c) =>
      c.parsers.length === 0 && c.formats.join() === "markdown"
    ),
  );
});
Deno.test("hourly cycle reads every retained page, does not search again and reports partial failure", async () => {
  const urls: string[] = [];
  const b = budget();
  const client = new Firecrawl(
    "test",
    true,
    (async (url, init) => {
      assert(String(url).endsWith("/scrape"));
      const p = JSON.parse(String(init?.body));
      urls.push(p.url);
      if (p.url === u(1)) return new Response("{}", { status: 408 });
      return new Response(
        JSON.stringify({
          success: true,
          data: { markdown: body, metadata: { statusCode: 200 } },
        }),
      );
    }) as typeof fetch,
  );
  const r = await collectStableSources(
    client,
    draft,
    intent,
    [0, 1, 2].map((n) => ({ url: u(n), label: "Source " + n })),
    false,
    b,
  );
  assert(
    urls.length === 3 && new Set(urls).size === 3 && r.verified === 2 &&
      r.total === 3 && !r.healthy && b.held === 3,
  );
});
Deno.test("daily rediscovery keeps checking retained sources and caps extra candidates and prepaid credits", async () => {
  const urls: string[] = [];
  const b = budget();
  const client = new Firecrawl(
    "test",
    true,
    (async (url, init) => {
      const p = JSON.parse(String(init?.body));
      if (String(url).endsWith("/search")) {
        return new Response(
          JSON.stringify({
            success: true,
            data: {
              web: [3, 4, 5, 6].map((n) => ({
                url: u(n),
                title: "Mira Tours",
              })),
            },
          }),
        );
      }
      urls.push(p.url);
      return new Response(
        JSON.stringify({
          success: true,
          data: { markdown: body, metadata: { statusCode: 200 } },
        }),
      );
    }) as typeof fetch,
  );
  const r = await collectStableSources(
    client,
    draft,
    intent,
    [0, 1, 2].map((n) => ({ url: u(n), label: "Source " + n })),
    true,
    b,
  );
  assert(
    r.documents.length === 6 && urls.length === 6 && b.held === 8 &&
      [u(0), u(1), u(2)].every((url) => urls.includes(url)),
  );
});
const reply = (value: unknown) =>
  new Response(JSON.stringify({
    status: "completed",
    model: MODEL,
    service_tier: "default",
    usage: { input_tokens: 100, output_tokens: 100 },
    output: [{
      content: [{ type: "output_text", text: JSON.stringify(value) }],
    }],
  }));
const doc = {
  url: u(0),
  label: "Mira",
  text: body,
  publication: null,
  changedText: null,
  retrieval: "search_scrape" as const,
};
Deno.test("first qualifying information is an initial result and completes a one-off without claiming novelty", async () => {
  const work: GeneralWork = {
    id: "w",
    lease: "l",
    kind: "start",
    phase: "baseline",
    generation: 1,
    watchId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    context: {
      draft,
      intent,
      documents: [doc],
      prepared: true,
      retrievalHealthy: true,
      plan: {
        id: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
        version: 1,
        sourcePolicy: "stable_v1",
        backgroundPreparation: true,
        sources: [{ url: u(0), label: "Mira" }],
        criteria: intent.criteria.map((c) => c.text),
      },
      memory: {
        generation: 1,
        baselineAt: null,
        observedAt: null,
        completed: false,
        facts: [],
      },
    },
  };
  let result: any;
  const client = new Firecrawl("", false);
  await generalOne(
    {
      async lease() {
        return work;
      },
      async finish(_, r) {
        result = r;
      },
    },
    "test",
    client,
    true,
    (async () =>
      reply({
        decision: {
          status: "match",
          fact: body,
          reason: "Une annonce correspond aux critères.",
          identity: {
            who: "Mira",
            what: "concert",
            where: "Tours",
            when: "2027-05-02",
            variant: null,
          },
          sameAs: null,
          evidence: [{ criterionId: "a", passageId: "d0p0" }],
        },
      })) as typeof fetch,
  );
  assert(
    result.decision.outcome === "baseline" &&
      result.decision.memory.completed && result.findings.length === 1 &&
      result.findings[0].finding.noveltyBasis === "initial",
  );
});
Deno.test("coverage retains only pages actually supporting the criteria rather than all search results", async () => {
  const work: GeneralWork = {
    id: "w",
    lease: "l",
    kind: "start",
    phase: "decide",
    generation: 1,
    watchId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    context: {
      draft,
      intent,
      documents: [doc, {
        ...doc,
        url: u(1),
        text: "Generic unrelated store homepage, no concert listings.",
      }],
      plan: {
        id: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
        version: 1,
        sourcePolicy: "stable_v1",
        backgroundPreparation: true,
        sources: [],
        intervalHours: 1,
        criteria: intent.criteria.map((c) => c.text),
      },
    },
  };
  let result: any;
  await generalOne(
    {
      async lease() {
        return work;
      },
      async finish(_, r) {
        result = r;
      },
    },
    "test",
    new Firecrawl("", false),
    true,
    (async () =>
      reply({
        status: "observable",
        reason: "La source publie la programmation.",
        evidence: [{ criterionId: "a", passageId: "d0p0" }],
      })) as typeof fetch,
  );
  assert(
    result.preparedPlan.sources.length === 1 &&
      result.preparedPlan.sources[0].url === u(0) &&
      result.preparedPlan.intervalHours === 1 && result.documents.length === 1,
  );
});

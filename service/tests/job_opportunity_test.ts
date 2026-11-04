import {
  decideNovelty,
  type Memory,
} from "../supabase/functions/_shared/novelty.ts";
import {
  type Document,
  type Draft,
  type Intent,
  type Judgment,
  modelRequest,
  parseJudgment,
} from "../supabase/functions/_shared/semantic.ts";
import {
  isJobRequest,
  type JobOpportunity,
  jobPublicationDay,
  sameJob,
} from "../supabase/functions/_shared/job-opportunity.ts";
import {
  generalOne,
  type GeneralWork,
} from "../supabase/functions/_shared/general-worker.ts";
import { Firecrawl } from "../supabase/functions/_shared/firecrawl.ts";
import { MODEL } from "../supabase/functions/_shared/semantic.ts";
const assert = (v: unknown, message = "assertion") => {
  if (!v) throw new Error(message);
};
const refused = (fn: () => unknown) => {
  let failed = false;
  try {
    fn();
  } catch {
    failed = true;
  }
  assert(failed);
};
const draft: Draft = {
  text: "Préviens-moi des offres d'emploi d'auteur ou scénariste en France.",
  language: "fr",
  revision: 1,
  mode: "continuous",
  answers: [],
};
const intent: Intent = {
  status: "understood",
  title: "Offres scénariste",
  criteria: [{ id: "job", text: draft.text, quote: draft.text }],
  queries: ["emploi scénariste France"],
  questions: [],
  explanation: "Annonces publiques",
};
const url = "https://jobs.example/scenariste";
const t0 = "2026-11-03T16:00:00Z",
  t1 = "2026-11-04T01:00:00Z",
  t2 = "2026-11-04T02:00:00Z";
const memory: Memory = {
  generation: 1,
  baselineAt: t0,
  observedAt: t0,
  completed: false,
  facts: [],
  jobPolicyVersion: 1,
};
const baseJob: JobOpportunity = {
  title: "Scénariste junior en alternance",
  employer: "Studio Exemple",
  location: "Paris, France",
  reference: "POSTE-42",
  url,
  blockQuote: "",
  roleEvidence: "Écriture de scénarios de fiction pour la télévision.",
  roleMatch: "exact",
  publicationQuote: null,
};
function observation(changes: Partial<JobOpportunity> = {}, changed = true) {
  const job = { ...baseJob, ...changes };
  job.blockQuote = changes.blockQuote ??
    [
      "### " + job.title,
      job.employer,
      job.location,
      job.reference,
      job.roleEvidence,
      job.publicationQuote,
    ].filter(Boolean).join("\n");
  const document: Document = {
    url,
    text: job.blockQuote,
    publication: null,
    changedText: changed ? job.blockQuote : null,
  };
  const response: Judgment = {
    status: "match",
    fact: job.title + " chez " + job.employer,
    reason: "Les fonctions de cette annonce correspondent au métier demandé.",
    identity: {
      who: "des offres",
      what: "emplois scénaristes",
      where: "France",
      when: "2026-11-04",
      variant: "AI wording",
    },
    sameAs: null,
    evidence: [{ criterionId: "job", url, quote: job.roleEvidence }],
    job,
  };
  return { documents: [document], response };
}
Deno.test("job requests use stricter posting proof without changing product/event judgments", () => {
  assert(isJobRequest(draft, intent));
  assert(
    isJobRequest({ ...draft, text: "Screenwriter jobs in France" }, {
      ...intent,
      criteria: [],
    }),
  );
  assert(
    !isJobRequest(
      { ...draft, text: "Préviens-moi des promotions sur la PS5" },
      { ...intent, criteria: [] },
    ),
  );
  const body = JSON.parse(
    modelRequest("judgment", {
      draft,
      intent,
      documents: observation().documents,
      previous: [],
    }),
  );
  const match = body.text.format.schema.properties.decision.anyOf[0];
  assert(
    match.required.includes("job") &&
      match.properties.job.required.includes("employer"),
  );
  assert(
    body.instructions.includes("adjacent occupation") &&
      body.instructions.includes("SAME block"),
  );
});
Deno.test("generic nightly list summaries are refused instead of becoming an opportunity", () => {
  const o = observation();
  const { job: _, ...old } = o.response;
  refused(() =>
    parseJudgment(
      {
        ...old,
        fact:
          "Des offres d'emploi pour scénariste sont disponibles en Ile-de-France.",
      },
      draft,
      intent,
      o.documents,
      [],
    )
  );
});
Deno.test("related occupations, invented employer and mixed posting evidence cannot pass", () => {
  const o = observation();
  for (
    const job of [{ ...o.response.job!, roleMatch: "related" }, {
      ...o.response.job!,
      employer: "Invented company",
    }, { ...o.response.job!, roleEvidence: "Unquoted creative duties" }]
  ) {
    refused(() =>
      parseJudgment({ ...o.response, job }, draft, intent, o.documents, [])
    );
  }
  const unrelated = "Rédacteur marketing pour écrire des fiches produit.";
  refused(() =>
    parseJudgment(
      {
        ...o.response,
        evidence: [{ criterionId: "job", url, quote: unrelated }],
      },
      draft,
      intent,
      [{
        ...o.documents[0],
        text: o.documents[0].text + "\n### Autre offre\n" + unrelated,
      }],
      [],
    )
  );
  refused(() =>
    parseJudgment(
      {
        ...o.response,
        job: {
          ...o.response.job!,
          blockQuote: o.documents[0].text + "\n### Autre offre\n" + unrelated,
        },
      },
      draft,
      intent,
      [{
        ...o.documents[0],
        text: o.documents[0].text + "\n### Autre offre\n" + unrelated,
      }],
      [],
    )
  );
});
Deno.test("a precise verified posting overrides the old generic identity and does not invent an event date", () => {
  const o = observation();
  const j = parseJudgment(o.response, draft, intent, o.documents, []);
  assert(
    j.identity?.who === baseJob.employer && j.identity.what === baseJob.title &&
      j.identity.when === null && j.identity.variant === "POSTE-42",
  );
});
Deno.test("same vacancy relabelled with another date or title on the same URL never notifies twice, including after restart", async () => {
  const first = await decideNovelty(
    memory,
    1,
    draft,
    intent,
    [observation()],
    t1,
    true,
  );
  assert(first.findings.length === 1);
  const again = observation({
    title: "Alternance — Scénariste Junior",
    publicationQuote: "Publié le 4 novembre 2026",
  });
  const second = await decideNovelty(
    JSON.parse(JSON.stringify(first.memory)),
    1,
    draft,
    intent,
    [again],
    t2,
    true,
  );
  assert(
    second.outcome === "no_change" && second.findings.length === 0 &&
      second.memory.facts.length === 1,
  );
});
Deno.test("tracking a different page URL does not make the same vacancy new", async () => {
  const a = await decideNovelty(
    memory,
    1,
    draft,
    intent,
    [observation()],
    t1,
    true,
  );
  const b = observation();
  b.documents[0].url = "https://other.example/offers";
  b.response.job!.url = b.documents[0].url;
  b.response.evidence[0].url = b.documents[0].url;
  const r = await decideNovelty(a.memory, 1, draft, intent, [b], t2, true);
  assert(r.findings.length === 0);
});
Deno.test("a genuinely different job on the SAME listing URL produces one new opportunity", async () => {
  const a = await decideNovelty(
    memory,
    1,
    draft,
    intent,
    [observation()],
    t1,
    true,
  );
  const b = observation({
    title: "Scénariste animation",
    employer: "Studio Animation",
    reference: "POSTE-43",
  });
  const r = await decideNovelty(a.memory, 1, draft, intent, [b], t2, true);
  assert(
    r.findings.length === 1 &&
      r.findings[0].fact.job?.employer === "Studio Animation",
  );
});
Deno.test("old 24 September/20 October postings resurfacing in changed listings do not notify", async () => {
  for (
    const publicationQuote of [
      "24 septembre",
      "20 octobre",
      "Publié le 20 octobre 2026",
      "2026-10-20",
    ]
  ) {
    const r = await decideNovelty(
      memory,
      1,
      draft,
      intent,
      [observation({ publicationQuote })],
      t1,
      true,
    );
    assert(
      r.outcome === "no_change" && r.findings.length === 0 &&
        r.memory.facts.length === 1,
      publicationQuote,
    );
  }
  assert(
    jobPublicationDay("31 février 2026", t1) === null &&
      jobPublicationDay("24 septembre", t1) === "2026-09-24",
  );
});
Deno.test("a previously visible undated card is not new when listing formatting changes", async () => {
  const o = observation();
  const previous = [{ ...o.documents[0], changedText: null }];
  const r = await decideNovelty(
    memory,
    1,
    draft,
    intent,
    [o],
    t1,
    true,
    previous,
  );
  assert(r.outcome === "no_change" && !r.findings.length);
});
Deno.test("page publication date cannot turn an undated old job into a freshly published job", async () => {
  const o = observation({}, false);
  o.documents[0].publication = "2026-11-04T00:30:00Z";
  const r = await decideNovelty(memory, 1, draft, intent, [o], t1, true);
  assert(r.outcome === "degraded" && !r.findings.length);
});
Deno.test("deployment upgrades existing memories silently and keeps historical facts", async () => {
  const legacy = { ...memory };
  delete legacy.jobPolicyVersion;
  legacy.facts = [{
    key: "b".repeat(64),
    identity: {
      who: "offres",
      what: "scénariste",
      where: "France",
      when: "2026-10-20",
      variant: null,
    },
    text: "Historical result",
  }];
  const r = await decideNovelty(
    legacy,
    1,
    draft,
    intent,
    [observation()],
    t1,
    true,
  );
  assert(
    r.outcome === "no_change" && !r.findings.length &&
      r.memory.facts.length === 2 && r.memory.jobPolicyVersion === 1,
  );
  const next = await decideNovelty(
    r.memory,
    1,
    draft,
    intent,
    [observation({ reference: "POSTE-99", employer: "Nouveau Studio" })],
    t2,
    true,
  );
  assert(next.findings.length === 1);
});
Deno.test("title formatting and token order do not create another job without a reference; distinct references/locations survive", () => {
  assert(
    sameJob({ ...baseJob, reference: null }, {
      ...baseJob,
      reference: null,
      title: "Alternance : scénariste junior H/F",
    }),
  );
  assert(!sameJob(baseJob, { ...baseJob, reference: "POSTE-43" }));
  assert(!sameJob(baseJob, { ...baseJob, location: "Lyon, France" }));
});
Deno.test("generic same-URL price improvement still notifies, without a URL-level block", async () => {
  const retail = {
    ...draft,
    text: "Préviens-moi des promotions PS5 en France",
  };
  const retailIntent = {
    ...intent,
    criteria: [{ id: "job", text: retail.text, quote: retail.text }],
  };
  const check = (price: string) => {
    const text = "PS5 neuve en promotion chez Shop à " + price +
      " euros en France.";
    return {
      documents: [{ url, text, publication: null, changedText: text }],
      response: {
        status: "match",
        fact: text,
        reason: "Prix vérifié",
        identity: {
          who: "Shop",
          what: "PS5",
          where: "France",
          when: null,
          variant: price + " EUR",
        },
        sameAs: null,
        evidence: [{ criterionId: "job", url, quote: text }],
      },
    };
  };
  const a = await decideNovelty(
    { ...memory, baselineAt: null, observedAt: null },
    1,
    retail,
    retailIntent,
    [check("499")],
    t0,
    true,
  );
  const b = await decideNovelty(
    a.memory,
    1,
    retail,
    retailIntent,
    [check("449")],
    t1,
    true,
  );
  const c = await decideNovelty(
    b.memory,
    1,
    retail,
    retailIntent,
    [check("449")],
    t2,
    true,
  );
  assert(b.findings.length === 1 && c.findings.length === 0);
});
Deno.test("worker persists atomic job title, once completion and source evidence through the real response parser", async () => {
  const o = observation();
  let result: any;
  let calls = 0;
  const work: GeneralWork = {
    id: "work",
    lease: "lease",
    kind: "start",
    phase: "baseline",
    generation: 1,
    watchId: "aaaaaaaa-aaaa-4aaa-8aaa-aaaaaaaaaaaa",
    context: {
      draft: { ...draft, mode: "once" },
      intent,
      documents: [{
        ...o.documents[0],
        label: "Jobs",
        retrieval: "search_scrape",
      }],
      prepared: true,
      retrievalHealthy: true,
      plan: {
        id: "bbbbbbbb-bbbb-4bbb-8bbb-bbbbbbbbbbbb",
        version: 1,
        sourcePolicy: "stable_v1",
        sources: [{ url, label: "Jobs" }],
        criteria: intent.criteria.map((c) => c.text),
      },
      memory: { ...memory, baselineAt: null, observedAt: null },
    },
  };
  const response = {
    ...o.response,
    evidence: [{ criterionId: "job", passageId: "d0p0" }],
  };
  await generalOne(
    {
      async lease() {
        return work;
      },
      async finish(_, r) {
        result = r;
      },
    },
    "test-key",
    new Firecrawl("", false),
    true,
    (async () => {
      calls++;
      return new Response(JSON.stringify({
        status: "completed",
        model: MODEL,
        service_tier: "default",
        usage: { input_tokens: 100, output_tokens: 100 },
        output: [{
          content: [{
            type: "output_text",
            text: JSON.stringify({ decision: response }),
          }],
        }],
      }));
    }) as typeof fetch,
  );
  assert(calls === 1 && !result.failed && result.findings.length === 1);
  assert(
    result.findings[0].finding.title ===
        baseJob.title + " — " + baseJob.employer &&
      result.decision.memory.completed &&
      result.decision.memory.facts[0].job.reference === "POSTE-42",
  );
});

Deno.test("job request history sends stable identities without duplicating all stored prose and posting metadata", () => {
  const previous = Array.from({ length: 20 }, (_, i) => ({
    key: String(i).padStart(64, "0"),
    identity: {
      who: "Studio Exemple",
      what: "Scénariste",
      where: "Paris",
      when: null,
      variant: String(i),
    },
    text: "Old result prose ".repeat(60),
    job: baseJob,
  }));
  const request = JSON.parse(
    modelRequest("judgment", {
      draft,
      intent,
      documents: observation().documents,
      previous,
    }),
  );
  const history = JSON.parse(request.input).previous;
  assert(
    history.length === 20 && history[0].identity.what === "Scénariste" &&
      !history[0].text && !history[0].job,
  );
});

Deno.test("employment statistics are not misclassified as individual vacancy monitoring", () => {
  const d = {
    ...draft,
    text: "Préviens-moi quand le taux d'emploi en France augmente",
  };
  assert(
    !isJobRequest(d, {
      ...intent,
      criteria: [{ id: "rate", text: d.text, quote: d.text }],
    }),
  );
});
Deno.test("a same-day newly added posting is not discarded just because publication has no time", async () => {
  const r = await decideNovelty(
    memory,
    1,
    draft,
    intent,
    [observation({ publicationQuote: "Publié le 3 novembre 2026" })],
    t1,
    true,
  );
  assert(r.findings.length === 1);
});
Deno.test("a previously visible undated vacancy can become newly qualifying on changed decisive evidence", async () => {
  const o = observation();
  o.response.evidence[0].quote = "Salaire : 55 000 EUR";
  o.documents[0].text += "\nSalaire : 55 000 EUR";
  o.documents[0].changedText = "Salaire : 55 000 EUR";
  o.response.job!.blockQuote = o.documents[0].text;
  const previous = [{
    ...o.documents[0],
    text: o.documents[0].text.replace("55 000", "45 000"),
    changedText: null,
  }];
  const salaryDraft = {
    ...draft,
    text:
      "Préviens-moi des offres d'emploi de scénariste en France à partir de 55 000 EUR",
  };
  const salaryIntent = {
    ...intent,
    criteria: [...intent.criteria, {
      id: "salary",
      text: "Salaire à partir de 55 000 EUR",
      quote: "55 000 EUR",
    }],
  };
  o.response.evidence = [{
    criterionId: "job",
    url,
    quote: baseJob.roleEvidence,
  }, { criterionId: "salary", url, quote: "Salaire : 55 000 EUR" }];
  const r = await decideNovelty(
    memory,
    1,
    salaryDraft,
    salaryIntent,
    [o],
    t1,
    true,
    previous,
  );
  assert(r.findings.length === 1);
});

Deno.test("a detailed posting can contain mission/profile sections without becoming a mixed listing", () => {
  const o = observation();
  o.documents[0].text = "# " + baseJob.title + "\n" + baseJob.employer +
    "\nParis, France\nPOSTE-42\n## Missions\n" + baseJob.roleEvidence +
    "\n## Profil\nExpérience de scénariste souhaitée.";
  o.documents[0].changedText = o.documents[0].text;
  o.response.job!.blockQuote = o.documents[0].text;
  assert(
    parseJudgment(o.response, draft, intent, o.documents, []).status ===
      "match",
  );
});

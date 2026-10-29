import {
  evidenceWindow,
  rankSources,
  searchLocale,
  sourceText,
} from "../supabase/functions/_shared/source-content.ts";
import type { Draft, Intent } from "../supabase/functions/_shared/semantic.ts";
const assert = (v: unknown) => {
  if (!v) throw new Error("assertion");
};
const draft: Draft = {
  text: "Offres développeur Android vers Montpellier",
  language: "fr",
  revision: 1,
  mode: "once",
  answers: [{ questionId: "region", value: "France" }],
};
const intent: Intent = {
  status: "understood",
  title: "Emploi Android",
  criteria: [{ id: "job", text: draft.text, quote: draft.text }],
  queries: ["emploi développeur Android Montpellier France"],
  questions: [],
  explanation: "Offres publiques",
};
Deno.test("search honors an explicit region and avoids the US default for legacy French requests", () => {
  assert(
    JSON.stringify(searchLocale(draft)) ===
      JSON.stringify({ location: "France", country: "FR" }),
  );
  assert(
    JSON.stringify(searchLocale({ ...draft, answers: [] })) ===
      JSON.stringify({ country: "FR" }),
  );
  assert(
    JSON.stringify(
      searchLocale({
        ...draft,
        language: "en",
        answers: [{ questionId: "region", value: "Belgique" }],
      }),
    ) === JSON.stringify({ location: "Belgique" }),
  );
});
Deno.test("specific city beats national category hits without inventing a publisher allowlist", () => {
  const hits = [
    {
      url: "https://emplois.fr/android",
      title: "Toutes les offres emploi développeur Android France",
      description: "Développeur Android emploi",
    },
    {
      url: "https://annonces.example/montpellier",
      title: "Développeur Android à Montpellier",
      description: "Offres locales",
    },
    {
      url: "https://autre.fr/android",
      title: "Emploi développeur Android en France",
    },
  ];
  assert(rankSources(hits, draft, intent)[0].url === hits[1].url);
});
Deno.test("long navigation and URL keyword repetition cannot hide a relevant later job listing", () => {
  const nav =
    ("[Développeur Android](https://annuaire.example/developpeur-android-montpellier) · Emploi · Se connecter\n")
      .repeat(90);
  const body =
    "## Développeur Android à Montpellier\nEntreprise locale · CDI · Kotlin et Jetpack Compose. Offre publiée en France.\n";
  const raw = nav + body + "Autres informations sans rapport.\n".repeat(90);
  const excerpt = evidenceWindow(raw, intent, 2600);
  assert(
    excerpt.includes(body) && excerpt.length === 2600 && raw.includes(excerpt),
  );
  assert(!excerpt.includes("invented"));
});
Deno.test("selection remains one literal contiguous excerpt and does not join remote facts", () => {
  const raw = "Développeur Android à Paris, CDI.\n" +
    "Menu sans rapport.\n".repeat(500) + "Montpellier : comptable, CDD.\n";
  const excerpt = evidenceWindow(raw, intent, 2600);
  assert(raw.includes(excerpt) && excerpt.length <= 2600);
  assert(
    !(excerpt.includes("Android à Paris") &&
      excerpt.includes("Montpellier : comptable")),
  );
  assert(
    evidenceWindow("Développeur Android à Montpellier", intent, 2600) ===
      "Développeur Android à Montpellier",
  );
});

Deno.test("source text preserves visible product facts while removing long image and tracking URLs", () => {
  const raw =
    "## Console PS5\n![Console](https://store.example/image.jpg)\n[PS5 en promotion](https://store.example/ps5?tracking=123)\nPrix habituel : 549 €. Prix réduit : 449 €.\n\nAutre produit à Montpellier : un livre.";
  const visible = sourceText(raw);
  assert(
    visible.includes("PS5 en promotion") &&
      visible.includes("Prix habituel : 549 €. Prix réduit : 449 €.") &&
      visible.includes("\n\nAutre produit"),
  );
  assert(!visible.includes("tracking") && !visible.includes("image.jpg"));
});
Deno.test("PS5 and promotion spelling variants rank the actual deals page above a generic console catalog", () => {
  const request = {
    ...draft,
    text: "Promotions sur la Playstation 5",
    answers: [],
  };
  const target = {
    ...intent,
    title: request.text,
    criteria: [{ id: "p", text: request.text, quote: request.text }],
    queries: ["promotions Playstation 5"],
  };
  const hits = [{
    url: "https://store.fr/consoles",
    title: "Consoles Playstation 5",
  }, { url: "https://deals.fr/ps5", title: "PS5 : nos promos" }];
  assert(rankSources(hits, request, target)[0].url === hits[1].url);
});

import type { Document, Draft, Intent } from "./semantic.ts";

/** Posting identity is independent of the listing URL, publication label and AI prose. */
export interface JobIdentity {
  title: string;
  employer: string;
  location: string | null;
  reference: string | null;
}
export interface JobOpportunity extends JobIdentity {
  url: string;
  blockQuote: string;
  roleEvidence: string;
  roleMatch: "exact" | "equivalent" | "related" | "unknown";
  publicationQuote: string | null;
}
export const jobSchema = {
  type: "object",
  additionalProperties: false,
  properties: {
    title: { type: "string", minLength: 1, maxLength: 200 },
    employer: { type: "string", minLength: 1, maxLength: 200 },
    location: { anyOf: [{ type: "string", maxLength: 200 }, { type: "null" }] },
    reference: {
      anyOf: [{ type: "string", maxLength: 200 }, { type: "null" }],
    },
    url: { type: "string", maxLength: 1000 },
    blockQuote: { type: "string", minLength: 1, maxLength: 1600 },
    roleEvidence: { type: "string", minLength: 1, maxLength: 400 },
    roleMatch: {
      type: "string",
      enum: ["exact", "equivalent", "related", "unknown"],
    },
    publicationQuote: {
      anyOf: [{ type: "string", maxLength: 100 }, { type: "null" }],
    },
  },
  required: [
    "title",
    "employer",
    "location",
    "reference",
    "url",
    "blockQuote",
    "roleEvidence",
    "roleMatch",
    "publicationQuote",
  ],
};
export const normalizedJobText = (s: string) =>
  s.normalize("NFKD")
    .replace(/\p{M}/gu, "").toLowerCase().replace(/[^\p{L}\p{N}]+/gu, " ")
    .trim();
const normalizedQuote = (s: string) =>
  s.normalize("NFC").replace(/\s+/g, " ").trim();
/** Covers the currently supported French/English public job requests; no category
 * or source allowlist is imposed on the generic engine. URLs are not user intent. */
export function isJobRequest(draft: Draft, intent: Intent): boolean {
  const text = normalizedJobText(
    [draft.text, ...intent.criteria.map((c) => c.text)].join(" ").replace(
      /https?:\/\/\S+/g,
      "",
    ),
  );
  return /\b(offres? (?:d )?emplois?|annonces? (?:d )?emplois?|recrutement|recrute|jobs?|vacanc(?:y|ies)|hiring)\b/
    .test(
      text,
    ) ||
    /\b(offres?|annonces?|offers?|opportunities|positions?|postes?|missions?)\b/
        .test(text) &&
      /\b(developpeur|developer|scenariste|screenwriter|auteur|writer|travail|work|professionnel|professionnelle)\b/
        .test(text);
}
/** Every claim must belong to ONE contiguous posting, not the search heading,
 * date filters, a neighbouring employer or a merely related writing occupation. */
export function validateJob(
  job: JobOpportunity,
  documents: Document[],
  evidence: { url: string; quote: string }[],
): void {
  const doc = documents.find((d) => d.url === job.url);
  const block = normalizedQuote(job.blockQuote);
  if (
    !doc || !normalizedQuote(doc.text).includes(block) ||
    !["exact", "equivalent"].includes(job.roleMatch) ||
    ![
      job.title,
      job.employer,
      job.roleEvidence,
      job.location,
      job.reference,
      job.publicationQuote,
    ]
      .filter((s): s is string => s !== null).every((s) =>
        s.trim() && block.includes(normalizedQuote(s))
      ) ||
    evidence.some((e) =>
      e.url !== job.url || !block.includes(normalizedQuote(e.quote))
    )
  ) {
    throw new Error("Unproven individual job posting");
  }
  // Sibling posting headings delimit separate cards. Deeper headings such as
  // Missions/Profil inside one detailed vacancy remain valid.
  const headings = [...job.blockQuote.matchAll(/^(#{1,6})[ \t]+(.+)$/gm)];
  const titleHeading = headings.find((h) =>
    normalizedJobText(h[2]).includes(normalizedJobText(job.title))
  );
  if (
    headings.length > 1 &&
    (!titleHeading ||
      headings.some((h) =>
        h !== titleHeading && h.index! > titleHeading.index! &&
        h[1].length <= titleHeading[1].length
      ))
  ) throw new Error("Mixed job postings");
}
export function jobIdentity(job: JobIdentity): JobIdentity {
  return {
    title: job.title,
    employer: job.employer,
    location: job.location,
    reference: job.reference,
  };
}
export function sameJob(a: JobIdentity, b: JobIdentity): boolean {
  const n = normalizedJobText;
  if (
    n(a.employer) !== n(b.employer) ||
    n(a.location ?? "") !== n(b.location ?? "")
  ) return false;
  if (a.reference && b.reference) return n(a.reference) === n(b.reference);
  // Formatting, punctuation, word order and H/F do not create another vacancy.
  const words = (s: string) =>
    [
      ...new Set(
        n(s).split(" ").filter((w) =>
          !["h", "f", "hf", "m", "w", "d", "en", "de", "le", "la", "et"]
            .includes(w)
        ),
      ),
    ].sort();
  return words(a.title).join(" ") === words(b.title).join(" ");
}
/** Reappearing/reordered old cards are not a new opportunity, even if only one
 * of the old cards was selected by the previous AI assessment. */
export function jobWasVisible(
  job: JobOpportunity,
  previous: Document[],
  evidence: { quote: string }[] = [],
): boolean {
  return previous.some((d) => {
    const text = normalizedJobText(d.text);
    return evidence.every((e) =>
      normalizedQuote(d.text).includes(normalizedQuote(e.quote))
    ) &&
      text.includes(normalizedJobText(job.employer)) &&
      (job.reference
        ? text.includes(normalizedJobText(job.reference))
        : text.includes(normalizedJobText(job.title)) &&
          text.includes(normalizedJobText(job.roleEvidence)));
  });
}
const months = [
  "janvier january",
  "fevrier february",
  "mars march",
  "avril april",
  "mai may",
  "juin june",
  "juillet july",
  "aout august",
  "septembre september",
  "octobre october",
  "novembre november",
  "decembre december",
];
/** Only an explicitly quoted calendar date is usable. A yearless label is
 * conservatively the most recent past occurrence, never evidence of fresh publication. */
export function jobPublicationDay(
  quote: string | null,
  observedAt: string,
): string | null {
  if (!quote) return null;
  const iso = quote.match(/\b(\d{4})-(\d\d)-(\d\d)\b/);
  let year: number, month: number, day: number;
  if (iso) [, year, month, day] = iso.map(Number);
  else {
    const parts = normalizedJobText(quote).match(
      /\b(\d{1,2}) ([a-z]+)(?: (\d{4}))?\b/,
    );
    if (!parts) return null;
    month = months.findIndex((names) => names.split(" ").includes(parts[2])) +
      1;
    if (!month) return null;
    day = Number(parts[1]);
    year = parts[3] ? Number(parts[3]) : new Date(observedAt).getUTCFullYear();
    if (
      !parts[3] &&
      `${year}-${String(month).padStart(2, "0")}-${
          String(day).padStart(2, "0")
        }` > observedAt.slice(0, 10)
    ) year--;
  }
  const date = `${year!}-${String(month!).padStart(2, "0")}-${
    String(day!).padStart(2, "0")
  }`;
  return Number.isFinite(Date.parse(date)) &&
      new Date(date).toISOString().slice(0, 10) === date
    ? date
    : null;
}

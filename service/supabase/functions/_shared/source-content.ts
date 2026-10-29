import type { Draft, Intent } from "./semantic.ts";

const fold = (s: string) =>
  s.normalize("NFD").replace(/\p{M}/gu, "").toLowerCase();
// Common spelling/abbreviation variants must rank as the same requested concept.
const concept = (word: string) =>
  /^promo(?:tion(?:nel(?:le)?)?)?s?$/.test(word) ? "promotion" : word;
const searchable = (text: string) =>
  fold(text).replace(/\bplay\s*station\s*5\b|\bps\s*5\b/g, "ps5");

/** Remove Markdown transport markup, keeping visible words and paragraph order.
 * Image URLs and link destinations are not facts about an offer. */
export function sourceText(markdown: string): string {
  return markdown.replace(/!\[[^\]]*\]\([^\n)]*\)/g, "")
    .replace(/\[([^\]]*)\]\([^\n)]*\)/g, "$1")
    .replace(/https?:\/\/[^\s<>()]+/g, "")
    .replace(/[ \t]+\n/g, "\n").replace(/\n{3,}/g, "\n\n").trim();
}
const stop = new Set(
  "les des une pour dans sur avec quand moi que qui est aux par pas plus tout tous toutes etre region surveillance nouvelles nouveau nouveaux offre offres emploi recherche france the and for from with this that new"
    .split(" "),
);
function terms(intent: Intent): Set<string> {
  return new Set(
    (searchable(
      intent.criteria.map((c) => c.text).join(" ") + " " +
        intent.queries.join(" "),
    ).match(/[\p{L}\p{N}]{3,}/gu) ?? []).map(concept).filter((w) =>
      !stop.has(w)
    ),
  );
}

/** V2 supports country/location, not the old `lang` option. Honor an explicit
 * region; for old French requests without one, prefer the French search market
 * instead of the provider's US default. This is ranking bias, NOT a new criterion. */
export function searchLocale(draft: Draft) {
  const region = (draft.answers as { questionId: string; value: string }[])
    .find((a) => a.questionId === "region")?.value.trim();
  return {
    ...(region ? { location: region } : {}),
    ...((region && fold(region) === "france") ||
        (!region && draft.language === "fr")
      ? { country: "FR" }
      : {}),
  };
}

/** Search snippets rank candidates; they never become evidence. Prefer specific
 * terms (a city, product or artist) over generic category words shared by every hit. */
export function rankSources<
  T extends {
    url: string;
    title?: unknown;
    description?: unknown;
    preferred?: boolean;
  },
>(items: T[], draft: Draft, intent: Intent): T[] {
  const words = terms(intent);
  const contents = items.map((v) =>
    searchable(
      [v.title, v.description, v.url].filter((v) => typeof v === "string").join(
        " ",
      ),
    )
  );
  const weights = new Map(
    [...words].map(
      (w) => [
        w,
        1 + Math.log(
          1 + items.length / Math.max(
                1,
                contents.filter((c) => c.includes(w)).length,
              ),
        ),
      ],
    ),
  );
  const region = fold(
    (draft.answers as { questionId: string; value: string }[]).find((a) =>
      a.questionId === "region"
    )?.value ?? "",
  );
  return items.map((item, index) => {
    const content = contents[index];
    const foreign = /\/en-us\/|\/en-gb\/|\/us\/|\busd\b|\bdollars?\b/.test(
      content,
    );
    const french = /\.fr\b|\/fr-fr\/|\bfrance\b/.test(content);
    const geography = region === "france"
      ? (foreign ? -100 : french ? 1 : 0)
      : 0;
    const language = draft.language === "fr" && french ? 0.5 : 0;
    const score = (item.preferred ? 150 : 0) + geography + language +
      [...words].reduce(
        (n, w) =>
          n +
          ((content.includes(w) ||
              (w === "promotion" &&
                /\bpromo(?:tion(?:nel(?:le)?)?)?s?\b/.test(content)))
            ? weights.get(w)!
            : 0),
        0,
      );
    return { item, index, score };
  }).filter((v) => v.score >= 0).sort((a, b) =>
    b.score - a.score || a.index - b.index
  ).map((v) => v.item);
}

/** A bounded, CONTIGUOUS excerpt from the actual body, not always its navigation
 * prefix. Never splice distant jobs/facts together or treat a link target as proof. */
export function evidenceWindow(
  text: string,
  intent: Intent,
  limit: number,
): string {
  if (text.length <= limit) return text;
  const words = terms(intent);
  // Blanking destinations preserves offsets in the original source, including accents.
  const scoring = text.replace(
    /https?:\/\/[^\s)<>]+/g,
    (url) => " ".repeat(url.length),
  );
  const hits = [...scoring.matchAll(/[\p{L}\p{N}]{3,}/gu)].map((m) => ({
    at: m.index!,
    word: concept(fold(m[0]) === "playstation" ? "ps5" : fold(m[0])),
  })).filter((h) => words.has(h.word));
  if (!hits.length) return text.slice(0, limit);
  const counts = new Map<string, number>();
  for (const h of hits) counts.set(h.word, (counts.get(h.word) ?? 0) + 1);
  const weights = new Map(
    [...counts].map(([w, n]) => [w, 1 + Math.log(1 + hits.length / n)]),
  );
  // Leave trailing context after a matched term, so its sentence is not cut off.
  let left = 0, right = 0, best = 0, bestScore = -1;
  const window = new Map<string, number>();
  for (const h of hits) {
    const start = Math.max(
      0,
      Math.min(text.length - limit, h.at - Math.floor(limit / 3)),
    );
    while (
      right < hits.length &&
      hits[right].at < start + limit - Math.min(320, Math.floor(limit / 5))
    ) {
      const w = hits[right++].word;
      window.set(w, (window.get(w) ?? 0) + 1);
    }
    while (left < right && hits[left].at < start) {
      const w = hits[left++].word;
      window.set(w, window.get(w)! - 1);
    }
    const score = [...window].reduce(
      (n, [w, count]) =>
        n + (count ? weights.get(w)! * (1 + Math.min(count - 1, 2) * 0.1) : 0),
      0,
    );
    if (score > bestScore) {
      bestScore = score;
      best = start;
    }
  }
  return text.slice(best, best + limit);
}

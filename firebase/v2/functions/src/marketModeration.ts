/**
 * Server-side text checks for Kampüs Dolabı listings and messages.
 *
 * Text is normalised before matching so that common evasions still match:
 * Turkish letters are folded to ASCII, digits and symbols used as letters are
 * mapped back ("s1g4ra"), repeated letters are collapsed ("sigaaara") and
 * letters split by spaces or punctuation are joined again ("s.i.g.a.r.a").
 */

const TURKISH_FOLD: Record<string, string> = {
  ç: "c", ğ: "g", ı: "i", ö: "o", ş: "s", ü: "u", â: "a", î: "i", û: "u",
};

const LEET: Record<string, string> = {
  "0": "o", "1": "i", "3": "e", "4": "a", "5": "s", "7": "t", "@": "a", $: "s", "!": "i", "|": "i",
};

// Turkish case and plural endings that may follow an exact term ("biralar", "rakıyı").
const SUFFIX = "(?:lar|ler|i|u|yi|yu|ya|ye|yla|yle|da|de|dan|den|ta|te|tan|ten|si|su|ci|cu|cilik|ciler|cular)*";

interface Term {
  /** Normalised term; a space marks a two-word phrase ("sahte diploma"). */
  term: string;
  /** "prefix": any word starting with it; "exact": the word itself plus Turkish endings. */
  mode: "prefix" | "exact";
}

const prefix = (term: string): Term => ({ term, mode: "prefix" });
const exact = (term: string): Term => ({ term, mode: "exact" });

/** Items whose sale is a crime or forbidden by the platform rules. Matching text is rejected. */
const BLOCKED_TERMS: Term[] = [
  // Tobacco and nicotine (4207 sayılı Kanun)
  prefix("sigara"), exact("tutun"), prefix("nargile"), prefix("vape"), prefix("iqos"), exact("puro"),
  prefix("bandrolsuz"), prefix("marlboro"), prefix("parliament"), prefix("winston"), prefix("snus"),
  // Alcohol (4250 / 4733 sayılı Kanun)
  prefix("alkol"), exact("bira"), exact("sarap"), exact("raki"), prefix("votka"), prefix("vodka"),
  prefix("viski"), prefix("whisky"), prefix("tekila"), prefix("likor"), prefix("konyak"),
  prefix("jagermeister"),
  // Drugs (TCK 188, 191)
  exact("esrar"), prefix("marihuana"), prefix("marijuana"), prefix("bonzai"), prefix("uyusturucu"),
  prefix("kokain"), exact("eroin"), prefix("ekstazi"), prefix("ecstasy"), exact("mdma"), exact("lsd"),
  prefix("metamfetamin"), prefix("kaptagon"), prefix("captagon"), exact("skunk"), exact("joint"),
  // Prescription medicines
  prefix("xanax"), prefix("rivotril"), prefix("lyrica"), prefix("ritalin"), prefix("concerta"),
  prefix("modafinil"), prefix("tramadol"), prefix("kodein"), exact("morfin"), prefix("antidepresan"),
  prefix("aderall"), prefix("adderall"),
  // Weapons (6136 sayılı Kanun)
  prefix("silah"), prefix("mermi"), prefix("fisek"), prefix("kurusiki"), prefix("sustali"),
  // Forged documents
  prefix("sahte diploma"), prefix("sahte kimlik"), prefix("sahte belge"), prefix("sahte rapor"),
];

/** Words that are innocent alone but suspicious; listings carrying them are highlighted for the admin. */
const FLAGGED_TERMS: Term[] = [
  exact("karton"), exact("kacak"), exact("tekel"), exact("ot"), exact("hap"), exact("cin"),
  exact("pod"), prefix("camel"), prefix("davidoff"), prefix("sahte"), prefix("replika"),
  exact("cakma"), prefix("muadil"), exact("kimlik"), prefix("ogrenci karti"), prefix("sinav soru"),
  prefix("odev yap"), prefix("fotokopi"), prefix("bicak"), prefix("biber gaz"), prefix("hesap sat"),
  prefix("whatsapp"), prefix("telegram"), prefix("instagram"), exact("wp"), exact("ig"),
  // Innocent in most listings ("likit fondöten", "reçeteli gözlük", "silikon tabancası", "kenevir çanta").
  exact("likit"), prefix("receteli"), prefix("tabanca"), prefix("kenevir"),
];

/** Flagged words that together describe contraband ("kaçak karton") and are blocked as a pair. */
const CONTRABAND_WORDS = new Set(["karton", "kacak", "tekel", "camel", "davidoff"]);

/** Spelled-out runs are only searched for terms at least this long, so "o t" never reads as "ot". */
const MIN_SPELLED_OUT_TERM = 4;

export function normalizeForMatching(text: string): string {
  return text
    .toLocaleLowerCase("tr-TR")
    .replace(/[çğıöşüâîû]/g, (letter) => TURKISH_FOLD[letter] ?? letter)
    .normalize("NFKD")
    .replace(/[̀-ͯ]/g, "")
    .replace(/[0134578@$!|]/g, (character) => LEET[character] ?? character)
    .replace(/([a-z])\1+/g, "$1");
}

function collapse(term: string): string {
  return term.replace(/([a-z])\1+/g, "$1");
}

/** Joins runs of one- and two-letter fragments, which is how "s i g a r a" or "si-ga-ra" splits. */
function spelledOutRuns(words: string[]): string[] {
  const runs: string[] = [];
  let current: string[] = [];
  for (const word of [...words, ""]) {
    if (word.length > 0 && word.length <= 2) {
      current.push(word);
      continue;
    }
    if (current.length >= 2) runs.push(collapse(current.join("")));
    current = [];
  }
  return runs;
}

interface Tokens {
  words: string[];
  pairs: string[];
  runs: string[];
}

function matchTerms(tokens: Tokens, terms: Term[]): string[] {
  const found = new Set<string>();
  for (const entry of terms) {
    const isPhrase = entry.term.includes(" ");
    const term = collapse(entry.term.replace(/ /g, ""));
    const pattern = entry.mode === "exact" ? new RegExp(`^${term}${SUFFIX}$`) : null;
    const candidates = isPhrase ? tokens.pairs : tokens.words;
    const hit = candidates.some((word) => pattern ? pattern.test(word) : word.startsWith(term))
      || (term.length >= MIN_SPELLED_OUT_TERM && tokens.runs.some((run) => run.includes(term)));
    if (hit) found.add(entry.term);
  }
  return [...found];
}

export interface MarketTextCheck {
  /** First forbidden term, when the text must be rejected. */
  blockedTerm: string | null;
  /** Suspicious terms worth showing to a moderator. */
  flaggedTerms: string[];
}

export function checkMarketText(...texts: string[]): MarketTextCheck {
  const words = normalizeForMatching(texts.join(" \n ")).split(/[^a-z]+/).filter(Boolean);
  const tokens: Tokens = {
    words,
    pairs: words.slice(0, -1).map((word, index) => collapse(word + words[index + 1])),
    runs: spelledOutRuns(words),
  };
  const blocked = matchTerms(tokens, BLOCKED_TERMS);
  const flagged = matchTerms(tokens, FLAGGED_TERMS);
  const contraband = flagged.filter((term) => CONTRABAND_WORDS.has(term));
  return {
    blockedTerm: blocked[0] ?? (contraband.length >= 2 ? contraband.join(" + ") : null),
    flaggedTerms: flagged,
  };
}

/**
 * Listings are public to every student, so phone numbers stay in private
 * messages. Detects Turkish mobile numbers written with any separators.
 */
export function containsPhoneNumber(text: string): boolean {
  const digitsOnly = text.replace(/[\s().\-_/+]+/g, "");
  return /(?:90|0)?5\d{9}/.test(digitsOnly) || /\d{10,}/.test(digitsOnly);
}

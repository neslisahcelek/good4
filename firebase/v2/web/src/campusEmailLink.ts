const actionHost = "good4tr-v2.firebaseapp.com";
const hosts = new Set([actionHost, "good4tr-v2.web.app"]);
const fields = ["link", "mode", "oobCode", "apiKey", "continueUrl", "requestId"];

/** Parse in memory only. A page carrying just a request ID is not an Auth proof. */
export function copyableCampusEmailLink(raw: string): string | null {
  try {
    if (raw.length > 16_384) return null;
    const trusted = (url: URL) => url.protocol === "https:" && hosts.has(url.hostname)
      && (!url.port || url.port === "443") && !url.username && !url.password
      && fields.every((field) => url.searchParams.getAll(field).length <= 1);
    let action = new URL(raw);
    for (let depth = 0; depth < 3; depth++) {
      if (!trusted(action)) return null;
      const nested = action.searchParams.get("link");
      if (nested !== null) action = new URL(nested);
    }
    if (!trusted(action) || action.searchParams.has("link")) return null;
    const landing = action.pathname === "/campus-email-verification";
    if ((!landing && (action.hostname !== actionHost || !["/__/auth/action", "/__/auth/links"].includes(action.pathname)))
      || action.searchParams.get("mode") !== "signIn" || !action.searchParams.get("oobCode")?.trim()
      || !action.searchParams.get("apiKey")?.trim()) return null;
    const continueValue = action.searchParams.get("continueUrl");
    const continuation = continueValue === null ? null : new URL(continueValue);
    if (continuation && (!trusted(continuation) || continuation.pathname !== "/campus-email-verification")) return null;
    if (!landing && !continuation) return null;
    const requestId = continuation?.searchParams.get("requestId") ?? (landing ? action.searchParams.get("requestId") : null);
    if (!requestId || !/^[a-f0-9]{64}$/.test(requestId)) return null;
    if (action.searchParams.has("requestId") && action.searchParams.get("requestId") !== requestId) return null;
    if (landing) {
      action.hostname = actionHost;
      action.pathname = "/__/auth/action";
      action.searchParams.delete("requestId");
      action.searchParams.set("continueUrl", continuation?.toString()
        ?? `https://${actionHost}/campus-email-verification?requestId=${requestId}`);
    }
    return action.toString();
  } catch {
    return null;
  }
}

export type CampusEmailBrowserProof = { requestId: string; oobCode: string };

export function campusEmailBrowserProof(raw: string): CampusEmailBrowserProof | null {
  const normalized = copyableCampusEmailLink(raw);
  if (!normalized) return null;
  const action = new URL(normalized);
  const continuation = new URL(action.searchParams.get("continueUrl")!);
  const oobCode = action.searchParams.get("oobCode")!;
  if (!/^[A-Za-z0-9_-]{1,2048}$/.test(oobCode)) return null;
  return { requestId: continuation.searchParams.get("requestId")!, oobCode };
}

/** One task per page opening, including React's repeated development effects.
 * Success is possible only after a successful response from the proof verifier. */
export function createCampusEmailVerificationTask(
  raw: string,
  finish: (proof: CampusEmailBrowserProof) => Promise<{ outcome?: unknown }>,
): () => Promise<void> {
  const proof = campusEmailBrowserProof(raw);
  let task: Promise<void> | undefined;
  return () => task ??= (async () => {
    if (!proof) throw new Error("CAMPUS_EMAIL_PROOF_INVALID");
    const response = await finish(proof);
    if (response.outcome !== "verified") throw new Error("CAMPUS_EMAIL_PROOF_INVALID");
  })();
}

export function campusEmailBrowserError(error: unknown): string {
  const message = error instanceof Error ? error.message : "";
  const code = error && typeof error === "object" && "code" in error ? error.code : "";
  if (message === "CAMPUS_EMAIL_LINK_EXPIRED" || code === "functions/deadline-exceeded") {
    return "Bağlantının süresi doldu. Good4'tan yeni bir doğrulama e-postası iste.";
  }
  if (message === "CAMPUS_EMAIL_TEMPORARILY_UNAVAILABLE" || code === "functions/unavailable"
    || code === "functions/internal" || code === "functions/not-found") {
    return "Şu an doğrulanamadı. Biraz sonra tekrar dene.";
  }
  return "Bağlantı geçersiz veya kullanılmış. Good4'tan yeni bir doğrulama e-postası iste.";
}

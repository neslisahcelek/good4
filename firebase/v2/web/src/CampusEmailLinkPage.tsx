import { useEffect, useState } from "react";
import { getApp, getApps, initializeApp } from "firebase/app";
import { connectFunctionsEmulator, getFunctions, httpsCallable } from "firebase/functions";
import "./firebase";
import { campusEmailBrowserError, campusEmailBrowserProof, createCampusEmailVerificationTask, type CampusEmailBrowserProof } from "./campusEmailLink";

async function finish(proof: CampusEmailBrowserProof): Promise<{ outcome?: unknown }> {
  // No school/admin login on this page. Keep the panel's existing Auth session
  // separate and receive only a verification result from the server.
  const name = "campus-email-browser";
  const existing = getApps().find((app) => app.name === name);
  const functions = getFunctions(existing ?? initializeApp(getApp().options, name), "europe-west1");
  if (!existing && import.meta.env.VITE_USE_FIREBASE_EMULATORS === "true") {
    connectFunctionsEmulator(functions, "127.0.0.1", 5105);
  }
  const call = httpsCallable<CampusEmailBrowserProof, { outcome?: unknown }>(functions, "completeCampusEmailVerificationFromBrowser");
  return (await call(proof)).data;
}

export default function CampusEmailLinkPage() {
  const [task] = useState(() => createCampusEmailVerificationTask(window.location.href, finish));
  // Nothing is sent until the student presses "Onayla": link scanners and a
  // student who did not ask for this e-mail cannot confirm it just by opening it.
  const [status, setStatus] = useState<"ready" | "checking" | "verified" | "error">(
    () => campusEmailBrowserProof(window.location.href) ? "ready" : "error");
  const [error, setError] = useState(() => campusEmailBrowserProof(window.location.href)
    ? "" : campusEmailBrowserError(new Error("CAMPUS_EMAIL_PROOF_INVALID")));

  useEffect(() => {
    document.title = "Okul e-postası doğrulaması | Good4";
    // Capture proof once in memory, then remove codes from the address bar and
    // future referrers. No clipboard, browser storage or URL values are logged.
    window.history.replaceState(null, "", window.location.pathname);
  }, []);

  function confirm() {
    if (status !== "ready") return;
    setStatus("checking");
    void task().then(() => setStatus("verified")).catch((reason: unknown) => {
      setError(campusEmailBrowserError(reason));
      setStatus("error");
    });
  }

  return (
    <main className="campus-link-page">
      <section className="campus-link-card" aria-live="polite">
        <img src="/good4-logo.png" alt="Good4" className="campus-link-logo" />
        {status === "verified" ? <>
          <div className="campus-link-check" aria-hidden="true">✓</div>
          <h1>E-postan onaylandı</h1>
          <p>Good4 uygulamasına geri dönebilirsin.</p>
        </> : status === "error" ? <>
          <h1>E-posta doğrulanamadı</h1>
          <p role="alert">{error}</p>
        </> : <>
          <h1>Okul e-postanı onayla</h1>
          <p>Kampüs Dolabı için okul e-postanı doğrulamak üzere aşağıdaki düğmeye bas.</p>
          <button type="button" className="campus-link-button" onClick={confirm} disabled={status === "checking"}>
            {status === "checking" ? "Onaylanıyor…" : "Onayla"}
          </button>
          <p className="campus-link-note">Bu isteği sen yapmadıysan onaylama, sayfayı kapat.</p>
        </>}
      </section>
    </main>
  );
}

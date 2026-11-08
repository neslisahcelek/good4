import { useEffect, useState } from "react";
import { APP_STORE_URL, GOOGLE_PLAY_URL, AppleIcon, PlayIcon } from "./DownloadPage";
import "./QuizJoinBridgePage.css";

export default function QuizJoinBridgePage() {
  const [sessionId, setSessionId] = useState<string>("");

  useEffect(() => {
    document.title = "Good4 Canlı Quize Katıl | Good4";

    const params = new URLSearchParams(window.location.search);
    const idFromParam = params.get("session") || params.get("sessionId");
    const pathParts = window.location.pathname.replace(/^\/quiz\/?/, "").split("/");
    const idFromPath = pathParts[0] || "";
    const activeId = idFromParam || idFromPath;

    if (activeId) {
      setSessionId(activeId);
      // Try to open the mobile app deep link automatically
      const appUrl = `good4://quiz?session=${encodeURIComponent(activeId)}`;
      window.location.href = appUrl;
    }
  }, []);

  const appDeepLink = sessionId ? `good4://quiz?session=${encodeURIComponent(sessionId)}` : "good4://";

  return (
    <main className="quiz-bridge-page">
      <div className="quiz-bridge-page__glow" aria-hidden="true" />
      <div className="quiz-bridge-card">
        <div className="quiz-bridge-logo">
          <img src="/good4-logo.png" alt="Good4" />
        </div>

        <div className="quiz-bridge-badge">
          <span className="quiz-bridge-badge__pulse" aria-hidden="true" />
          <span>CANLI QUİZ</span>
        </div>

        <h1 className="quiz-bridge-title">Yarışmaya Katılın</h1>
        <p className="quiz-bridge-subtitle">
          Canlı quiz oturumuna bağlanmak için aşağıdaki butona dokunarak Good4 uygulamasını açın.
        </p>

        <a className="quiz-bridge-open-btn" href={appDeepLink}>
          Good4 Uygulamasında Aç ⚡
        </a>

        <div className="quiz-bridge-hint">
          <strong>💡 Alternatif Katılım:</strong>
          <br />
          Good4 uygulamasını açtıktan sonra ilgili Topluluk sayfasına gidip ekrandaki yeşil <strong>“Canlı Quiz Başladı”</strong> kartına dokunarak da doğrudan katılabilirsiniz.
        </div>

        <div className="quiz-bridge-divider">
          <span>Uygulama telefonunuzda yüklü değil mi?</span>
        </div>

        <div className="quiz-bridge-stores">
          <a className="quiz-bridge-store-btn" href={APP_STORE_URL}>
            <AppleIcon />
            <span>App Store</span>
          </a>
          <a className="quiz-bridge-store-btn" href={GOOGLE_PLAY_URL}>
            <PlayIcon />
            <span>Google Play</span>
          </a>
        </div>
      </div>
    </main>
  );
}

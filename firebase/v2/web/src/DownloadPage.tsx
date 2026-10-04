import { useEffect } from "react";
import "./DownloadPage.css";

export const APP_STORE_URL = "https://apps.apple.com/tr/app/good4/id6762288474?l=tr";
export const GOOGLE_PLAY_URL = "https://play.google.com/store/apps/details?id=com.good4&pcampaignid=web_share";

export function AppleIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="currentColor" aria-hidden="true">
      <path d="M17.05 12.54c.03 2.04 1.8 2.72 1.82 2.73-.02.05-.28.98-.94 1.94-.57.83-1.16 1.65-2.1 1.67-.92.02-1.22-.54-2.28-.54-1.06 0-1.39.52-2.26.56-.91.03-1.6-.9-2.17-1.72-1.18-1.69-2.08-4.78-.87-6.86a3.37 3.37 0 0 1 2.84-1.73c.89-.02 1.72.61 2.27.61.54 0 1.56-.76 2.63-.65.45.02 1.72.18 2.53 1.38-.07.04-1.51.88-1.47 2.61ZM15.4 7.41c.48-.58.8-1.39.71-2.21-.7.03-1.55.47-2.05 1.05-.45.52-.84 1.34-.74 2.13.78.06 1.58-.4 2.08-.97Z" />
    </svg>
  );
}

export function PlayIcon() {
  return (
    <svg viewBox="0 0 24 24" aria-hidden="true">
      <path fill="#4285F4" d="M3.6 2.4 13.9 12 3.6 21.6c-.3-.3-.5-.8-.5-1.4V3.8c0-.6.2-1.1.5-1.4Z" />
      <path fill="#34A853" d="m3.6 2.4 12.7 7.3-2.4 2.3L3.6 2.4Z" />
      <path fill="#FBBC04" d="m13.9 12 2.4 2.3-12.7 7.3L13.9 12Z" />
      <path fill="#EA4335" d="m16.3 9.7 3.7 2.1c.7.4.7.8 0 1.2l-3.7 2.1-2.4-3.1 2.4-2.3Z" />
    </svg>
  );
}

export default function DownloadPage() {
  useEffect(() => {
    document.title = "Good4'u indir | Üniversite öğrencileri için";
    const description = document.querySelector<HTMLMetaElement>('meta[name="description"]');
    if (description) {
      description.content = "Good4'u App Store veya Google Play'den indir. Ders programı, yemek menüsü, topluluklar ve kampüs haritası bir arada.";
    }
  }, []);

  return (
    <main className="download-page">
      <div className="download-page__glow" aria-hidden="true" />
      <div className="download-page__content">
        <div className="download-page__logo">
          <img src="/good4-logo.png" alt="Good4" />
        </div>
        <p className="download-page__eyebrow">ÜNİVERSİTE ÖĞRENCİLERİ İÇİN</p>
        <h1>Kampüs hayatın<br /><span>tek uygulamada.</span></h1>
        <p className="download-page__intro">Ders programı, günün menüsü, topluluklar ve kampüs haritası cebinde. Good4'u kullandığın mağazadan indir.</p>
        <div className="download-page__stores" aria-label="Uygulama mağazaları">
          <a className="download-page__store" href={APP_STORE_URL}>
            <AppleIcon />
            <span><small>iPhone için</small><strong>App Store'dan indir</strong></span>
            <span className="download-page__arrow" aria-hidden="true">↗</span>
          </a>
          <a className="download-page__store" href={GOOGLE_PLAY_URL}>
            <PlayIcon />
            <span><small>Android için</small><strong>Google Play'den indir</strong></span>
            <span className="download-page__arrow" aria-hidden="true">↗</span>
          </a>
        </div>
        <p className="download-page__note">Telefonuna uygun mağazayı seçerek devam et.</p>
      </div>
    </main>
  );
}

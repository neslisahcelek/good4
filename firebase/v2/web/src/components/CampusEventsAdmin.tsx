import { useCallback, useEffect, useRef, useState } from "react";
import { httpsCallable } from "firebase/functions";
import { functions } from "../firebase";

type CampusEvent = {
  id: string;
  title: string;
  communityName: string;
  location: string;
  startsAtMs: number;
  categoryId: string;
  sourceImageUrl: string;
  posterUrl: string;
};
type Listing = { events: CampusEvent[]; lastRunAtMs: number | null };

const listCampusEvents = httpsCallable<void, Listing>(functions, "listCampusEvents");
const setCampusEventPoster = httpsCallable<{ eventId: string; base64: string; contentType: string }, { posterUrl: string }>(functions, "setCampusEventPoster");
const clearCampusEventPoster = httpsCallable<{ eventId: string }, { eventId: string }>(functions, "clearCampusEventPoster");
const runCampusEventsImport = httpsCallable<void, { imported: number; removed: number }>(functions, "runCampusEventsImport");

const CATEGORY_LABELS: Record<string, string> = {
  "sports-nature": "Spor ve Doğa",
  "culture-arts": "Kültür ve Sanat",
  "social-entertainment": "Sosyal ve Eğlence",
  "academic-science": "Akademik ve Bilim",
  "career-entrepreneurship": "Kariyer ve Girişimcilik",
  technology: "Teknoloji",
  volunteering: "Gönüllülük",
  other: "Diğer",
};
const ACCEPTED_TYPES = ["image/jpeg", "image/png", "image/webp"];

function formatWhen(ms: number): string {
  return new Intl.DateTimeFormat("tr-TR", {
    weekday: "short", day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", timeZone: "Europe/Istanbul",
  }).format(new Date(ms));
}

function readFileBase64(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader();
    reader.onload = () => {
      const value = typeof reader.result === "string" ? reader.result : "";
      const separator = value.indexOf(",");
      if (separator < 0) reject(new Error("Görsel okunamadı."));
      else resolve(value.slice(separator + 1));
    };
    reader.onerror = () => reject(new Error("Görsel okunamadı."));
    reader.readAsDataURL(file);
  });
}

/**
 * Events imported from etkinlik.akdeniz.edu.tr. The app shows the admin's design when there is one, otherwise the
 * site's poster, otherwise its own line poster; imports never overwrite a design uploaded here.
 */
export function CampusEventsAdmin() {
  const [listing, setListing] = useState<Listing | null>(null);
  const [loading, setLoading] = useState(true);
  const [busyId, setBusyId] = useState<string | null>(null);
  const [syncing, setSyncing] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const fileInput = useRef<HTMLInputElement>(null);
  const [targetId, setTargetId] = useState<string | null>(null);

  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      setListing((await listCampusEvents()).data);
    } catch {
      setError("Etkinlikler yüklenemedi. Tekrar deneyin.");
    } finally {
      setLoading(false);
    }
  }, []);
  useEffect(() => { void load(); }, [load]);

  function chooseDesign(eventId: string) {
    setTargetId(eventId);
    fileInput.current?.click();
  }

  async function uploadDesign(file: File | undefined) {
    const eventId = targetId;
    if (fileInput.current) fileInput.current.value = "";
    if (!file || !eventId) return;
    setError(""); setNotice("");
    if (!ACCEPTED_TYPES.includes(file.type)) { setError("Yalnızca JPG, PNG veya WebP yükleyin."); return; }
    if (file.size > 5 * 1024 * 1024) { setError("Tasarım en fazla 5 MB olabilir."); return; }
    setBusyId(eventId);
    try {
      await setCampusEventPoster({ eventId, base64: await readFileBase64(file), contentType: file.type });
      setNotice("Yeni tasarım yayında. Uygulamada birkaç dakika içinde görünür.");
      await load();
    } catch {
      setError("Tasarım yüklenemedi. Dosyayı kontrol edip tekrar deneyin.");
    } finally {
      setBusyId(null);
    }
  }

  async function removeDesign(eventId: string) {
    setBusyId(eventId); setError(""); setNotice("");
    try {
      await clearCampusEventPoster({ eventId });
      setNotice("Tasarım kaldırıldı; sitedeki afiş ya da otomatik afiş gösterilecek.");
      await load();
    } catch {
      setError("Tasarım kaldırılamadı. Tekrar deneyin.");
    } finally {
      setBusyId(null);
    }
  }

  async function syncNow() {
    setSyncing(true); setError(""); setNotice("");
    try {
      const result = await runCampusEventsImport();
      setNotice(`Güncellendi: ${result.data.imported} etkinlik${result.data.removed ? `, ${result.data.removed} kaldırıldı` : ""}.`);
      await load();
    } catch {
      setError("Site şu anda okunamadı. Biraz sonra tekrar deneyin.");
    } finally {
      setSyncing(false);
    }
  }

  const events = listing?.events ?? [];
  return <>
    {error && <div className="inline-message inline-message--error admin-message" role="alert">{error}</div>}
    {notice && <div className="inline-message inline-message--success admin-message" role="status">{notice}</div>}
    <input ref={fileInput} type="file" accept={ACCEPTED_TYPES.join(",")} hidden onChange={(event) => void uploadDesign(event.target.files?.[0])} />
    <section className="admin-section" aria-labelledby="campus-events-title">
      <div className="section-title-row">
        <div>
          <h2 id="campus-events-title">Kampüs Etkinlikleri</h2>
          <p>
            etkinlik.akdeniz.edu.tr'den 3 saatte bir otomatik alınır.
            {listing?.lastRunAtMs ? ` Son güncelleme: ${formatWhen(listing.lastRunAtMs)}.` : ""}
            {" "}Bir etkinliğe kendi tasarımınızı yüklediğinizde uygulamada o gösterilir.
            {" "}En iyi sonuç için 1080×1350 (4:5) hazırlayın; listede sol alt köşeye saat yazıldığı için orayı boş bırakın.
          </p>
        </div>
        <button className="quiet-button" onClick={() => void syncNow()} disabled={syncing || loading}>
          {syncing ? "Güncelleniyor…" : "Şimdi güncelle"}
        </button>
      </div>
      {loading && !listing ? <div className="admin-card admin-loading"><span className="spinner spinner--green" /><p>Etkinlikler yükleniyor…</p></div>
        : events.length === 0 ? <div className="admin-card empty-state"><h3>Yaklaşan etkinlik yok</h3><p>Sitede yeni etkinlik yayınlandığında burada listelenir.</p></div>
        : <div className="coupon-review-list">{events.map((event) => {
          const shown = event.posterUrl || event.sourceImageUrl;
          return <article className="admin-card coupon-review-card" key={event.id}>
            <div className="coupon-review-main" style={{ display: "flex", gap: 16, alignItems: "flex-start" }}>
              <div style={{ width: 96, flex: "0 0 96px", aspectRatio: "4 / 5", borderRadius: 12, overflow: "hidden", background: "#F2F1ED", display: "flex", alignItems: "center", justifyContent: "center", textAlign: "center" }}>
                {shown
                  ? <img src={shown} alt={`${event.title} afişi`} style={{ width: "100%", height: "100%", objectFit: "cover" }} />
                  : <small style={{ padding: 8 }}>Otomatik çizgi afiş</small>}
              </div>
              <div style={{ minWidth: 0 }}>
                <div className="coupon-tags">
                  <span>{CATEGORY_LABELS[event.categoryId] ?? "Diğer"}</span>
                  <span>{formatWhen(event.startsAtMs)}</span>
                  <span>{event.posterUrl ? "Özel tasarım" : event.sourceImageUrl ? "Sitedeki afiş" : "Otomatik afiş"}</span>
                </div>
                <h3 style={{ margin: "6px 0 2px" }}>{event.title}</h3>
                <p style={{ margin: 0 }}>{event.communityName}{event.location ? ` · ${event.location}` : ""}</p>
              </div>
            </div>
            <div className="review-actions" style={{ flexWrap: "nowrap", whiteSpace: "nowrap" }}>
              {event.posterUrl && <button className="quiet-button" disabled={busyId !== null} onClick={() => void removeDesign(event.id)}>Tasarımı kaldır</button>}
              <button className="primary-button" disabled={busyId !== null} onClick={() => chooseDesign(event.id)}>
                {busyId === event.id ? "Yükleniyor…" : event.posterUrl ? "Tasarımı değiştir" : "Tasarım yükle"}
              </button>
            </div>
          </article>;
        })}</div>}
    </section>
  </>;
}

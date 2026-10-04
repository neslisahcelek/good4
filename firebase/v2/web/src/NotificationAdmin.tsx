import { useCallback, useEffect, useRef, useState, type FormEvent } from "react";
import { httpsCallable } from "firebase/functions";
import { auth, functions, getFirestoreDb } from "./firebase";

type Community = { id: string; name: string };
type Draft = { title: string; body: string; audience: "all" | "followers"; organizationId: string; eventId: string };
type Preview = { audienceCount: number; enabled: boolean };
type History = { id: string; title: string; kind: string; audience: string; actorUid: string; status: string; createdAt: string | null; accepted: number; failed: number; users: number };
const previewAnnouncement = httpsCallable<Draft, Preview>(functions, "previewAnnouncement");
const sendAnnouncement = httpsCallable<Draft & { requestId: string; test: boolean }, { jobId: string }>(functions, "sendAnnouncement");
const resumeJob = httpsCallable<{ jobId: string }, { resumed: boolean }>(functions, "resumeNotificationJob");
const setCostControl = httpsCallable<{ manualPaused: boolean }, { optionalJobsAllowed: boolean }>(functions, "setCostControl");
const getHistory = httpsCallable<void, { jobs: History[] }>(functions, "getNotificationHistory");

export function NotificationAdmin({ uid, communities }: { uid: string; communities: Community[] }) {
  const [draft, setDraft] = useState<Draft>({ title: "", body: "", audience: "all", organizationId: "", eventId: "" });
  const [preview, setPreview] = useState<Preview | null>(null);
  const [events, setEvents] = useState<{ id: string; title: string; organizationId: string }[]>([]);
  const [history, setHistory] = useState<History[]>([]);
  const [busy, setBusy] = useState(false);
  const [message, setMessage] = useState("");
  const [error, setError] = useState("");
  const inFlight = useRef(false);
  const mounted = useRef(true);
  const generation = useRef(0);
  // Keep the same id across ambiguous network failures and remounts: retrying must not broadcast twice.
  const requestKey = `good4-announcement-${uid}`;
  const requestRef = useRef<{ fingerprint: string; id: string } | null>(null);
  const current = useCallback(() => mounted.current && auth.currentUser?.uid === uid, [uid]);
  const refreshHistory = useCallback(async () => {
    try { const response = await getHistory(); if (current()) setHistory(response.data.jobs); }
    catch { if (current()) setError("Gönderim geçmişi yüklenemedi."); }
  }, [current]);
  useEffect(() => {
    mounted.current = true;
    void refreshHistory();
    void (async () => {
      try {
        const [{ collection, getDocs, query, where, limit }, database] = await Promise.all([import("firebase/firestore/lite"), getFirestoreDb()]);
        const snapshot = await getDocs(query(collection(database, "events"), where("status", "==", "published"), limit(100)));
        if (current()) setEvents(snapshot.docs.map((doc) => ({ id: doc.id, title: String(doc.data().title ?? ""), organizationId: String(doc.data().organizationId ?? "") })));
      } catch { if (current()) setError("Etkinlik seçenekleri yüklenemedi."); }
    })();
    return () => { mounted.current = false; generation.current++; };
  }, [current, refreshHistory]);
  function change(value: Partial<Draft>) {
    generation.current++;
    setDraft((old) => ({ ...old, ...value }));
    setPreview(null); setMessage(""); setError("");
  }
  async function previewDraft(event: FormEvent) {
    event.preventDefault();
    if (inFlight.current) return;
    const version = generation.current;
    inFlight.current = true; setBusy(true); setError("");
    try { const response = await previewAnnouncement(draft); if (current() && generation.current === version) setPreview(response.data); }
    catch { if (current()) setError("Önizleme hazırlanamadı. Başlık, mesaj ve hedef kitleyi kontrol et."); }
    finally { inFlight.current = false; if (current()) setBusy(false); }
  }
  async function send(test: boolean) {
    if (inFlight.current || !preview?.enabled || !current()) return;
    inFlight.current = true; setBusy(true); setError("");
    const fingerprint = JSON.stringify({ ...draft, test });
    let persisted: { fingerprint: string; id: string } | null = requestRef.current;
    try { persisted ??= JSON.parse(sessionStorage.getItem(requestKey) ?? "null"); } catch { /* Storage may be unavailable. */ }
    if (!persisted || persisted.fingerprint !== fingerprint) persisted = { fingerprint, id: crypto.randomUUID() };
    requestRef.current = persisted;
    try { sessionStorage.setItem(requestKey, JSON.stringify(persisted)); } catch { /* In-memory id still protects double clicks. */ }
    try {
      await sendAnnouncement({ ...draft, requestId: persisted.id, test });
      if (current()) {
        setMessage(test ? "Test bildirimi kendi mobil hesabına kuyruğa alındı. Bu hesapla telefonda giriş yapmış ve bildirim izni vermiş olmalısın." : "Duyuru kuyruğa alındı. Sonuçları gönderim geçmişinden takip edebilirsin.");
        setPreview(null);
        // Retain request id for this exact content; a retry or accidental second click returns the same job.
        await refreshHistory();
      }
    } catch { if (current()) setError("Gönderim doğrulanamadı. Aynı içerikle tekrar deneyebilirsin; aynı istek ikinci gönderim oluşturmaz."); }
    finally { inFlight.current = false; if (current()) setBusy(false); }
  }
  const choices = draft.audience === "followers" ? events.filter((event) => event.organizationId === draft.organizationId) : events;
  return <section className="admin-section" aria-labelledby="notifications-title">
    <div className="section-title-row"><div><h2 id="notifications-title">Bildirimler</h2><p>Öğrencilerin izin ve kategori tercihleri gönderimde dikkate alınır.</p></div></div>
    <form className="admin-card" onSubmit={(event) => void previewDraft(event)}>
      <fieldset disabled={busy} className="notification-form-fields">
        <label>Başlık<input required maxLength={120} value={draft.title} onChange={(event) => change({ title: event.target.value })} /></label>
        <label>Mesaj<textarea required maxLength={2000} rows={5} value={draft.body} onChange={(event) => change({ body: event.target.value })} /></label>
        <label>Hedef kitle<select value={draft.audience} onChange={(event) => change({ audience: event.target.value as Draft["audience"], organizationId: "", eventId: "" })}><option value="all">Tüm aktif mobil hesaplar</option><option value="followers">Topluluk takipçileri</option></select></label>
        {draft.audience === "followers" ? <label>Topluluk<select required value={draft.organizationId} onChange={(event) => change({ organizationId: event.target.value, eventId: "" })}><option value="">Topluluk seç</option>{communities.map((community) => <option key={community.id} value={community.id}>{community.name}</option>)}</select></label> : null}
        <label>Açılacak etkinlik (isteğe bağlı)<select value={draft.eventId} onChange={(event) => change({ eventId: event.target.value })}><option value="">Duyuru detayını aç</option>{choices.map((event) => <option key={event.id} value={event.id}>{event.title}</option>)}</select></label>
        <button type="submit" className="primary-button">Önizleme ve hedef kitle</button>
      </fieldset>
    </form>
    {preview ? <div className="admin-card" aria-live="polite"><h3>{draft.title}</h3><p className="notification-preview-body">{draft.body}</p><p>Hedef kitle: {preview.audienceCount} hesap. Telefon gönderimi yalnızca bildirim izni ve ilgili kategori açık olan cihazlara yapılır.</p>
      {!preview.enabled ? <p role="status">Bildirim gönderimi henüz etkinleştirilmedi.</p> : null}
      <div className="notification-actions"><button className="quiet-button" disabled={busy || !preview.enabled} onClick={() => void send(true)}>Kendi cihazıma test gönder</button><button className="primary-button" disabled={busy || !preview.enabled || preview.audienceCount === 0} onClick={() => void send(false)}>Duyuruyu gönder</button></div>
    </div> : null}
    {message ? <p role="status">{message}</p> : null}{error ? <p role="alert">{error}</p> : null}
    <div className="section-title-row"><ButtonCostControls /><h3>Gönderim geçmişi</h3><button className="quiet-button" disabled={busy} onClick={() => void refreshHistory()}>Yenile</button></div>
    <p>FCM kabul sayısı, kullanıcının bildirimi gördüğünü veya cihazına teslim edildiğini göstermez.</p>
    {history.length === 0 ? <p>Henüz gönderim yok.</p> : <div className="notification-history"><table><caption className="sr-only">Son 50 bildirim işi</caption><thead><tr><th>Başlık / gönderen</th><th>Hedef</th><th>Zaman</th><th>Durum</th><th>Hesap</th><th>FCM kabul</th><th>Geçersiz cihaz</th></tr></thead><tbody>{history.map((job) => <tr key={job.id}><td>{job.title}<br /><small>{job.actorUid}</small></td><td>{job.audience === "followers" ? "Takipçiler" : job.audience === "registrations" ? "Katılımcılar" : job.audience === "self" ? "Test" : "Genel"}</td><td>{job.createdAt ? new Date(job.createdAt).toLocaleString("tr-TR") : "—"}</td><td>{({ queued: "Kuyrukta", processing: "İşleniyor", complete: "Tamamlandı", cancelled: "Durduruldu", waiting: "Bekliyor", expired: "Süresi doldu" } as Record<string, string>)[job.status] ?? job.status}{job.status === "waiting" && <button type="button" onClick={() => void resumeJob({ jobId: job.id }).then(refreshHistory).catch((error) => setError(String(error)))}>Devam et</button>}</td><td>{job.users}</td><td>{job.accepted}</td><td>{job.failed}</td></tr>)}</tbody></table></div>}
  </section>;
}

function ButtonCostControls() {
  const [message, setMessage] = useState("");
  async function setPaused(manualPaused: boolean) {
    try { const response = await setCostControl({ manualPaused }); setMessage(response.data.optionalJobsAllowed ? "Ek işler açık." : "Ek işler durduruldu; bütçe eşiği de geçerli olabilir."); }
    catch { setMessage("Maliyet kontrolü güncellenemedi."); }
  }
  return <div><button type="button" onClick={() => void setPaused(true)}>Ek işleri durdur</button><button type="button" onClick={() => void setPaused(false)}>Elle durdurmayı kaldır</button><span role="status">{message}</span></div>;
}

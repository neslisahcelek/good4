import { useCallback, useEffect, useState, type ReactNode } from "react";
import { httpsCallable } from "firebase/functions";
import { functions } from "../firebase";

type MarketPhoto = { url: string; thumbUrl: string };
type MarketListing = {
  id: string;
  sellerUid: string;
  sellerName: string;
  universityName: string;
  category: string;
  condition: string;
  title: string;
  description: string;
  price: number;
  photos: MarketPhoto[];
  status: string;
  moderationFlags: string[];
  createdAt: string | null;
};
type MarketReport = {
  id: string;
  targetType: "listing" | "conversation";
  targetId: string;
  reportedUid: string;
  reason: string;
  note: string;
  createdAt: string | null;
  listing: MarketListing | null;
};
type MarketViolation = {
  id: string;
  uid: string;
  term: string;
  context: "listing" | "message";
  excerpt: string;
  suspended: boolean;
  createdAt: string | null;
};
type ModerationQueue = { pendingListings: MarketListing[]; reports: MarketReport[]; violations: MarketViolation[] };
type ReportConversation = {
  sellerUid: string | null;
  buyerUid: string | null;
  sellerName: string;
  buyerName: string;
  listingTitle: string;
  messages: { senderUid: string; type: string; text: string; createdAt: string | null }[];
};

const listMarketModerationQueue = httpsCallable<void, ModerationQueue>(functions, "listMarketModerationQueue");
const reviewMarketListing = httpsCallable<
  { listingId: string; decision: "approve" | "reject"; reason?: string },
  { status: string }
>(functions, "reviewMarketListing");
const resolveMarketReport = httpsCallable<
  { reportId: string; action: "dismiss" | "removeListing" | "suspendUser"; suspendDays?: number },
  { resolved: true }
>(functions, "resolveMarketReport");
const getMarketReportConversation = httpsCallable<{ reportId: string }, ReportConversation>(
  functions, "getMarketReportConversation");

const CATEGORY_LABELS: Record<string, string> = {
  clothing: "Kıyafet & Ayakkabı", accessories: "Çanta & Aksesuar", electronics: "Elektronik", sports: "Spor & Outdoor",
  books: "Kitap & Kırtasiye", dorm: "Yurt & Ev Eşyası", hobby: "Hobi & Müzik", other: "Diğer",
};
const CONDITION_LABELS: Record<string, string> = { new: "Yeni / etiketli", likeNew: "Yeni gibi", good: "İyi", fair: "Kullanılmış" };
const REASON_LABELS: Record<string, string> = {
  prohibited: "Yasaklı ürün", scam: "Dolandırıcılık şüphesi", harassment: "Taciz veya hakaret",
  inappropriate: "Uygunsuz içerik", other: "Diğer",
};
const REJECT_PRESETS = [
  "Yasaklı ürün",
  "Fotoğraflar ürünü net göstermiyor",
  "Açıklama yetersiz",
  "Ticari satış / toplu ürün",
  "Uygunsuz içerik",
];

function formatDate(value: string | null): string {
  if (!value) return "";
  return new Intl.DateTimeFormat("tr-TR", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit" })
    .format(new Date(value));
}

function formatPrice(price: number): string {
  return price === 0 ? "Ücretsiz" : `${price.toLocaleString("tr-TR")} ₺`;
}

function errorText(error: unknown): string {
  const message = (error as { message?: string })?.message ?? "";
  return /[a-zçğıöşü]/.test(message) ? message : `İşlem tamamlanamadı (${message || "bilinmeyen hata"}).`;
}

function ListingCard({ listing, children }: { listing: MarketListing; children?: ReactNode }) {
  return (
    <div className="coupon-review-main">
      <div className="market-photo-row">
        {listing.photos.map((photo) => (
          <a key={photo.url} href={photo.url} target="_blank" rel="noopener noreferrer">
            <img src={photo.thumbUrl || photo.url} alt="" loading="lazy" />
          </a>
        ))}
      </div>
      <div className="coupon-review-title">
        <div><h3>{listing.title}</h3><p>{formatPrice(listing.price)} · {CATEGORY_LABELS[listing.category] ?? listing.category}</p></div>
      </div>
      <div className="coupon-tags">
        <span>{listing.sellerName}</span>
        <span>{listing.universityName}</span>
        <span>{CONDITION_LABELS[listing.condition] ?? listing.condition}</span>
        {listing.createdAt && <span>{formatDate(listing.createdAt)}</span>}
      </div>
      {listing.moderationFlags.length > 0 && (
        <p className="market-flags">Dikkat: {listing.moderationFlags.join(", ")}</p>
      )}
      <p className="coupon-description">{listing.description}</p>
      {children}
    </div>
  );
}

/** Kampüs Dolabı moderation: approve or reject new listings, handle reports, see blocked attempts. */
export function CampusClosetAdmin({ onPendingCountChange }: { onPendingCountChange?: (count: number) => void }) {
  const [queue, setQueue] = useState<ModerationQueue | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [rejecting, setRejecting] = useState<string | null>(null);
  const [rejectReason, setRejectReason] = useState("");
  const [conversations, setConversations] = useState<Record<string, ReportConversation>>({});

  const load = useCallback(async () => {
    setLoading(true);
    setError(null);
    try {
      const result = await listMarketModerationQueue();
      setQueue(result.data);
      onPendingCountChange?.(result.data.pendingListings.length + result.data.reports.length);
    } catch (caught) {
      setError(errorText(caught));
    } finally {
      setLoading(false);
    }
  }, [onPendingCountChange]);

  useEffect(() => { void load(); }, [load]);

  async function run(key: string, action: () => Promise<unknown>, success: string) {
    setBusy(key);
    setError(null);
    setNotice(null);
    try {
      await action();
      setNotice(success);
      await load();
    } catch (caught) {
      setError(errorText(caught));
    } finally {
      setBusy(null);
    }
  }

  async function showConversation(reportId: string) {
    setBusy(`conversation:${reportId}`);
    try {
      const result = await getMarketReportConversation({ reportId });
      setConversations((current) => ({ ...current, [reportId]: result.data }));
    } catch (caught) {
      setError(errorText(caught));
    } finally {
      setBusy(null);
    }
  }

  return (
    <>
      {error && <div className="inline-message inline-message--error admin-message" role="alert">{error}</div>}
      {notice && <div className="inline-message inline-message--success admin-message" role="status">{notice}</div>}

      <section className="admin-section" aria-labelledby="market-pending-title">
        <div className="section-title-row">
          <div>
            <h2 id="market-pending-title">Onay bekleyen ilanlar</h2>
            <p>Öğrencilere "yaklaşık 30 dakika içinde yayına alınır" deniyor. Yasak ürün, kötü fotoğraf veya ticari satışları reddet.</p>
          </div>
          <button className="quiet-button" onClick={() => void load()} disabled={loading}>Yenile</button>
        </div>
        {loading && !queue ? (
          <div className="admin-card admin-loading"><span className="spinner spinner--green" /><p>Yükleniyor…</p></div>
        ) : !queue || queue.pendingListings.length === 0 ? (
          <div className="admin-card empty-state"><h3>Bekleyen ilan yok</h3><p>Yeni ilanlar burada görünecek.</p></div>
        ) : (
          <div className="coupon-review-list">
            {queue.pendingListings.map((listing) => {
              const key = `listing:${listing.id}`;
              return (
                <article className="admin-card coupon-review-card" key={listing.id}>
                  <ListingCard listing={listing}>
                    {rejecting === listing.id && (
                      <div className="market-reject-box">
                        <div className="coupon-tags">
                          {REJECT_PRESETS.map((preset) => (
                            <button type="button" key={preset} className="quiet-button" onClick={() => setRejectReason(preset)}>{preset}</button>
                          ))}
                        </div>
                        <input
                          value={rejectReason}
                          onChange={(event) => setRejectReason(event.target.value.slice(0, 300))}
                          placeholder="Öğrenciye gösterilecek gerekçe"
                        />
                      </div>
                    )}
                  </ListingCard>
                  <div className="review-actions">
                    {rejecting === listing.id ? (
                      <>
                        <button className="quiet-button" disabled={busy === key} onClick={() => { setRejecting(null); setRejectReason(""); }}>Vazgeç</button>
                        <button
                          className="danger-button"
                          disabled={busy === key || rejectReason.trim().length < 3}
                          onClick={() => void run(key, () => reviewMarketListing({ listingId: listing.id, decision: "reject", reason: rejectReason.trim() })
                            .then(() => { setRejecting(null); setRejectReason(""); }), "İlan reddedildi.")}
                        >Reddet</button>
                      </>
                    ) : (
                      <>
                        <button className="danger-button" disabled={busy === key} onClick={() => setRejecting(listing.id)}>Reddet</button>
                        <button
                          className="primary-button compact-button"
                          disabled={busy === key}
                          onClick={() => void run(key, () => reviewMarketListing({ listingId: listing.id, decision: "approve" }), "İlan yayına alındı.")}
                        >{busy === key ? "İşleniyor…" : "Onayla"}</button>
                      </>
                    )}
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="admin-section" aria-labelledby="market-reports-title">
        <div className="section-title-row">
          <div>
            <h2 id="market-reports-title">Şikayetler</h2>
            <p>Konuşmalar yalnızca şikayet edildiğinde okunabilir; her görüntüleme işlem geçmişine yazılır.</p>
          </div>
        </div>
        {!queue || queue.reports.length === 0 ? (
          <div className="admin-card empty-state"><h3>Açık şikayet yok</h3></div>
        ) : (
          <div className="coupon-review-list">
            {queue.reports.map((report) => {
              const key = `report:${report.id}`;
              const conversation = conversations[report.id];
              return (
                <article className="admin-card coupon-review-card" key={report.id}>
                  <div className="coupon-review-main">
                    <div className="coupon-tags">
                      <span>{report.targetType === "listing" ? "İlan şikayeti" : "Konuşma şikayeti"}</span>
                      <span>{REASON_LABELS[report.reason] ?? report.reason}</span>
                      {report.createdAt && <span>{formatDate(report.createdAt)}</span>}
                    </div>
                    {report.note && <p className="coupon-description">“{report.note}”</p>}
                    {report.listing && <ListingCard listing={report.listing} />}
                    {report.targetType === "conversation" && !conversation && (
                      <button className="quiet-button" disabled={busy === `conversation:${report.id}`} onClick={() => void showConversation(report.id)}>
                        Konuşmayı göster
                      </button>
                    )}
                    {conversation && (
                      <div className="market-conversation">
                        {conversation.messages.map((message, index) => (
                          <p key={index}>
                            <strong>{message.senderUid === conversation.sellerUid ? `${conversation.sellerName} (satıcı)` : `${conversation.buyerName} (alıcı)`}</strong>
                            {" "}<small>{formatDate(message.createdAt)}</small><br />{message.text}
                          </p>
                        ))}
                      </div>
                    )}
                  </div>
                  <div className="review-actions">
                    <button className="quiet-button" disabled={busy === key}
                      onClick={() => void run(key, () => resolveMarketReport({ reportId: report.id, action: "dismiss" }), "Şikayet kapatıldı.")}>Yok say</button>
                    {report.listing && (
                      <button className="danger-button" disabled={busy === key}
                        onClick={() => void run(key, () => resolveMarketReport({ reportId: report.id, action: "removeListing" }), "İlan kaldırıldı.")}>İlanı kaldır</button>
                    )}
                    <button className="danger-button" disabled={busy === key}
                      onClick={() => {
                        const days = Number(window.prompt("Şikayet edilen kullanıcı kaç gün Kampüs Dolabı'nı kullanamasın? (Kalıcı için 3650)", "7"));
                        if (Number.isInteger(days) && days >= 1 && days <= 3650) {
                          void run(key, () => resolveMarketReport({ reportId: report.id, action: "suspendUser", suspendDays: days }), "Kullanıcı askıya alındı.");
                        }
                      }}>Kullanıcıyı askıya al</button>
                  </div>
                </article>
              );
            })}
          </div>
        )}
      </section>

      <section className="admin-section" aria-labelledby="market-violations-title">
        <div className="section-title-row">
          <div>
            <h2 id="market-violations-title">Engellenen denemeler</h2>
            <p>Yasak kelime filtresine takılan son 50 ilan ve mesaj. 24 saatte 3 deneme hesabı 24 saat askıya alır.</p>
          </div>
        </div>
        <div className="admin-card audit-card">
          {!queue || queue.violations.length === 0 ? <div className="empty-state compact-empty"><h3>Kayıt yok</h3></div> : (
            <div className="audit-list">
              {queue.violations.map((violation) => (
                <div className="market-violation-row" key={violation.id}>
                  <div>
                    <strong>{violation.term}</strong> · {violation.context === "listing" ? "ilan" : "mesaj"}
                    {violation.suspended && " · askıya alındı"}
                    <br /><small>{violation.excerpt}</small>
                  </div>
                  <small>{formatDate(violation.createdAt)}<br />{violation.uid.slice(0, 8)}…</small>
                </div>
              ))}
            </div>
          )}
        </div>
      </section>
    </>
  );
}

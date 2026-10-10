import { FormEvent, useCallback, useEffect, useRef, useState } from "react";
import {
  GoogleAuthProvider,
  onAuthStateChanged,
  sendPasswordResetEmail,
  signInWithEmailAndPassword,
  signInWithPopup,
  signOut,
  type User,
} from "firebase/auth";
import { httpsCallable } from "firebase/functions";
import { NotificationAdmin } from "./NotificationAdmin";
import { auth, functions, getFirestoreDb } from "./firebase";
import { CampusClosetAdmin } from "./components/CampusClosetAdmin";
import { SocialActivitiesAdmin } from "./components/SocialActivitiesAdmin";
import { socialText } from "./socialStrings";
import { EVENT_CATEGORIES } from "../../functions/src/eventCategories";
import { parseKykMenuText, type KykMenuDay } from "../../functions/src/kykMenuParser";

const eventCategoryLabel = (id?: string) => EVENT_CATEGORIES.find((category) => category.id === id)?.label ?? "Kategori belirtilmemiş";
import {
  BusinessAppShell,
  Button,
  Card,
  CommunityAppShell,
  CommunityDashboardSkeleton,
  CommunityIcon,
  ConfirmationDialog,
  EmptyState,
  PageHeader,
  StatCard,
  StatusBadge,
  Table,
  type BusinessSection,
  type CommunitySection,
} from "./components/CommunityUi";

type BusinessContext = {
  organizationId: string;
  organizationName: string;
  membershipRole: "owner" | "staff";
};

type PortalContext =
  | ({ portalRole: "business" } & BusinessContext)
  | {
    portalRole: "community";
    organizationId: string;
    organizationName: string;
    university: string;
    legacyTestCommunityId: string;
    membershipRole: "manager" | "staff";
  }
  | { portalRole: "admin"; displayName: string };

type CommunityEntry = {
  id: string;
  kind: "event" | "coupon";
  title: string;
  description: string;
  date: string;
  time: string;
  location: string;
  categoryId?: string;
  businessId: string;
  businessName: string;
  discountType: "percentage" | "fixed" | "freeItem";
  discountValue: number;
  capacity: number;
  totalLimit: number;
  status: "published" | "pending" | "cancelled";
  registrationCount: number;
  attendanceCount: number;
  participants: Array<{ userId: string; displayName: string; registeredAt: string | null; checkedIn: boolean }>;
};
type CommunityDashboardData = {
  community: { name: string; university: string; description: string; logoUrl: string; coverUrl: string };
  followerCount: number;
  businesses: Array<{ id: string; name: string }>;
  entries: CommunityEntry[];
  nextCursor: string | null;
};
type EventView = "list" | "form" | "detail";
type EventDetailTab = "overview" | "participants";
type EventStatusFilter = "all" | CommunityEntry["status"];

type AdminBusiness = { id: string; name: string; legacyTestBusinessId: string };
type AdminCommunity = { id: string; name: string; university: string; legacyTestCommunityId: string };
type LegacyBusiness = { id: string; name: string; linked: boolean };
type PendingCoupon = {
  communityId: string;
  entryId: string;
  communityName: string;
  businessId: string;
  businessName: string;
  title: string;
  description: string;
  expiresOn: string;
  discountLabel: string;
  totalLimit: number | null;
};
type AuditRow = {
  id: string;
  action: string;
  actorUid: string;
  targetType: string;
  targetId: string;
  createdAt: string | null;
  metadata: Record<string, string | number | boolean | null>;
};
type AdminFeedback = {
  id: string;
  subject: string;
  message: string;
  userId: string;
  userEmail: string;
  userDisplayName: string;
  status: string;
  environment: "v2" | "legacyTest";
  createdAt: string | null;
  report: { communityId: string; entryId: string; kind: "event" | "coupon" } | null;
};
type DiningMenuDay = {
  date: string;
  dayName: string;
  meals: string[];
  calories: number | null;
};
type DiningMenu = {
  weekLabel: string;
  weekStart: string;
  weekEnd: string;
  days: DiningMenuDay[];
  updatedAt: string | null;
};
type HomeBanner = {
  imageUrl: string;
  advertiserName: string;
  targetUrl: string;
  startsOn: string;
  endsOn: string;
  active: boolean;
  updatedAt: string | null;
};
type AcademicCalendarEvent = {
  id: string;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  faculty: string;
  category: string;
  academicYear: string;
  sourcePage: number | null;
  active: boolean;
  updatedAt: string | null;
};
type AdminDashboardData = {
  nextCursors: Record<string, string | null>;
  businesses: AdminBusiness[];
  communities: AdminCommunity[];
  legacyBusinesses: LegacyBusiness[];
  pendingCoupons: PendingCoupon[];
  feedback: AdminFeedback[];
  audits: AuditRow[];
  diningMenu: DiningMenu | null;
  homeBanner: HomeBanner | null;
  homeBanners?: Array<HomeBanner | null>;
  calendarEvents: AcademicCalendarEvent[];
};

type RedeemOutcome =
  | "redeemed"
  | "not_found"
  | "wrong_business"
  | "already_redeemed"
  | "expired"
  | "campaign_inactive"
  | "stale_code"
  | "campaign_limit_reached";

type RedeemResult = {
  outcome: RedeemOutcome;
  campaignId?: string;
  campaignTitle?: string;
};

type Feedback = {
  tone: "success" | "warning" | "error";
  title: string;
  description: string;
};

type BusinessRedemption = {
  id: string;
  code: string;
  campaignId: string;
  campaignTitle: string;
  redeemedAt: Date | null;
};

const getPortalContext = httpsCallable<void, PortalContext>(functions, "getPortalContext");
const redeemCampaignCode = httpsCallable<{ code: string }, RedeemResult>(functions, "redeemCampaignCode");
const redeemLegacyTestCoupon = httpsCallable<{ code: string }, RedeemResult>(functions, "redeemLegacyTestCoupon");
const getAdminDashboard = httpsCallable<Record<string, unknown>, AdminDashboardData>(functions, "getAdminDashboard");
const reviewLegacyCoupon = httpsCallable<
  { communityId: string; entryId: string; decision: "approve" | "reject" },
  { status: "published" | "cancelled" }
>(functions, "reviewLegacyCoupon");
const moderateContentReport = httpsCallable<
  { feedbackId: string; decision: "dismiss" | "remove" },
  { status: "dismissed" | "resolved" }
>(functions, "moderateContentReport");
const saveDiningMenu = httpsCallable<{
  weekLabel: string;
  weekStart: string;
  weekEnd: string;
  days: DiningMenuDay[];
}, DiningMenu>(functions, "saveDiningMenu");
const createCampaign = httpsCallable<{
  organizationId: string; title: string; description: string; startsAt: string; endsAt: string;
  status: "published"; totalLimit: number | null;
}, { campaignId: string }>(functions, "createCampaign");
const endCampaign = httpsCallable<{ campaignId: string }, { status: "ended" }>(functions, "endCampaign");
type AdminCampaign = {
  id: string; title: string; description: string; organizationId: string; status: string;
  startsAt: number; endsAt: number; totalLimit: number | null; redemptionCount: number;
};
const saveKykMenu = httpsCallable<{ days: KykMenuDay[] }, { savedDates: string[] }>(functions, "saveKykMenu");
const saveHomeBanner = httpsCallable<{
  imageUrl: string;
  advertiserName: string;
  targetUrl: string;
  startsOn: string;
  endsOn: string;
  active: boolean;
  slot: number;
}, HomeBanner>(functions, "saveHomeBanner");
const uploadHomeBannerImage = httpsCallable<{
  base64: string;
  contentType: string;
  slot: number;
}, { imageUrl: string }>(functions, "uploadHomeBannerImage");
const saveAcademicCalendarEvent = httpsCallable<{
  eventId?: string;
  title: string;
  description: string;
  startDate: string;
  endDate: string;
  faculty: string;
  category: string;
  academicYear: string;
  sourcePage: number | null;
  active: boolean;
}, AcademicCalendarEvent>(functions, "saveAcademicCalendarEvent");
const createOrganization = httpsCallable<
  { name: string; type: "business" | "community"; university?: string; legacyTestBusinessId?: string },
  { organizationId: string }
>(functions, "createOrganization");
const assignOrganizationMemberByEmail = httpsCallable<
  { organizationId: string; email: string; displayName: string; role: "owner" | "manager" | "staff"; temporaryPassword?: string },
  { organizationId: string; userId: string; created: boolean }
>(functions, "assignOrganizationMemberByEmail");
export type CommunityApplication = {
  id: string;
  communityName: string;
  university: string;
  applicantName: string;
  applicantEmail: string;
  description: string;
  socialUrl: string;
  status: "pending" | "approved" | "rejected";
  rejectionReason: string;
  createdAt: string | null;
};
const listCommunityApplications = httpsCallable<void, { applications: CommunityApplication[] }>(functions, "listCommunityApplications");
const reviewCommunityApplication = httpsCallable<
  { applicationId: string; decision: "approve" | "reject"; reason?: string },
  { status: "approved" | "rejected"; organizationId?: string }
>(functions, "reviewCommunityApplication");
const getCommunityPortalDashboard = httpsCallable<Record<string, unknown>, CommunityDashboardData>(functions, "getCommunityPortalDashboard");
const getCommunityEventParticipants = httpsCallable<{ eventId: string; cursor?: string }, { participants: CommunityEntry["participants"]; nextCursor: string | null }>(functions, "getCommunityEventParticipants");
const saveCommunityPortalEntry = httpsCallable<{
  entryId?: string;
  kind: "event" | "coupon";
  title: string;
  description: string;
  date: string;
  time?: string;
  location?: string;
  capacity?: number;
  categoryId?: string;
  businessId?: string;
  discountType?: "percentage" | "fixed" | "freeItem";
  discountValue?: number;
  totalLimit?: number;
}, { entryId: string; status: "published" | "pending" }>(functions, "saveCommunityPortalEntry");
const cancelCommunityPortalEntry = httpsCallable<{ entryId: string }, { status: "cancelled" }>(functions, "cancelCommunityPortalEntry");

const feedbackByOutcome: Record<RedeemOutcome, Feedback> = {
  redeemed: {
    tone: "success",
    title: "Kod onaylandı",
    description: "Öğrencinin kampanya hakkı başarıyla kullanıldı.",
  },
  not_found: {
    tone: "error",
    title: "Kod bulunamadı",
    description: "Kodu kontrol edip tekrar deneyin.",
  },
  wrong_business: {
    tone: "error",
    title: "Bu kod bu işletmeye ait değil",
    description: "Öğrenciden doğru işletmenin kampanyasını açmasını isteyin.",
  },
  already_redeemed: {
    tone: "warning",
    title: "Kod daha önce kullanılmış",
    description: "Bu kampanya kodu ikinci kez kullanılamaz.",
  },
  expired: {
    tone: "warning",
    title: "Kodun süresi dolmuş",
    description: "Öğrenciden geçerli bir kampanya göstermesini isteyin.",
  },
  campaign_inactive: {
    tone: "warning",
    title: "Kampanya şu anda aktif değil",
    description: "Kampanyanın tarih veya yayın durumunu kontrol edin.",
  },
  stale_code: {
    tone: "warning",
    title: "Kod güncel değil",
    description: "Öğrenciden kuponunu uygulamada yeniden açıp orada görünen kodu göstermesini isteyin.",
  },
  campaign_limit_reached: {
    tone: "warning",
    title: "Kampanya limiti dolmuş",
    description: "Bu kampanya için yeni kod kullanımı kabul edilemiyor.",
  },
};

/** Which entrance the panel was opened from: Good4 staff and businesses, or community managers. */
export type PortalEntrance = "staff" | "community";
export const STAFF_PANEL_URL = "https://panel.good4tr.com";
export const COMMUNITY_PANEL_URL = "https://good4tr.com/topluluk-paneli";

/** Google sign-in for both entrances: communities apply and sign in at /topluluk-basvuru and /topluluk-paneli, businesses at the staff panel. */
export async function signInWithGoogle(): Promise<void> {
  const provider = new GoogleAuthProvider();
  provider.setCustomParameters({ prompt: "select_account" });
  await signInWithPopup(auth, provider);
}

export function isPopupDismissed(error: unknown): boolean {
  const code = typeof error === "object" && error && "code" in error ? String(error.code) : "";
  return code === "auth/popup-closed-by-user" || code === "auth/cancelled-popup-request";
}

export function authErrorMessage(error: unknown): string {
  const code = typeof error === "object" && error && "code" in error
    ? String(error.code)
    : "";
  if (code === "auth/popup-blocked") {
    return "Tarayıcı Google penceresini engelledi. Açılır pencerelere izin verip tekrar deneyin.";
  }
  if (["auth/invalid-credential", "auth/user-not-found", "auth/wrong-password"].includes(code)) {
    return "E-posta veya şifre hatalı.";
  }
  if (code === "auth/too-many-requests") {
    return "Çok fazla deneme yapıldı. Birkaç dakika sonra tekrar deneyin.";
  }
  if (code === "auth/network-request-failed") {
    return "İnternet bağlantınızı kontrol edin.";
  }
  return "Giriş yapılamadı. Lütfen tekrar deneyin.";
}

export function functionErrorMessage(error: unknown): string {
  const code = typeof error === "object" && error && "code" in error
    ? String(error.code)
    : "";
  const message = error instanceof Error ? error.message : "";
  if (code.includes("unauthenticated")) {
    return "Oturumunuz sona erdi. Yeniden giriş yapın.";
  }
  if (code.includes("permission-denied") || message.includes("ROLE_NOT_ALLOWED")) {
    return "Bu hesap işletme panelini kullanmaya yetkili değil.";
  }
  if (message.includes("BUSINESS_MEMBERSHIP_NOT_FOUND")) {
    return "Hesabınıza bağlı aktif bir işletme bulunamadı.";
  }
  if (message.includes("LEGACY_TEST_BUSINESS_NOT_LINKED")) {
    return "Bu işletme Good4Test kuponlarına henüz bağlanmamış.";
  }
  if (message.includes("LEGACY_TEST_COMMUNITY_NOT_LINKED")) {
    return "Topluluk hesabı mobil uygulamadaki toplulukla henüz eşleştirilmemiş.";
  }
  if (message.includes("COMMUNITY_MEMBERSHIP_NOT_FOUND")) {
    return "Hesabınıza bağlı aktif bir topluluk bulunamadı.";
  }
  if (message.includes("COMMUNITY_MANAGER_REQUIRED")) {
    return "Bu işlem yalnızca topluluk yöneticisi tarafından yapılabilir.";
  }
  if (message.includes("EVENT_CATEGORY_INVALID")) {
    return "Geçerli bir etkinlik kategorisi seçin.";
  }
  if (message.includes("DISCOUNT_VALUE_INVALID")) {
    return "İndirim değerini kontrol edin. Yüzde indirimi 1–100 arasında olmalıdır.";
  }
  if (message.includes("ENTRY_NOT_EDITABLE")) {
    return "Yayından kaldırılmış içerik düzenlenemez. Yeni bir içerik oluşturun.";
  }
  if (message.includes("DATE_INVALID") || message.includes("TIME_INVALID")) {
    return "Geçerli bir tarih ve saat seçin.";
  }
  if (message.includes("LEGACY_TEST_BUSINESS_ALREADY_LINKED")) {
    return "Seçilen Good4Test işletmesi başka bir işletmeye bağlı.";
  }
  if (message.includes("TEMPORARY_PASSWORD_REQUIRED")) {
    return "Yeni hesap için en az 12 karakterli geçici şifre girin.";
  }
  if (message.includes("COUPON_ALREADY_REVIEWED")) {
    return "Bu kupon daha önce değerlendirilmiş. Liste yenileniyor.";
  }
  if (message.includes("EMAIL_INVALID")) {
    return "Geçerli bir e-posta adresi girin.";
  }
  if (message.includes("STUDENT_ACCOUNT_NOT_ALLOWED")) {
    return "Bu Google hesabı Good4 uygulamasında öğrenci hesabı olarak kullanılıyor. Topluluğun kendi Google hesabıyla başvurun.";
  }
  if (message.includes("ACCOUNT_ALREADY_HAS_ROLE")) {
    return "Bu hesap zaten bir işletme ya da topluluk paneline bağlı.";
  }
  if (message.includes("APPLICATION_ALREADY_REVIEWED")) {
    return "Bu başvuru daha önce değerlendirilmiş. Liste yenileniyor.";
  }
  if (message.includes("SOCIAL_URL_INVALID")) {
    return "Bağlantı https:// ile başlayan geçerli bir adres olmalı.";
  }
  if (message.includes("GOOGLE_SIGN_IN_REQUIRED") || message.includes("VERIFIED_EMAIL_REQUIRED")) {
    return "Başvuru için Google hesabıyla giriş yapın.";
  }
  if (code.includes("failed-precondition") || code.includes("internal")) {
    return "İşlem tamamlanamadı. Lütfen tekrar deneyin.";
  }
  return "Bağlantı kurulamadı. Sayfayı yenileyip tekrar deneyin.";
}

function normalizeCode(value: string): string {
  return value.toUpperCase().replace(/[^A-Z0-9]/g, "").slice(0, 12);
}

function formatCode(value: string): string {
  return value.length > 4 ? `${value.slice(0, 4)} ${value.slice(4)}` : value;
}

export function BrandMark({ compact = false, label = "Yönetim Paneli" }: { compact?: boolean; label?: string }) {
  return (
    <div className={`brand-mark ${compact ? "brand-mark--compact" : ""}`}>
      <img src="/good4-logo.png" alt="Good4" />
      <div>
        <span>Good4</span>
        {!compact && <small>{label}</small>}
      </div>
    </div>
  );
}

function LoginScreen({ portal }: { portal: PortalEntrance }) {
  const community = portal === "community";
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");
  const [resetSent, setResetSent] = useState(false);
  const [googleSubmitting, setGoogleSubmitting] = useState(false);

  async function handleGoogle() {
    setMessage("");
    setResetSent(false);
    setGoogleSubmitting(true);
    try {
      await signInWithGoogle();
    } catch (error) {
      if (!isPopupDismissed(error)) setMessage(authErrorMessage(error));
    } finally {
      setGoogleSubmitting(false);
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setResetSent(false);
    setSubmitting(true);
    try {
      await signInWithEmailAndPassword(auth, email.trim(), password);
    } catch (error) {
      setMessage(authErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleReset() {
    setMessage("");
    setResetSent(false);
    if (!email.trim()) {
      setMessage("Önce e-posta adresinizi yazın.");
      return;
    }
    try {
      await sendPasswordResetEmail(auth, email.trim());
      setResetSent(true);
    } catch (error) {
      setMessage(authErrorMessage(error));
    }
  }

  return (
    <main className="login-shell">
      <section className="login-form-area">
        <div className="login-card">
          <div className="login-card__brand"><BrandMark label={community ? "Topluluk Paneli" : "Yönetim Paneli"} /></div>
          <h1>{community ? "Topluluk paneline giriş" : "Yönetim paneline giriş"}</h1>
          <p className="card-intro">
            {community
              ? "Topluluğunuzun yönetici hesabıyla devam edin."
              : "Good4 tarafından tanımlanan hesabınızla devam edin."}
          </p>

          <form onSubmit={handleSubmit} noValidate>
            <label className="field-label" htmlFor="email">E-posta</label>
            <input
              id="email"
              name="email"
              type="email"
              autoComplete="email"
              value={email}
              onChange={(event) => setEmail(event.target.value)}
              placeholder="ornek@good4tr.com"
              required
            />

            <div className="label-row">
              <label className="field-label" htmlFor="password">Şifre</label>
              <button className="text-button" type="button" onClick={handleReset}>
                Şifremi unuttum
              </button>
            </div>
            <div className="password-field">
              <input
                id="password"
                name="password"
                type={showPassword ? "text" : "password"}
                autoComplete="current-password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="Şifreniz"
                required
              />
              <button
                type="button"
                onClick={() => setShowPassword((current) => !current)}
                aria-label={showPassword ? "Şifreyi gizle" : "Şifreyi göster"}
              >
                {showPassword ? "Gizle" : "Göster"}
              </button>
            </div>

            {message && <div className="inline-message inline-message--error" role="alert">{message}</div>}
            {resetSent && (
              <div className="inline-message inline-message--success" role="status">
                Şifre yenileme bağlantısı e-posta adresinize gönderildi.
              </div>
            )}

            <button className="primary-button" type="submit" disabled={submitting || !email || !password}>
              {submitting ? <><span className="spinner" /> Giriş yapılıyor</> : "Giriş yap"}
            </button>
          </form>
          <div className="auth-divider"><span>veya</span></div>
          <button className="secondary-button google-button" type="button" onClick={() => void handleGoogle()} disabled={googleSubmitting}>
            {googleSubmitting ? "Google açılıyor…" : "Google ile giriş yap"}
          </button>
          <p className="support-copy">
            {community ? (
              <>Topluluğunuz henüz Good4'da değilse <a href="/topluluk-basvuru">başvuru yapın</a>.<br />
                Good4 ekibi misiniz? <a href={STAFF_PANEL_URL}>Yönetim paneline gidin</a>.</>
            ) : (
              <>Hesap erişimi için Good4 yöneticinizle iletişime geçin.<br />
                Topluluk yöneticisi misiniz? <a href={COMMUNITY_PANEL_URL}>Topluluk paneline gidin</a>.</>
            )}
          </p>
        </div>
      </section>
    </main>
  );
}

function AccessError({ message }: { message: string }) {
  return (
    <main className="centered-page">
      <div className="access-card">
        <BrandMark compact />
        <div className="result-icon result-icon--error" aria-hidden="true">!</div>
        <h1>Panele erişilemiyor</h1>
        <p>{message}</p>
        <button className="secondary-button" onClick={() => void signOut(auth)}>Farklı hesapla giriş yap</button>
      </div>
    </main>
  );
}

/** A signed-in account that belongs to the other entrance is sent there instead of being shown its panel here. */
function WrongPortal({ portal }: { portal: PortalEntrance }) {
  const toCommunity = portal === "staff";
  return (
    <main className="centered-page">
      <div className="access-card">
        <BrandMark compact />
        <h1>{toCommunity ? "Topluluk paneline gidin" : "Bu panel topluluklar içindir"}</h1>
        <p>
          {toCommunity
            ? "Bu hesap bir topluluk yöneticisi hesabı. Topluluğunuzu topluluk panelinden yönetebilirsiniz."
            : "Bu hesap Good4 yönetim paneline aittir."}
        </p>
        <a className="primary-button" href={toCommunity ? COMMUNITY_PANEL_URL : STAFF_PANEL_URL}>
          {toCommunity ? "Topluluk paneline git" : "Yönetim paneline git"}
        </a>
        <button className="secondary-button" onClick={() => void signOut(auth)}>Farklı hesapla giriş yap</button>
      </div>
    </main>
  );
}

function firestoreDate(value: unknown): Date | null {
  if (!value || typeof value !== "object" || !("toDate" in value) || typeof value.toDate !== "function") return null;
  const date = value.toDate();
  return date instanceof Date && !Number.isNaN(date.getTime()) ? date : null;
}

function businessDateKey(value: Date): string {
  return new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" }).format(value);
}

function formatBusinessUsageDate(value: Date | null): string {
  if (!value) return "Tarih bekleniyor";
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Europe/Istanbul",
  }).format(value);
}

function BusinessUsageList({ items, compact = false }: { items: BusinessRedemption[]; compact?: boolean }) {
  if (items.length === 0) {
    return (
      <EmptyState
        title="Henüz kullanım yok"
        description="V2 kampanya kodları doğrulandığında kullanım kayıtları burada görünecek."
      />
    );
  }

  return (
    <Table label="Kupon kullanımları" className={`business-usage-table ${compact ? "is-compact" : ""}`}>
      <div className="business-usage-table__header" role="row">
        <span role="columnheader">Kampanya</span>
        <span role="columnheader">Kullanım zamanı</span>
        <span role="columnheader">Durum</span>
      </div>
      <div role="rowgroup">
        {items.map((item) => (
          <div className="business-usage-table__row" role="row" key={item.id}>
            <div role="cell" data-label="Kampanya">
              <strong>{item.campaignTitle}</strong>
              <span>Kod •••• {item.code.slice(-4)}</span>
            </div>
            <time role="cell" data-label="Kullanım zamanı" dateTime={item.redeemedAt?.toISOString()}>{formatBusinessUsageDate(item.redeemedAt)}</time>
            <div role="cell" data-label="Durum"><span className="event-state-pill is-checked">Başarılı</span></div>
          </div>
        ))}
      </div>
    </Table>
  );
}

function VerificationPanel({ user, context }: { user: User; context: BusinessContext }) {
  const [section, setSection] = useState<BusinessSection>("verify");
  const [code, setCode] = useState("");
  const [submitting, setSubmitting] = useState(false);
  const [feedback, setFeedback] = useState<Feedback | null>(null);
  const [campaignTitle, setCampaignTitle] = useState("");
  const [redemptions, setRedemptions] = useState<BusinessRedemption[]>([]);
  const [usageLoading, setUsageLoading] = useState(true);
  const [usageError, setUsageError] = useState("");
  const [usageSearch, setUsageSearch] = useState("");
  const [usagePeriod, setUsagePeriod] = useState<"all" | "today" | "month">("all");
  const codeInput = useRef<HTMLInputElement>(null);
  const normalizedCode = normalizeCode(code);

  const refreshUsage = useCallback(async (showLoader = true) => {
    if (showLoader) setUsageLoading(true);
    try {
      const [{ collection, getDocs, query, where, limit, orderBy }, database] = await Promise.all([
        import("firebase/firestore/lite"),
        getFirestoreDb(),
      ]);
      const [redemptionSnapshot, campaignSnapshot] = await Promise.all([
        getDocs(query(collection(database, "redemptions"), where("organizationId", "==", context.organizationId), orderBy("redeemedAt", "desc"), limit(50))),
        getDocs(query(collection(database, "campaigns"), where("organizationId", "==", context.organizationId), limit(100))),
      ]);
      const campaignTitles = new Map(campaignSnapshot.docs.map((document) => [
        document.id,
        String(document.data().title ?? "Kampanya"),
      ]));
      const nextRedemptions = redemptionSnapshot.docs.map((document) => {
        const data = document.data();
        const campaignId = String(data.campaignId ?? "");
        return {
          id: document.id,
          code: String(data.code ?? document.id),
          campaignId,
          campaignTitle: campaignTitles.get(campaignId) ?? "Kampanya",
          redeemedAt: firestoreDate(data.redeemedAt),
        };
      }).sort((left, right) => (right.redeemedAt?.getTime() ?? 0) - (left.redeemedAt?.getTime() ?? 0));
      setRedemptions(nextRedemptions);
      setUsageError("");
    } catch (error) {
      setUsageError(functionErrorMessage(error));
    } finally {
      if (showLoader) setUsageLoading(false);
    }
  }, [context.organizationId]);

  useEffect(() => {
    void refreshUsage();
  }, [refreshUsage]);

  async function handleVerify(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (normalizedCode.length < 6) {
      setFeedback({
        tone: "error",
        title: "Kod eksik görünüyor",
        description: "Öğrencinin gösterdiği kodun tamamını girin.",
      });
      return;
    }

    setSubmitting(true);
    setFeedback(null);
    setCampaignTitle("");
    try {
      const isLegacyCode = /^[0-9]{6}$/.test(normalizedCode);
      const response = await (isLegacyCode
        ? redeemLegacyTestCoupon({ code: normalizedCode })
        : redeemCampaignCode({ code: normalizedCode }));
      const nextFeedback = feedbackByOutcome[response.data.outcome];
      setFeedback(nextFeedback);
      setCampaignTitle(response.data.campaignTitle ?? "");
      if (response.data.outcome === "redeemed") {
        setCode("");
        if (!isLegacyCode) await refreshUsage(false);
      }
    } catch (error) {
      setFeedback({
        tone: "error",
        title: "Kod doğrulanamadı",
        description: functionErrorMessage(error),
      });
    } finally {
      setSubmitting(false);
      window.setTimeout(() => codeInput.current?.focus(), 0);
    }
  }

  function resetForm() {
    setCode("");
    setFeedback(null);
    setCampaignTitle("");
    codeInput.current?.focus();
  }

  const currentDate = new Date();
  const todayKey = businessDateKey(currentDate);
  const monthKey = todayKey.slice(0, 7);
  const todayUsage = redemptions.filter((item) => item.redeemedAt && businessDateKey(item.redeemedAt) === todayKey).length;
  const monthUsage = redemptions.filter((item) => item.redeemedAt && businessDateKey(item.redeemedAt).startsWith(monthKey)).length;
  const normalizedUsageSearch = usageSearch.trim().toLocaleLowerCase("tr-TR");
  const filteredUsage = redemptions.filter((item) => {
    const matchesSearch = !normalizedUsageSearch
      || `${item.campaignTitle} ${item.code}`.toLocaleLowerCase("tr-TR").includes(normalizedUsageSearch);
    const itemDateKey = item.redeemedAt ? businessDateKey(item.redeemedAt) : "";
    const matchesPeriod = usagePeriod === "all"
      || (usagePeriod === "today" && itemDateKey === todayKey)
      || (usagePeriod === "month" && itemDateKey.startsWith(monthKey));
    return matchesSearch && matchesPeriod;
  });

  return (
    <BusinessAppShell
      businessName={context.organizationName}
      userEmail={user.email ?? ""}
      activeSection={section}
      usageCount={redemptions.length}
      onNavigate={(nextSection) => {
        setSection(nextSection);
        window.scrollTo({ top: 0, behavior: "smooth" });
      }}
      onLogout={() => void signOut(auth)}
    >
      {section === "overview" ? (
        <div className="business-page business-overview">
          <PageHeader
            title="Genel Bakış"
            description="İşletmenin Good4 üzerindeki güncel V2 kampanya kullanımlarını hızlıca takip et."
            action={<Button variant="primary" icon="verify" onClick={() => setSection("verify")}>Kupon Doğrula</Button>}
          />
          {usageError ? <div className="inline-message inline-message--error" role="alert">{usageError}</div> : null}
          <section className="community-stat-grid business-stat-grid" aria-label="İşletme kullanım özeti">
            <StatCard label="Bugünkü kullanım" value={usageLoading ? "—" : todayUsage} detail="Bugün doğrulanan V2 kodları" />
            <StatCard label="Bu ay" value={usageLoading ? "—" : monthUsage} detail="Bu ay doğrulanan V2 kodları" />
            <StatCard label="Toplam doğrulama" value={usageLoading ? "—" : redemptions.length} detail="Kayıtlı V2 kullanımlarının tamamı" />
          </section>
          <Card className="business-recent-card">
            <div className="business-card-heading"><div><h2>Son Kullanımlar</h2><p>En son doğrulanan V2 kampanya kodları.</p></div><Button variant="ghost" onClick={() => setSection("usage")}>Tümünü gör</Button></div>
            {usageLoading ? <div className="business-list-skeleton" aria-label="Kullanımlar yükleniyor" aria-busy="true"><span /><span /><span /></div> : <BusinessUsageList items={redemptions.slice(0, 5)} compact />}
          </Card>
        </div>
      ) : null}

      {section === "verify" ? (
        <div className="business-page business-verify-page">
          <PageHeader title="Kupon Doğrula" description="Öğrencinin kampanya kodunu girerek doğrula." />
          <div className="business-verify-layout">
            <Card className="business-verification-card">
              <div className="business-verification-card__heading"><span aria-hidden="true"><CommunityIcon name="verify" /></span><div><h2>Kampanya kodu</h2><p>Kodu boşluklarıyla birlikte veya doğrudan yazabilirsin.</p></div></div>
              <form onSubmit={handleVerify}>
                <label className="field-label" htmlFor="campaign-code">Kampanya kodunu gir</label>
                <div className="code-input-wrap">
                  <input
                    ref={codeInput}
                    id="campaign-code"
                    name="campaign-code"
                    type="text"
                    inputMode="text"
                    autoComplete="off"
                    autoCapitalize="characters"
                    spellCheck={false}
                    value={formatCode(normalizedCode)}
                    onChange={(event) => {
                      setCode(event.target.value);
                      setFeedback(null);
                    }}
                    placeholder="123456 veya ABCD 1234"
                    aria-describedby="code-help"
                    autoFocus
                  />
                  {normalizedCode ? <button type="button" className="clear-button" onClick={resetForm} aria-label="Kodu temizle">×</button> : null}
                </div>
                <p id="code-help" className="field-help">6 haneli Good4Test veya 8 haneli V2 kodunu kullan.</p>
                <Button className="business-verify-button" variant="primary" type="submit" disabled={submitting || normalizedCode.length < 6}>
                  {submitting ? <><span className="spinner" /> Kontrol ediliyor</> : "Doğrula"}
                </Button>
              </form>

              {feedback ? (
                <div className={`business-result business-result--${feedback.tone}`} role="status" aria-live="polite">
                  <div className={`result-icon result-icon--${feedback.tone}`} aria-hidden="true">{feedback.tone === "success" ? "✓" : feedback.tone === "warning" ? "!" : "×"}</div>
                  <div><span>{feedback.tone === "success" ? "Doğrulama başarılı" : "Doğrulama sonucu"}</span><h2>{feedback.title}</h2>{campaignTitle ? <strong>{campaignTitle}</strong> : null}<p>{feedback.description}</p><Button type="button" variant="secondary" onClick={resetForm}>Yeni kod doğrula</Button></div>
                </div>
              ) : null}
            </Card>
            <aside className="business-verify-note"><strong>Hızlı doğrulama</strong><p>İşlem başarılı olduğunda kod mevcut akışta otomatik olarak kullanılmış sayılır. İkinci bir onay adımı gerekmez.</p></aside>
          </div>
        </div>
      ) : null}

      {section === "usage" ? (
        <div className="business-page">
          <PageHeader title="Kullanımlar" description="İşletmende doğrulanan V2 kampanya kodlarını incele." action={<Button variant="primary" icon="verify" onClick={() => setSection("verify")}>Kupon Doğrula</Button>} />
          {usageError ? <div className="inline-message inline-message--error" role="alert">{usageError}</div> : null}
          <Card className="business-usage-card">
            <div className="business-usage-toolbar">
              <label><span className="visually-hidden">Kullanımlarda ara</span><input type="search" value={usageSearch} onChange={(event) => setUsageSearch(event.target.value)} placeholder="Kampanya veya kod ara" /></label>
              <label><span className="visually-hidden">Tarihe göre filtrele</span><select value={usagePeriod} onChange={(event) => setUsagePeriod(event.target.value as typeof usagePeriod)}><option value="all">Tüm tarihler</option><option value="today">Bugün</option><option value="month">Bu ay</option></select></label>
            </div>
            {usageLoading ? <div className="business-list-skeleton" aria-label="Kullanımlar yükleniyor" aria-busy="true"><span /><span /><span /></div> : filteredUsage.length > 0 ? <BusinessUsageList items={filteredUsage} /> : <EmptyState title="Eşleşen kullanım bulunamadı" description="Arama metnini veya tarih filtresini değiştirerek tekrar dene." action={<Button variant="ghost" onClick={() => { setUsageSearch(""); setUsagePeriod("all"); }}>Filtreleri temizle</Button>} />}
          </Card>
        </div>
      ) : null}

      {section === "profile" ? (
        <div className="business-page business-profile-page">
          <PageHeader title="İşletme Profili" description="Good4 işletme hesabına bağlı temel bilgileri görüntüle." />
          <Card className="business-profile-card">
            <div className="business-card-heading"><div><h2>İşletme Bilgileri</h2><p>Bu bilgiler mevcut Good4 kurum kaydından gelir.</p></div></div>
            <dl>
              <div><dt>İşletme adı</dt><dd>{context.organizationName}</dd></div>
              <div><dt>Hesap e-postası</dt><dd>{user.email ?? "Belirtilmedi"}</dd></div>
              <div><dt>Panel yetkisi</dt><dd>{context.membershipRole === "owner" ? "İşletme sahibi" : "Personel"}</dd></div>
              <div><dt>Hesap durumu</dt><dd><span className="event-state-pill is-checked">Aktif</span></dd></div>
            </dl>
            <p className="business-profile-note">Profil alanlarını değiştiren mevcut bir iş akışı bulunmadığı için bu ekran salt okunurdur. Güncelleme için Good4 yöneticinle iletişime geç.</p>
          </Card>
        </div>
      ) : null}
    </BusinessAppShell>
  );
}

function formatCommunityEventDate(dateValue: string, timeValue: string): string {
  const eventDate = new Date(`${dateValue}T${timeValue || "12:00"}:00`);
  if (Number.isNaN(eventDate.getTime())) return [dateValue, timeValue].filter(Boolean).join(" · ");
  const formattedDate = new Intl.DateTimeFormat("tr-TR", {
    day: "numeric",
    month: "long",
    weekday: "short",
    timeZone: "Europe/Istanbul",
  }).format(eventDate);
  return timeValue ? `${formattedDate} · ${timeValue}` : formattedDate;
}

function formatCommunityParticipantDate(value: string | null): string {
  if (!value) return "Tarih bilgisi yok";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Tarih bilgisi yok";
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Europe/Istanbul",
  }).format(date);
}

function CommunityPanel({ user, context }: {
  user: User;
  context: Extract<PortalContext, { portalRole: "community" }>;
}) {
  const [tab, setTab] = useState<CommunitySection>("overview");
  const [data, setData] = useState<CommunityDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [busyId, setBusyId] = useState("");
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [editingId, setEditingId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [date, setDate] = useState("");
  const [time, setTime] = useState("");
  const [location, setLocation] = useState("");
  const [capacity, setCapacity] = useState("");
  const [categoryId, setCategoryId] = useState("");
  const [businessId, setBusinessId] = useState("");
  const [discountType, setDiscountType] = useState<"percentage" | "fixed" | "freeItem">("percentage");
  const [discountValue, setDiscountValue] = useState("");
  const [totalLimit, setTotalLimit] = useState("");
  const [eventView, setEventView] = useState<EventView>("list");
  const [eventDetailTab, setEventDetailTab] = useState<EventDetailTab>("overview");
  const [selectedEventId, setSelectedEventId] = useState("");
  const [eventSearch, setEventSearch] = useState("");
  const [eventStatus, setEventStatus] = useState<EventStatusFilter>("all");
  const [pendingCancellation, setPendingCancellation] = useState<CommunityEntry | null>(null);

  const dashboardRequest = useRef<{ key: string; promise: Promise<void> } | null>(null);
  const [participants, setParticipants] = useState<CommunityEntry["participants"]>([]);
  const [participantCursor, setParticipantCursor] = useState<string | null>(null);
  const [participantsLoading, setParticipantsLoading] = useState(false);
  const activeParticipantEvent = useRef("");
  const participantGeneration = useRef(0);
  const participantRequest = useRef<string | null>(null);
  async function loadParticipants(cursor?: string) {
    const eventId = selectedEventId;
    const requestKey = `${participantGeneration.current}:${eventId}:${cursor ?? ""}`;
    if (participantRequest.current === requestKey) return;
    participantRequest.current = requestKey;
    const generation = participantGeneration.current;
    setParticipantsLoading(true);
    try {
      const response = await getCommunityEventParticipants({ eventId, ...(cursor ? { cursor } : {}) });
      if (activeParticipantEvent.current !== eventId || participantGeneration.current !== generation) return;
      setParticipants((current) => cursor ? [...current, ...response.data.participants] : response.data.participants);
      setParticipantCursor(response.data.nextCursor);
    } catch (requestError) { setError(functionErrorMessage(requestError)); }
    finally { if (participantRequest.current === requestKey) { participantRequest.current = null; setParticipantsLoading(false); } }
  }
  useEffect(() => {
    participantGeneration.current++;
    participantRequest.current = null;
    activeParticipantEvent.current = selectedEventId;
    setParticipants([]); setParticipantCursor(null); setParticipantsLoading(false);
    if (eventDetailTab === "participants" && eventView === "detail" && selectedEventId) void loadParticipants();
    return () => { activeParticipantEvent.current = ""; };
  }, [selectedEventId, eventDetailTab, eventView]);
  async function refresh(showLoader = true, cursor?: string) {
    const section = tab === "coupons" ? "coupons" : "events";
    const key = `${section}:${cursor ?? ""}`;
    if (dashboardRequest.current?.key === key) return dashboardRequest.current.promise;
    const promise = (async () => {
      if (showLoader) setLoading(true);
      try {
        const response = await getCommunityPortalDashboard({ section, ...(cursor ? { cursor } : {}) });
        if (dashboardRequest.current?.key !== key) return;
        setData((current) => cursor && current ? { ...response.data, entries: [...current.entries, ...response.data.entries] } : response.data);
        setBusinessId((current) => current || response.data.businesses[0]?.id || "");
        setError("");
      } catch (requestError) { setError(functionErrorMessage(requestError)); }
      finally { if (showLoader) setLoading(false); }
    })();
    dashboardRequest.current = { key, promise };
    try { await promise; } finally { if (dashboardRequest.current?.promise === promise) dashboardRequest.current = null; }
  }
  useEffect(() => { void refresh(); }, [tab === "coupons"]);

  function resetForm() {
    setEditingId("");
    setTitle("");
    setDescription("");
    setDate("");
    setTime("");
    setLocation("");
    setCapacity("");
    setCategoryId("");
    setDiscountValue("");
    setTotalLimit("");
  }

  function editEntry(entry: CommunityEntry) {
    setTab(entry.kind === "event" ? "events" : "coupons");
    if (entry.kind === "event") setEventView("form");
    setEditingId(entry.id);
    setTitle(entry.title);
    setDescription(entry.description);
    setDate(entry.date);
    setTime(entry.time);
    setLocation(entry.location);
    setCapacity(entry.capacity ? String(entry.capacity) : "");
    setCategoryId(entry.categoryId ?? "");
    setBusinessId(entry.businessId || data?.businesses[0]?.id || "");
    setDiscountType(entry.discountType);
    setDiscountValue(entry.discountValue ? String(entry.discountValue) : "");
    setTotalLimit(entry.totalLimit ? String(entry.totalLimit) : "");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function saveEntry(event: FormEvent<HTMLFormElement>, kind: "event" | "coupon") {
    event.preventDefault();
    setSaving(true);
    setError("");
    setNotice("");
    try {
      await saveCommunityPortalEntry({
        ...(editingId ? { entryId: editingId } : {}),
        kind, title: title.trim(), description: description.trim(), date,
        ...(kind === "event" ? {
          time, location: location.trim(), capacity: Number.parseInt(capacity || "0", 10), categoryId,
        } : {
          businessId, discountType,
          discountValue: discountType === "freeItem" ? 0 : Number.parseInt(discountValue || "0", 10),
          totalLimit: Number.parseInt(totalLimit || "0", 10),
        }),
      });
      setNotice(kind === "event"
        ? `Etkinlik ${editingId ? "güncellendi" : "yayınlandı"}.`
        : `Kupon ${editingId ? "güncellendi" : "oluşturuldu"}; Good4 onayına gönderildi.`);
      resetForm();
      await refresh(false);
      if (kind === "event") setEventView("list");
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  async function performCancelEntry(entry: CommunityEntry) {
    setBusyId(entry.id);
    setError("");
    setNotice("");
    try {
      await cancelCommunityPortalEntry({ entryId: entry.id });
      setNotice("İçerik yayından kaldırıldı.");
      if (editingId === entry.id) resetForm();
      await refresh(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      setBusyId("");
    }
  }

  async function cancelEntry(entry: CommunityEntry) {
    if (!window.confirm(`“${entry.title}” içeriğini yayından kaldırmak istiyor musunuz?`)) return;
    await performCancelEntry(entry);
  }

  const events = data?.entries.filter((entry) => entry.kind === "event") ?? [];
  const coupons = data?.entries.filter((entry) => entry.kind === "coupon") ?? [];
  const activeEvents = events.filter((entry) => entry.status === "published");
  const activeCoupons = coupons.filter((entry) => entry.status === "published");
  const today = new Intl.DateTimeFormat("en-CA", { timeZone: "Europe/Istanbul" }).format(new Date());
  const upcomingEvents = activeEvents
    .filter((entry) => entry.date >= today)
    .slice()
    .sort((left, right) => `${left.date}T${left.time}`.localeCompare(`${right.date}T${right.time}`));
  const attendance = activeEvents.reduce((sum, entry) => sum + entry.attendanceCount, 0);
  const normalizedEventSearch = eventSearch.trim().toLocaleLowerCase("tr-TR");
  const filteredEvents = events.filter((entry) => {
    const matchesStatus = eventStatus === "all" || entry.status === eventStatus;
    const matchesSearch = !normalizedEventSearch
      || `${entry.title} ${entry.location}`.toLocaleLowerCase("tr-TR").includes(normalizedEventSearch);
    return matchesStatus && matchesSearch;
  });
  const selectedEvent = events.find((entry) => entry.id === selectedEventId) ?? null;
  const editingEvent = events.find((entry) => entry.id === editingId && entry.kind === "event") ?? null;

  function navigate(section: CommunitySection) {
    if (section === "events" || section === "coupons") resetForm();
    if (section === "events") setEventView("list");
    setTab(section);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function startNewEvent() {
    resetForm();
    setTab("events");
    setEventView("form");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function manageEvent(entry: CommunityEntry) {
    setSelectedEventId(entry.id);
    setEventDetailTab("overview");
    setTab("events");
    setEventView("detail");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  function closeEventForm() {
    resetForm();
    setEventView("list");
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function confirmEventCancellation() {
    if (!pendingCancellation) return;
    await performCancelEntry(pendingCancellation);
    setPendingCancellation(null);
  }

  return (
    <CommunityAppShell
      communityName={data?.community.name ?? context.organizationName}
      university={data?.community.university ?? context.university}
      logoUrl={data?.community.logoUrl ?? ""}
      userEmail={user.email ?? ""}
      activeSection={tab}
      counts={{ events: events.length, coupons: coupons.length }}
      onNavigate={navigate}
      onLogout={() => void signOut(auth)}
    >
      {loading ? <CommunityDashboardSkeleton /> : null}

      {!loading ? (
        <>
          {tab === "overview" ? (
            <PageHeader
              title="Genel Bakış"
              description="Topluluğunun güncel durumunu, yaklaşan etkinliklerini ve katılım verilerini tek yerden takip et."
              action={<Button variant="primary" icon="calendar" onClick={startNewEvent}>Etkinlik Oluştur</Button>}
            />
          ) : null}
          {tab === "coupons" ? <PageHeader title="Kuponlar" description="Topluluğuna özel işletme kuponlarını oluştur ve yayın durumlarını yönet." /> : null}

          {error ? <div className="inline-message inline-message--error admin-message" role="alert">{error}</div> : null}
          {notice ? <div className="inline-message inline-message--success admin-message" role="status">{notice}</div> : null}

          {data?.nextCursor && <Button disabled={loading} onClick={() => void refresh(true, data.nextCursor!)}>Daha fazla kayıt yükle</Button>}
          {data && tab === "overview" ? (
            <div className="community-overview">
              <section className="community-stat-grid" aria-label="Topluluk özeti">
                <StatCard label="Takipçi" value={data.followerCount} detail="Topluluğu takip eden öğrenci" />
                <StatCard label="Yaklaşan etkinlik" value={upcomingEvents.length} detail="Yayında ve tarihi yaklaşan" />
                <StatCard label="Toplam katılım" value={attendance} detail="Etkinliklerde giriş yapan" />
                <StatCard label="Aktif kupon" value={activeCoupons.length} detail="Şu anda yayında olan" />
              </section>

              <section className="community-overview-section" aria-labelledby="upcoming-events-title">
                <div className="community-section-heading">
                  <div>
                    <h2 id="upcoming-events-title">Yaklaşan Etkinlikler</h2>
                    <p>Kayıt ve katılım verileri 15 saniyede bir güncellenir.</p>
                  </div>
                  <Button variant="ghost" onClick={() => void refresh(false)}>Şimdi yenile</Button>
                </div>

                {upcomingEvents.length === 0 ? (
                  <EmptyState
                    title="Yaklaşan etkinlik bulunmuyor"
                    description="Yeni bir etkinlik oluşturarak topluluğunu yeniden bir araya getir."
                    action={<Button variant="secondary" icon="calendar" onClick={startNewEvent}>Etkinlik oluştur</Button>}
                  />
                ) : (
                  <div className="overview-event-list" role="list">
                    {upcomingEvents.map((entry) => (
                      <article className="overview-event-row" role="listitem" key={entry.id}>
                        <div className="overview-event-row__date">
                          <CommunityIcon name="calendar" />
                        </div>
                        <div className="overview-event-row__main">
                          <h3>{entry.title}</h3>
                          <p>{formatCommunityEventDate(entry.date, entry.time)}{entry.location ? ` · ${entry.location}` : ""}</p>
                        </div>
                        <div className="overview-event-row__capacity">
                          <strong>{entry.registrationCount}{entry.capacity ? ` / ${entry.capacity}` : ""}</strong>
                          <span>{entry.capacity ? "Kayıt / kontenjan" : "Kayıt"}</span>
                        </div>
                        <StatusBadge status={entry.status} />
                        <Button variant="ghost" className="overview-event-row__action" onClick={() => manageEvent(entry)}>
                          Yönet <CommunityIcon name="arrow" />
                        </Button>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            </div>
          ) : null}

          {data && tab === "events" && eventView === "list" ? (
            <div className="event-page">
              <PageHeader
                title="Etkinlikler"
                description="Topluluğunun etkinliklerini oluştur, yayınla ve yönet."
                action={<Button variant="primary" icon="calendar" onClick={startNewEvent}>Etkinlik Oluştur</Button>}
              />

              <Card className="event-directory">
                <div className="event-toolbar">
                  <label className="event-search">
                    <span className="visually-hidden">Etkinliklerde ara</span>
                    <input
                      type="search"
                      value={eventSearch}
                      onChange={(event) => setEventSearch(event.target.value)}
                      placeholder="Etkinlik adı veya konum ara"
                    />
                  </label>
                  <label className="event-status-filter">
                    <span className="visually-hidden">Yayın durumuna göre filtrele</span>
                    <select value={eventStatus} onChange={(event) => setEventStatus(event.target.value as EventStatusFilter)}>
                      <option value="all">Tüm durumlar</option>
                      <option value="published">Yayında</option>
                      <option value="pending">Onay bekliyor</option>
                      <option value="cancelled">Yayından kaldırıldı</option>
                    </select>
                  </label>
                </div>

                {events.length === 0 ? (
                  <EmptyState
                    title="Henüz etkinlik yok"
                    description="İlk etkinliğini oluşturarak topluluğunun katılımını yönetmeye başla."
                    action={<Button variant="secondary" icon="calendar" onClick={startNewEvent}>İlk etkinliğini oluştur</Button>}
                  />
                ) : filteredEvents.length === 0 ? (
                  <EmptyState
                    title="Eşleşen etkinlik bulunamadı"
                    description="Arama metnini veya durum filtresini değiştirerek tekrar dene."
                    action={<Button variant="ghost" onClick={() => { setEventSearch(""); setEventStatus("all"); }}>Filtreleri temizle</Button>}
                  />
                ) : (
                  <Table label="Etkinlikler" className="event-table">
                    <div className="event-table__header" role="row">
                      <span role="columnheader">Etkinlik</span>
                      <span role="columnheader">Tarih ve saat</span>
                      <span role="columnheader">Kayıt</span>
                      <span role="columnheader">Durum</span>
                      <span role="columnheader" className="visually-hidden">İşlemler</span>
                    </div>
                    <div role="rowgroup">
                      {filteredEvents.map((entry) => (
                        <div className="event-table__row" role="row" key={entry.id}>
                          <div className="event-table__event" role="cell">
                            <strong>{entry.title}</strong>
                            <span>{entry.location || "Konum belirtilmedi"}</span>
                          </div>
                          <div className="event-table__date" role="cell" data-label="Tarih ve saat">
                            <strong>{formatCommunityEventDate(entry.date, entry.time)}</strong>
                          </div>
                          <div className="event-table__count" role="cell" data-label="Kayıt">
                            <strong>{entry.registrationCount}{entry.capacity ? ` / ${entry.capacity}` : ""}</strong>
                            <span>{entry.capacity ? "Kayıt / kontenjan" : "Kayıt"}</span>
                          </div>
                          <div className="event-table__status" role="cell" data-label="Durum"><StatusBadge status={entry.status} /></div>
                          <div className="event-table__actions" role="cell">
                            <Button variant="secondary" onClick={() => manageEvent(entry)}>Etkinliği Yönet</Button>
                            <details className="event-actions-menu">
                              <summary aria-label={`${entry.title} için diğer işlemler`}>•••</summary>
                              <div>
                                <button type="button" disabled={entry.status === "cancelled"} onClick={() => editEntry(entry)}>Düzenle</button>
                                <button type="button" className="is-danger" disabled={entry.status === "cancelled" || busyId === entry.id} onClick={() => setPendingCancellation(entry)}>
                                  Yayından kaldır
                                </button>
                              </div>
                            </details>
                          </div>
                        </div>
                      ))}
                    </div>
                  </Table>
                )}
              </Card>
            </div>
          ) : null}

          {data && tab === "events" && eventView === "form" ? (
            <div className="event-page event-form-page">
              <PageHeader
                title={editingId ? "Etkinliği Düzenle" : "Etkinlik Oluştur"}
                description={editingId ? "Etkinlik bilgilerini güncelle ve mevcut yayın akışını koru." : "Etkinliğinin temel bilgilerini girerek topluluğunla paylaş."}
                action={<Button variant="ghost" onClick={closeEventForm}>Etkinliklere dön</Button>}
              />
              <form className="event-form-layout" onSubmit={(event) => void saveEntry(event, "event")}>
                <div className="event-form-content">
                  <Card className="event-form-section">
                    <div className="event-form-section__heading">
                      <span>01</span><div><h2>Etkinlik Bilgileri</h2><p>Öğrencilerin etkinliği tanımasını sağlayacak temel bilgiler.</p></div>
                    </div>
                    <label className="field-label" htmlFor="event-title">Etkinlik adı</label>
                    <input id="event-title" value={title} onChange={(event) => setTitle(event.target.value)} placeholder="Örn. Sürdürülebilir Kampüs Buluşması" required />
                    <label className="field-label field-label-spaced" htmlFor="event-category">Kategori</label>
                    <select id="event-category" value={categoryId} onChange={(event) => setCategoryId(event.target.value)} required>
                      <option value="" disabled>Kategori seç</option>
                      {EVENT_CATEGORIES.map((category) => <option key={category.id} value={category.id}>{category.label}</option>)}
                    </select>
                    <label className="field-label field-label-spaced" htmlFor="event-description">Açıklama</label>
                    <textarea id="event-description" value={description} onChange={(event) => setDescription(event.target.value)} rows={5} placeholder="Etkinliğin içeriğini ve katılımcıları nelerin beklediğini anlat." required />
                  </Card>

                  <Card className="event-form-section">
                    <div className="event-form-section__heading">
                      <span>02</span><div><h2>Tarih ve Konum</h2><p>Etkinliğin ne zaman ve nerede gerçekleşeceğini belirt.</p></div>
                    </div>
                    <div className="two-column-fields event-form-fields">
                      <div><label className="field-label" htmlFor="event-date">Tarih</label><input id="event-date" type="date" value={date} onChange={(event) => setDate(event.target.value)} required /></div>
                      <div><label className="field-label" htmlFor="event-time">Başlangıç saati</label><input id="event-time" type="time" value={time} onChange={(event) => setTime(event.target.value)} required /></div>
                    </div>
                    <label className="field-label field-label-spaced" htmlFor="event-location">Konum</label>
                    <input id="event-location" value={location} onChange={(event) => setLocation(event.target.value)} placeholder="Örn. Kampüs Kültür Merkezi" required />
                  </Card>

                  <Card className="event-form-section">
                    <div className="event-form-section__heading">
                      <span>03</span><div><h2>Katılım</h2><p>Etkinliğe kabul edilecek öğrenci sayısını belirle.</p></div>
                    </div>
                    <label className="field-label" htmlFor="event-capacity">Kontenjan</label>
                    <input id="event-capacity" className="event-capacity-input" type="number" min="0" value={capacity} onChange={(event) => setCapacity(event.target.value)} placeholder="0" />
                    <p className="field-help">Sınırsız katılım için 0 yazabilir veya alanı boş bırakabilirsin.</p>
                  </Card>

                  <div className="event-form-actions">
                    <Button type="button" variant="secondary" onClick={closeEventForm}>İptal</Button>
                    <Button type="submit" variant="primary" disabled={saving || !title.trim() || !description.trim() || !date || !time || !location.trim() || !EVENT_CATEGORIES.some((category) => category.id === categoryId)}>
                      {saving ? "Kaydediliyor…" : editingId ? "Değişiklikleri Kaydet" : "Etkinliği Oluştur"}
                    </Button>
                  </div>
                </div>

                <aside className="event-form-summary" aria-label="Etkinlik özeti">
                  <Card>
                    <p className="section-label">Etkinlik özeti</p>
                    <h2>{title.trim() || "Etkinlik adı"}</h2>
                    <StatusBadge status={editingEvent?.status ?? "published"} />
                    <dl>
                      <div><dt>Kategori</dt><dd>{categoryId ? eventCategoryLabel(categoryId) : "Henüz seçilmedi"}</dd></div>
                      <div><dt>Tarih ve saat</dt><dd>{date ? formatCommunityEventDate(date, time) : "Henüz seçilmedi"}</dd></div>
                      <div><dt>Konum</dt><dd>{location.trim() || "Henüz belirtilmedi"}</dd></div>
                      <div><dt>Kontenjan</dt><dd>{capacity && Number(capacity) > 0 ? `${capacity} kişi` : "Sınırsız"}</dd></div>
                    </dl>
                  </Card>
                </aside>
              </form>
            </div>
          ) : null}

          {data && tab === "events" && eventView === "detail" && selectedEvent ? (
            <div className="event-page event-detail-page">
              <Button variant="ghost" className="event-back-link" onClick={() => setEventView("list")}>← Etkinlikler</Button>
              <PageHeader
                title={selectedEvent.title}
                description={`${formatCommunityEventDate(selectedEvent.date, selectedEvent.time)}${selectedEvent.location ? ` · ${selectedEvent.location}` : ""}`}
                action={selectedEvent.status !== "cancelled" ? (
                  <div className="event-detail-actions">
                    <Button variant="secondary" onClick={() => editEntry(selectedEvent)}>Düzenle</Button>
                    <Button variant="danger" disabled={busyId === selectedEvent.id} onClick={() => setPendingCancellation(selectedEvent)}>Yayından kaldır</Button>
                  </div>
                ) : <Button variant="secondary" onClick={() => setEventView("list")}>Etkinliklere dön</Button>}
              />

              <div className="event-detail-status"><StatusBadge status={selectedEvent.status} /></div>
              <section className="community-stat-grid event-metric-grid" aria-label="Etkinlik özeti">
                <StatCard label="Kayıt sayısı" value={selectedEvent.registrationCount} detail="Etkinliğe kayıt olan öğrenci" />
                <StatCard label="Check-in" value={selectedEvent.attendanceCount} detail="Etkinlikte giriş yapan" />
                <StatCard label="Kontenjan" value={selectedEvent.capacity || "∞"} detail={selectedEvent.capacity ? "Toplam katılım sınırı" : "Katılım sınırı bulunmuyor"} />
              </section>

              <div className="event-detail-tabs" role="tablist" aria-label="Etkinlik yönetimi">
                <button type="button" role="tab" aria-selected={eventDetailTab === "overview"} className={eventDetailTab === "overview" ? "is-active" : ""} onClick={() => setEventDetailTab("overview")}>Genel Bakış</button>
                <button type="button" role="tab" aria-selected={eventDetailTab === "participants"} className={eventDetailTab === "participants" ? "is-active" : ""} onClick={() => setEventDetailTab("participants")}>Katılımcılar <span>{selectedEvent.registrationCount}</span></button>
              </div>

              {eventDetailTab === "overview" ? (
                <Card className="event-overview-card">
                  <div><p className="section-label">Etkinlik açıklaması</p><h2>Genel bilgiler</h2></div>
                  <p>{selectedEvent.description}</p>
                  <dl>
                    <div><dt>Kategori</dt><dd>{eventCategoryLabel(selectedEvent.categoryId)}</dd></div>
                    <div><dt>Tarih</dt><dd>{formatCommunityEventDate(selectedEvent.date, selectedEvent.time)}</dd></div>
                    <div><dt>Konum</dt><dd>{selectedEvent.location || "Belirtilmedi"}</dd></div>
                    <div><dt>Yayın durumu</dt><dd><StatusBadge status={selectedEvent.status} /></dd></div>
                  </dl>
                </Card>
              ) : (
                <Card className="participant-card">
                  <div className="participant-card__heading"><div><h2>Katılımcılar</h2><p>Kayıt ve check-in durumları güncel etkinlik verisinden gösterilir.</p></div></div>
                  {participantsLoading && <p role="status">Katılımcılar yükleniyor…</p>}
                  {participants.length === 0 && !participantsLoading ? (
                    <EmptyState title="Henüz kayıt yok" description="Öğrenciler etkinliğe kayıt olduğunda burada görünecek." />
                  ) : (
                    <Table label={`${selectedEvent.title} katılımcıları`} className="participant-table">
                      <div className="participant-table__header" role="row"><span role="columnheader">Öğrenci</span><span role="columnheader">Kayıt durumu</span><span role="columnheader">Check-in durumu</span></div>
                      <div role="rowgroup">
                        {participants.map((participant) => (
                          <div className="participant-table__row" role="row" key={participant.userId}>
                            <div role="cell" data-label="Öğrenci"><span className="participant-avatar">{participant.displayName.slice(0, 1).toLocaleUpperCase("tr-TR")}</span><div><strong>{participant.displayName}</strong><small>{formatCommunityParticipantDate(participant.registeredAt)}</small></div></div>
                            <div role="cell" data-label="Kayıt durumu"><span className="event-state-pill">Kayıtlı</span></div>
                            <div role="cell" data-label="Check-in durumu"><span className={`event-state-pill ${participant.checkedIn ? "is-checked" : ""}`}>{participant.checkedIn ? "Geldi" : "Bekleniyor"}</span></div>
                          </div>
                        ))}
                      </div>
                    </Table>
                  )}
                  {participantCursor && <Button disabled={participantsLoading} onClick={() => void loadParticipants(participantCursor)}>Daha fazla katılımcı</Button>}
                </Card>
              )}
            </div>
          ) : null}

          {data && tab === "events" && eventView === "detail" && !selectedEvent ? (
            <EmptyState title="Etkinlik bulunamadı" description="Etkinlik kaldırılmış veya artık erişilebilir olmayabilir." action={<Button variant="secondary" onClick={() => setEventView("list")}>Etkinliklere dön</Button>} />
          ) : null}

          {data && tab === "coupons" ? (
            <section className="admin-section community-manage-grid">
              <div className="admin-card form-card sticky-form">
                <div className="form-card-heading"><div><p className="section-label">Kupon formu</p><h2>{editingId ? "Kuponu düzenle" : "İndirim kuponu ekle"}</h2><p>Kupon, Good4 onayından sonra yayınlanır.</p></div></div>
                <form onSubmit={(event) => void saveEntry(event, "coupon")}>
                  <label className="field-label" htmlFor="coupon-title">Kampanya adı</label><input id="coupon-title" value={title} onChange={(event) => setTitle(event.target.value)} required />
                  <label className="field-label field-label-spaced" htmlFor="coupon-description">Açıklama</label><textarea id="coupon-description" value={description} onChange={(event) => setDescription(event.target.value)} rows={4} required />
                  <label className="field-label field-label-spaced" htmlFor="coupon-business">İşletme</label><select id="coupon-business" value={businessId} onChange={(event) => setBusinessId(event.target.value)} required><option value="" disabled>İşletme seçin</option>{data.businesses.map((business) => <option key={business.id} value={business.id}>{business.name}</option>)}</select>
                  <div className="two-column-fields"><div><label className="field-label" htmlFor="discount-type">İndirim türü</label><select id="discount-type" value={discountType} onChange={(event) => setDiscountType(event.target.value as typeof discountType)}><option value="percentage">Yüzde (%)</option><option value="fixed">Sabit tutar (TL)</option><option value="freeItem">Ücretsiz ürün</option></select></div>{discountType !== "freeItem" ? <div><label className="field-label" htmlFor="discount-value">Değer</label><input id="discount-value" type="number" min="1" max={discountType === "percentage" ? 100 : undefined} value={discountValue} onChange={(event) => setDiscountValue(event.target.value)} required /></div> : null}</div>
                  <div className="two-column-fields"><div><label className="field-label" htmlFor="coupon-date">Son kullanım</label><input id="coupon-date" type="date" value={date} onChange={(event) => setDate(event.target.value)} required /></div><div><label className="field-label" htmlFor="coupon-limit">Toplam limit</label><input id="coupon-limit" type="number" min="0" value={totalLimit} onChange={(event) => setTotalLimit(event.target.value)} placeholder="0 = sınırsız" /></div></div>
                  <button className="primary-button" disabled={saving || !businessId}>{saving ? "Kaydediliyor…" : editingId ? "Değişiklikleri kaydet" : "Onaya gönder"}</button>
                  {editingId ? <button className="quiet-button full-button" type="button" onClick={resetForm}>Düzenlemeyi iptal et</button> : null}
                </form>
              </div>
              <div className="community-entry-grid">{coupons.map((entry) => <CommunityEntryCard key={entry.id} entry={entry} onEdit={editEntry} onCancel={cancelEntry} busy={busyId === entry.id} />)}</div>
            </section>
          ) : null}
        </>
      ) : null}

      <ConfirmationDialog
        open={Boolean(pendingCancellation)}
        title="Etkinliği yayından kaldır"
        description={pendingCancellation ? `“${pendingCancellation.title}” öğrenciler için yayından kaldırılacak. Bu işlem mevcut kayıtları silmez.` : ""}
        confirmLabel="Yayından kaldır"
        busy={Boolean(pendingCancellation && busyId === pendingCancellation.id)}
        onClose={() => setPendingCancellation(null)}
        onConfirm={() => void confirmEventCancellation()}
      />
    </CommunityAppShell>
  );
}

function CommunityEntryCard({ entry, onEdit, onCancel, busy }: {
  entry: CommunityEntry;
  onEdit: (entry: CommunityEntry) => void;
  onCancel: (entry: CommunityEntry) => void;
  busy: boolean;
}) {
  const statusLabel = entry.status === "published" ? "Yayında" : entry.status === "pending" ? "Onay bekliyor" : "Yayından kaldırıldı";
  return (
    <article className={`admin-card community-entry-card ${entry.status === "cancelled" ? "is-cancelled" : ""}`}>
      <div className="community-entry-card__heading"><div><span className="entry-kind">{entry.kind === "event" ? "Etkinlik" : "Kupon"}</span><h3>{entry.title}</h3></div><span className={`status-pill status-pill--${entry.status}`}>{statusLabel}</span></div>
      <p className="entry-description">{entry.description}</p>
      <dl className="entry-facts"><div><dt>Tarih</dt><dd>{entry.date}{entry.time ? ` · ${entry.time}` : ""}</dd></div><div><dt>{entry.kind === "event" ? "Konum" : "İşletme"}</dt><dd>{entry.kind === "event" ? entry.location : entry.businessName}</dd></div></dl>
      {entry.kind === "event" ? (
        <><div className="entry-numbers"><div><strong>{entry.registrationCount}</strong><span>Kayıtlı</span></div><div><strong>{entry.attendanceCount}</strong><span>Giriş yaptı</span></div><div><strong>{entry.capacity || "∞"}</strong><span>Kontenjan</span></div></div>
        <details className="participant-list"><summary>Katılımcıları görüntüle</summary>{entry.participants.length === 0 ? <p>Henüz kayıt yok.</p> : entry.participants.map((participant) => <div key={participant.userId}><span>{participant.displayName}</span><strong className={participant.checkedIn ? "arrived" : ""}>{participant.checkedIn ? "Geldi" : "Kayıtlı"}</strong></div>)}</details></>
      ) : <div className="entry-numbers"><div><strong>{entry.discountType === "percentage" ? `%${entry.discountValue}` : entry.discountType === "fixed" ? `${entry.discountValue} TL` : "Ücretsiz"}</strong><span>İndirim</span></div><div><strong>{entry.totalLimit || "∞"}</strong><span>Toplam limit</span></div></div>}
      {entry.status !== "cancelled" && <div className="entry-actions"><button className="quiet-button" onClick={() => onEdit(entry)}>Düzenle</button><button className="danger-button" disabled={busy} onClick={() => onCancel(entry)}>{busy ? "İşleniyor…" : "Yayından kaldır"}</button></div>}
    </article>
  );
}

type AdminTab = "notifications" | "coupons" | "menu" | "kyk" | "suspended" | "ads" | "calendar" | "feedback" | "businesses" | "communities" | "market" | "social" | "audit";
type DiningMenuDayForm = { date: string; dayName: string; meals: string; calories: string };

const diningDayNames = ["Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma"];
const calendarFaculties = [
  "Genel",
  "Tıp Fakültesi",
  "Diş Hekimliği Fakültesi",
  "Hukuk Fakültesi",
  "Lisansüstü Eğitim",
  "Uzaktan Eğitim",
  "Yaz Okulu",
  "Yabancı Diller Yüksekokulu",
  "Güzel Sanatlar Fakültesi",
  "Antalya Devlet Konservatuvarı",
  "Yatay Geçiş ve ÇAP",
  "Değişim Programları",
  "2547 Ek Sınav",
  "Diğer Birimler",
];

function addDays(date: string, count: number): string {
  const parsed = new Date(`${date}T12:00:00Z`);
  if (Number.isNaN(parsed.getTime())) return "";
  parsed.setUTCDate(parsed.getUTCDate() + count);
  return parsed.toISOString().slice(0, 10);
}

function diningWeekLabel(weekStart: string, weekEnd: string): string {
  if (!weekStart || !weekEnd) return "";
  const formatter = new Intl.DateTimeFormat("tr-TR", { day: "numeric", month: "short", year: "numeric", timeZone: "UTC" });
  return `${formatter.format(new Date(`${weekStart}T12:00:00Z`))} – ${formatter.format(new Date(`${weekEnd}T12:00:00Z`))}`;
}

function emptyDiningDays(weekStart = ""): DiningMenuDayForm[] {
  return diningDayNames.map((dayName, index) => ({
    date: weekStart ? addDays(weekStart, index) : "",
    dayName,
    meals: "",
    calories: "",
  }));
}

function diningDaysForForm(menu: DiningMenu | null): DiningMenuDayForm[] {
  if (!menu?.days.length) return emptyDiningDays(menu?.weekStart ?? "");
  return menu.days.map((day) => ({
    date: day.date,
    dayName: day.dayName,
    meals: day.meals.join("\n"),
    calories: day.calories === null ? "" : String(day.calories),
  }));
}

const auditLabels: Record<string, string> = {
  "legacyCoupon.approved": "Kupon onaylandı",
  "legacyCoupon.rejected": "Kupon reddedildi",
  "organization.created": "Kurum oluşturuldu",
  "organization.memberAssigned": "Kurum yetkilisi atandı",
  "legacyCoupon.redeemed": "Kupon kullanıldı",
  "campaignCode.redeemed": "Kampanya kodu kullanıldı",
  "diningMenu.updated": "Haftalık yemek menüsü güncellendi",
  "kykMenu.updated": "KYK yemek menüsü güncellendi",
  "campaign.created": "Askıda yemek kampanyası oluşturuldu",
  "campaign.ended": "Askıda yemek kampanyası bitirildi",
  "homeBanner.published": "Ana sayfa reklamı yayınlandı",
  "homeBanner.unpublished": "Ana sayfa reklamı yayından kaldırıldı",
  "academicCalendar.created": "Takvim kaydı oluşturuldu",
  "academicCalendar.updated": "Takvim kaydı güncellendi",
  "communityApplication.approved": "Topluluk başvurusu onaylandı",
  "communityApplication.rejected": "Topluluk başvurusu reddedildi",
};

function readImageSize(file: File): Promise<{ width: number; height: number }> {
  return new Promise((resolve, reject) => {
    const url = URL.createObjectURL(file);
    const image = new Image();
    image.onload = () => {
      URL.revokeObjectURL(url);
      resolve({ width: image.naturalWidth, height: image.naturalHeight });
    };
    image.onerror = () => {
      URL.revokeObjectURL(url);
      reject(new Error("Görsel okunamadı."));
    };
    image.src = url;
  });
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

function formatAdminDate(value: string | null): string {
  if (!value) return "Tarih bekleniyor";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return "Tarih bilinmiyor";
  return new Intl.DateTimeFormat("tr-TR", {
    dateStyle: "medium",
    timeStyle: "short",
    timeZone: "Europe/Istanbul",
  }).format(date);
}

function AdminPanel({ user, context }: { user: User; context: Extract<PortalContext, { portalRole: "admin" }> }) {
  const [tab, setTab] = useState<AdminTab>("coupons");
  const [marketPendingCount, setMarketPendingCount] = useState(0);
  const [socialPendingCount, setSocialPendingCount] = useState(0);
  const [data, setData] = useState<AdminDashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [reviewing, setReviewing] = useState("");
  const [businessName, setBusinessName] = useState("");
  const [legacyBusinessId, setLegacyBusinessId] = useState("");
  const [creatingBusiness, setCreatingBusiness] = useState(false);
  const [memberOrganizationId, setMemberOrganizationId] = useState("");
  const [memberEmail, setMemberEmail] = useState("");
  const [memberName, setMemberName] = useState("");
  const [memberRole, setMemberRole] = useState<"owner" | "staff">("owner");
  const [temporaryPassword, setTemporaryPassword] = useState("");
  const [assigningMember, setAssigningMember] = useState(false);
  const [communityName, setCommunityName] = useState("");
  const [communityUniversity, setCommunityUniversity] = useState("");
  const [creatingCommunity, setCreatingCommunity] = useState(false);
  const [communityOrganizationId, setCommunityOrganizationId] = useState("");
  const [communityEmail, setCommunityEmail] = useState("");
  const [communityManagerName, setCommunityManagerName] = useState("");
  const [communityTemporaryPassword, setCommunityTemporaryPassword] = useState("");
  const [assigningCommunityManager, setAssigningCommunityManager] = useState(false);
  const [communityApplications, setCommunityApplications] = useState<CommunityApplication[]>([]);
  const [menuWeekStart, setMenuWeekStart] = useState("");
  const [menuWeekEnd, setMenuWeekEnd] = useState("");
  const [menuDays, setMenuDays] = useState<DiningMenuDayForm[]>(() => emptyDiningDays());
  const [campaigns, setCampaigns] = useState<AdminCampaign[]>([]);
  const [campaignBusinessId, setCampaignBusinessId] = useState("");
  const [campaignTitle, setCampaignTitle] = useState("");
  const [campaignDescription, setCampaignDescription] = useState("");
  const [campaignStartsAt, setCampaignStartsAt] = useState("");
  const [campaignEndsAt, setCampaignEndsAt] = useState("");
  const [campaignLimit, setCampaignLimit] = useState("");
  const [savingCampaign, setSavingCampaign] = useState(false);
  const [kykText, setKykText] = useState("");
  const [savingKyk, setSavingKyk] = useState(false);
  const kykPreview = parseKykMenuText(kykText);
  const [savingMenu, setSavingMenu] = useState(false);
  const [bannerFile, setBannerFile] = useState<File | null>(null);
  const [bannerPreviewUrl, setBannerPreviewUrl] = useState("");
  const [bannerAdvertiserName, setBannerAdvertiserName] = useState("");
  const [bannerTargetUrl, setBannerTargetUrl] = useState("");
  const [bannerStartsOn, setBannerStartsOn] = useState("");
  const [bannerEndsOn, setBannerEndsOn] = useState("");
  const [bannerActive, setBannerActive] = useState(true);
  const [bannerSlot, setBannerSlot] = useState(1);
  const [savingBanner, setSavingBanner] = useState(false);
  const [calendarEditingId, setCalendarEditingId] = useState("");
  const [calendarTitle, setCalendarTitle] = useState("");
  const [calendarDescription, setCalendarDescription] = useState("");
  const [calendarStartDate, setCalendarStartDate] = useState("");
  const [calendarEndDate, setCalendarEndDate] = useState("");
  const [calendarFaculty, setCalendarFaculty] = useState("Genel");
  const [calendarCategory, setCalendarCategory] = useState("Akademik");
  const [calendarAcademicYear, setCalendarAcademicYear] = useState("2026-2027");
  const [calendarSourcePage, setCalendarSourcePage] = useState("");
  const [calendarActive, setCalendarActive] = useState(true);
  const [savingCalendar, setSavingCalendar] = useState(false);

  async function loadAdminPage(kind: string, cursor: string) {
    setLoading(true);
    try {
      const response = await getAdminDashboard({ [`${kind}Cursor`]: cursor, includeLegacy: kind.startsWith("legacy") });
      setData((current) => !current ? response.data : { ...current,
        nextCursors: { ...current.nextCursors, [kind]: response.data.nextCursors[kind] },
        ...(kind === "organizations" ? { businesses: [...current.businesses, ...response.data.businesses], communities: [...current.communities, ...response.data.communities] } : {}),
        ...(kind === "calendar" ? { calendarEvents: [...current.calendarEvents, ...response.data.calendarEvents] } : {}),
        ...(kind === "legacyBusinesses" ? { legacyBusinesses: [...current.legacyBusinesses, ...response.data.legacyBusinesses] } : {}),
        ...(kind === "legacyCommunities" ? { pendingCoupons: [...current.pendingCoupons, ...response.data.pendingCoupons] } : {}),
      });
    } catch (requestError) { setError(functionErrorMessage(requestError)); }
    finally { setLoading(false); }
  }
  const adminRequest = useRef<Promise<void> | null>(null);
  async function refresh(showLoader = true, clearExistingError = true): Promise<void> {
    if (adminRequest.current) return adminRequest.current;
    const promise = refreshDashboard(showLoader, clearExistingError).finally(() => { if (adminRequest.current === promise) adminRequest.current = null; });
    adminRequest.current = promise;
    return promise;
  }
  async function refreshDashboard(showLoader = true, clearExistingError = true) {
    if (showLoader) setLoading(true);
    if (clearExistingError) setError("");
    try {
      const response = await getAdminDashboard({ includeLegacy: tab === "coupons" || tab === "businesses" });
      setData(response.data);
      setMemberOrganizationId((current) => current || response.data.businesses[0]?.id || "");
      setCommunityOrganizationId((current) => current || response.data.communities[0]?.id || "");
      setMenuWeekStart(response.data.diningMenu?.weekStart ?? "");
      setMenuWeekEnd(response.data.diningMenu?.weekEnd ?? "");
      setMenuDays(diningDaysForForm(response.data.diningMenu));
      fillBannerForm(bannerForSlot(response.data, bannerSlot));
      await loadCommunityApplications(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      if (showLoader) setLoading(false);
    }
  }

  async function loadCommunityApplications(clearExistingError = true) {
    if (clearExistingError) setError("");
    try {
      setCommunityApplications((await listCommunityApplications()).data.applications);
    } catch (requestError) {
      setError(`Topluluk başvuruları yüklenemedi. ${functionErrorMessage(requestError)}`);
    }
  }

  async function handleCommunityApplication(application: CommunityApplication, decision: "approve" | "reject") {
    let reason = "";
    if (decision === "reject") {
      const answer = window.prompt(`“${application.communityName}” başvurusunu reddetme gerekçesi (başvurana gösterilir):`);
      if (answer === null) return;
      reason = answer.trim();
    } else if (!window.confirm(`“${application.communityName}” topluluğu oluşturulsun ve ${application.applicantEmail} yönetici olsun mu?`)) {
      return;
    }
    setReviewing(`application:${application.id}`);
    setError("");
    setNotice("");
    try {
      await reviewCommunityApplication({ applicationId: application.id, decision, ...(reason ? { reason } : {}) });
      setNotice(decision === "approve"
        ? "Topluluk oluşturuldu. Başvuran kişi Google hesabıyla panele giriş yapabilir."
        : "Başvuru reddedildi.");
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      setReviewing("");
      await refresh(false, false);
    }
  }

  useEffect(() => { void refresh(); }, [tab === "coupons" || tab === "businesses"]);
  useEffect(() => {
    if (tab !== "suspended") return;
    setCampaignBusinessId((current) => current || data?.businesses[0]?.id || "");
    loadCampaigns().catch(() => setError("Kampanyalar yüklenemedi."));
  }, [tab, data]);

  async function handleReview(coupon: PendingCoupon, decision: "approve" | "reject") {
    if (decision === "reject" && !window.confirm(`“${coupon.title}” kuponunu reddetmek istiyor musunuz?`)) return;
    const key = `${coupon.communityId}:${coupon.entryId}`;
    setReviewing(key);
    setError("");
    setNotice("");
    try {
      await reviewLegacyCoupon({
        communityId: coupon.communityId,
        entryId: coupon.entryId,
        decision,
      });
      setNotice(decision === "approve" ? "Kupon yayınlandı." : "Kupon reddedildi.");
      await refresh(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
      await refresh(false);
    } finally {
      setReviewing("");
    }
  }

  async function handleContentReport(item: AdminFeedback, decision: "dismiss" | "remove") {
    if (!item.report || !item.id.startsWith("v2:")) return;
    if (decision === "remove" && !window.confirm(`“${item.subject}” bildirimindeki içeriği yayından kaldırmak istiyor musunuz?`)) return;
    setReviewing(item.id);
    setError("");
    setNotice("");
    try {
      await moderateContentReport({ feedbackId: item.id.slice(3), decision });
      setNotice(decision === "remove" ? "İçerik yayından kaldırıldı; bildirim sonuçlandırıldı." : "Bildirim incelendi.");
      await refresh(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
      await refresh(false);
    } finally {
      setReviewing("");
    }
  }

  async function handleCreateBusiness(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreatingBusiness(true);
    setError("");
    setNotice("");
    try {
      await createOrganization({
        name: businessName.trim(),
        type: "business",
        ...(legacyBusinessId ? { legacyTestBusinessId: legacyBusinessId } : {}),
      });
      setBusinessName("");
      setLegacyBusinessId("");
      setNotice("İşletme oluşturuldu. Şimdi yetkili kullanıcı atayabilirsiniz.");
      await refresh(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      setCreatingBusiness(false);
    }
  }

  async function handleAssignMember(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAssigningMember(true);
    setError("");
    setNotice("");
    try {
      const response = await assignOrganizationMemberByEmail({
        organizationId: memberOrganizationId,
        email: memberEmail.trim(),
        displayName: memberName.trim(),
        role: memberRole,
        ...(temporaryPassword ? { temporaryPassword } : {}),
      });
      setMemberEmail("");
      setMemberName("");
      setTemporaryPassword("");
      setNotice(response.data.created
        ? "Yeni işletme hesabı oluşturuldu ve yetkilendirildi."
        : "Mevcut hesap işletmeye yetkili olarak atandı.");
      await refresh(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      setAssigningMember(false);
    }
  }

  async function handleCreateCommunity(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setCreatingCommunity(true);
    setError("");
    setNotice("");
    try {
      await createOrganization({
        name: communityName.trim(),
        type: "community",
        university: communityUniversity.trim(),
      });
      setCommunityName("");
      setCommunityUniversity("");
      setNotice("Topluluk oluşturuldu. Şimdi yönetici hesabını atayabilirsiniz.");
      await refresh(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      setCreatingCommunity(false);
    }
  }

  async function handleAssignCommunityManager(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setAssigningCommunityManager(true);
    setError("");
    setNotice("");
    try {
      const response = await assignOrganizationMemberByEmail({
        organizationId: communityOrganizationId,
        email: communityEmail.trim(),
        displayName: communityManagerName.trim(),
        role: "manager",
        ...(communityTemporaryPassword ? { temporaryPassword: communityTemporaryPassword } : {}),
      });
      setCommunityEmail("");
      setCommunityManagerName("");
      setCommunityTemporaryPassword("");
      setNotice(response.data.created
        ? "Yeni topluluk yöneticisi hesabı oluşturuldu ve yetkilendirildi."
        : "Mevcut hesap topluluk yöneticisi olarak atandı.");
      await refresh(false);
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    } finally {
      setAssigningCommunityManager(false);
    }
  }

  function handleMenuWeekStart(value: string) {
    const weekEnd = value ? addDays(value, 4) : "";
    setMenuWeekStart(value);
    setMenuWeekEnd(weekEnd);
    setMenuDays((current) => diningDayNames.map((dayName, index) => ({
      ...(current[index] ?? { meals: "", calories: "" }),
      date: value ? addDays(value, index) : "",
      dayName,
    })));
  }

  function updateMenuDay(index: number, field: "meals" | "calories", value: string) {
    setMenuDays((current) => current.map((day, dayIndex) => dayIndex === index ? { ...day, [field]: value } : day));
  }

  async function handleSaveDiningMenu(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingMenu(true);
    setError("");
    setNotice("");
    try {
      const days = menuDays.map((day) => ({
        date: day.date,
        dayName: day.dayName,
        meals: day.meals.split("\n").map((meal) => meal.trim()).filter(Boolean),
        calories: day.calories.trim() ? Number(day.calories) : null,
      }));
      if (!menuWeekStart || !menuWeekEnd || days.some((day) => !day.date || day.meals.length === 0)) {
        throw new Error("Her gün için en az bir yemek yazın.");
      }
      const response = await saveDiningMenu({
        weekLabel: diningWeekLabel(menuWeekStart, menuWeekEnd),
        weekStart: menuWeekStart,
        weekEnd: menuWeekEnd,
        days,
      });
      setData((current) => current ? { ...current, diningMenu: response.data } : current);
      setNotice("Haftalık menü yayınlandı. Mobil uygulama artık bu menüyü gösterecek.");
    } catch (requestError) {
      setError(requestError instanceof Error && !('code' in requestError)
        ? requestError.message
        : functionErrorMessage(requestError));
    } finally {
      setSavingMenu(false);
    }
  }

  async function loadCampaigns() {
    const [{ collection, getDocs, limit, query }, database] = await Promise.all([
      import("firebase/firestore/lite"),
      getFirestoreDb(),
    ]);
    const snapshot = await getDocs(query(collection(database, "campaigns"), limit(100)));
    const millis = (value: unknown) => (value as { toMillis?: () => number } | null)?.toMillis?.() ?? 0;
    setCampaigns(snapshot.docs.map((document) => {
      const item = document.data();
      return {
        id: document.id,
        title: String(item.title ?? ""),
        description: String(item.description ?? ""),
        organizationId: String(item.organizationId ?? ""),
        status: String(item.status ?? ""),
        startsAt: millis(item.startsAt),
        endsAt: millis(item.endsAt),
        totalLimit: typeof item.totalLimit === "number" ? item.totalLimit : null,
        redemptionCount: typeof item.redemptionCount === "number" ? item.redemptionCount : 0,
      };
    }).sort((left, right) => right.startsAt - left.startsAt));
  }

  async function handleCreateCampaign(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingCampaign(true);
    setError("");
    setNotice("");
    try {
      const startsAt = new Date(campaignStartsAt);
      const endsAt = new Date(campaignEndsAt);
      if (Number.isNaN(startsAt.getTime()) || Number.isNaN(endsAt.getTime()) || endsAt <= startsAt) {
        throw new Error("Bitiş zamanı başlangıçtan sonra olmalı.");
      }
      const limit = campaignLimit.trim() ? Number.parseInt(campaignLimit, 10) : null;
      if (limit !== null && (!Number.isInteger(limit) || limit <= 0)) {
        throw new Error("Adet boş bırakılabilir ya da pozitif bir sayı olmalı.");
      }
      await createCampaign({
        organizationId: campaignBusinessId,
        title: campaignTitle.trim(),
        description: campaignDescription.trim(),
        startsAt: startsAt.toISOString(),
        endsAt: endsAt.toISOString(),
        status: "published",
        totalLimit: limit,
      });
      setCampaignTitle("");
      setCampaignDescription("");
      setCampaignLimit("");
      setNotice("Askıda yemek yayınlandı. Öğrenciler uygulamada hemen görecek.");
      await loadCampaigns();
    } catch (requestError) {
      setError(requestError instanceof Error && !('code' in requestError)
        ? requestError.message
        : functionErrorMessage(requestError));
    } finally {
      setSavingCampaign(false);
    }
  }

  async function handleEndCampaign(campaign: AdminCampaign) {
    if (!window.confirm(`"${campaign.title}" bitirilsin mi? Öğrenciler artık kod alamaz.`)) return;
    setError("");
    setNotice("");
    try {
      await endCampaign({ campaignId: campaign.id });
      setNotice("Kampanya bitirildi.");
      await loadCampaigns();
    } catch (requestError) {
      setError(functionErrorMessage(requestError));
    }
  }

  async function handleSaveKykMenu(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingKyk(true);
    setError("");
    setNotice("");
    try {
      if (kykPreview.errors.length > 0 || kykPreview.days.length === 0) {
        throw new Error("Önizlemedeki hataları düzeltin.");
      }
      const response = await saveKykMenu({ days: kykPreview.days });
      setNotice(`${response.data.savedDates.length} günlük KYK menüsü yayınlandı. Mobil uygulama hemen gösterecek.`);
      setKykText("");
    } catch (requestError) {
      setError(requestError instanceof Error && !('code' in requestError)
        ? requestError.message
        : functionErrorMessage(requestError));
    } finally {
      setSavingKyk(false);
    }
  }

  function bannerForSlot(dashboard: AdminDashboardData | null, slot: number): HomeBanner | null {
    if (!dashboard) return null;
    return dashboard.homeBanners?.[slot - 1] ?? (slot === 1 ? dashboard.homeBanner : null);
  }

  function fillBannerForm(banner: HomeBanner | null) {
    setBannerAdvertiserName(banner?.advertiserName ?? "");
    setBannerTargetUrl(banner?.targetUrl ?? "");
    setBannerStartsOn(banner?.startsOn ?? "");
    setBannerEndsOn(banner?.endsOn ?? "");
    setBannerActive(banner?.active ?? true);
    setBannerFile(null);
    setBannerPreviewUrl((current) => {
      if (current) URL.revokeObjectURL(current);
      return "";
    });
  }

  function selectBannerSlot(slot: number) {
    setBannerSlot(slot);
    fillBannerForm(bannerForSlot(data, slot));
  }

  async function handleSaveHomeBanner(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingBanner(true);
    setError("");
    setNotice("");
    try {
      let imageUrl = bannerForSlot(data, bannerSlot)?.imageUrl ?? "";
      if (bannerFile) {
        if (bannerFile.size > 5 * 1024 * 1024) throw new Error("Görsel en fazla 5 MB olabilir.");
        if (!['image/jpeg', 'image/png', 'image/webp'].includes(bannerFile.type)) {
          throw new Error("Yalnızca JPG, PNG veya WebP yükleyin.");
        }
        const size = await readImageSize(bannerFile);
        if (size.width < 960 || size.height < 384 || Math.abs(size.width / size.height - 2.5) > 0.03) {
          throw new Error("Görsel 5:2 oranında olmalı. Önerilen ölçü 1200×480 px, minimum 960×384 px.");
        }
        const uploaded = await uploadHomeBannerImage({
          base64: await readFileBase64(bannerFile),
          contentType: bannerFile.type,
          slot: bannerSlot,
        });
        imageUrl = `${uploaded.data.imageUrl}&v=${Date.now()}`;
      }
      if (!imageUrl) throw new Error("Bir reklam görseli seçin.");
      if (!bannerStartsOn || !bannerEndsOn || bannerStartsOn > bannerEndsOn) {
        throw new Error("Geçerli bir yayın tarih aralığı seçin.");
      }
      const response = await saveHomeBanner({
        imageUrl,
        advertiserName: bannerAdvertiserName.trim(),
        targetUrl: bannerTargetUrl.trim(),
        startsOn: bannerStartsOn,
        endsOn: bannerEndsOn,
        active: bannerActive,
        slot: bannerSlot,
      });
      setData((current) => {
        if (!current) return current;
        const banners = [0, 1, 2, 3].map((index) => bannerForSlot(current, index + 1));
        banners[bannerSlot - 1] = response.data;
        return { ...current, homeBanners: banners, homeBanner: banners[0] };
      });
      setBannerFile(null);
      setBannerPreviewUrl((current) => {
        if (current) URL.revokeObjectURL(current);
        return "";
      });
      setNotice(bannerActive ? "Reklam mobil uygulamanın ana sayfasında yayınlandı." : "Reklam kaydedildi ancak yayında değil.");
    } catch (requestError) {
      setError(requestError instanceof Error && !('code' in requestError)
        ? requestError.message
        : functionErrorMessage(requestError));
    } finally {
      setSavingBanner(false);
    }
  }

  function resetCalendarForm() {
    setCalendarEditingId("");
    setCalendarTitle("");
    setCalendarDescription("");
    setCalendarStartDate("");
    setCalendarEndDate("");
    setCalendarFaculty("Genel");
    setCalendarCategory("Akademik");
    setCalendarAcademicYear("2026-2027");
    setCalendarSourcePage("");
    setCalendarActive(true);
  }

  function editCalendarEvent(item: AcademicCalendarEvent) {
    setCalendarEditingId(item.id);
    setCalendarTitle(item.title);
    setCalendarDescription(item.description);
    setCalendarStartDate(item.startDate);
    setCalendarEndDate(item.endDate);
    setCalendarFaculty(item.faculty);
    setCalendarCategory(item.category);
    setCalendarAcademicYear(item.academicYear);
    setCalendarSourcePage(item.sourcePage === null ? "" : String(item.sourcePage));
    setCalendarActive(item.active);
    window.scrollTo({ top: 0, behavior: "smooth" });
  }

  async function handleSaveCalendarEvent(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSavingCalendar(true);
    setError("");
    setNotice("");
    try {
      if (!calendarStartDate || !calendarEndDate || calendarStartDate > calendarEndDate) {
        throw new Error("Geçerli bir tarih aralığı seçin.");
      }
      const response = await saveAcademicCalendarEvent({
        ...(calendarEditingId ? { eventId: calendarEditingId } : {}),
        title: calendarTitle.trim(),
        description: calendarDescription.trim(),
        startDate: calendarStartDate,
        endDate: calendarEndDate,
        faculty: calendarFaculty,
        category: calendarCategory.trim() || "Akademik",
        academicYear: calendarAcademicYear.trim(),
        sourcePage: calendarSourcePage ? Number(calendarSourcePage) : null,
        active: calendarActive,
      });
      setData((current) => current ? {
        ...current,
        calendarEvents: [...current.calendarEvents.filter((item) => item.id !== response.data.id), response.data]
          .sort((left, right) => left.startDate.localeCompare(right.startDate)),
      } : current);
      setNotice(calendarEditingId ? "Takvim kaydı güncellendi." : "Takvim kaydı uygulamada yayınlandı.");
      resetCalendarForm();
    } catch (requestError) {
      setError(requestError instanceof Error && !('code' in requestError)
        ? requestError.message
        : functionErrorMessage(requestError));
    } finally {
      setSavingCalendar(false);
    }
  }

  const unlinkedLegacyBusinesses = data?.legacyBusinesses.filter((business) => !business.linked) ?? [];

  return (
    <div className="panel-shell admin-shell">
      <header className="panel-header">
        <BrandMark compact />
        <div className="account-menu">
          <div>
            <strong>{context.displayName || "Good4 Yöneticisi"}</strong>
            <span>{user.email}</span>
          </div>
          <button className="logout-button" onClick={() => void signOut(auth)}>Çıkış</button>
        </div>
      </header>

      <main className="admin-content">
        {data && Object.entries(data.nextCursors ?? {}).filter(([, cursor]) => cursor).map(([kind, cursor]) => <button type="button" key={kind} disabled={loading} onClick={() => void loadAdminPage(kind, cursor!)}>{{ organizations: "Daha fazla kurum", calendar: "Daha fazla takvim kaydı", legacyBusinesses: "Daha fazla eski işletme", legacyCommunities: "Daha fazla eski topluluk kuponu" }[kind] ?? "Daha fazla kayıt"}</button>)}
        <section className="admin-heading">
          <div>
            <p className="section-label">Good4 yönetimi</p>
            <h1>Kontrol paneli</h1>
            <p>Kuponları ve kullanıcı geri bildirimlerini takip edin; kurum hesaplarını yönetin.</p>
          </div>
          {data && (
            <div className="admin-summary" aria-label="Yönetim özeti">
              <div><strong>{data.pendingCoupons.length}</strong><span>Bekleyen kupon</span></div>
              <div><strong>{data.feedback.filter((item) => item.report && item.status === "new").length}</strong><span>Açık içerik bildirimi</span></div>
              <div><strong>{data.businesses.length}</strong><span>Aktif işletme</span></div>
              <div><strong>{data.communities.length}</strong><span>Aktif topluluk</span></div>
            </div>
          )}
        </section>

        <div className="admin-workspace">
        <nav className="admin-tabs admin-sidebar" aria-label="Yönetim bölümleri">
          <button className={tab === "coupons" ? "active" : ""} aria-current={tab === "coupons" ? "page" : undefined} onClick={() => setTab("coupons")}>
            Bekleyen kuponlar {data && <span>{data.pendingCoupons.length}</span>}
          </button>
          <button className={tab === "menu" ? "active" : ""} aria-current={tab === "menu" ? "page" : undefined} onClick={() => setTab("menu")}>Yemek Menüsü</button>
          <button className={tab === "kyk" ? "active" : ""} aria-current={tab === "kyk" ? "page" : undefined} onClick={() => setTab("kyk")}>KYK Menüsü</button>
          <button className={tab === "suspended" ? "active" : ""} aria-current={tab === "suspended" ? "page" : undefined} onClick={() => setTab("suspended")}>Askıda Yemek</button>
          <button className={tab === "ads" ? "active" : ""} aria-current={tab === "ads" ? "page" : undefined} onClick={() => setTab("ads")}>Ana Sayfa Reklamı</button>
          <button className={tab === "calendar" ? "active" : ""} aria-current={tab === "calendar" ? "page" : undefined} onClick={() => setTab("calendar")}>Akademik Takvim</button>
          <button className={tab === "feedback" ? "active" : ""} aria-current={tab === "feedback" ? "page" : undefined} onClick={() => setTab("feedback")}>
            Bildirimler ve geri bildirimler {data && <span>{data.feedback.filter((item) => item.report && item.status === "new").length}</span>}
          </button>
          <button className={tab === "businesses" ? "active" : ""} aria-current={tab === "businesses" ? "page" : undefined} onClick={() => setTab("businesses")}>İşletmeler</button>
          <button className={tab === "communities" ? "active" : ""} aria-current={tab === "communities" ? "page" : undefined} onClick={() => setTab("communities")}>
            Topluluklar {communityApplications.length > 0 && <span>{communityApplications.length}</span>}
          </button>
          <button className={tab === "market" ? "active" : ""} aria-current={tab === "market" ? "page" : undefined} onClick={() => setTab("market")}>
            Kampüs Dolabı {marketPendingCount > 0 && <span>{marketPendingCount}</span>}
          </button>
          <button className={tab === "social" ? "active" : ""} aria-current={tab === "social" ? "page" : undefined} onClick={() => setTab("social")}>
            {socialText("title")} {socialPendingCount > 0 && <span>{socialPendingCount}</span>}
          </button>
          <button className={tab === "audit" ? "active" : ""} aria-current={tab === "audit" ? "page" : undefined} onClick={() => setTab("audit")}>İşlem geçmişi</button>
          <button className={tab === "notifications" ? "active" : ""} onClick={() => setTab("notifications")}>Bildirimler</button>
          <button className={tab === "audit" ? "active" : ""} onClick={() => setTab("audit")}>İşlem geçmişi</button>
        </nav>

        <div className="admin-tab-content">

        {error && <div className="inline-message inline-message--error admin-message" role="alert">{error}</div>}
        {notice && <div className="inline-message inline-message--success admin-message" role="status">{notice}</div>}

        {loading && <section className="admin-card admin-loading"><span className="spinner spinner--green" /><p>Bilgiler yükleniyor…</p></section>}

        {!loading && data && tab === "notifications" && <NotificationAdmin key={user.uid} uid={user.uid} communities={data.communities} />}

        {!loading && data && tab === "coupons" && (
          <section className="admin-section" aria-labelledby="pending-coupons-title">
            <div className="section-title-row">
              <div><h2 id="pending-coupons-title">Bekleyen kuponlar</h2><p>Onaylanan kupon öğrencilerin uygulamasında yayınlanır.</p></div>
              <button className="quiet-button" onClick={() => void refresh(false)}>Yenile</button>
            </div>
            {data.pendingCoupons.length === 0 ? (
              <div className="admin-card empty-state"><h3>Bekleyen kupon yok</h3><p>Yeni başvurular burada görünecek.</p></div>
            ) : (
              <div className="coupon-review-list">
                {data.pendingCoupons.map((coupon) => {
                  const key = `${coupon.communityId}:${coupon.entryId}`;
                  const busy = reviewing === key;
                  return (
                    <article className="admin-card coupon-review-card" key={key}>
                      <div className="coupon-review-main">
                        <div className="coupon-review-title">
                          <div><h3>{coupon.title}</h3><p>{coupon.communityName} → {coupon.businessName}</p></div>
                        </div>
                        <div className="coupon-tags">
                          <span>{coupon.discountLabel}</span>
                          <span>Son gün: {coupon.expiresOn}</span>
                          <span>{coupon.totalLimit ? `${coupon.totalLimit} kullanım` : "Limitsiz"}</span>
                        </div>
                        <p className="coupon-description">{coupon.description}</p>
                      </div>
                      <div className="review-actions">
                        <button className="danger-button" disabled={busy} onClick={() => void handleReview(coupon, "reject")}>Reddet</button>
                        <button className="primary-button compact-button" disabled={busy} onClick={() => void handleReview(coupon, "approve")}>
                          {busy ? "İşleniyor…" : "Onayla ve yayınla"}
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        )}

        {!loading && data && tab === "menu" && (
          <section className="admin-section" aria-labelledby="dining-menu-title">
            <div className="section-title-row">
              <div>
                <h2 id="dining-menu-title">Haftalık yemek menüsü</h2>
                <p>Burada yayınlanan menü, öğrencilerin ana sayfasındaki Günün Menüsü alanında gösterilir.</p>
              </div>
              {data.diningMenu?.updatedAt && <small className="menu-updated-at">Son güncelleme: {formatAdminDate(data.diningMenu.updatedAt)}</small>}
            </div>
            <form className="admin-card dining-menu-form" onSubmit={handleSaveDiningMenu}>
              <div className="dining-week-fields">
                <div>
                  <label className="field-label" htmlFor="menu-week-start">Hafta başlangıcı</label>
                  <input id="menu-week-start" type="date" value={menuWeekStart} onChange={(event) => handleMenuWeekStart(event.target.value)} required />
                </div>
                <div>
                  <label className="field-label" htmlFor="menu-week-end">Hafta bitişi</label>
                  <input id="menu-week-end" type="date" value={menuWeekEnd} readOnly aria-readonly="true" required />
                </div>
                <div className="dining-week-label">
                  <span>Uygulamada görünecek hafta</span>
                  <strong>{diningWeekLabel(menuWeekStart, menuWeekEnd) || "Hafta seçilmedi"}</strong>
                </div>
              </div>

              <div className="dining-days">
                {menuDays.map((day, index) => (
                  <fieldset className="dining-day-card" key={`${day.dayName}-${index}`}>
                    <legend>{day.dayName}</legend>
                    <div className="dining-day-meta">
                      <label>
                        <span>Tarih</span>
                        <input type="date" value={day.date} readOnly aria-label={`${day.dayName} tarihi`} />
                      </label>
                      <label>
                        <span>Toplam kalori</span>
                        <input type="number" min="0" max="5000" value={day.calories} onChange={(event) => updateMenuDay(index, "calories", event.target.value)} placeholder="İsteğe bağlı" />
                      </label>
                    </div>
                    <label>
                      <span>Yemekler — her satıra bir yemek</span>
                      <textarea rows={5} value={day.meals} onChange={(event) => updateMenuDay(index, "meals", event.target.value)} placeholder={"Çorba\nAna yemek\nPilav veya makarna\nTatlı veya içecek"} required />
                    </label>
                  </fieldset>
                ))}
              </div>

              <div className="dining-menu-actions">
                <p>Kaydettiğinizde önceki haftalık menü bu içerikle değiştirilir.</p>
                <button className="primary-button compact-button" disabled={savingMenu || !menuWeekStart || menuDays.some((day) => !day.meals.trim())}>
                  {savingMenu ? "Yayınlanıyor…" : "Menüyü yayınla"}
                </button>
              </div>
            </form>
          </section>
        )}

        {!loading && data && tab === "suspended" && (
          <section className="admin-section" aria-labelledby="suspended-title">
            <div className="section-title-row">
              <div>
                <h2 id="suspended-title">Askıda yemek</h2>
                <p>Yayınlanan kampanyayı edu doğrulaması yapmış öğrenciler uygulamada görür, 10 dakikalık kod alır ve işletmede gösterir.</p>
              </div>
            </div>
            <form className="admin-card dining-menu-form" onSubmit={handleCreateCampaign}>
              <label className="field-label" htmlFor="campaign-business">İşletme</label>
              <select id="campaign-business" value={campaignBusinessId} onChange={(event) => setCampaignBusinessId(event.target.value)} required>
                <option value="" disabled>İşletme seçin</option>
                {data.businesses.map((business) => <option key={business.id} value={business.id}>{business.name}</option>)}
              </select>
              <label className="field-label field-label-spaced" htmlFor="campaign-title">Başlık</label>
              <input id="campaign-title" value={campaignTitle} onChange={(event) => setCampaignTitle(event.target.value)} maxLength={160} placeholder="Örn. Askıda öğle yemeği" required />
              <label className="field-label field-label-spaced" htmlFor="campaign-description">Açıklama</label>
              <textarea id="campaign-description" rows={3} value={campaignDescription} onChange={(event) => setCampaignDescription(event.target.value)} maxLength={2000} placeholder="Ne veriliyor, nasıl alınır?" />
              <div className="dining-week-fields">
                <div>
                  <label className="field-label" htmlFor="campaign-starts">Başlangıç</label>
                  <input id="campaign-starts" type="datetime-local" value={campaignStartsAt} onChange={(event) => setCampaignStartsAt(event.target.value)} required />
                </div>
                <div>
                  <label className="field-label" htmlFor="campaign-ends">Bitiş</label>
                  <input id="campaign-ends" type="datetime-local" value={campaignEndsAt} onChange={(event) => setCampaignEndsAt(event.target.value)} required />
                </div>
                <div>
                  <label className="field-label" htmlFor="campaign-limit">Toplam adet</label>
                  <input id="campaign-limit" type="number" min="1" value={campaignLimit} onChange={(event) => setCampaignLimit(event.target.value)} placeholder="Sınırsız" />
                </div>
              </div>
              <div className="dining-menu-actions">
                <p>Kampanya hemen yayınlanır.</p>
                <button className="primary-button compact-button" disabled={savingCampaign || !campaignBusinessId || !campaignTitle.trim()}>
                  {savingCampaign ? "Yayınlanıyor…" : "Askıda yemeği yayınla"}
                </button>
              </div>
            </form>
            <div className="admin-card">
              <h3>Kampanyalar</h3>
              {campaigns.length === 0 ? <p>Henüz kampanya yok.</p> : (
                <ul className="admin-list">
                  {campaigns.map((campaign) => {
                    const business = data.businesses.find((item) => item.id === campaign.organizationId)?.name ?? campaign.organizationId;
                    const live = campaign.status === "published" && Date.now() < campaign.endsAt;
                    return (
                      <li key={campaign.id}>
                        <div>
                          <strong>{campaign.title}</strong> · {business}
                          <div><small>
                            {new Date(campaign.startsAt).toLocaleString("tr-TR")} – {new Date(campaign.endsAt).toLocaleString("tr-TR")}
                            {" · "}{campaign.redemptionCount}{campaign.totalLimit ? ` / ${campaign.totalLimit}` : ""} kullanıldı
                            {" · "}{live ? "Yayında" : campaign.status === "ended" ? "Bitirildi" : "Süresi doldu"}
                          </small></div>
                        </div>
                        {live && <button className="secondary-button compact-button" type="button" onClick={() => void handleEndCampaign(campaign)}>Bitir</button>}
                      </li>
                    );
                  })}
                </ul>
              )}
            </div>
          </section>
        )}

        {!loading && data && tab === "kyk" && (
          <section className="admin-section" aria-labelledby="kyk-menu-title">
            <div className="section-title-row">
              <div>
                <h2 id="kyk-menu-title">KYK yemek menüsü</h2>
                <p>Aylık listeyi yapıştırın. Her gün ayrı kaydedilir; aynı tarih yeniden yayınlanırsa üzerine yazılır.</p>
              </div>
            </div>
            <form className="admin-card dining-menu-form" onSubmit={handleSaveKykMenu}>
              <label>
                <span className="field-label">Menü metni</span>
                <textarea rows={14} value={kykText} onChange={(event) => setKykText(event.target.value)} placeholder={"1 EKİM 2026\n\nKAHVALTI\n- Haşlanmış yumurta\n- Çay / bitki çayı\n\nAKŞAM YEMEĞİ\n- Mercimek çorbası\n- Tavuk sote"} />
              </label>
              {kykPreview.errors.length > 0 && (
                <div className="inline-message inline-message--error" role="alert">
                  {kykPreview.errors.map((message) => <div key={message}>{message}</div>)}
                </div>
              )}
              {kykPreview.days.length > 0 && (
                <div className="dining-days">
                  {kykPreview.days.map((day) => (
                    <fieldset className="dining-day-card" key={day.date}>
                      <legend>{day.date.split("-").reverse().join(".")}</legend>
                      <p><strong>Kahvaltı</strong></p>
                      <ul>{day.breakfast.map((item) => <li key={item}>{item}</li>)}</ul>
                      <p><strong>Akşam yemeği</strong></p>
                      <ul>{day.dinner.map((item) => <li key={item}>{item}</li>)}</ul>
                    </fieldset>
                  ))}
                </div>
              )}
              <div className="dining-menu-actions">
                <p>{kykPreview.days.length} gün okundu.</p>
                <button className="primary-button compact-button" disabled={savingKyk || kykPreview.days.length === 0 || kykPreview.errors.length > 0}>
                  {savingKyk ? "Yayınlanıyor…" : "KYK menüsünü yayınla"}
                </button>
              </div>
            </form>
          </section>
        )}

        {!loading && data && tab === "ads" && (
          <section className="admin-section" aria-labelledby="home-banner-title">
            <div className="section-title-row">
              <div>
                <h2 id="home-banner-title">Ana sayfa reklamları</h2>
                <p>En fazla 4 reklam, öğrencilerin ana sayfasında hava durumu ve yemek kartlarının altında kayan bir slider'da görünür.</p>
              </div>
              {bannerForSlot(data, bannerSlot)?.updatedAt && <small className="menu-updated-at">Son güncelleme: {formatAdminDate(bannerForSlot(data, bannerSlot)!.updatedAt)}</small>}
            </div>
            <nav className="admin-tabs" aria-label="Reklam alanları">
              {[1, 2, 3, 4].map((slot) => {
                const slotBanner = bannerForSlot(data, slot);
                return (
                  <button key={slot} type="button" className={bannerSlot === slot ? "active" : ""} onClick={() => selectBannerSlot(slot)}>
                    {slot}. reklam {slotBanner?.imageUrl ? <span>{slotBanner.active ? "Yayında" : "Kapalı"}</span> : <span>Boş</span>}
                  </button>
                );
              })}
            </nav>
            <form className="admin-card home-banner-form" onSubmit={handleSaveHomeBanner}>
              <div className="home-banner-preview" aria-label="Reklam önizlemesi">
                {(bannerPreviewUrl || bannerForSlot(data, bannerSlot)?.imageUrl) ? (
                  <img src={bannerPreviewUrl || bannerForSlot(data, bannerSlot)?.imageUrl} alt="Ana sayfa reklam önizlemesi" />
                ) : (
                  <div><strong>1200 × 480 px</strong><span>Reklam önizlemesi burada görünecek</span></div>
                )}
                <small>Reklam</small>
              </div>
              <p className="field-help">5:2 oran · Önerilen 1200×480 px · Minimum 960×384 px · JPG, PNG veya WebP · En fazla 5 MB</p>
              <div className="home-banner-fields">
                <label>
                  <span>Reklam görseli</span>
                  <input type="file" accept="image/jpeg,image/png,image/webp" onChange={(event) => {
                    const file = event.target.files?.[0] ?? null;
                    setBannerFile(file);
                    setBannerPreviewUrl((current) => {
                      if (current) URL.revokeObjectURL(current);
                      return file ? URL.createObjectURL(file) : "";
                    });
                  }} />
                </label>
                <label>
                  <span>İşletme / reklamveren adı</span>
                  <input value={bannerAdvertiserName} onChange={(event) => setBannerAdvertiserName(event.target.value)} maxLength={80} placeholder="Örn. Kampüs Kahve" required />
                </label>
                <label>
                  <span>Yönlendirme bağlantısı</span>
                  <input type="url" value={bannerTargetUrl} onChange={(event) => setBannerTargetUrl(event.target.value)} placeholder="https://… (isteğe bağlı)" />
                </label>
                <label>
                  <span>Başlangıç tarihi</span>
                  <input type="date" value={bannerStartsOn} onChange={(event) => setBannerStartsOn(event.target.value)} required />
                </label>
                <label>
                  <span>Bitiş tarihi</span>
                  <input type="date" value={bannerEndsOn} min={bannerStartsOn} onChange={(event) => setBannerEndsOn(event.target.value)} required />
                </label>
                <label className="home-banner-toggle">
                  <input type="checkbox" checked={bannerActive} onChange={(event) => setBannerActive(event.target.checked)} />
                  <span>Reklam yayında</span>
                </label>
              </div>
              <div className="dining-menu-actions">
                <p>Tarih aralığı dışındayken reklam uygulamada otomatik olarak gizlenir.</p>
                <button className="primary-button compact-button" disabled={savingBanner || !bannerAdvertiserName.trim() || !bannerStartsOn || !bannerEndsOn || (!bannerFile && !bannerForSlot(data, bannerSlot)?.imageUrl)}>
                  {savingBanner ? "Yayınlanıyor…" : "Reklamı kaydet"}
                </button>
              </div>
            </form>
          </section>
        )}

        {!loading && data && tab === "calendar" && (
          <section className="admin-section" aria-labelledby="academic-calendar-title">
            <div className="section-title-row">
              <div><h2 id="academic-calendar-title">Akademik takvim</h2><p>Öğrencilerin uygulamasındaki ortak takvimi ekleyin ve güncelleyin. Filtre yalnızca fakülte/birim bazındadır.</p></div>
              <strong>{data.calendarEvents.length} kayıt</strong>
            </div>
            <form className="admin-card calendar-event-form" onSubmit={handleSaveCalendarEvent}>
              <div className="calendar-form-grid">
                <label><span>Başlık</span><input value={calendarTitle} onChange={(event) => setCalendarTitle(event.target.value)} maxLength={180} required /></label>
                <label><span>Fakülte / birim</span><select value={calendarFaculty} onChange={(event) => setCalendarFaculty(event.target.value)}>{calendarFaculties.map((faculty) => <option key={faculty}>{faculty}</option>)}</select></label>
                <label><span>Başlangıç tarihi</span><input type="date" value={calendarStartDate} onChange={(event) => { setCalendarStartDate(event.target.value); if (!calendarEndDate) setCalendarEndDate(event.target.value); }} required /></label>
                <label><span>Bitiş tarihi</span><input type="date" min={calendarStartDate} value={calendarEndDate} onChange={(event) => setCalendarEndDate(event.target.value)} required /></label>
                <label><span>Akademik yıl</span><input value={calendarAcademicYear} onChange={(event) => setCalendarAcademicYear(event.target.value)} pattern="[0-9]{4}-[0-9]{4}" required /></label>
                <label><span>Tür</span><input value={calendarCategory} onChange={(event) => setCalendarCategory(event.target.value)} maxLength={40} required /></label>
                <label><span>Kaynak PDF sayfası</span><input type="number" min="1" max="999" value={calendarSourcePage} onChange={(event) => setCalendarSourcePage(event.target.value)} placeholder="İsteğe bağlı" /></label>
                <label className="home-banner-toggle"><input type="checkbox" checked={calendarActive} onChange={(event) => setCalendarActive(event.target.checked)} /><span>Uygulamada yayında</span></label>
                <label className="calendar-description"><span>Açıklama</span><textarea rows={3} value={calendarDescription} onChange={(event) => setCalendarDescription(event.target.value)} maxLength={1000} placeholder="Öğrencinin görmesi gereken ek bilgi" /></label>
              </div>
              <div className="dining-menu-actions"><p>{calendarEditingId ? "Seçili kaydı düzenliyorsunuz." : "Yeni kayıt bütün öğrenciler için yayınlanır."}</p><div className="entry-actions">{calendarEditingId && <button type="button" className="quiet-button" onClick={resetCalendarForm}>Vazgeç</button>}<button className="primary-button compact-button" disabled={savingCalendar || !calendarTitle.trim() || !calendarStartDate || !calendarEndDate}>{savingCalendar ? "Kaydediliyor…" : calendarEditingId ? "Değişiklikleri kaydet" : "Takvime ekle"}</button></div></div>
            </form>
            <div className="calendar-admin-list">
              {data.calendarEvents.map((item) => <article className={`admin-card calendar-admin-row ${item.active ? "" : "is-cancelled"}`} key={item.id}><div><span className="entry-kind">{item.faculty}</span><h3>{item.title}</h3><p>{item.startDate}{item.endDate !== item.startDate ? ` – ${item.endDate}` : ""} · {item.category}</p></div><div className="entry-actions"><span className={`status-pill status-pill--${item.active ? "published" : "cancelled"}`}>{item.active ? "Yayında" : "Gizli"}</span><button className="quiet-button" onClick={() => editCalendarEvent(item)}>Düzenle</button></div></article>)}
            </div>
          </section>
        )}

        {!loading && data && tab === "businesses" && (
          <section className="admin-section admin-business-grid">
            <div className="admin-card form-card">
              <div className="form-card-heading"><div><p className="section-label">Yeni kayıt</p><h2>İşletme oluştur</h2><p>İşletmeyi sisteme ekleyin.</p></div></div>
              <form onSubmit={handleCreateBusiness}>
                <label className="field-label" htmlFor="business-name">İşletme adı</label>
                <input id="business-name" value={businessName} onChange={(event) => setBusinessName(event.target.value)} placeholder="Örn. Good4 Kahve" required />
                <label className="field-label field-label-spaced" htmlFor="legacy-business">Good4Test eşleşmesi</label>
                <select id="legacy-business" value={legacyBusinessId} onChange={(event) => setLegacyBusinessId(event.target.value)}>
                  <option value="">Yeni işletme, eski eşleşme yok</option>
                  {unlinkedLegacyBusinesses.map((business) => <option key={business.id} value={business.id}>{business.name}</option>)}
                </select>
                <p className="field-help">Mevcut mobil kuponları doğrulayacaksa eski işletmeyi seçin.</p>
                <button className="primary-button" disabled={creatingBusiness || !businessName.trim()}>{creatingBusiness ? "Oluşturuluyor…" : "İşletmeyi oluştur"}</button>
              </form>
            </div>

            <div className="admin-card form-card">
              <div className="form-card-heading"><div><p className="section-label">Erişim</p><h2>Yetkili kullanıcı ata</h2><p>İşletme paneline erişim verin.</p></div></div>
              <form onSubmit={handleAssignMember}>
                <label className="field-label" htmlFor="member-business">İşletme</label>
                <select id="member-business" value={memberOrganizationId} onChange={(event) => setMemberOrganizationId(event.target.value)} required>
                  <option value="" disabled>İşletme seçin</option>
                  {data.businesses.map((business) => <option key={business.id} value={business.id}>{business.name}</option>)}
                </select>
                <label className="field-label field-label-spaced" htmlFor="member-email">E-posta</label>
                <input id="member-email" type="email" autoComplete="off" value={memberEmail} onChange={(event) => setMemberEmail(event.target.value)} placeholder="yetkili@isletme.com" required />
                <div className="two-column-fields">
                  <div><label className="field-label" htmlFor="member-name">Ad</label><input id="member-name" value={memberName} onChange={(event) => setMemberName(event.target.value)} placeholder="Yetkili adı" /></div>
                  <div><label className="field-label" htmlFor="member-role">Yetki</label><select id="member-role" value={memberRole} onChange={(event) => setMemberRole(event.target.value as "owner" | "staff")}><option value="owner">İşletme sahibi</option><option value="staff">Personel</option></select></div>
                </div>
                <label className="field-label field-label-spaced" htmlFor="temporary-password">Geçici şifre</label>
                <input id="temporary-password" type="password" autoComplete="new-password" value={temporaryPassword} onChange={(event) => setTemporaryPassword(event.target.value)} placeholder="Yeni hesap için en az 12 karakter" />
                <p className="field-help">E-posta sistemde kayıtlıysa şifre alanını boş bırakabilirsiniz.</p>
                <button className="primary-button" disabled={assigningMember || !memberOrganizationId || !memberEmail.trim()}>{assigningMember ? "Atanıyor…" : "Kullanıcıyı yetkilendir"}</button>
              </form>
            </div>

            <div className="admin-card business-list-card">
              <div className="section-title-row"><div><h2>Aktif işletmeler</h2><p>{data.businesses.length} işletme</p></div></div>
              <div className="business-list">
                {data.businesses.map((business) => (
                  <div key={business.id}><span className="business-avatar">{business.name.slice(0, 1).toLocaleUpperCase("tr-TR")}</span><div><strong>{business.name}</strong><small>{business.legacyTestBusinessId ? "Good4Test bağlı" : "V2 işletmesi"}</small></div></div>
                ))}
              </div>
            </div>
          </section>
        )}

        {!loading && data && tab === "feedback" && (
          <section className="admin-section" aria-labelledby="feedback-title">
            <div className="section-title-row">
              <div><h2 id="feedback-title">Bildirimler ve geri bildirimler</h2><p>İçerik bildirimlerini inceleyin; uygunsuz içeriği yayından kaldırın.</p></div>
              <button className="quiet-button" onClick={() => void refresh(false)}>Yenile</button>
            </div>
            {data.feedback.length === 0 ? (
              <div className="admin-card empty-state"><h3>Henüz geri bildirim yok</h3><p>Öğrencilerin gönderdiği mesajlar burada görünecek.</p></div>
            ) : (
              <div className="feedback-list">
                {[...data.feedback].sort((left, right) => Number(Boolean(right.report && right.status === "new")) - Number(Boolean(left.report && left.status === "new"))).map((item) => (
                  <article className="admin-card feedback-card" key={item.id}>
                    <div className="feedback-card__header">
                      <div>
                        <span className="feedback-source">{item.environment === "v2" ? "V2 uygulama" : "Test uygulaması"}</span>
                        <h3>{item.subject}</h3>
                      </div>
                      <time>{formatAdminDate(item.createdAt)}</time>
                    </div>
                    <p className="feedback-message">{item.message}</p>
                    {item.report && (
                      <div className="coupon-tags">
                        <span>{item.report.kind === "event" ? "Etkinlik bildirimi" : "Kupon bildirimi"}</span>
                        <span>Durum: {item.status === "new" ? "İnceleme bekliyor" : item.status === "resolved" ? "İçerik kaldırıldı" : "İncelendi"}</span>
                      </div>
                    )}
                    <div className="feedback-sender">
                      <strong>{item.userDisplayName || "Good4 kullanıcısı"}</strong>
                      <span>{item.userEmail || item.userId}</span>
                    </div>
                    {item.report && item.status === "new" && (
                      <div className="review-actions">
                        <button className="quiet-button" disabled={reviewing === item.id} onClick={() => void handleContentReport(item, "dismiss")}>İncelendi, işlem yok</button>
                        <button className="danger-button" disabled={reviewing === item.id} onClick={() => void handleContentReport(item, "remove")}>
                          {reviewing === item.id ? "İşleniyor…" : "İçeriği yayından kaldır"}
                        </button>
                      </div>
                    )}
                  </article>
                ))}
              </div>
            )}
          </section>
        )}

        {!loading && data && tab === "communities" && (
          <section className="admin-section" aria-labelledby="community-applications-title">
            <div className="section-title-row">
              <div>
                <h2 id="community-applications-title">Topluluk başvuruları</h2>
                <p>Topluluklar <a href="/topluluk-basvuru">good4tr.com/topluluk-basvuru</a> adresinden Google hesabıyla başvurur. Onay, topluluğu oluşturur ve başvuranı yönetici yapar. Yöneticiler <a href={COMMUNITY_PANEL_URL}>good4tr.com/topluluk-paneli</a> adresinden giriş yapar.</p>
              </div>
              <button className="quiet-button" onClick={() => void loadCommunityApplications()}>Yenile</button>
            </div>
            {communityApplications.length === 0 ? (
              <div className="admin-card empty-state"><h3>Bekleyen başvuru yok</h3><p>Yeni başvurular burada görünecek.</p></div>
            ) : (
              <div className="coupon-review-list">
                {communityApplications.map((application) => {
                  const busy = reviewing === `application:${application.id}`;
                  return (
                    <article className="admin-card coupon-review-card application-review-card" key={application.id}>
                      <div className="coupon-review-main">
                        <div className="coupon-review-title">
                          <div><h3>{application.communityName}</h3><p>{application.university}</p></div>
                        </div>
                        <div className="coupon-tags">
                          <span>{application.applicantName}</span>
                          <span>{application.applicantEmail}</span>
                          {application.createdAt && <span>{formatAdminDate(application.createdAt)}</span>}
                          {application.socialUrl && <span><a href={application.socialUrl} target="_blank" rel="noopener noreferrer">Bağlantıyı aç</a></span>}
                        </div>
                        {application.description && <p className="coupon-description">{application.description}</p>}
                      </div>
                      <div className="review-actions">
                        <button className="danger-button" disabled={busy} onClick={() => void handleCommunityApplication(application, "reject")}>Reddet</button>
                        <button className="primary-button compact-button" disabled={busy} onClick={() => void handleCommunityApplication(application, "approve")}>
                          {busy ? "İşleniyor…" : "Onayla"}
                        </button>
                      </div>
                    </article>
                  );
                })}
              </div>
            )}
          </section>
        )}

        {!loading && data && tab === "communities" && (
          <section className="admin-section admin-business-grid">
            <div className="admin-card form-card">
              <div className="form-card-heading"><div><p className="section-label">Yeni kayıt</p><h2>Topluluk oluştur</h2><p>Topluluğu ve üniversitesini sisteme ekleyin.</p></div></div>
              <form onSubmit={handleCreateCommunity}>
                <label className="field-label" htmlFor="community-name">Topluluk adı</label>
                <input id="community-name" value={communityName} onChange={(event) => setCommunityName(event.target.value)} placeholder="Örn. Kadın Girişimciler Topluluğu" required />
                <label className="field-label field-label-spaced" htmlFor="community-university">Üniversite</label>
                <input id="community-university" value={communityUniversity} onChange={(event) => setCommunityUniversity(event.target.value)} placeholder="Örn. Akdeniz Üniversitesi" required />
                <button className="primary-button" disabled={creatingCommunity || !communityName.trim() || !communityUniversity.trim()}>{creatingCommunity ? "Oluşturuluyor…" : "Topluluğu oluştur"}</button>
              </form>
            </div>

            <div className="admin-card form-card">
              <div className="form-card-heading"><div><p className="section-label">Erişim</p><h2>Topluluk yöneticisi ata</h2><p>Mevcut hesabı kullanın veya yeni hesap oluşturun.</p></div></div>
              <form onSubmit={handleAssignCommunityManager}>
                <label className="field-label" htmlFor="community-organization">Topluluk</label>
                <select id="community-organization" value={communityOrganizationId} onChange={(event) => setCommunityOrganizationId(event.target.value)} required>
                  <option value="" disabled>Topluluk seçin</option>
                  {data.communities.map((community) => <option key={community.id} value={community.id}>{community.name}</option>)}
                </select>
                <label className="field-label field-label-spaced" htmlFor="community-email">E-posta</label>
                <input id="community-email" type="email" autoComplete="off" value={communityEmail} onChange={(event) => setCommunityEmail(event.target.value)} placeholder="yonetici@topluluk.com" required />
                <label className="field-label field-label-spaced" htmlFor="community-manager-name">Yönetici adı</label>
                <input id="community-manager-name" value={communityManagerName} onChange={(event) => setCommunityManagerName(event.target.value)} placeholder="Yetkili adı" />
                <label className="field-label field-label-spaced" htmlFor="community-temporary-password">Geçici şifre</label>
                <input id="community-temporary-password" type="password" autoComplete="new-password" value={communityTemporaryPassword} onChange={(event) => setCommunityTemporaryPassword(event.target.value)} placeholder="Yeni hesap için en az 12 karakter" />
                <p className="field-help">V2'de kayıtlı hesap için boş bırakın. Eski Good4Test hesabı ilk kez taşınıyorsa geçici şifre gerekir.</p>
                <button className="primary-button" disabled={assigningCommunityManager || !communityOrganizationId || !communityEmail.trim()}>{assigningCommunityManager ? "Atanıyor…" : "Yöneticiyi yetkilendir"}</button>
              </form>
            </div>

            <div className="admin-card business-list-card">
              <div className="section-title-row"><div><h2>Aktif topluluklar</h2><p>{data.communities.length} topluluk</p></div></div>
              <div className="business-list">
                {data.communities.length === 0 ? <div className="empty-state compact-empty"><h3>Henüz topluluk yok</h3></div> : data.communities.map((community) => (
                  <div key={community.id}><span className="business-avatar">{community.name.slice(0, 1).toLocaleUpperCase("tr-TR")}</span><div><strong>{community.name}</strong><small>{community.university}{community.legacyTestCommunityId ? " · Good4Test bağlı" : ""}</small></div></div>
                ))}
              </div>
            </div>
          </section>
        )}

        {!loading && data && tab === "market" && <CampusClosetAdmin onPendingCountChange={setMarketPendingCount} />}
        {!loading && data && tab === "social" && <SocialActivitiesAdmin onPendingCountChange={setSocialPendingCount} />}

        {!loading && data && tab === "audit" && (
          <section className="admin-section" aria-labelledby="audit-title">
            <div className="section-title-row"><div><h2 id="audit-title">İşlem geçmişi</h2><p>Son 50 kritik yönetim işlemi.</p></div><button className="quiet-button" onClick={() => void refresh(false)}>Yenile</button></div>
            <div className="admin-card audit-card">
              {data.audits.length === 0 ? <div className="empty-state compact-empty"><h3>Henüz işlem yok</h3></div> : (
                <div className="audit-list">
                  {data.audits.map((audit) => (
                    <div className="audit-row" key={audit.id}>
                      <span className="audit-dot" />
                      <div><strong>{auditLabels[audit.action] ?? audit.action}</strong><small>{audit.targetId}</small></div>
                      <time>{formatAdminDate(audit.createdAt)}</time>
                    </div>
                  ))}
                </div>
              )}
            </div>
          </section>
        )}
        </div>
        </div>
      </main>
    </div>
  );
}

export default function App({ portal = "staff" }: { portal?: PortalEntrance }) {
  const [authReady, setAuthReady] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [context, setContext] = useState<PortalContext | null>(null);
  const [contextLoading, setContextLoading] = useState(false);
  const [accessError, setAccessError] = useState("");

  useEffect(() => onAuthStateChanged(auth, (nextUser) => {
    setUser(nextUser);
    setContext(null);
    setAccessError("");
    setAuthReady(true);

    if (!nextUser) {
      setContextLoading(false);
      return;
    }

    setContextLoading(true);
    void getPortalContext()
      .then((response) => setContext(response.data))
      .catch((error) => setAccessError(functionErrorMessage(error)))
      .finally(() => setContextLoading(false));
  }), []);

  if (!authReady || contextLoading) {
    return (
      <main className="loading-page">
        <BrandMark compact />
        <span className="spinner spinner--green" />
        <p>Panel hazırlanıyor…</p>
      </main>
    );
  }

  if (!user) {
    return <LoginScreen portal={portal} />;
  }

  if (accessError || !context) {
    return <AccessError message={accessError || "İşletme bilgisi bulunamadı."} />;
  }

  if ((portal === "community") !== (context.portalRole === "community")) {
    return <WrongPortal portal={portal} />;
  }

  if (context.portalRole === "admin") {
    return <AdminPanel user={user} context={context} />;
  }

  if (context.portalRole === "community") {
    return <CommunityPanel user={user} context={context} />;
  }

  return <VerificationPanel user={user} context={context} />;
}

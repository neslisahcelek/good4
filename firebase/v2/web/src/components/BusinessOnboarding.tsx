import { useState, type FormEvent } from "react";
import { signOut, type User } from "firebase/auth";
import { httpsCallable } from "firebase/functions";
import { auth, functions } from "../firebase";
import { BusinessAppShell, Button, Card, CommunityIcon, PageHeader, StatCard, type BusinessSection } from "./CommunityUi";
import { IllustratedEmpty } from "./PanelBlocks";

export type BusinessApplicationView = {
  id: string;
  businessName: string;
  contactName: string;
  phone: string;
  applicantEmail: string;
  status: "pending" | "approved" | "rejected";
  rejectionReason: string;
  createdAt: string | null;
};

const submitBusinessApplication = httpsCallable<
  { businessName: string; contactName: string; phone: string },
  { application: BusinessApplicationView }
>(functions, "submitBusinessApplication");

type Describe = (error: unknown) => string;

function useApplicationForm(initial: Pick<BusinessApplicationView, "businessName" | "contactName" | "phone"> | null,
  onSaved: (application: BusinessApplicationView) => void, describeError: Describe) {
  const [businessName, setBusinessName] = useState(initial?.businessName ?? "");
  const [contactName, setContactName] = useState(initial?.contactName ?? "");
  const [phone, setPhone] = useState(initial?.phone ?? "");
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSaving(true);
    setError("");
    try {
      const response = await submitBusinessApplication({ businessName, contactName, phone });
      onSaved(response.data.application);
    } catch (requestError) {
      setError(describeError(requestError));
    } finally {
      setSaving(false);
    }
  }

  const fields = (
    <>
      <label className="field-label" htmlFor="business-name">İşletme adı</label>
      <input id="business-name" value={businessName} onChange={(event) => setBusinessName(event.target.value)} maxLength={160} required autoComplete="organization" />
      <label className="field-label field-label-spaced" htmlFor="business-contact">Yetkili adı soyadı</label>
      <input id="business-contact" value={contactName} onChange={(event) => setContactName(event.target.value)} maxLength={120} required autoComplete="name" />
      <label className="field-label field-label-spaced" htmlFor="business-phone">Telefon</label>
      <input id="business-phone" type="tel" inputMode="tel" value={phone} onChange={(event) => setPhone(event.target.value)} placeholder="0532 123 45 67" required autoComplete="tel" />
      {error ? <div className="inline-message inline-message--error" role="alert">{error}</div> : null}
    </>
  );
  return { fields, submit, saving };
}

/** First visit of a Google account without a role on the staff panel: three fields, then straight into the panel. */
export function BusinessApplicationForm({ user, onSubmitted, describeError }: {
  user: User;
  onSubmitted: (application: BusinessApplicationView) => void;
  describeError: Describe;
}) {
  const { fields, submit, saving } = useApplicationForm(null, onSubmitted, describeError);
  return (
    <main className="login-shell">
      <section className="login-form-area">
        <div className="login-card application-card">
          <div className="login-card__brand">
            <div className="brand-mark"><img src="/good4-logo.png" alt="Good4" /><div><span>Good4</span><small>İşletme Paneli</small></div></div>
          </div>
          <h1>İşletmeni Good4'a ekle</h1>
          <p className="card-intro">
            İşletme adını ve iletişim bilgini yaz. Panelin hemen açılır; Good4 başvurunu onayladığında öğrenci kuponlarını doğrulayabilirsin.
          </p>
          <form onSubmit={(event) => void submit(event)}>
            {fields}
            <button className="primary-button" disabled={saving}>{saving ? "Gönderiliyor…" : "Başvuruyu gönder"}</button>
          </form>
          <p className="support-copy">
            {user.email} hesabıyla başvuruyorsun. <button type="button" className="text-button" onClick={() => void signOut(auth)}>Farklı hesapla giriş yap</button>
          </p>
        </div>
      </section>
    </main>
  );
}

/** The business panel before approval: everything is visible, code verification stays locked. */
export function BusinessPendingPanel({ user, application, onChanged, describeError }: {
  user: User;
  application: BusinessApplicationView;
  onChanged: (application: BusinessApplicationView) => void;
  describeError: Describe;
}) {
  const [section, setSection] = useState<BusinessSection>("verify");
  const [code, setCode] = useState("");
  const [savedNotice, setSavedNotice] = useState("");
  const { fields, submit, saving } = useApplicationForm(application, (next) => {
    setSavedNotice(application.status === "rejected" ? "Başvurun yeniden incelemeye gönderildi." : "Bilgilerin güncellendi.");
    onChanged(next);
  }, describeError);
  const rejected = application.status === "rejected";

  const banner = (
    <div className={`pending-banner ${rejected ? "pending-banner--rejected" : ""}`.trim()} role="status">
      <span aria-hidden="true"><CommunityIcon name={rejected ? "close" : "history"} /></span>
      <div>
        <strong>{rejected ? "Başvurun onaylanmadı" : "Başvurun inceleniyor"}</strong>
        <p>
          {rejected
            ? <>{application.rejectionReason ? <>Gerekçe: {application.rejectionReason}. </> : null}Bilgilerini İşletme Profili'nden düzeltip yeniden gönderebilirsin.</>
            : "Good4 onayladığında kupon doğrulama açılacak. Onaydan sonra bu sayfayı yenilemen yeterli."}
        </p>
      </div>
      {!rejected ? <button type="button" className="pill-button" onClick={() => window.location.reload()}>Durumu yenile</button> : null}
    </div>
  );

  return (
    <BusinessAppShell
      businessName={application.businessName}
      userEmail={user.email ?? ""}
      activeSection={section}
      usageCount={0}
      onNavigate={(next) => { setSection(next); window.scrollTo({ top: 0, behavior: "smooth" }); }}
      onLogout={() => void signOut(auth)}
    >
      {banner}

      {section === "overview" ? (
        <div className="business-page business-overview">
          <PageHeader title="Genel Bakış" description="Onaydan sonra doğrulanan kampanya kodları burada görünecek."
            action={<Button variant="primary" icon="verify" onClick={() => setSection("verify")}>Kupon Doğrula</Button>} />
          <section className="community-stat-grid business-stat-grid" aria-label="İşletme kullanım özeti">
            <StatCard label="Bugünkü kullanım" value={0} detail="Bugün doğrulanan V2 kodları" />
            <StatCard label="Bu ay" value={0} detail="Bu ay doğrulanan V2 kodları" />
            <StatCard label="Toplam doğrulama" value={0} detail="Kayıtlı V2 kullanımlarının tamamı" />
          </section>
        </div>
      ) : null}

      {section === "verify" ? (
        <div className="business-page business-verify-page">
          <PageHeader title="Kupon Doğrula" description="Öğrencinin kampanya kodunu girerek doğrula." />
          <div className="business-verify-layout">
            <Card className="business-verification-card">
              <div className="business-verification-card__heading"><span aria-hidden="true"><CommunityIcon name="verify" /></span><div><h2>Kampanya kodu</h2><p>Kodu boşluklarıyla birlikte veya doğrudan yazabilirsin.</p></div></div>
              <form onSubmit={(event) => event.preventDefault()}>
                <label className="field-label" htmlFor="pending-campaign-code">Kampanya kodunu gir</label>
                <div className="code-input-wrap">
                  <input id="pending-campaign-code" type="text" inputMode="numeric" pattern="[0-9 ]*" autoComplete="off" spellCheck={false}
                    value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, "").slice(0, 8))}
                    placeholder="1234 5678" aria-describedby="pending-code-help" />
                </div>
                <p id="pending-code-help" className="field-help">Doğrulama, Good4 başvurunu onayladığında açılacak.</p>
                <Button className="business-verify-button" variant="primary" type="submit" disabled>
                  <CommunityIcon name="history" /> Onay bekleniyor
                </Button>
              </form>
            </Card>
            <aside className="business-verify-note"><strong>Neden kilitli?</strong><p>Kuponlar yalnızca Good4'un onayladığı işletmelerde kullanılabilir. Onaydan sonra kod girip doğrulamaya hemen başlayabilirsin.</p></aside>
          </div>
        </div>
      ) : null}

      {section === "usage" ? (
        <div className="business-page">
          <PageHeader title="Kullanımlar" description="İşletmende doğrulanan V2 kampanya kodlarını incele." />
          <Card className="business-usage-card">
            <IllustratedEmpty illustration="coupon" title="Henüz kullanım yok">Onaydan sonra doğruladığın kodlar burada listelenecek.</IllustratedEmpty>
          </Card>
        </div>
      ) : null}

      {section === "profile" ? (
        <div className="business-page business-profile-page">
          <PageHeader title="İşletme Profili" description={rejected ? "Bilgilerini düzelt ve başvurunu yeniden gönder." : "Başvuru bilgilerini onaydan önce güncelleyebilirsin."} />
          <Card className="business-profile-card">
            <div className="business-card-heading"><div><h2>Başvuru bilgileri</h2><p>Hesap e-postası: {user.email}</p></div>
              <span className={`event-state-pill ${rejected ? "" : "is-pending"}`.trim()}>{rejected ? "Onaylanmadı" : "Onay bekliyor"}</span></div>
            <form onSubmit={(event) => { setSavedNotice(""); void submit(event); }}>
              {fields}
              {savedNotice ? <div className="inline-message inline-message--success" role="status">{savedNotice}</div> : null}
              <button className="primary-button" disabled={saving}>{saving ? "Kaydediliyor…" : rejected ? "Yeniden gönder" : "Bilgileri güncelle"}</button>
            </form>
          </Card>
        </div>
      ) : null}
    </BusinessAppShell>
  );
}

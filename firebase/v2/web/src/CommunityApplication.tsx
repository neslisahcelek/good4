import { FormEvent, useEffect, useState } from "react";
import { onAuthStateChanged, signOut, type User } from "firebase/auth";
import { httpsCallable } from "firebase/functions";
import { auth, functions } from "./firebase";
import {
  BrandMark,
  authErrorMessage,
  functionErrorMessage,
  isPopupDismissed,
  signInWithGoogle,
  type CommunityApplication as Application,
} from "./App";

const getMyCommunityApplication = httpsCallable<void, { application: Application | null; accountRole: string }>(
  functions, "getMyCommunityApplication",
);
const submitCommunityApplication = httpsCallable<{
  communityName: string;
  university: string;
  applicantName: string;
  description: string;
  socialUrl: string;
}, { status: "pending" }>(functions, "submitCommunityApplication");

type Loaded = { application: Application | null; accountRole: string };

function ApplicationForm({ user, previous, onSubmitted }: {
  user: User;
  previous: Application | null;
  onSubmitted: () => Promise<void>;
}) {
  const [communityName, setCommunityName] = useState(previous?.communityName ?? "");
  const [university, setUniversity] = useState(previous?.university ?? "Akdeniz Üniversitesi");
  const [applicantName, setApplicantName] = useState(previous?.applicantName ?? user.displayName ?? "");
  const [description, setDescription] = useState(previous?.description ?? "");
  const [socialUrl, setSocialUrl] = useState(previous?.socialUrl ?? "");
  const [submitting, setSubmitting] = useState(false);
  const [message, setMessage] = useState("");

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setMessage("");
    setSubmitting(true);
    try {
      await submitCommunityApplication({
        communityName: communityName.trim(),
        university: university.trim(),
        applicantName: applicantName.trim(),
        description: description.trim(),
        socialUrl: socialUrl.trim(),
      });
      await onSubmitted();
    } catch (error) {
      setMessage(functionErrorMessage(error));
    } finally {
      setSubmitting(false);
    }
  }

  const ready = communityName.trim() && university.trim() && applicantName.trim();
  return (
    <form onSubmit={handleSubmit} noValidate>
      <label className="field-label" htmlFor="application-community">Topluluk adı</label>
      <input id="application-community" value={communityName} onChange={(event) => setCommunityName(event.target.value)} maxLength={160} placeholder="Örn. Akdeniz Satranç Topluluğu" required />
      <label className="field-label field-label-spaced" htmlFor="application-university">Üniversite</label>
      <input id="application-university" value={university} onChange={(event) => setUniversity(event.target.value)} maxLength={160} required />
      <label className="field-label field-label-spaced" htmlFor="application-name">Başvuran kişinin adı</label>
      <input id="application-name" value={applicantName} onChange={(event) => setApplicantName(event.target.value)} maxLength={120} placeholder="Ad Soyad" required />
      <label className="field-label field-label-spaced" htmlFor="application-description">Topluluğu kısaca tanıtın</label>
      <textarea id="application-description" value={description} onChange={(event) => setDescription(event.target.value)} maxLength={1000} placeholder="Ne tür etkinlikler yapıyorsunuz, kaç üyeniz var?" />
      <label className="field-label field-label-spaced" htmlFor="application-social">Instagram veya web sitesi</label>
      <input id="application-social" type="url" value={socialUrl} onChange={(event) => setSocialUrl(event.target.value)} maxLength={300} placeholder="https://instagram.com/toplulugunuz" />
      <p className="field-help">İsteğe bağlı. Topluluğun gerçekten var olduğunu doğrulamamıza yardımcı olur.</p>
      {message && <div className="inline-message inline-message--error" role="alert">{message}</div>}
      <button className="primary-button" type="submit" disabled={submitting || !ready}>
        {submitting ? <><span className="spinner" /> Gönderiliyor</> : previous ? "Başvuruyu güncelle" : "Başvuruyu gönder"}
      </button>
      <p className="field-help">
        Bilgileriniz yalnızca başvurunuzu değerlendirmek için kullanılır. Ayrıntılar için <a href="/gizlilik-politikasi">gizlilik politikası</a>.
      </p>
    </form>
  );
}

export default function CommunityApplicationPage() {
  const [authReady, setAuthReady] = useState(false);
  const [user, setUser] = useState<User | null>(null);
  const [loaded, setLoaded] = useState<Loaded | null>(null);
  const [loading, setLoading] = useState(false);
  const [editing, setEditing] = useState(false);
  const [message, setMessage] = useState("");
  const [signingIn, setSigningIn] = useState(false);

  async function load() {
    setLoading(true);
    setMessage("");
    try {
      const response = await getMyCommunityApplication();
      setLoaded(response.data);
      setEditing(false);
    } catch (error) {
      setMessage(functionErrorMessage(error));
    } finally {
      setLoading(false);
    }
  }

  useEffect(() => onAuthStateChanged(auth, (nextUser) => {
    setUser(nextUser);
    setLoaded(null);
    setAuthReady(true);
    if (nextUser) void load();
  }), []);

  async function handleGoogle() {
    setMessage("");
    setSigningIn(true);
    try {
      await signInWithGoogle();
    } catch (error) {
      if (!isPopupDismissed(error)) setMessage(authErrorMessage(error));
    } finally {
      setSigningIn(false);
    }
  }

  const usesGoogle = user?.providerData.some((provider) => provider.providerId === "google.com") ?? false;
  const application = loaded?.application ?? null;
  const role = loaded?.accountRole ?? "";

  let body;
  if (!authReady || (user && !loaded && loading)) {
    body = <div className="application-status"><p>Yükleniyor…</p></div>;
  } else if (!user) {
    body = (
      <>
        <button className="secondary-button google-button" type="button" onClick={() => void handleGoogle()} disabled={signingIn}>
          {signingIn ? "Google açılıyor…" : "Google ile devam et"}
        </button>
        <p className="field-help">
          Topluluğun kendi Google hesabını kullanın (ör. toplulugunuz@gmail.com). Good4 uygulamasında öğrenci olarak
          kullandığınız hesapla başvuru yapılamaz; onaylandığında panele de bu hesapla gireceksiniz.
        </p>
      </>
    );
  } else if (!usesGoogle) {
    body = (
      <div className="application-status">
        <h2>Google hesabı gerekli</h2>
        <p>Başvuru yalnızca Google hesabıyla yapılabilir. Çıkış yapıp topluluğun Google hesabıyla devam edin.</p>
      </div>
    );
  } else if (!loaded) {
    body = null;
  } else if (role === "student") {
    body = (
      <div className="application-status">
        <h2>Bu hesap öğrenci hesabı</h2>
        <p>
          Bu Google hesabı Good4 uygulamasında öğrenci olarak kullanılıyor. Öğrenci hesabınızı korumak için
          topluluğun kendi Google hesabıyla başvurun.
        </p>
      </div>
    );
  } else if (role) {
    body = (
      <div className="application-status">
        <h2>{application?.status === "approved" ? "Başvurunuz onaylandı" : "Bu hesap zaten panele bağlı"}</h2>
        <p>Yönetim paneline bu Google hesabıyla giriş yapabilirsiniz.</p>
        <a className="primary-button" href="/">Panele git</a>
      </div>
    );
  } else if (application?.status === "pending" && !editing) {
    body = (
      <div className="application-status">
        <h2>Başvurunuz inceleniyor</h2>
        <p>
          <strong>{application.communityName}</strong> başvurunuz Good4 ekibine ulaştı. Onaylandığında bu Google
          hesabıyla yönetim paneline giriş yapabileceksiniz.
        </p>
        <button className="quiet-button application-edit" type="button" onClick={() => setEditing(true)}>Başvuruyu düzenle</button>
      </div>
    );
  } else {
    body = (
      <>
        {application?.status === "rejected" && (
          <div className="inline-message inline-message--error" role="status">
            Önceki başvurunuz onaylanmadı{application.rejectionReason ? `: ${application.rejectionReason}` : "."} Bilgileri
            güncelleyip yeniden gönderebilirsiniz.
          </div>
        )}
        <ApplicationForm user={user} previous={application} onSubmitted={load} />
      </>
    );
  }

  return (
    <main className="login-shell">
      <section className="login-form-area">
        <div className="login-card application-card">
          <div className="login-card__brand"><BrandMark /></div>
          <h1>Topluluk başvurusu</h1>
          <p className="card-intro">
            Topluluğunuzu Good4'e ekleyin. Başvurunuz onaylandığında etkinliklerinizi yönetim panelinden yayınlayabilir,
            öğrencilere uygulamada ulaşabilirsiniz.
          </p>
          {user && (
            <div className="application-account">
              <span>{user.email}</span>
              <button className="text-button" type="button" onClick={() => void signOut(auth)}>Farklı hesap</button>
            </div>
          )}
          {body}
          {message && <div className="inline-message inline-message--error" role="alert">{message}</div>}
          <p className="support-copy">Zaten onaylı bir hesabınız var mı? <a href="/">Panele giriş yapın</a>.</p>
        </div>
      </section>
    </main>
  );
}

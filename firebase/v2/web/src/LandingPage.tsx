import { useEffect, type ReactNode } from "react";
import { APP_STORE_URL, AppleIcon, GOOGLE_PLAY_URL, PlayIcon } from "./DownloadPage";
import "./LandingPage.css";

type Device = "iphone" | "pixel";

type Feature = {
  id: string;
  eyebrow: string;
  title: string;
  body: string;
  points: string[];
  device: Device;
  // Base name of public/landing/<name>@{1,2,3}x.webp, or null while the screenshot is missing.
  screen: string | null;
  tint: string;
};

const FEATURES: Feature[] = [
  {
    id: "ders-programi",
    eyebrow: "DERS PROGRAMI",
    title: "Bölümünü seç, haftan hazır.",
    body: "Resmî ders programın bölüm ve sınıfına göre Good4'ta. Hangi ders, hangi saatte, hangi derslikte; tek bakışta.",
    points: ["Hafta hafta gezin", "Derslik ve öğretim üyesi bilgisi", "Resmî programa tek dokunuşla ulaş"],
    device: "iphone",
    screen: "ios-schedule",
    tint: "#dcebf7",
  },
  {
    id: "yemek",
    eyebrow: "GÜNÜN MENÜSÜ",
    title: "Bugün yemekhanede ne var?",
    body: "KYK ve merkezi yemekhane menüsü ana sayfanda. Kahvaltıdan akşam yemeğine, kapıya gitmeden önce bil.",
    points: ["Kahvaltı, öğle ve akşam", "KYK ve kampüs yemekhanesi", "Ana sayfada, uygulamayı açar açmaz"],
    device: "iphone",
    screen: "ios-dining",
    tint: "#e8f5c5",
  },
  {
    id: "topluluklar",
    eyebrow: "TOPLULUKLAR",
    title: "Kulübünü takip et, hiçbir etkinliği kaçırma.",
    body: "Kampüsündeki toplulukları keşfet, etkinliklere tek dokunuşla kaydol. Girişte QR biletini göster, topluluk kuponlarıyla anlaşmalı işletmelerde indirim kazan.",
    points: ["Etkinlik kaydı ve QR bilet", "Topluluğa özel kuponlar", "Kategoriye göre keşfet"],
    device: "iphone",
    screen: "ios-communities",
    tint: "#d7f1e6",
  },
  {
    id: "kampus-dolabi",
    eyebrow: "KAMPÜS DOLABI",
    title: "Kampüsün kendi ikinci el pazarı.",
    body: "Kitabını, notunu, eşyanı kampüsteki öğrencilere sat ya da ücretsiz ver. Okul e-postasıyla doğrulanmış öğrenciler arasında, telefon numarası paylaşmadan.",
    points: ["Teklif ver, uygulama içinden yazış", "Okul e-postasıyla doğrulanmış öğrenciler", "İlanlar 30 gün yayında, istersen uzat"],
    device: "iphone",
    screen: "ios-closet",
    tint: "#fbe3ec",
  },
  {
    id: "kampus-haritasi",
    eyebrow: "KAMPÜS HARİTASI",
    title: "Kampüste kaybolmak yok.",
    body: "Fakülteni, kütüphaneyi, yemekhaneyi ve ATM'yi haritada bul, tek dokunuşla yol tarifi al.",
    points: ["Fakülte, kütüphane, yemekhane ve ATM araması", "Kategoriye göre filtrele", "Bulunduğun yerden yol tarifi"],
    device: "iphone",
    screen: "ios-map",
    tint: "#dcefe4",
  },
];

const MORE_FEATURES = [
  { title: "Akademik Takvim", body: "Sınav haftası, kayıt yenileme ve tatiller tek takvimde." },
  { title: "Kampüs havası", body: "Kampüsündeki anlık hava durumu ana sayfanda." },
  { title: "Bildirimler", body: "Takip ettiğin topluluklardan ve ilanına gelen tekliflerden anında haberdar ol." },
  { title: "Askıda Yemek · Yakında", body: "Kampüs çevresindeki işletmelerin öğrencilere ısmarladığı yemekler." },
  { title: "Sana göre ana sayfa", body: "Kartları sırala, en çok kullandığın özellik en üstte dursun." },
];

const FAQ = [
  { q: "Good4 ücretli mi?", a: "Hayır. Good4 öğrenciler için tamamen ücretsiz; App Store ve Google Play'den indirebilirsin." },
  { q: "Hangi üniversitelerde var?", a: "Good4 kampüs kampüs büyüyor. Uygulamayı indirip üniversiteni seçtiğinde kampüsüne ait ders programı, menü ve toplulukları görürsün." },
  { q: "Neden okul e-postamı doğrulamam gerekiyor?", a: "Kampüs Dolabı yalnızca gerçek öğrencilere açık. Okul e-postan sadece öğrenci olduğunu doğrulamak için kullanılır, ilanlarda adın maskelenerek görünür." },
  { q: "Topluluğumu Good4'a nasıl eklerim?", a: "Topluluk başvuru formunu doldur; onaylandıktan sonra topluluk panelinden etkinlik ve kupon oluşturabilirsin." },
  { q: "Hesabımı silebilir miyim?", a: "Evet. Uygulama içinden ya da hesap silme sayfasından hesabını ve verilerini kalıcı olarak silebilirsin." },
];

function StoreButtons({ compact = false }: { compact?: boolean }) {
  return (
    <div className={`landing-stores${compact ? " landing-stores--compact" : ""}`} aria-label="Uygulama mağazaları">
      <a className="landing-store" href={APP_STORE_URL}>
        <AppleIcon />
        <span><small>iPhone için</small><strong>App Store'dan indir</strong></span>
      </a>
      <a className="landing-store" href={GOOGLE_PLAY_URL}>
        <PlayIcon />
        <span><small>Android için</small><strong>Google Play'den indir</strong></span>
      </a>
    </div>
  );
}

/** Screens are exported at their exact display size (tools/landing-demo/export-screens.py). */
function screenSources(name: string) {
  return {
    src: `/landing/${name}@2x.webp`,
    srcSet: `/landing/${name}@1x.webp 1x, /landing/${name}@2x.webp 2x, /landing/${name}@3x.webp 3x`,
  };
}

function Screen({ src, alt, label, tint }: { src: string | null; alt: string; label: string; tint: string }) {
  if (src) return <img className="device__screen-img" {...screenSources(src)} alt={alt} loading="lazy" decoding="async" />;
  return (
    <div className="device__placeholder" style={{ background: tint }} role="img" aria-label={alt}>
      <img src="/good4-logo.png" alt="" />
      <strong>{label}</strong>
      <span>Demo ekran görüntüsü eklenecek</span>
    </div>
  );
}

function PhoneFrame({ device, children, className = "" }: { device: Device; children: ReactNode; className?: string }) {
  return (
    <div className={`device device--${device} ${className}`}>
      <div className="device__body">
        <div className="device__screen">
          {children}
          <span className="device__camera" aria-hidden="true" />
        </div>
      </div>
    </div>
  );
}

function useReveal() {
  useEffect(() => {
    const items = document.querySelectorAll<HTMLElement>("[data-reveal]");
    if (!("IntersectionObserver" in window)) {
      items.forEach((item) => item.classList.add("is-visible"));
      return;
    }
    const observer = new IntersectionObserver((entries) => {
      entries.forEach((entry) => {
        if (entry.isIntersecting) {
          entry.target.classList.add("is-visible");
          observer.unobserve(entry.target);
        }
      });
    }, { threshold: 0.18 });
    items.forEach((item) => observer.observe(item));
    return () => observer.disconnect();
  }, []);
}

export default function LandingPage() {
  useEffect(() => {
    document.title = "Good4 | Kampüs hayatın tek uygulamada";
    const description = document.querySelector<HTMLMetaElement>('meta[name="description"]');
    if (description) {
      description.content = "Good4: ders programı, günün menüsü, topluluklar, Kampüs Dolabı ve kampüs haritası tek uygulamada. App Store ve Google Play'den ücretsiz indir.";
    }
  }, []);
  useReveal();

  return (
    <div className="landing">
      <header className="landing-nav">
        <a className="landing-nav__brand" href="/">
          <img src="/good4-logo.png" alt="" />
          <span>Good4</span>
        </a>
        <nav aria-label="Sayfa bölümleri">
          <a href="#ozellikler">Özellikler</a>
          <a href="#topluluklar-icin">Topluluklar için</a>
          <a href="#sss">SSS</a>
        </nav>
        <a className="landing-nav__cta" href="#indir">İndir</a>
      </header>

      <main>
        <section className="landing-hero">
          <div className="landing-hero__glow" aria-hidden="true" />
          <div className="landing-hero__copy">
            <p className="landing-eyebrow">ÜNİVERSİTE ÖĞRENCİLERİ İÇİN</p>
            <h1>Kampüs hayatın<br /><span>tek uygulamada.</span></h1>
            <p className="landing-hero__intro">
              Ders programın, günün menüsü, toplulukların ve kampüsün ikinci el pazarı cebinde. iPhone ve Android'de ücretsiz.
            </p>
            <StoreButtons />
          </div>
          <div className="landing-hero__devices" aria-hidden="true">
            <PhoneFrame device="pixel" className="landing-hero__back">
              <Screen src="ios-schedule" alt="" label="Ders programı" tint="#dcebf7" />
            </PhoneFrame>
            <PhoneFrame device="iphone" className="landing-hero__front">
              <img className="device__screen-img" {...screenSources("ios-home")} alt="" fetchPriority="high" />
            </PhoneFrame>
          </div>
        </section>

        <section className="landing-features" id="ozellikler" aria-label="Özellikler">
          {FEATURES.map((feature, index) => (
            <article
              key={feature.id}
              id={feature.id}
              className={`landing-feature${index % 2 ? " landing-feature--flip" : ""}`}
              data-reveal
            >
              <div className="landing-feature__copy">
                <p className="landing-eyebrow">{feature.eyebrow}</p>
                <h2>{feature.title}</h2>
                <p>{feature.body}</p>
                <ul>
                  {feature.points.map((point) => <li key={point}>{point}</li>)}
                </ul>
              </div>
              <div className="landing-feature__stage" style={{ ["--tint" as string]: feature.tint }}>
                <PhoneFrame device={feature.device}>
                  <Screen
                    src={feature.screen}
                    alt={`Good4 ${feature.eyebrow.toLocaleLowerCase("tr")} ekranı`}
                    label={feature.eyebrow}
                    tint={feature.tint}
                  />
                </PhoneFrame>
              </div>
            </article>
          ))}
        </section>

        <section className="landing-more" aria-labelledby="landing-more-title" data-reveal>
          <p className="landing-eyebrow">VE DAHA FAZLASI</p>
          <h2 id="landing-more-title">Kampüste ihtiyacın olan her şey.</h2>
          <div className="landing-more__grid">
            {MORE_FEATURES.map((item) => (
              <div className="landing-more__item" key={item.title}>
                <h3>{item.title}</h3>
                <p>{item.body}</p>
              </div>
            ))}
          </div>
        </section>

        <section className="landing-trust" aria-labelledby="landing-trust-title" data-reveal>
          <h2 id="landing-trust-title">Öğrenciler için, öğrencilerle güvenli.</h2>
          <div className="landing-trust__grid">
            <div><strong>Okul e-postasıyla doğrulama</strong><p>Kampüs Dolabı yalnızca okul e-postasını doğrulamış öğrencilere açık.</p></div>
            <div><strong>Adın maskelenir</strong><p>İlanlarda tam adın değil, maskelenmiş hâli görünür; telefon numarası paylaşman gerekmez.</p></div>
            <div><strong>Kontrol sende</strong><p>İstemediğin topluluğu ya da kullanıcıyı engelle, hesabını istediğin zaman sil.</p></div>
          </div>
        </section>

        <section className="landing-community" id="topluluklar-icin" aria-labelledby="landing-community-title" data-reveal>
          <div>
            <p className="landing-eyebrow">TOPLULUKLAR İÇİN</p>
            <h2 id="landing-community-title">Topluluğunu kampüsün cebine taşı.</h2>
            <p>
              Etkinlik oluştur, kayıtları takip et, girişte QR biletleri okut. Anlaşmalı işletmelerle üyelerine özel kuponlar sun. Hepsi tek panelden.
            </p>
            <ul>
              <li>Etkinlik ve kayıt yönetimi</li>
              <li>QR ile etkinlik girişi</li>
              <li>İşletmelerle topluluk kuponları</li>
            </ul>
            <a className="landing-button" href="/topluluk-basvuru">Topluluk başvurusu yap</a>
          </div>
        </section>

        <section className="landing-faq" id="sss" aria-labelledby="landing-faq-title">
          <h2 id="landing-faq-title">Sık sorulan sorular</h2>
          {FAQ.map((item) => (
            <details key={item.q}>
              <summary>{item.q}</summary>
              <p>{item.a}</p>
            </details>
          ))}
        </section>

        <section className="landing-final" id="indir" aria-labelledby="landing-final-title">
          <img src="/good4-logo.png" alt="" />
          <h2 id="landing-final-title">Kampüs hayatını Good4 ile kolaylaştır.</h2>
          <p>Ücretsiz indir, üniversiteni seç, hemen başla.</p>
          <StoreButtons compact />
        </section>
      </main>

      <footer className="landing-footer">
        <span>© {new Date().getFullYear()} Good4</span>
        <nav aria-label="Yasal">
          <a href="/gizlilik-politikasi">Gizlilik Politikası</a>
          <a href="/uyelik-sozlesmesi">Üyelik Sözleşmesi</a>
          <a href="/hesabimi-sil">Hesabımı Sil</a>
          <a href="/topluluk-basvuru">Topluluk Başvurusu</a>
        </nav>
      </footer>
    </div>
  );
}

import { useEffect } from "react";

type PrivacyDataGroup = {
  title: string;
  data: string;
  purpose: string;
  basis: string;
};

type PrivacySection = {
  title: string;
  paragraphs: string[];
  dataGroups?: PrivacyDataGroup[];
};

const processingGroups: PrivacyDataGroup[] = [
  {
    title: "Hesap ve iletişim bilgileri",
    data: "Ad ve soyad, e-posta adresi, Good4 kullanıcı numarası, hesap ve doğrulama durumu, sizin eklediğiniz telefon numarası ve seçtiğiniz giriş yöntemine ait kimlik doğrulama bilgileri. Şifre ile girişte parola kimlik doğrulama hizmetince işlenir; Good4 profilinde okunabilir metin olarak tutulmaz.",
    purpose: "Hesap açmak ve yönetmek, kimliğinizi doğrulamak, hesabınızla ilgili bildirimleri iletmek ve destek taleplerini yanıtlamak.",
    basis: "Hesap ve hizmet sözleşmesinin kurulması veya ifası için gerekli olması (KVKK m. 5/2-c). Güvenlik kayıtları bakımından, temel hak ve özgürlüklerinize zarar vermemek kaydıyla meşru menfaat (m. 5/2-f).",
  },
  {
    title: "Eğitim ve işletme bilgileri",
    data: "Öğrenci hesabında üniversite ve seçtiğiniz profil bilgileri; işletme hesabında yetkili kişinin iletişim bilgileri ile işletme adı, telefonu ve adresi gibi işletme hesabına eklenen bilgiler.",
    purpose: "Hesap türünü ve ilgili kampüs/işletme hizmetlerini belirlemek, işletme hesabını yönetmek ve seçtiğiniz hizmetleri sunmak.",
    basis: "İlgili hizmet sözleşmesinin kurulması veya ifası için gerekli olması (KVKK m. 5/2-c). Kanuni bir yükümlülük doğarsa, o yükümlülükle sınırlı olarak m. 5/2-ç.",
  },
  {
    title: "Etkinlik ve teklif işlemleri",
    data: "Takip ettiğiniz topluluklar, etkinlik kayıt ve katılımı, QR/check-in durumu ve kullandığınız sürümde sunulan özelliklere bağlı olarak rezervasyon, teklif veya kupon talebi ve kullanımına ilişkin işlem kayıtları.",
    purpose: "Seçtiğiniz etkinlik ve teklifleri sunmak, katılımı veya kullanımı doğrulamak, işlemin durumunu göstermek ve anlaşmazlıkları incelemek.",
    basis: "Talep ettiğiniz hizmetin sunulması için sözleşmenin kurulması veya ifası (KVKK m. 5/2-c); gerekli olduğu ölçüde bir hakkın tesisi, kullanılması veya korunması (m. 5/2-e).",
  },
  {
    title: "Geri bildirim ve destek içerikleri",
    data: "Bize gönderdiğiniz konu ve mesajlar ile bunlarla ilişkilendirilen hesap kimliği, ad ve e-posta adresi.",
    purpose: "Talebinizi incelemek, size dönüş yapmak, hizmet sorunlarını gidermek ve benzer sorunları önlemek.",
    basis: "Talebinizi karşılamak ve hizmeti yürütmek için gerekli olması (KVKK m. 5/2-c) veya temel hak ve özgürlüklerinize zarar vermemek kaydıyla hizmet güvenliği ve kalitesine ilişkin meşru menfaat (m. 5/2-f). Bir uyuşmazlıkta gerekli olursa m. 5/2-e.",
  },
  {
    title: "Cihaz ve güvenlik kayıtları",
    data: "Uygulama/web sürümü, işletim sistemi ve cihaz/tarayıcı bilgileri; bağlantı, oturum, hata ve güvenlik kayıtları. IP adresi gibi bazı teknik kayıtlar hizmet sağlayıcılar tarafından bağlantı sırasında üretilebilir.",
    purpose: "Hizmeti çalıştırmak, oturum güvenliğini sağlamak, kötüye kullanımı önlemek ve teknik hataları araştırmak.",
    basis: "Hizmet güvenliği ve kötüye kullanımın önlenmesine yönelik meşru menfaat (KVKK m. 5/2-f); kanuni zorunluluk bulunması hâlinde m. 5/2-ç.",
  },
  {
    title: "Mobil analitik ve çökme raporları",
    data: "Hedeflenen Good4 V2 Android ve iOS yayın paketlerinde Firebase Analytics ve Firebase Crashlytics SDK'ları bulunmaz; bu SDK'lar aracılığıyla uygulama etkileşimi veya çökme raporu toplanmaz ya da gönderilmez.",
    purpose: "Bu sürümde analitik ve çökme raporu toplama amacıyla veri işlenmez.",
    basis: "Mevcut sürümde bu veri işleme faaliyeti bulunmadığından bu kategori için hukuki sebep uygulanmaz. SDK'lar yeniden eklenmeden önce amaç, hukuki sebep ve gerekiyorsa kullanıcı tercihi belirlenerek metin güncellenmelidir.",
  },
];

const sections: PrivacySection[] = [
  {
    title: "1. Veri sorumlusu ve iletişim",
    paragraphs: [
      "Good4 mobil uygulaması ve Good4 V2 web hizmetleri kapsamında kişisel verilerinizin veri sorumlusu Ahmetcan Kılınç'tır.",
      "Adres: Ahatlı Mah. 3174 Sk. No: 6 İç Kapı No: 9, Kepez / Antalya. E-posta: cannklnc7@gmail.com.",
    ],
  },
  {
    title: "2. İşlenen veriler, amaçlar ve hukuki sebepler",
    paragraphs: [
      "Aşağıdaki bilgiler, Good4'ta kullandığınız hesap türüne ve tercih ettiğiniz özelliklere göre işlenir. Her veri, yalnızca ilgili olduğu amaç için ve tabloda belirtilen hukuki işleme şartına dayanılarak kullanılır; bir satırdaki bütün hukuki sebepler her işlem için birlikte uygulanmaz.",
    ],
    dataGroups: processingGroups,
  },
  {
    title: "3. Verileri nasıl elde ediyoruz?",
    paragraphs: [
      "Veriler; kayıt ve profil alanlarına sizin tarafınızdan girilmesi, uygulama/web hizmetini kullanmanız, destek veya geri bildirim göndermeniz ve Google ile giriş seçeneğini kullanmanız yoluyla doğrudan elde edilir. Google ile girişte kimlik doğrulaması için gerekli hesap tanımlayıcıları ve Google tarafından sağlanan temel hesap bilgileri kullanılır; Good4, Gmail, Drive, takvim veya kişiler içeriğine erişim istemez.",
      "Uygulama ve altyapı sağlayıcıları hizmetin çalışması sırasında oturum, bağlantı, cihaz ve güvenlik kayıtları oluşturabilir. Hedeflenen Good4 V2 mobil paketlerinde uygulama analitiği ve çökme tanılama SDK'ları bulunmaz. İsteğe bağlı profil alanlarını doldurmamanız, ilgili alanı kullanan bazı özelliklere erişiminizi etkileyebilir.",
    ],
  },
  {
    title: "4. Veriler kimlerle ve hangi amaçla paylaşılır?",
    paragraphs: [
      "Altyapı, kimlik doğrulama, veri saklama ve sunucu işlevleri için veriler hizmet sağlayıcısı sıfatıyla Google Firebase/Google Cloud hizmetlerine (Firebase Authentication, Firestore, Storage ve Cloud Functions gibi) iletilebilir. Hedeflenen Good4 V2 mobil paketlerinde Firebase Analytics ve Crashlytics SDK'ları bulunmaz. Google bu verileri Good4 adına hizmeti işletmek, saklamak ve güvenliğini sağlamak için işler.",
      "Bir etkinliğe kaydolduğunuzda, katıldığınız topluluk veya etkinlik düzenleyicisi katılımı yönetmek için gerekli sınırlı bilgilere (örneğin görünen ad, kayıt ve check-in durumu) erişebilir. Bir teklif ya da kupon kullandığınızda, ilgili topluluk veya işletme işlemi doğrulamak için gerekli işlem bilgilerini görebilir. Bu gruplar sizin kullanmadığınız özelliklere ilişkin verileri bu kapsamda almaz.",
      "Kişisel veriler, hukuki yükümlülük veya usulüne uygun bir talep bulunması hâlinde yetkili kamu kurumları ve yargı mercileriyle paylaşılabilir. Veriler satılmaz ve reklam amacıyla üçüncü kişilere kiralanmaz.",
    ],
  },
  {
    title: "5. Yurt dışına aktarım",
    paragraphs: [
      "Good4 V2 projesinin Firebase Console ayarlarında varsayılan Firestore veritabanı eur3 (Avrupa çoklu bölge), varsayılan Cloud Storage depolama alanı US-EAST1 (ABD) ve dağıtılmış Cloud Functions işlevleri europe-west1 (Belçika) konumundadır. Firebase'in ürün açıklamasına göre Firebase Authentication yalnızca ABD veri merkezlerinde çalışır ve verileri ABD'de işler. Bu konumlar ilgili hizmetlere aittir; bütün altyapı, destek erişimi veya alt işleyenlerin yalnızca bu konumlarda çalıştığı anlamına gelmez.",
      "Bu hizmetler nedeniyle kişisel veriler Türkiye dışına aktarılır. Aktarım için KVKK'nın 9. maddesindeki uygun mekanizmanın Good4 ile ilgili Google tüzel kişisi arasında kurulması gerekir. Good4 hesabına uygulanacak Google Cloud Veri İşleme Eki'nin kabul kaydı, Türk standart sözleşmesinin tarafları ve imzaları ile KVKK bildirim kaydı henüz doğrulanamamıştır. Bu kayıtlar teyit edilmeden metin nihai sürüm sayılmamalıdır.",
    ],
  },
  {
    title: "6. Saklama ve güvenlik",
    paragraphs: [
      "Kişisel veriler, ilgili hesabı ve seçtiğiniz hizmetleri sunmak için gerekli olduğu süre boyunca saklanır. Hesap kapandıktan sonra, başka bir işleme sebebi kalmayan veriler silinir, yok edilir veya anonim hâle getirilir. Kanuni saklama yükümlülüğü veya bir hakkın korunması için gerekli işlem kayıtları yalnızca ilgili süre ve amaçla sınırlı tutulabilir.",
      "Good4; yetkisiz erişim, kayıp, değişiklik ve kötüye kullanım risklerini azaltmak için erişim yetkileri, kimlik doğrulama ve altyapı güvenlik kontrolleri gibi uygun teknik ve idari tedbirler uygular. Kişisel veri ihlali şüphesi fark ederseniz bize e-posta yoluyla bildirebilirsiniz.",
      "Uygulanan kesin saklama süreleri ve hesap kapatma sonrasında ilişkili işlem kayıtlarının silinme veya anonimleştirilme takvimi, hizmetin kayıt ve imha planıyla birlikte ayrıca belirlenip işletilmelidir.",
    ],
  },
  {
    title: "7. Çerezler ve cihazdaki yerel depolama",
    paragraphs: [
      "Good4 V2 web uygulamasında oturumunuzu tarayıcıyı kapattıktan sonra da açık tutabilmek için Firebase Authentication'ın yerel oturum saklama özelliği kullanılır. Bu oturum bilgisi tarayıcının localStorage alanında saklanır ve web hesabına giriş için gereklidir.",
      "Good4 V2 web uygulamasında reklam takibi amacıyla kullanılan bir çerez yapılandırması bulunmamaktadır. Hedeflenen Good4 V2 mobil paketlerinde Firebase Analytics ve Firebase Crashlytics SDK'ları bulunmaz. Tarayıcı ayarlarından site verilerini silebilir ya da engelleyebilirsiniz; bu durumda web oturumunuz kapanabilir veya tekrar giriş yapmanız gerekebilir.",
    ],
  },
  {
    title: "8. KVKK kapsamındaki haklarınız ve başvuru",
    paragraphs: [
      "6698 sayılı Kişisel Verilerin Korunması Kanunu'nun 11. maddesi kapsamında kişisel verilerinizin işlenip işlenmediğini öğrenme; işlenmişse bilgi talep etme; işleme amacını ve amaca uygun kullanımı öğrenme; yurt içinde veya yurt dışında aktarıldığı üçüncü kişileri bilme; eksik veya yanlış işlenmişse düzeltilmesini isteme; kanuni şartlar oluştuğunda silinmesini veya yok edilmesini isteme; bu işlemlerin aktarıldığı üçüncü kişilere bildirilmesini talep etme; münhasıran otomatik sistemlerle analiz sonucu aleyhinize çıkan sonuca itiraz etme ve kanuna aykırı işleme nedeniyle zarara uğramanız hâlinde zararın giderilmesini talep etme haklarına sahipsiniz.",
      "Başvurunuzu, kimliğinizi ve talebinizi anlamaya yetecek bilgilerle birlikte cannklnc7@gmail.com adresine e-posta göndererek veya yukarıdaki posta adresine yazılı olarak iletebilirsiniz. Başvuru, KVKK ve ilgili mevzuattaki usul ve süreler içinde yanıtlanır. Güvenlik için kimlik doğrulaması istenebilir.",
    ],
  },
  {
    title: "9. Bu metin ve güncellemeler",
    paragraphs: [
      "Bu metin, kişisel veri işleme hakkında bilgilendirme amacı taşır; tek başına açık rıza veya sözleşme kabulü değildir. Açık rıza gerektiren ayrı bir işlem bulunursa, aydınlatmadan ayrı ve özgür iradeye dayalı bir onay istenir.",
      "Hizmetler veya mevzuat değiştikçe metin güncellenebilir. Önemli değişikliklerin yürürlük tarihi bu sayfada belirtilir.",
    ],
  },
];

export default function PrivacyPolicy() {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = "Good4 KVKK Aydınlatma Metni ve Gizlilik Politikası";
    return () => { document.title = previousTitle; };
  }, []);

  return (
    <div className="privacy-page">
      <header className="privacy-header">
        <a className="privacy-brand" href="https://good4tr.com" aria-label="Good4 ana sayfa">
          <img src="/good4-logo.png" alt="" />
          <span>Good4</span>
        </a>
        <nav className="legal-header-links" aria-label="Yasal sayfalar">
          <a href="/gizlilik-politikasi">Gizlilik Politikası</a>
          <a href="/uyelik-sozlesmesi">Üyelik Sözleşmesi</a>
          <a className="privacy-home-link" href="https://good4tr.com">Ana sayfa</a>
        </nav>
      </header>

      <main className="privacy-content">
        <div className="privacy-intro">
          <p className="privacy-eyebrow">Kişisel verileriniz</p>
          <h1>KVKK Aydınlatma Metni ve Gizlilik Politikası</h1>
          <p>
            Good4'un hangi kişisel verileri hangi amaçlarla kullandığını, kimlerle
            paylaşabildiğini ve KVKK kapsamındaki haklarınızı burada bulabilirsiniz.
          </p>
          <time dateTime="2026-09-24">Son güncelleme: 24 Eylül 2026</time>
          <p className="privacy-review-note">
            Bu çalışma sürümüdür. Google ile aktarım mekanizması ve dağıtılmış
            hesap silme ve saklama akışı doğrulanmadan kamuya açık nihai metin olarak yayımlanmamalıdır.
          </p>
        </div>

        <div className="privacy-layout">
          <nav className="privacy-nav" aria-label="Sayfa içeriği">
            <strong>İçindekiler</strong>
            {sections.map((section, index) => (
              <a key={section.title} href={`#bolum-${index + 1}`}>{section.title}</a>
            ))}
          </nav>

          <article className="privacy-document">
            {sections.map((section, index) => (
              <section id={`bolum-${index + 1}`} key={section.title}>
                <h2>{section.title}</h2>
                {section.paragraphs.map((paragraph) => <p key={paragraph}>{paragraph}</p>)}
                {section.dataGroups && (
                  <div className="privacy-data-groups">
                    {section.dataGroups.map((group) => (
                      <article className="privacy-data-group" key={group.title}>
                        <h3>{group.title}</h3>
                        <p><strong>Veri örnekleri: </strong>{group.data}</p>
                        <p><strong>Amaç: </strong>{group.purpose}</p>
                        <p><strong>Hukuki sebep: </strong>{group.basis}</p>
                      </article>
                    ))}
                  </div>
                )}
              </section>
            ))}
          </article>
        </div>
      </main>

      <footer className="privacy-footer">
        <span>© 2026 Good4</span>
        <a href="/hesabimi-sil">Hesap silme</a>
        <a href="/gizlilik-politikasi">Gizlilik Politikası</a>
        <a href="/uyelik-sozlesmesi">Üyelik Sözleşmesi</a>
        <a href="mailto:cannklnc7@gmail.com">İletişim</a>
      </footer>
    </div>
  );
}

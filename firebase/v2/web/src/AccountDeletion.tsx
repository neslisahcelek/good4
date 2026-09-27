import { useEffect } from "react";

const deletionEmail = "cannklnc7@gmail.com";
const deletionRequestUrl = `mailto:${deletionEmail}?subject=${encodeURIComponent("Good4 hesap silme talebi")}`;

export default function AccountDeletion() {
  useEffect(() => {
    const previousTitle = document.title;
    document.title = "Good4 Hesap Silme";
    return () => { document.title = previousTitle; };
  }, []);

  return (
    <div className="privacy-page">
      <header className="privacy-header">
        <a className="privacy-brand" href="https://good4tr.com" aria-label="Good4 ana sayfa">
          <img src="/good4-logo.png" alt="" />
          <span>Good4</span>
        </a>
        <nav className="legal-header-links" aria-label="İlgili sayfalar">
          <a href="/gizlilik">KVKK ve Gizlilik</a>
          <a className="privacy-home-link" href="https://good4tr.com">Ana sayfa</a>
        </nav>
      </header>

      <main className="privacy-content">
        <div className="privacy-intro">
          <p className="privacy-eyebrow">Hesap yönetimi</p>
          <h1>Good4 hesabımı nasıl silebilirim?</h1>
          <p>Hesabınızı uygulamadan silebilir veya uygulamaya erişemiyorsanız silme talebinizi e-posta ile iletebilirsiniz.</p>
        </div>

        <article className="privacy-document">
          <section>
            <h2>Uygulama içinden silme</h2>
            <p>Good4 uygulamasında hesabınıza giriş yapın. Hesap Ayarları &gt; Hesabımı sil yolunu izleyin ve ekrandaki onayı tamamlayın. Bu işlem geri alınamaz.</p>
          </section>
          <section>
            <h2>Uygulamaya erişemiyorsanız</h2>
            <p>Uygulamayı yeniden yüklemeniz gerekmez. Hesap silme talebinizi aşağıdaki adrese, mümkünse Good4 hesabınızla ilişkili e-posta adresinizden gönderin. Talepte hesabınızı belirlememize yetecek bilgiyi ve hesabınızı silmek istediğinizi belirtin.</p>
            <p><a href={deletionRequestUrl}>Good4 hesap silme talebi gönder</a> · <a href={`mailto:${deletionEmail}`}>{deletionEmail}</a></p>
            <p>Parolanızı, doğrulama kodunuzu, kimlik numaranızı veya Firebase oturum bilgilerinizi e-posta ile göndermeyin. Hesabın size ait olduğunu doğrulamak için güvenli ek adımlar gerekebilir.</p>
          </section>
          <section>
            <h2>Verilerinize ne olur?</h2>
            <p>Hesabınız ve doğrudan hesabınıza bağlı kişisel kayıtlar silinir. Paylaşılan etkinlik, kampanya, işlem ve güvenlik geçmişindeki hesap tanımlayıcıları gerektiğinde anonimleştirilir. Hukuki veya güvenlik gerekçesiyle saklanması gereken kayıtlar ilgili amaç ve süreyle sınırlı olarak tutulabilir.</p>
            <p>Ayrıntılar için <a href="/gizlilik">KVKK Aydınlatma Metni ve Gizlilik Politikası</a> sayfasına bakabilirsiniz.</p>
          </section>
        </article>
      </main>

      <footer className="privacy-footer">
        <span>© 2026 Good4</span>
        <a href={`mailto:${deletionEmail}`}>İletişim</a>
      </footer>
    </div>
  );
}

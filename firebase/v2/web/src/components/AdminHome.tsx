import type { AdminSection } from "./CommunityUi";
import { ActionCard, HeroBand, SummaryCard } from "./PanelBlocks";

/** Admin landing page: greeting, today's queues and shortcuts. Counts come from the dashboard already loaded. */
export function AdminHome({ displayName, counts, totals, onNavigate }: {
  displayName: string;
  counts: { coupons: number; feedback: number; communityApplications: number; businessApplications: number };
  totals: { businesses: number; communities: number };
  onNavigate: (section: AdminSection) => void;
}) {
  const firstName = displayName.trim().split(/\s+/)[0] || "Yönetici";
  const today = new Intl.DateTimeFormat("tr-TR", { weekday: "long", day: "numeric", month: "long" }).format(new Date());
  const waiting = counts.coupons + counts.feedback + counts.communityApplications + counts.businessApplications;
  const firstQueue: AdminSection | null = counts.coupons ? "coupons" : counts.feedback ? "feedback"
    : counts.communityApplications ? "communities" : counts.businessApplications ? "businesses" : null;

  return (
    <div className="admin-home">
      <header className="admin-home__greeting">
        <h1>Merhaba {firstName}</h1>
        <p>{today}</p>
      </header>

      <HeroBand
        eyebrow="Bugün seni bekleyenler"
        title={waiting > 0 ? `${waiting} iş onayını bekliyor` : "Bekleyen iş yok, her şey yolunda"}
        actions={firstQueue ? (
          <button type="button" className="pill-button" onClick={() => onNavigate(firstQueue)}>İncelemeye başla</button>
        ) : (
          <button type="button" className="pill-button" onClick={() => onNavigate("menu")}>Günün menüsüne bak</button>
        )}
      >
        Kupon onayları, içerik bildirimleri, işletme ve topluluk başvuruları burada toplanır. Kampüs Dolabı ve Sosyal
        şikayetleri kendi bölümlerinde.
      </HeroBand>

      <div className="admin-home__grid">
        <div className="admin-home__column">
          <h2 className="admin-home__section-title">Onay bekleyenler</h2>
          <ActionCard illustration="coupon" title="Bekleyen kuponlar" count={counts.coupons}
            actionLabel="Kuponlara git" onAction={() => onNavigate("coupons")}>
            {counts.coupons > 0
              ? `${counts.coupons} topluluk kuponu onayını bekliyor.`
              : "Şu an onay bekleyen kupon yok. Yeni başvurular burada görünecek."}
          </ActionCard>
          <ActionCard illustration="message" title="Bildirimler ve geri bildirimler" count={counts.feedback}
            actionLabel="Bildirimleri aç" onAction={() => onNavigate("feedback")}>
            {counts.feedback > 0
              ? `${counts.feedback} açık içerik bildirimi var. Kullanıcı şikayetlerini inceleyip sonuçlandır.`
              : "Açık içerik bildirimi yok. Öğrencilerden gelen geri bildirimleri buradan okuyabilirsin."}
          </ActionCard>
          <ActionCard illustration="coupon" title="İşletme başvuruları" count={counts.businessApplications}
            actionLabel="Başvurulara bak" onAction={() => onNavigate("businesses")}>
            {counts.businessApplications > 0
              ? `${counts.businessApplications} işletme kupon doğrulamayı açmak için onay bekliyor.`
              : "Bekleyen işletme başvurusu yok."}
          </ActionCard>
          <ActionCard illustration="users" title="Topluluk başvuruları" count={counts.communityApplications}
            actionLabel="Başvurulara bak" onAction={() => onNavigate("communities")}>
            {counts.communityApplications > 0
              ? `${counts.communityApplications} topluluk Good4'a katılmak için onay bekliyor.`
              : "Bekleyen topluluk başvurusu yok."}
          </ActionCard>
          <ActionCard illustration="bag" title="Kampüs Dolabı" actionLabel="İlanları incele" onAction={() => onNavigate("market")}>
            Yayına alınmayı bekleyen ilanlar ve öğrenci şikayetleri.
          </ActionCard>
          <ActionCard illustration="spark" title="Sosyal" actionLabel="Şikayetlere bak" onAction={() => onNavigate("social")}>
            Sosyal etkinlik ve sohbet şikayetleri, ihlal kayıtları.
          </ActionCard>
        </div>

        <aside className="admin-home__column">
          <SummaryCard
            title="Platform özeti"
            hint="Yönetim panelini açtığında yüklenen güncel sayılar. Yenilemek için ilgili bölümdeki Yenile düğmesini kullan."
            rows={[
              { label: "Aktif işletme", value: totals.businesses },
              { label: "Aktif topluluk", value: totals.communities },
              { label: "Bekleyen kupon", value: counts.coupons },
              { label: "Açık içerik bildirimi", value: counts.feedback },
            ]}
          />
          <h2 className="admin-home__section-title">Hızlı işlemler</h2>
          <ActionCard compact illustration="dining" title="Günün menüsü" actionLabel="Menüyü güncelle" onAction={() => onNavigate("menu")}>
            Merkezi yemekhane menüsünü kontrol et.
          </ActionCard>
          <ActionCard compact illustration="megaphone" title="Ana sayfa reklamı" actionLabel="Reklamları yönet" onAction={() => onNavigate("ads")}>
            Uygulamanın ana sayfasındaki afişler.
          </ActionCard>
          <ActionCard compact illustration="bell" title="Telefon bildirimi" actionLabel="Bildirim gönder" onAction={() => onNavigate("notifications")}>
            Öğrencilere duyuru gönder.
          </ActionCard>
        </aside>
      </div>
    </div>
  );
}

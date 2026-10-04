// Fictional demo content for landing-page and store screenshots. Loaded only into the
// local Firebase emulators (demo-good4-v2); none of these people, clubs or businesses exist.

export const UNIVERSITY = "Akdeniz Üniversitesi";
export const EDU_DOMAIN = "akdeniz.edu.tr";

// Test-only credentials for the local Auth emulator.
export const DEMO_STUDENT = {
  email: "elif.demir@demo.edu.tr",
  password: "Good4-demo-2026!",
  displayName: "Elif Demir",
  eduEmail: "elif.demir@ogr.akdeniz.edu.tr",
};

export const SELLERS = [
  { key: "mert", email: "mert.kaya@demo.edu.tr", displayName: "Mert Kaya" },
  { key: "zeynep", email: "zeynep.aksoy@demo.edu.tr", displayName: "Zeynep Aksoy" },
  { key: "burak", email: "burak.sahin@demo.edu.tr", displayName: "Burak Şahin" },
];

export const COMMUNITIES = [
  {
    id: "fotograf", name: "Fotoğrafçılık Topluluğu", glyph: "camera", colors: ["#1d3557", "#457b9d"], followed: true,
    description: "Kampüste ve şehirde fotoğraf yürüyüşleri, sergiler ve atölyeler.", followers: 312,
  },
  {
    id: "doga", name: "Dağcılık ve Doğa Sporları", glyph: "mountain", colors: ["#2d6a4f", "#52b788"], followed: true,
    description: "Hafta sonu yürüyüşleri, kamp ve tırmanış eğitimleri.", followers: 488,
  },
  {
    id: "yazilim", name: "Yazılım ve Teknoloji Topluluğu", glyph: "code", colors: ["#3a0ca3", "#7209b7"], followed: false,
    description: "Hackathonlar, mobil ve web atölyeleri, sektör buluşmaları.", followers: 541,
  },
  {
    id: "girisim", name: "Girişimcilik Kulübü", glyph: "rocket", colors: ["#b5179e", "#f72585"], followed: false,
    description: "Fikirden ürüne: pitch geceleri, mentorluk ve kampüs girişimleri.", followers: 267,
  },
  {
    id: "gonullu", name: "Gönüllüler Topluluğu", glyph: "heart", colors: ["#c1440e", "#f4a261"], followed: false,
    description: "Sahil temizliğinden kitap bağışına kampüsün gönüllülük ağı.", followers: 395,
  },
];

// `day` is days from the seed date; times are Europe/Istanbul.
export const EVENTS = [
  {
    id: "gunbatimi", community: "fotograf", categoryId: "culture-arts", chip: "Kültür ve Sanat",
    title: "Gün Batımı Fotoğraf Yürüyüşü", when: "9 Ekim · 17:30", place: "Konyaaltı Sahili", day: 5, start: "17:30", hours: 2,
    location: "Konyaaltı Sahili, Beach Park girişi", capacity: 40, registered: 27,
    description: "Telefonun ya da fotoğraf makinenle gel; kompozisyon ve ışık üzerine kısa bir sohbetin ardından gün batımını birlikte çekiyoruz.",
  },
  {
    id: "kanyon", community: "doga", categoryId: "sports-nature", chip: "Spor ve Doğa",
    title: "Göynük Kanyonu Doğa Yürüyüşü", when: "11 Ekim · 08:00", place: "Kemer", day: 7, start: "08:00", hours: 8,
    location: "Merkezi Kütüphane önü (servis kalkışı)", capacity: 30, registered: 30,
    description: "Orta zorlukta 12 km'lik parkur. Servis, rehber ve sigorta topluluk tarafından karşılanır.",
  },
  {
    id: "kmp", community: "yazilim", categoryId: "technology", chip: "Teknoloji",
    title: "Kotlin Multiplatform Atölyesi", when: "14 Ekim · 18:00", place: "Mühendislik B-102", day: 10, start: "18:00", hours: 2,
    location: "Mühendislik Fakültesi B-102", capacity: 60, registered: 44,
    description: "Tek kod tabanıyla iOS ve Android uygulaması geliştirmeye giriş. Bilgisayarını getirmeyi unutma.",
  },
  {
    id: "pitch", community: "girisim", categoryId: "career-entrepreneurship", chip: "Kariyer ve Girişimcilik",
    title: "Kampüs Pitch Gecesi", when: "16 Ekim · 19:00", place: "Kültür Merkezi", day: 12, start: "19:00", hours: 3,
    location: "Atatürk Kültür Merkezi, Küçük Salon", capacity: 120, registered: 86,
    description: "Öğrenci girişimleri 3 dakikada fikirlerini anlatıyor, jüri ve mentorlar geri bildirim veriyor.",
  },
  {
    id: "sahil", community: "gonullu", categoryId: "volunteering", chip: "Gönüllülük",
    title: "Sahil Temizliği Buluşması", when: "18 Ekim · 10:00", place: "Lara Plajı", day: 14, start: "10:00", hours: 3,
    location: "Lara Halk Plajı girişi", capacity: 0, registered: 58,
    description: "Eldiven ve çuvallar bizden. Temizlik sonrası birlikte kahvaltı yapıyoruz.",
  },
];

export const BUSINESSES = [
  { id: "durum", name: "Kampüs Dürüm Evi" },
  { id: "limon", name: "Limon Kafe" },
  { id: "simit", name: "Hasan Usta Simit Fırını" },
];

export const MEALS = [
  {
    id: "durum-menu", business: "durum", title: "Tavuk Dürüm + Ayran",
    description: "Lavaşta tavuk dürüm ve ayran. Kodu kasada göster, 10 dakika geçerli.", total: 30, used: 12, days: 6,
  },
  {
    id: "limon-kahvalti", business: "limon", title: "Öğrenci Kahvaltı Tabağı",
    description: "Peynir, zeytin, domates, salatalık, haşlanmış yumurta ve sınırsız çay.", total: 20, used: 9, days: 3,
  },
  {
    id: "simit-cay", business: "simit", title: "Simit + Çay",
    description: "Taze simit ve demli çay. Sabah 07:00–11:00 arası geçerli.", total: 50, used: 31, days: 10,
  },
];

// `art` picks the illustration in make-assets.mjs; prices are whole lira.
export const LISTINGS = [
  {
    id: "calculus", seller: "mert", art: "books", tint: "#d8e8f5", category: "books", condition: "good", price: 350, hoursAgo: 2,
    title: "Calculus kitap seti",
    description: "Thomas Calculus 1 ve 2, Türkçe baskı. İki kitap birlikte, birkaç sayfada kurşun kalem notu var. Kampüste elden teslim edebilirim.",
  },
  {
    id: "lamba", seller: "zeynep", art: "lamp", tint: "#fbefc8", category: "dorm", condition: "likeNew", price: 250, hoursAgo: 5,
    title: "LED masa lambası",
    description: "Yurtta bir dönem kullandım, kutusu duruyor. 3 kademeli, ışık rengi ayarlanabiliyor.",
  },
  {
    id: "buzdolabi", seller: "burak", art: "fridge", tint: "#e3ecef", category: "dorm", condition: "good", price: 2400, hoursAgo: 9,
    title: "Mini buzdolabı 45 L",
    description: "Sessiz çalışıyor, sorunsuz. Mezun oluyorum, taşıma için yardım edebilirim.",
  },
  {
    id: "kask", seller: "mert", art: "helmet", tint: "#fde0dc", category: "sports", condition: "likeNew", price: 400, hoursAgo: 20,
    title: "Bisiklet kaskı",
    description: "M beden, birkaç kez kullanıldı, çizik yok. Ayarlanabilir arka tokası var.",
  },
  {
    id: "gitar", seller: "zeynep", art: "guitar", tint: "#f6e2cc", category: "hobby", condition: "good", price: 1800, hoursAgo: 28,
    title: "Akustik gitar",
    description: "Yeni teller takıldı, kılıfı ve capo'su ile birlikte veriyorum.",
  },
  {
    id: "mont", seller: "burak", art: "jacket", tint: "#dbe6f2", category: "clothing", condition: "likeNew", price: 650, hoursAgo: 40,
    title: "Kot ceket (M)",
    description: "Az giyildi, yıkanmış ve ütülü. Ölçü için mesaj atabilirsin.",
  },
  {
    id: "notlar", seller: "mert", art: "notes", tint: "#e6f2df", category: "books", condition: "good", price: 0, hoursAgo: 52,
    title: "İktisat ders notları",
    description: "İktisada Giriş dersinin geçen dönem el yazısı notları, temiz ve düzenli. Ücretsiz veriyorum.",
  },
];

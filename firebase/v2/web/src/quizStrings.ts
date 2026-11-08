export const quizStrings = {
  // Navigation & Page headers
  navItem: "Quizler",
  pageTitle: "Quiz Yönetimi",
  pageDescription: "Topluluğun için canlı bilgi yarışmaları ve soru setleri oluştur, düzenle ve yönet.",
  newQuizButton: "Yeni Quiz Oluştur",
  backToList: "Quiz Listesine Dön",

  // List view
  listTitle: "Kayıtlı Quizler",
  listDescription: "Topluluğuna ait oluşturulmuş soru setleri. Mobil uygulamada canlı oturum başlatmak için quizlerin hazır durumda olması gerekir.",
  emptyTitle: "Henüz quiz bulunmuyor",
  emptyDescription: "Topluluğuna özel ilk quizi oluşturarak öğrencilerin katılabileceği canlı yarışmalar düzenle.",
  emptyAction: "İlk quizi oluştur",
  tableHeaderTitle: "Quiz Başlığı",
  tableHeaderQuestions: "Soru Sayısı",
  tableHeaderStatus: "Durum",
  tableHeaderUpdated: "Son Güncelleme",
  tableHeaderActions: "İşlemler",
  refreshButton: "Yenile",
  editButton: "Düzenle",
  deleteButton: "Sil",
  statusDraft: "Taslak",
  statusReady: "Hazır",
  maxQuizzesHint: "Topluluk başına en fazla 20 quiz oluşturulabilir.",

  // Editor view
  createHeading: "Yeni Quiz Oluştur",
  editHeading: "Quizi Düzenle",
  editorHint: "Quiz başlığını belirleyin, soruları ve seçenekleri ekleyin. Hazır olarak kaydettiğinizde mobil uygulamada canlı oturum başlatabilirsiniz.",
  titleLabel: "Quiz Başlığı",
  titlePlaceholder: "Örn: Genel Kültür Yarışması #1",
  titleHelp: "En fazla 80 karakter",

  questionsHeading: "Sorular",
  questionsCount: (count: number, max: number) => `${count} / ${max} soru`,
  addQuestionButton: "Soru Ekle",
  noQuestionsAdded: "Henüz hiç soru eklenmedi. 'Soru Ekle' butonuna basarak soru oluşturmaya başlayın.",
  maxQuestionsReached: "Bir quize en fazla 30 soru ekleyebilirsiniz.",

  questionNumber: (n: number) => `Soru ${n}`,
  questionTextLabel: "Soru Metni",
  questionTextPlaceholder: "Soruyu buraya yazın...",
  questionTextHelp: "En fazla 120 karakter",
  questionTypeLabel: "Soru Tipi",
  typeMultipleChoice: "Çoktan Seçmeli (2-4 şık)",
  typeTrueFalse: "Doğru / Yanlış",
  timeLimitLabel: "Süre Sınırı",
  timeLimitOption: (sec: number) => `${sec} saniye${sec === 20 ? " (varsayılan)" : ""}`,

  optionsHeading: "Seçenekler ve Doğru Cevap",
  optionsHelp: "Öğrencilere gösterilecek şıklar arasından doğru olanı radyo butonuyla işaretleyin.",
  addOptionButton: "Seçenek Ekle",
  optionPlaceholder: (index: number) => `Seçenek ${index + 1}`,
  optionHelp: "En fazla 75 karakter",
  removeOption: "Seçeneği Kaldır",
  correctOptionRadio: "Doğru seçenek",
  correctBadge: "Doğru Cevap",

  trueOptionLabel: "Doğru",
  falseOptionLabel: "Yanlış",

  moveUp: "Yukarı Taşı",
  moveDown: "Aşağı Taşı",
  deleteQuestion: "Soruyu Sil",

  // Save actions
  saveDraftButton: "Taslak Olarak Kaydet",
  saveReadyButton: "Hazır Olarak Kaydet",
  cancelButton: "Vazgeç",
  saving: "Kaydediliyor…",
  loading: "Quizler yükleniyor…",
  deleting: "Siliniyor…",

  // Dialogs & Feedback
  deleteDialogTitle: "Quizi Sil",
  deleteDialogDescription: (title: string) => `“${title}” quizini silmek istediğinize emin misiniz? Bu işlem geri alınamaz.`,
  deleteConfirm: "Quizi Sil",

  saveSuccessNotice: (status: "draft" | "ready") =>
    status === "ready"
      ? "Quiz başarıyla 'Hazır' olarak kaydedildi. Mobil uygulamadan canlı oturum başlatılabilir."
      : "Quiz başarıyla 'Taslak' olarak kaydedildi.",
  deleteSuccessNotice: "Quiz başarıyla silindi.",

  // Validation messages
  validationTitleRequired: "Lütfen quiz başlığı girin.",
  validationTitleTooLong: "Quiz başlığı en fazla 80 karakter olabilir.",
  validationQuestionsRequiredForReady: "Quizi 'Hazır' olarak kaydetmek için en az bir soru eklemelisiniz.",
  validationMaxQuestions: "Bir quize en fazla 30 soru ekleyebilirsiniz.",
  validationQuestionTextRequired: (n: number) => `${n}. sorunun metni boş bırakılamaz.`,
  validationQuestionTextTooLong: (n: number) => `${n}. sorunun metni en fazla 120 karakter olabilir.`,
  validationOptionsMin: (n: number) => `${n}. soru için en az 2 seçenek girilmelidir.`,
  validationOptionEmpty: (n: number, optIndex: number) => `${n}. sorunun ${optIndex + 1}. seçeneği boş bırakılamaz.`,
  validationOptionTooLong: (n: number, optIndex: number) => `${n}. sorunun ${optIndex + 1}. seçeneği en fazla 75 karakter olabilir.`,
  validationOptionsDuplicate: (n: number) => `${n}. soruda yinelenen seçenekler var, tüm seçenekler birbirinden farklı olmalıdır.`,
  validationCorrectIndexInvalid: (n: number) => `${n}. soru için lütfen doğru bir seçenek işaretleyin.`,

  // Presenter View Strings
  presenterTitle: "Sunum Görünümü",
  startPresentation: "Sunumu Başlat",
  resumePresentation: "Canlı Sunuma Dön",
  openInNewWindow: "Yeni Pencerede Aç",
  toggleFullscreen: "Tam Ekran",
  exitPresentation: "Sunumdan Çık",
  exitConfirmTitle: "Sunumdan Çıkılsın mı?",
  exitConfirmDesc: "Sunumdan çıkmak aktif oturumu iptal etmez. Dilediğiniz zaman panodan tekrar 'Canlı Sunuma Dön' diyebilirsiniz.",
  cancelSession: "Oturumu İptal Et",
  cancelConfirmDesc: "Quiz oturumu tamamen sonlandırılacak ve tüm öğrenciler çıkarılacaktır. Emin misiniz?",
  lobbyTitle: "Canlı Quize Katılın",
  lobbySubtitle: "Good4 uygulamasını açarak topluluk sayfasından veya QR kodu tarayarak katılabilirsiniz.",
  lobbyScanQr: "Kamerayla QR Kodu Tarayın",
  lobbyOrJoinApp: "Veya Good4 uygulamasından katılın",
  lobbyPlayersCount: (count: number) => `${count} Katılımcı Hazır`,
  lobbyWaitingPlayers: "Katılımcılar bekleniyor…",
  lobbyLockSession: "Lobiyi Kilitle",
  lobbyUnlockSession: "Lobiyi Aç",
  lobbyLockedNotice: "Lobi kilitli — Yeni katılımcı kabul edilmiyor",
  startGameButton: "Oyunu Başlat",
  spaceShortcutHint: "(Boşluk veya Enter)",
  questionCounter: (current: number, total: number) => `Soru ${current} / ${total}`,
  answeredCounter: (answered: number, total: number) => `${answered} / ${total} Cevaplandı`,
  earlyRevealButton: "Cevapları Göster",
  unansweredCount: (count: number) => `${count} kişi yanıtlayamadı`,
  nextQuestionButton: "Sonraki Soru",
  viewPodiumButton: "Final Podyumunu Gör",
  podiumTitle: "Yarışma Tamamlandı!",
  podiumFirst: "1. Şampiyon",
  podiumSecond: "2. Sıra",
  podiumThird: "3. Sıra",
  scoreboardTitle: "Tüm Sıralama",
  scoreboardRank: "Sıra",
  scoreboardPlayer: "Katılımcı",
  scoreboardScore: "Puan",
  scoreboardCorrectCount: "Doğru",
  detailedReportButton: "Detaylı Raporu İncele",
  kickPlayerTooltip: "Katılımcıyı çıkar",
  kickedNotice: "Katılımcı oturumdan çıkarıldı.",
  sessionEndedNotice: "Oturum sona erdi.",
  sessionCancelledNotice: "Oturum iptal edildi.",

  // Legacy/Alternate presenter keys for compatibility
  presenterLobbyHeading: "Katılımcılar Bekleniyor",
  presenterHowToJoin: "Nasıl Katılınır?",
  presenterStep1: "1. Good4 uygulamasını açın",
  presenterStep2: "2. Topluluk sayfasından 'Canlı Quiz'e katılın veya kamerayla QR kodu tarayın",
  presenterSessionCode: "Oturum Kodu",
  presenterPlayersCount: (count: number) => `${count} Katılımcı`,
  presenterWaitingForPlayers: "Katılımcılar bekleniyor… Telefonunuzdan hemen katılın!",
  presenterLockLobby: "Katılıma Kilitle",
  presenterUnlockLobby: "Katılıma Aç",
  presenterLobbyLocked: "Lobi Kilitli",
  presenterLobbyUnlocked: "Lobi Açık",
  presenterStartGame: "Oyunu Başlat (Space)",
  presenterKickPlayer: "Katılımcıyı Çıkar",
  presenterKickConfirm: "Bu katılımcıyı çıkarmak istediğinize emin misiniz?",
  presenterQuestionProgress: (current: number, total: number) => `Soru ${current} / ${total}`,
  presenterAnswersCount: (answered: number, total: number) => `${answered} / ${total} Cevap Verildi`,
  presenterEarlyReveal: "Cevapları Göster (Space)",
  presenterTimeRemaining: "Kalan Süre",
  presenterSeconds: "sn",
  presenterCorrectAnswer: "Doğru Cevap",
  presenterUnansweredCount: (count: number) => `${count} kişi yanıt vermedi`,
  presenterNextQuestion: "Sonraki Soru (Space)",
  presenterShowPodium: "Final Podyumunu Gör (Space)",
  presenterPodiumHeading: "Yarışma Tamamlandı!",
  presenterLeaderboardHeading: "Genel Sıralama",
  presenterRankHeader: "Sıra",
  presenterPlayerHeader: "Katılımcı",
  presenterScoreHeader: "Puan",
  presenterCorrectCountHeader: "Doğru Sayısı",
  presenterExit: "Sunumdan Çık",
  presenterReport: "Raporu Görüntüle",
  presenterFullscreen: "Tam Ekran",
  presenterExitFullscreen: "Tam Ekrandan Çık",
  presenterNewTab: "Yeni Sekmede Aç",
  presenterClose: "Kapat",
};

export function quizErrorMessage(error: unknown): string {
  const code = typeof error === "object" && error && "code" in error ? String((error as { code: unknown }).code) : "";
  const message = error instanceof Error ? error.message : typeof error === "string" ? error : "";

  if (message.includes("QUIZ_SESSION_ACTIVE")) return "Bu topluluk için zaten aktif bir quiz oturumu bulunuyor.";
  if (message.includes("QUIZ_SESSION_NOT_FOUND")) return "Quiz oturumu bulunamadı veya süresi dolmuş olabilir.";
  if (message.includes("QUIZ_SESSION_ENDED")) return "Quiz oturumu sona erdi.";
  if (message.includes("QUIZ_HOST_CANNOT_PLAY")) return "Quiz yöneticisi yarışmaya katılamaz.";
  if (message.includes("QUIZ_NOT_FOLLOWER")) return "Yalnızca topluluk takipçileri katılabilir.";
  if (message.includes("QUIZ_KICKED")) return "Bu oturumdan çıkarıldınız.";
  if (message.includes("QUIZ_LOCKED")) return "Oturum kilitli, yeni katılımcı kabul edilmiyor.";
  if (message.includes("QUIZ_FULL")) return "Quiz oturumu maksimum katılımcı sayısına ulaştı.";
  if (message.includes("QUIZ_NICKNAME_TAKEN")) return "Bu takma ad zaten alınmış.";
  if (message.includes("QUIZ_INVALID_PHASE")) return "Geçersiz oturum aşaması.";
  if (message.includes("QUIZ_NO_PLAYERS")) return "Yarışmayı başlatmak için en az bir katılımcı olmalıdır.";
  if (message.includes("QUIZ_ALREADY_ANSWERED")) return "Bu soruya zaten cevap verdiniz.";
  if (message.includes("QUIZ_TIME_UP")) return "Süre doldu.";
  if (message.includes("QUIZ_REVEAL_TOO_EARLY")) return "Cevapları göstermek için henüz erken.";
  if (message.includes("QUIZ_REPORT_NOT_FOUND")) return "Quiz raporu bulunamadı.";
  if (message.includes("QUIZ_PLAYER_NOT_FOUND")) return "Katılımcı bulunamadı.";
  if (message.includes("QUIZ_NOT_READY")) return "Quiz hazır durumda değil veya soru eklenmemiş.";
  if (message.includes("QUIZ_NOT_FOUND")) return "Quiz bulunamadı veya silinmiş olabilir.";
  if (message.includes("QUIZ_TITLE_INVALID")) return "Quiz başlığı 1 ile 80 karakter arasında olmalıdır.";
  if (message.includes("QUIZ_STATUS_INVALID")) return "Geçersiz quiz durumu.";
  if (message.includes("QUIZ_QUESTIONS_INVALID")) return "Bir quizde en fazla 30 soru bulunabilir.";
  if (message.includes("QUIZ_QUESTIONS_REQUIRED")) return "Quizi hazır olarak kaydetmek için en az bir soru eklemelisiniz.";
  if (message.includes("QUIZ_QUESTION_INVALID")) return "Soru formatı geçersiz.";
  if (message.includes("QUIZ_QUESTION_TYPE_INVALID")) return "Geçersiz soru tipi.";
  if (message.includes("QUIZ_QUESTION_TEXT_INVALID")) return "Soru metni 1 ile 120 karakter arasında olmalıdır.";
  if (message.includes("QUIZ_OPTIONS_INVALID")) return "Çoktan seçmeli sorular için 2 ile 4 arasında seçenek girilmeli ve her seçenek en fazla 75 karakter olmalıdır.";
  if (message.includes("QUIZ_OPTIONS_DUPLICATE")) return "Bir sorudaki seçenekler birbirinden farklı olmalıdır.";
  if (message.includes("QUIZ_CORRECT_INDEX_INVALID")) return "Lütfen doğru seçeneği işaretleyin.";
  if (message.includes("QUIZ_TIME_LIMIT_INVALID")) return "Soru süresi 10, 20, 30 veya 60 saniye olmalıdır.";
  if (message.includes("QUIZ_LIMIT_REACHED")) return "Topluluk başına en fazla 20 quiz oluşturulabilir.";
  if (message.includes("QUIZ_MANAGER_REQUIRED")) return "Bu işlem yalnızca yetkili topluluk yöneticileri tarafından yapılabilir.";
  if (message.includes("QUIZ_RESTRICTED_TO_TEST_COMMUNITIES")) return "Canlı quiz özelliği şu anda yalnızca dahili test topluluklarında kullanılabilir.";
  if (message.includes("COMMUNITY_NOT_ACTIVE")) return "Topluluk hesabı aktif durumda değil.";
  if (message.includes("COMMUNITY_ID_INVALID")) return "Geçersiz topluluk bilgisi.";
  if (code.includes("permission-denied")) return "Bu işlem için yetkiniz bulunmuyor.";
  if (code.includes("unauthenticated")) return "Oturum süreniz sona erdi, lütfen tekrar giriş yapın.";
  if (code.includes("deadline-exceeded") || message.includes("network")) return "Bağlantı zaman aşımına uğradı. Lütfen internetinizi kontrol edin.";

  return message || "İşlem sırasında beklenmedik bir hata oluştu. Lütfen tekrar deneyin.";
}

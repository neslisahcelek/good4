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
};

export function quizErrorMessage(error: unknown): string {
  const code = typeof error === "object" && error && "code" in error ? String((error as { code: unknown }).code) : "";
  const message = error instanceof Error ? error.message : typeof error === "string" ? error : "";

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
  if (message.includes("COMMUNITY_NOT_ACTIVE")) return "Topluluk hesabı aktif durumda değil.";
  if (message.includes("COMMUNITY_ID_INVALID")) return "Geçersiz topluluk bilgisi.";
  if (code.includes("permission-denied")) return "Bu işlem için yetkiniz bulunmuyor.";
  if (code.includes("unauthenticated")) return "Oturum süreniz sona erdi, lütfen tekrar giriş yapın.";
  if (code.includes("deadline-exceeded") || message.includes("network")) return "Bağlantı zaman aşımına uğradı. Lütfen internetinizi kontrol edin.";

  return message || "İşlem sırasında beklenmedik bir hata oluştu. Lütfen tekrar deneyin.";
}

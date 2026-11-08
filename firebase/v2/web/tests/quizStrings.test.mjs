import assert from "node:assert/strict";
import { test } from "node:test";
import { quizErrorMessage, quizStrings } from "../src/quizStrings.ts";

test("quizStrings provides correct strings and limits", () => {
  assert.equal(quizStrings.navItem, "Quizler");
  assert.equal(quizStrings.pageTitle, "Quiz Yönetimi");
  assert.equal(quizStrings.statusDraft, "Taslak");
  assert.equal(quizStrings.statusReady, "Hazır");
  assert.equal(quizStrings.timeLimitOption(20), "20 saniye (varsayılan)");
  assert.equal(quizStrings.timeLimitOption(10), "10 saniye");
  assert.equal(quizStrings.questionsCount(5, 30), "5 / 30 soru");
  assert.equal(quizStrings.questionNumber(1), "Soru 1");
  assert.equal(quizStrings.optionPlaceholder(0), "Seçenek 1");
  assert.equal(quizStrings.saveSuccessNotice("ready"), "Quiz başarıyla 'Hazır' olarak kaydedildi. Mobil uygulamadan canlı oturum başlatılabilir.");
  assert.equal(quizStrings.saveSuccessNotice("draft"), "Quiz başarıyla 'Taslak' olarak kaydedildi.");

  // Presenter View strings assertions
  assert.equal(quizStrings.presenterTitle, "Sunum Görünümü");
  assert.equal(quizStrings.startPresentation, "Sunumu Başlat");
  assert.equal(quizStrings.resumePresentation, "Canlı Sunuma Dön");
  assert.equal(quizStrings.openInNewWindow, "Yeni Pencerede Aç");
  assert.equal(quizStrings.toggleFullscreen, "Tam Ekran");
  assert.equal(quizStrings.exitPresentation, "Sunumdan Çık");
  assert.equal(quizStrings.exitConfirmTitle, "Sunumdan Çıkılsın mı?");
  assert.equal(quizStrings.lobbyTitle, "Canlı Quize Katılın");
  assert.equal(quizStrings.lobbyPlayersCount(5), "5 Katılımcı Hazır");
  assert.equal(quizStrings.questionCounter(2, 10), "Soru 2 / 10");
  assert.equal(quizStrings.answeredCounter(7, 12), "7 / 12 Cevaplandı");
  assert.equal(quizStrings.unansweredCount(3), "3 kişi yanıtlayamadı");
  assert.equal(quizStrings.podiumTitle, "Yarışma Tamamlandı!");
  assert.equal(quizStrings.detailedReportButton, "Detaylı Raporu İncele");
});

test("quizErrorMessage translates backend error codes to Turkish messages", () => {
  assert.match(quizErrorMessage(new Error("QUIZ_TITLE_INVALID")), /Quiz başlığı/);
  assert.match(quizErrorMessage(new Error("QUIZ_QUESTIONS_REQUIRED")), /en az bir soru/);
  assert.match(quizErrorMessage(new Error("QUIZ_OPTIONS_INVALID")), /2 ile 4 arasında seçenek/);
  assert.match(quizErrorMessage(new Error("QUIZ_OPTIONS_DUPLICATE")), /birbirinden farklı/);
  assert.match(quizErrorMessage(new Error("QUIZ_LIMIT_REACHED")), /en fazla 20 quiz/);
  assert.match(quizErrorMessage(new Error("QUIZ_MANAGER_REQUIRED")), /topluluk yönetici/);
  assert.match(quizErrorMessage({ code: "permission-denied" }), /yetkiniz bulunmuyor/);
  assert.match(quizErrorMessage({ code: "unauthenticated" }), /Oturum süreniz sona erdi/);
  assert.match(quizErrorMessage(new Error("QUIZ_SESSION_ACTIVE")), /aktif bir quiz oturumu/);
  assert.match(quizErrorMessage(new Error("QUIZ_NO_PLAYERS")), /en az bir katılımcı/);
  assert.match(quizErrorMessage(new Error("QUIZ_LOCKED")), /kilitli/);
});

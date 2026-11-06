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
});

import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import { db as database } from "./firebase.js";
import {
  cancelQuizSessionService,
  cleanupQuizDataService,
  deleteQuizService,
  eraseQuizDataForUser,
  getQuizReportService,
  getQuizService,
  joinQuizSessionService,
  kickQuizPlayerService,
  listQuizzesService,
  nextQuizQuestionService,
  parseNickname,
  parseQuizQuestion,
  revealQuizQuestionService,
  saveQuizService,
  scoreAnswer,
  setQuizLockService,
  startQuizGameService,
  startQuizSessionService,
  submitQuizAnswerService,
  QUIZ_LIMITS,
} from "./quiz.js";

const nowMillis = Date.parse("2026-11-06T12:00:00Z");

beforeEach(async () => {
  if (
    !/^(localhost|127\.0\.0\.1|\[::1\]):\d+$/.test(process.env.FIRESTORE_EMULATOR_HOST ?? "") ||
    !process.env.GCLOUD_PROJECT?.startsWith("demo-")
  ) {
    throw new Error("A localhost Firestore emulator and demo project are required");
  }

  await Promise.all([
    "users",
    "organizations",
    "quizActive",
    "quizSessions",
    "quizReports",
    "rateLimits",
    "auditLogs",
  ].map((collection) => database.recursiveDelete(database.collection(collection))));

  await Promise.all([
    database.doc("users/manager-1").set({ role: "student", status: "active" }),
    database.doc("users/student-1").set({ role: "student", status: "active" }),
    database.doc("users/student-2").set({ role: "student", status: "active" }),
    database.doc("users/student-3").set({ role: "student", status: "active" }),
    database.doc("users/stranger-1").set({ role: "student", status: "active" }),
    database.doc("organizations/comm-1").set({ type: "community", status: "active", listed: false }),
    database.doc("organizations/comm-1/members/manager-1").set({ role: "manager", status: "active" }),
    database.doc("organizations/comm-1/followers/student-1").set({ userId: "student-1" }),
    database.doc("organizations/comm-1/followers/student-2").set({ userId: "student-2" }),
    database.doc("organizations/comm-1/followers/student-3").set({ userId: "student-3" }),
  ]);
});

after(async () => {
  await database.terminate();
});

// --- 1. Pure unit tests ----------------------------------------------------

test("scoreAnswer calculates speed-based score and handles bounds", () => {
  const timeLimitMs = 10000;
  // Instant answer (0ms elapsed): full 1000 points
  assert.equal(scoreAnswer(true, 0, timeLimitMs), 1000);
  // Halfway through: 750 points
  assert.equal(scoreAnswer(true, 5000, timeLimitMs), 750);
  // Last millisecond: 500 points
  assert.equal(scoreAnswer(true, 10000, timeLimitMs), 500);
  // Wrong answer: always 0 points
  assert.equal(scoreAnswer(false, 0, timeLimitMs), 0);
  assert.equal(scoreAnswer(false, 5000, timeLimitMs), 0);
  // Negative elapsed (early arrival tolerance): clamped to 0 -> 1000 points
  assert.equal(scoreAnswer(true, -100, timeLimitMs), 1000);
  // Elapsed past limit: clamped to timeLimitMs -> 500 points
  assert.equal(scoreAnswer(true, 12000, timeLimitMs), 500);
});

test("parseNickname validates characters, length, whitespace, and profanity", () => {
  // Valid nicknames
  const valid = parseNickname("  Ali_Veli 10  ");
  assert.equal(valid.display, "Ali_Veli 10");
  assert.ok(typeof valid.key === "string" && valid.key.length === 64);

  // Length boundaries (2 - 16 characters)
  assert.equal(parseNickname("ab").display, "ab");
  assert.equal(parseNickname("1234567890123456").display, "1234567890123456");
  assert.throws(() => parseNickname("a"), /QUIZ_NICKNAME_INVALID/);
  assert.throws(() => parseNickname("12345678901234567"), /QUIZ_NICKNAME_INVALID/);

  // Invalid characters
  assert.throws(() => parseNickname("Ali@Home"), /QUIZ_NICKNAME_INVALID/);
  assert.throws(() => parseNickname("Ali!"), /QUIZ_NICKNAME_INVALID/);

  // Blocked exact words
  assert.throws(() => parseNickname("amk"), /QUIZ_NICKNAME_BLOCKED/);
  assert.throws(() => parseNickname("AQ"), /QUIZ_NICKNAME_BLOCKED/);

  // Blocked substring matches
  assert.throws(() => parseNickname("pro_siktir_x"), /QUIZ_NICKNAME_BLOCKED/);
  assert.throws(() => parseNickname("the_fuck_99"), /QUIZ_NICKNAME_BLOCKED/);
  assert.throws(() => parseNickname("hitler_fan"), /QUIZ_NICKNAME_BLOCKED/);
});

test("parseQuizQuestion parses multiple choice and true/false questions", () => {
  const mc = parseQuizQuestion({
    type: "multiple_choice",
    text: "Hangi gezegen Güneş'e en yakındır?",
    options: ["Merkür", "Venüs", "Dünya", "Mars"],
    correctIndex: 0,
    timeLimitSec: 20,
  });
  assert.equal(mc.type, "multiple_choice");
  assert.equal(mc.correctIndex, 0);
  assert.equal(mc.options.length, 4);

  const tf = parseQuizQuestion({
    type: "true_false",
    text: "Dünya düzdür.",
    correctIndex: 1,
    timeLimitSec: 10,
  });
  assert.equal(tf.type, "true_false");
  assert.deepEqual(tf.options, ["true", "false"]);
  assert.equal(tf.correctIndex, 1);

  // Invalid question scenarios
  assert.throws(() => parseQuizQuestion({ type: "essay" }), /QUIZ_QUESTION_TYPE_INVALID/);
  assert.throws(() => parseQuizQuestion({ type: "multiple_choice", text: "Q", options: ["A"], correctIndex: 0, timeLimitSec: 10 }), /QUIZ_OPTIONS_INVALID/);
  assert.throws(() => parseQuizQuestion({ type: "multiple_choice", text: "Q", options: ["A", "a"], correctIndex: 0, timeLimitSec: 10 }), /QUIZ_OPTIONS_DUPLICATE/);
  assert.throws(() => parseQuizQuestion({ type: "multiple_choice", text: "Q", options: ["A", "B"], correctIndex: 2, timeLimitSec: 10 }), /QUIZ_CORRECT_INDEX_INVALID/);
  assert.throws(() => parseQuizQuestion({ type: "multiple_choice", text: "Q", options: ["A", "B"], correctIndex: 0, timeLimitSec: 15 }), /QUIZ_TIME_LIMIT_INVALID/);
});

// --- 2. Quiz CRUD ----------------------------------------------------------

test("saveQuizService saves draft and ready quizzes, updates questions, and limits count", async () => {
  const questions = [
    { type: "multiple_choice", text: "Soru 1", options: ["A", "B"], correctIndex: 0, timeLimitSec: 20 },
    { type: "true_false", text: "Soru 2", correctIndex: 0, timeLimitSec: 10 },
  ];

  // Community manager can save a quiz
  const result = await saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    title: "Genel Kültür",
    status: "ready",
    questions,
  }, nowMillis);
  assert.ok(result.quizId);
  assert.equal(result.questionCount, 2);

  const quizDoc = await database.doc(`organizations/comm-1/quizzes/${result.quizId}`).get();
  assert.equal(quizDoc.get("title"), "Genel Kültür");
  assert.equal(quizDoc.get("status"), "ready");
  assert.equal(quizDoc.get("questionCount"), 2);

  // Ready quiz with empty questions is rejected
  await assert.rejects(saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    title: "Boş Quiz",
    status: "ready",
    questions: [],
  }), /QUIZ_QUESTIONS_REQUIRED/);

  // Non-manager is rejected
  await assert.rejects(saveQuizService(database, "student-1", {
    organizationId: "comm-1",
    title: "Yetkisiz",
    status: "draft",
    questions: [],
  }), /QUIZ_MANAGER_REQUIRED/);

  // Listed (public) community is rejected
  await database.doc("organizations/comm-listed").set({ type: "community", status: "active", listed: true });
  await database.doc("organizations/comm-listed/members/manager-1").set({ role: "manager", status: "active" });
  await assert.rejects(saveQuizService(database, "manager-1", {
    organizationId: "comm-listed",
    title: "Listed Quiz",
    status: "draft",
    questions: [],
  }), /QUIZ_RESTRICTED_TO_TEST_COMMUNITIES/);

  // Updating existing quiz to 1 question removes the old 2nd question doc
  const updated = await saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    quizId: result.quizId,
    title: "Genel Kültür (Güncellendi)",
    status: "ready",
    questions: [questions[0]],
  }, nowMillis + 1000);
  assert.equal(updated.questionCount, 1);
  const qList = await database.collection(`organizations/comm-1/quizzes/${result.quizId}/questions`).get();
  assert.equal(qList.size, 1);
  assert.equal(qList.docs[0]?.id, "000");

  // Maximum quizzes per community (20) enforced
  for (let i = 1; i < QUIZ_LIMITS.maxQuizzesPerCommunity; i++) {
    await database.collection("organizations/comm-1/quizzes").add({ title: `Quiz ${i}` });
  }
  await assert.rejects(saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    title: "Fazla Quiz",
    status: "draft",
    questions: [],
  }), /QUIZ_LIMIT_REACHED/);
});

test("listQuizzesService and getQuizService return quizzes in order", async () => {
  const q1 = await saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    title: "Quiz 1",
    status: "draft",
    questions: [],
  }, nowMillis);
  const q2 = await saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    title: "Quiz 2",
    status: "ready",
    questions: [{ type: "true_false", text: "Soru 1", correctIndex: 0, timeLimitSec: 10 }],
  }, nowMillis + 5000);

  const list = await listQuizzesService(database, "manager-1", { organizationId: "comm-1" });
  assert.equal(list.quizzes.length, 2);
  assert.equal(list.quizzes[0]?.quizId, q2.quizId); // latest first

  const detail = await getQuizService(database, "manager-1", { organizationId: "comm-1", quizId: q2.quizId });
  assert.equal(detail.title, "Quiz 2");
  assert.equal(detail.questions.length, 1);

  // Non-manager cannot list or get
  await assert.rejects(listQuizzesService(database, "student-1", { organizationId: "comm-1" }), /QUIZ_MANAGER_REQUIRED/);
  await assert.rejects(getQuizService(database, "student-1", { organizationId: "comm-1", quizId: q2.quizId }), /QUIZ_MANAGER_REQUIRED/);
});

test("deleteQuizService recursively deletes quiz and subcollections", async () => {
  const quiz = await saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    title: "Silinecek",
    status: "ready",
    questions: [{ type: "true_false", text: "Soru", correctIndex: 0, timeLimitSec: 10 }],
  }, nowMillis);

  await deleteQuizService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId });
  assert.equal((await database.doc(`organizations/comm-1/quizzes/${quiz.quizId}`).get()).exists, false);
  assert.equal((await database.collection(`organizations/comm-1/quizzes/${quiz.quizId}/questions`).get()).empty, true);
});

// --- 3. Session Lifecycle, Lock & Play ---------------------------------------

async function createReadyQuiz() {
  return saveQuizService(database, "manager-1", {
    organizationId: "comm-1",
    title: "Oyun Quizi",
    status: "ready",
    questions: [
      { type: "multiple_choice", text: "Soru 1", options: ["A", "B", "C", "D"], correctIndex: 2, timeLimitSec: 10 },
      { type: "true_false", text: "Soru 2", correctIndex: 0, timeLimitSec: 20 },
    ],
  }, nowMillis);
}

test("startQuizSessionService creates session with frozen questions and enforces single active session", async () => {
  const quiz = await createReadyQuiz();

  const session = await startQuizSessionService(database, "manager-1", {
    organizationId: "comm-1",
    quizId: quiz.quizId,
  }, nowMillis);
  assert.ok(session.sessionId);

  const sessionDoc = await database.doc(`quizSessions/${session.sessionId}`).get();
  assert.equal(sessionDoc.get("status"), "lobby");
  assert.equal(sessionDoc.get("totalQuestions"), 2);
  assert.equal(sessionDoc.get("locked"), false);

  // Frozen questions are saved in session subcollection
  const q0 = await database.doc(`quizSessions/${session.sessionId}/questions/0`).get();
  assert.equal(q0.get("correctIndex"), 2);
  assert.equal(q0.get("timeLimitSec"), 10);

  // Active lock document exists
  const lock = await database.doc("quizActive/comm-1").get();
  assert.equal(lock.get("sessionId"), session.sessionId);

  // Cannot start a second active session in the same community
  await assert.rejects(startQuizSessionService(database, "manager-1", {
    organizationId: "comm-1",
    quizId: quiz.quizId,
  }), /QUIZ_SESSION_ACTIVE/);

  // Non-manager cannot start session
  await assert.rejects(startQuizSessionService(database, "student-1", {
    organizationId: "comm-1",
    quizId: quiz.quizId,
  }), /QUIZ_MANAGER_REQUIRED/);
});

test("joinQuizSessionService enforces follower status, capacity, nickname uniqueness, and reconnects", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);

  // Host cannot join as a player
  await assert.rejects(joinQuizSessionService(database, "manager-1", {
    sessionId: session.sessionId,
    nickname: "HostPlayer",
  }), /QUIZ_HOST_CANNOT_PLAY/);

  // Non-follower cannot join
  await assert.rejects(joinQuizSessionService(database, "stranger-1", {
    sessionId: session.sessionId,
    nickname: "Yabanci",
  }), /QUIZ_NOT_FOLLOWER/);

  // Follower joins successfully
  const join1 = await joinQuizSessionService(database, "student-1", {
    sessionId: session.sessionId,
    nickname: "Kral Oyuncu",
  }, nowMillis);
  assert.equal(join1.nickname, "Kral Oyuncu");
  assert.equal(join1.sessionId, session.sessionId);

  const lobbyCounter = await database.doc(`quizSessions/${session.sessionId}/counters/lobby`).get();
  assert.equal(lobbyCounter.get("playerCount"), 1);

  // Same user reconnects: returns original nickname without incrementing playerCount
  const reconnect = await joinQuizSessionService(database, "student-1", {
    sessionId: session.sessionId,
    nickname: "Farkli Ad",
  }, nowMillis);
  assert.equal(reconnect.nickname, "Kral Oyuncu");
  const lobbyCounter2 = await database.doc(`quizSessions/${session.sessionId}/counters/lobby`).get();
  assert.equal(lobbyCounter2.get("playerCount"), 1);

  // Different user with the same normalized nickname is rejected
  await assert.rejects(joinQuizSessionService(database, "student-2", {
    sessionId: session.sessionId,
    nickname: "kral  oyuncu",
  }), /QUIZ_NICKNAME_TAKEN/);

  // Capacity limit (50 players)
  await database.doc(`quizSessions/${session.sessionId}/counters/lobby`).update({ playerCount: 50 });
  await assert.rejects(joinQuizSessionService(database, "student-2", {
    sessionId: session.sessionId,
    nickname: "Ogrenci 2",
  }), /QUIZ_FULL/);
  await database.doc(`quizSessions/${session.sessionId}/counters/lobby`).update({ playerCount: 1 });

  // When session is locked, new players cannot join
  await setQuizLockService(database, "manager-1", { sessionId: session.sessionId, locked: true });
  await assert.rejects(joinQuizSessionService(database, "student-2", {
    sessionId: session.sessionId,
    nickname: "Ogrenci 2",
  }), /QUIZ_LOCKED/);
});

test("setQuizLockService allows host to toggle lock only during lobby phase", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);

  // Non-host cannot set lock
  await assert.rejects(setQuizLockService(database, "student-1", { sessionId: session.sessionId, locked: true }), /QUIZ_HOST_ONLY/);

  const lockRes = await setQuizLockService(database, "manager-1", { sessionId: session.sessionId, locked: true });
  assert.equal(lockRes.locked, true);
  const sessionDoc = await database.doc(`quizSessions/${session.sessionId}`).get();
  assert.equal(sessionDoc.get("locked"), true);
});

test("startQuizGameService requires at least 1 player and advances to question phase", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);

  // Cannot start without players
  await assert.rejects(startQuizGameService(database, "manager-1", { sessionId: session.sessionId }, nowMillis), /QUIZ_NO_PLAYERS/);

  await joinQuizSessionService(database, "student-1", { sessionId: session.sessionId, nickname: "Oyuncu 1" }, nowMillis);
  await joinQuizSessionService(database, "student-2", { sessionId: session.sessionId, nickname: "Oyuncu 2" }, nowMillis);

  const game = await startQuizGameService(database, "manager-1", { sessionId: session.sessionId }, nowMillis);
  assert.equal(game.status, "question");

  const sessionDoc = await database.doc(`quizSessions/${session.sessionId}`).get();
  assert.equal(sessionDoc.get("status"), "question");
  assert.equal(sessionDoc.get("currentIndex"), 0);
  assert.equal(sessionDoc.get("locked"), true);
  // Verify correctIndex is NOT exposed in currentQuestion
  assert.equal(sessionDoc.get("currentQuestion.correctIndex"), undefined);
  assert.equal(sessionDoc.get("currentQuestion.text"), "Soru 1");
  assert.equal(sessionDoc.get("reveal"), null);
});

test("submitQuizAnswerService enforces timing, duplicate answer check, and records points", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);
  await joinQuizSessionService(database, "student-1", { sessionId: session.sessionId, nickname: "Oyuncu 1" }, nowMillis);
  await joinQuizSessionService(database, "student-2", { sessionId: session.sessionId, nickname: "Oyuncu 2" }, nowMillis);
  await startQuizGameService(database, "manager-1", { sessionId: session.sessionId }, nowMillis);

  const startsMs = nowMillis + QUIZ_LIMITS.startDelayMs; // 3000ms delay
  const endsMs = startsMs + 10000; // 10s time limit

  // Non-player cannot submit
  await assert.rejects(submitQuizAnswerService(database, "stranger-1", {
    sessionId: session.sessionId, index: 0, option: 2,
  }, startsMs + 1000), /QUIZ_NOT_PLAYER/);

  // Submitting too early (before question starts)
  await assert.rejects(submitQuizAnswerService(database, "student-1", {
    sessionId: session.sessionId, index: 0, option: 2,
  }, startsMs - QUIZ_LIMITS.earlyAnswerToleranceMs - 100), /QUIZ_QUESTION_NOT_STARTED/);

  // Submitting correct answer 2000ms into question
  const submit1 = await submitQuizAnswerService(database, "student-1", {
    sessionId: session.sessionId, index: 0, option: 2, // correctIndex is 2
  }, startsMs + 2000);
  assert.equal(submit1.accepted, true);

  const ansDoc = await database.doc(`quizSessions/${session.sessionId}/answers/0_student-1`).get();
  assert.equal(ansDoc.get("correct"), true);
  assert.equal(ansDoc.get("option"), 2);
  assert.equal(ansDoc.get("points"), scoreAnswer(true, 2000, 10000));

  // Second submission by the same user is rejected (cannot change answer)
  await assert.rejects(submitQuizAnswerService(database, "student-1", {
    sessionId: session.sessionId, index: 0, option: 1,
  }, startsMs + 3000), /QUIZ_ALREADY_ANSWERED/);

  // Submitting after time limit + grace period is rejected
  await assert.rejects(submitQuizAnswerService(database, "student-2", {
    sessionId: session.sessionId, index: 0, option: 2,
  }, endsMs + QUIZ_LIMITS.answerGraceMs + 100), /QUIZ_TIME_UP/);

  // Submitting within grace period (endsMs + 500ms) is accepted
  const submit2 = await submitQuizAnswerService(database, "student-2", {
    sessionId: session.sessionId, index: 0, option: 1, // wrong answer
  }, endsMs + 500);
  assert.equal(submit2.accepted, true);
  const ansDoc2 = await database.doc(`quizSessions/${session.sessionId}/answers/0_student-2`).get();
  assert.equal(ansDoc2.get("correct"), false);
  assert.equal(ansDoc2.get("points"), 0);
});

test("revealQuizQuestionService calculates rankings, distribution, and supports backup player trigger", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);
  await joinQuizSessionService(database, "student-1", { sessionId: session.sessionId, nickname: "Oyuncu 1" }, nowMillis);
  await joinQuizSessionService(database, "student-2", { sessionId: session.sessionId, nickname: "Oyuncu 2" }, nowMillis);
  await joinQuizSessionService(database, "student-3", { sessionId: session.sessionId, nickname: "Oyuncu 3" }, nowMillis);
  await startQuizGameService(database, "manager-1", { sessionId: session.sessionId }, nowMillis);

  const startsMs = nowMillis + QUIZ_LIMITS.startDelayMs;
  const endsMs = startsMs + 10000;

  // Student 1 answers correctly at 2000ms
  await submitQuizAnswerService(database, "student-1", { sessionId: session.sessionId, index: 0, option: 2 }, startsMs + 2000);
  // Student 2 answers correctly at 5000ms
  await submitQuizAnswerService(database, "student-2", { sessionId: session.sessionId, index: 0, option: 2 }, startsMs + 5000);
  // Student 3 answers wrongly at 1000ms
  await submitQuizAnswerService(database, "student-3", { sessionId: session.sessionId, index: 0, option: 0 }, startsMs + 1000);

  // Player backup trigger test: player cannot reveal before endsAt + playerRevealGraceMs
  await assert.rejects(revealQuizQuestionService(database, "student-1", {
    sessionId: session.sessionId, index: 0,
  }, endsMs + QUIZ_LIMITS.playerRevealGraceMs - 500), /QUIZ_REVEAL_TOO_EARLY/);

  // But once endsAt + playerRevealGraceMs has passed, player backup trigger works!
  const backupReveal = await revealQuizQuestionService(database, "student-1", {
    sessionId: session.sessionId, index: 0,
  }, endsMs + QUIZ_LIMITS.playerRevealGraceMs + 500);
  assert.equal(backupReveal.status, "reveal");
  assert.equal(backupReveal.alreadyRevealed, false);

  // Idempotency: second call by host or player returns alreadyRevealed: true
  const secondReveal = await revealQuizQuestionService(database, "manager-1", {
    sessionId: session.sessionId, index: 0,
  }, endsMs + 5000);
  assert.equal(secondReveal.alreadyRevealed, true);

  // Verify session state after reveal
  const sessionDoc = await database.doc(`quizSessions/${session.sessionId}`).get();
  assert.equal(sessionDoc.get("status"), "reveal");
  assert.equal(sessionDoc.get("reveal.correctIndex"), 2);
  assert.deepEqual(sessionDoc.get("reveal.distribution"), [1, 0, 2, 0]); // option 0: 1, option 2: 2

  // Player 1 scored higher than Player 2 because of faster answer
  const p1 = await database.doc(`quizSessions/${session.sessionId}/players/student-1`).get();
  const p2 = await database.doc(`quizSessions/${session.sessionId}/players/student-2`).get();
  const p3 = await database.doc(`quizSessions/${session.sessionId}/players/student-3`).get();
  assert.ok(p1.get("score") > p2.get("score"));
  assert.equal(p1.get("rank"), 1);
  assert.equal(p2.get("rank"), 2);
  assert.equal(p3.get("score"), 0);
  assert.equal(p3.get("rank"), 3);
  assert.equal(p1.get("streak"), 1);
  assert.equal(p3.get("streak"), 0);
});

test("nextQuizQuestionService advances to question 2 and finishes on completion", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);
  await joinQuizSessionService(database, "student-1", { sessionId: session.sessionId, nickname: "Oyuncu 1" }, nowMillis);
  await startQuizGameService(database, "manager-1", { sessionId: session.sessionId }, nowMillis);

  // Question 0: Host reveals
  await revealQuizQuestionService(database, "manager-1", { sessionId: session.sessionId, index: 0 }, nowMillis + 5000);

  // Non-host cannot advance
  await assert.rejects(nextQuizQuestionService(database, "student-1", { sessionId: session.sessionId }), /QUIZ_HOST_ONLY/);

  // Advance to question 1
  const next1 = await nextQuizQuestionService(database, "manager-1", { sessionId: session.sessionId }, nowMillis + 6000);
  assert.equal(next1.status, "question");
  const sAfterQ1 = await database.doc(`quizSessions/${session.sessionId}`).get();
  assert.equal(sAfterQ1.get("currentIndex"), 1);
  assert.equal(sAfterQ1.get("currentQuestion.text"), "Soru 2");

  // Question 1: Host reveals
  await revealQuizQuestionService(database, "manager-1", { sessionId: session.sessionId, index: 1 }, nowMillis + 15000);

  // Question 1 was the last question (totalQuestions = 2): next completes game
  const finish = await nextQuizQuestionService(database, "manager-1", { sessionId: session.sessionId }, nowMillis + 16000);
  assert.equal(finish.status, "finished");

  const sFinished = await database.doc(`quizSessions/${session.sessionId}`).get();
  assert.equal(sFinished.get("status"), "finished");
  assert.ok(Array.isArray(sFinished.get("podium")));

  // Active session lock released
  assert.equal((await database.doc("quizActive/comm-1").get()).exists, false);

  // Report generated
  const reportDoc = await database.doc(`quizReports/${session.sessionId}`).get();
  assert.equal(reportDoc.exists, true);
  assert.equal(reportDoc.get("organizationId"), "comm-1");
  assert.equal(reportDoc.get("playerCount"), 1);

  // Community manager can read report
  const report = await getQuizReportService(database, "manager-1", { sessionId: session.sessionId });
  assert.equal(report.title, "Oyun Quizi");
  assert.equal(report.playerCount, 1);
  assert.equal(report.ranking[0].nickname, "Oyuncu 1");
  // Non-manager cannot read report
  await assert.rejects(getQuizReportService(database, "student-1", { sessionId: session.sessionId }), /QUIZ_MANAGER_REQUIRED/);
});

test("kickQuizPlayerService kicks player and prevents re-entry and answering", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);
  await joinQuizSessionService(database, "student-1", { sessionId: session.sessionId, nickname: "Troll" }, nowMillis);

  // Only host can kick
  await assert.rejects(kickQuizPlayerService(database, "student-2", {
    sessionId: session.sessionId, playerId: "student-1",
  }), /QUIZ_HOST_ONLY/);

  const kick = await kickQuizPlayerService(database, "manager-1", {
    sessionId: session.sessionId, playerId: "student-1",
  });
  assert.equal(kick.kicked, true);

  const playerDoc = await database.doc(`quizSessions/${session.sessionId}/players/student-1`).get();
  assert.equal(playerDoc.get("status"), "kicked");

  // Re-joining fails
  await assert.rejects(joinQuizSessionService(database, "student-1", {
    sessionId: session.sessionId, nickname: "Troll",
  }), /QUIZ_KICKED/);

  // Kicked player cannot answer questions
  await joinQuizSessionService(database, "student-2", { sessionId: session.sessionId, nickname: "Oyuncu 2" }, nowMillis);
  await startQuizGameService(database, "manager-1", { sessionId: session.sessionId }, nowMillis);
  await assert.rejects(submitQuizAnswerService(database, "student-1", {
    sessionId: session.sessionId, index: 0, option: 0,
  }, nowMillis + QUIZ_LIMITS.startDelayMs + 500), /QUIZ_NOT_PLAYER/);
});

test("cancelQuizSessionService cancels active session and frees active lock", async () => {
  const quiz = await createReadyQuiz();
  const session = await startQuizSessionService(database, "manager-1", { organizationId: "comm-1", quizId: quiz.quizId }, nowMillis);

  // Only host can cancel
  await assert.rejects(cancelQuizSessionService(database, "student-1", { sessionId: session.sessionId }), /QUIZ_HOST_ONLY/);

  const cancel = await cancelQuizSessionService(database, "manager-1", { sessionId: session.sessionId }, nowMillis);
  assert.equal(cancel.status, "cancelled");

  const sessionDoc = await database.doc(`quizSessions/${session.sessionId}`).get();
  assert.equal(sessionDoc.get("status"), "cancelled");
  assert.equal((await database.doc("quizActive/comm-1").get()).exists, false);
});

test("cleanupQuizDataService cancels idle sessions and deletes expired sessions/reports", async () => {
  const idleSessionRef = database.collection("quizSessions").doc("idle-1");
  await idleSessionRef.set({
    organizationId: "comm-1",
    status: "lobby",
    lastActivityAt: Timestamp.fromMillis(nowMillis - QUIZ_LIMITS.idleCancelMs - 1000),
  });
  await database.doc("quizActive/comm-1").set({ sessionId: "idle-1" });

  const expiredSessionRef = database.collection("quizSessions").doc("expired-1");
  await expiredSessionRef.set({
    status: "finished",
    expiresAt: Timestamp.fromMillis(nowMillis - 1000),
  });
  await expiredSessionRef.collection("players").doc("p1").set({ userId: "p1" });

  const expiredReportRef = database.collection("quizReports").doc("report-1");
  await expiredReportRef.set({
    expiresAt: Timestamp.fromMillis(nowMillis - 1000),
  });

  const activeSessionRef = database.collection("quizSessions").doc("active-fresh");
  await activeSessionRef.set({
    organizationId: "comm-2",
    status: "lobby",
    lastActivityAt: Timestamp.fromMillis(nowMillis),
  });

  const cleaned = await cleanupQuizDataService(database, nowMillis);
  assert.equal(cleaned.cancelled, 1);
  assert.equal(cleaned.sessionsDeleted, 1);
  assert.equal(cleaned.reportsDeleted, 1);

  // Idle session cancelled and lock released
  assert.equal((await idleSessionRef.get()).get("status"), "cancelled");
  assert.equal((await database.doc("quizActive/comm-1").get()).exists, false);

  // Expired session and report deleted
  assert.equal((await expiredSessionRef.get()).exists, false);
  assert.equal((await expiredReportRef.get()).exists, false);

  // Active fresh session untouched
  assert.equal((await activeSessionRef.get()).get("status"), "lobby");
});

test("eraseQuizDataForUser removes user records across sessions", async () => {
  const s1 = database.collection("quizSessions").doc("s1");
  const s2 = database.collection("quizSessions").doc("s2");

  await Promise.all([
    s1.collection("players").doc("student-1").set({ userId: "student-1", nickname: "S1" }),
    s1.collection("answers").doc("0_student-1").set({ userId: "student-1", option: 1 }),
    s1.collection("nicknames").doc("hash-1").set({ userId: "student-1" }),
    s2.collection("players").doc("student-1").set({ userId: "student-1", nickname: "S1" }),
    s2.collection("players").doc("student-2").set({ userId: "student-2", nickname: "S2" }),
  ]);

  await eraseQuizDataForUser(database, "student-1");

  assert.equal((await s1.collection("players").doc("student-1").get()).exists, false);
  assert.equal((await s1.collection("answers").doc("0_student-1").get()).exists, false);
  assert.equal((await s1.collection("nicknames").doc("hash-1").get()).exists, false);
  assert.equal((await s2.collection("players").doc("student-1").get()).exists, false);
  // Other user's record untouched
  assert.equal((await s2.collection("players").doc("student-2").get()).exists, true);
});

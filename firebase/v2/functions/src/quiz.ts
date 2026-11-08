import { createHash } from "node:crypto";
import {
  FieldValue,
  Timestamp,
  type DocumentReference,
  type Firestore,
  type Transaction,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { normalizeForMatching } from "./marketModeration.js";
import { requireActiveActor } from "./shared.js";

/** Live community quiz. See `.agents/skills/good4-live-quiz/SKILL.md` for the rules this module enforces. */

export const QUIZ_LIMITS = {
  maxPlayers: 50,
  maxQuestions: 30,
  maxQuizzesPerCommunity: 20,
  maxTitleLength: 80,
  maxQuestionLength: 120,
  maxOptionLength: 75,
  minNicknameLength: 2,
  maxNicknameLength: 16,
  timeLimitsSec: [10, 20, 30, 60],
  /** Players see "get ready" for this long so every device starts the question together. */
  startDelayMs: 3000,
  /** Network slack for an answer sent right at the deadline. */
  answerGraceMs: 1500,
  /** Answers may arrive slightly before the local countdown reaches zero (clock offset). */
  earlyAnswerToleranceMs: 300,
  /** Any player may trigger the reveal once the question is this far past its deadline (host went away). */
  playerRevealGraceMs: 2000,
  idleCancelMs: 2 * 60 * 60 * 1000,
  sessionRetentionMs: 3 * 24 * 60 * 60 * 1000,
  reportRetentionMs: 10 * 24 * 60 * 60 * 1000,
  counterShards: 5,
  maxScore: 1000,
} as const;

export const ACTIVE_SESSION_STATUSES = ["lobby", "question", "reveal"] as const;
const QUESTION_TYPES = ["multiple_choice", "true_false"] as const;
type QuestionType = (typeof QUESTION_TYPES)[number];

export interface QuizQuestion {
  type: QuestionType;
  text: string;
  options: string[];
  correctIndex: number;
  timeLimitSec: number;
}

type Id = string;
const ID_PATTERN = /^[A-Za-z0-9_-]{1,128}$/;

function invalid(code: string): HttpsError {
  return new HttpsError("invalid-argument", code);
}

function requireId(value: unknown, code: string): Id {
  if (typeof value !== "string" || !ID_PATTERN.test(value)) throw invalid(code);
  return value;
}

function cleanText(value: unknown, maxLength: number, code: string): string {
  if (typeof value !== "string") throw invalid(code);
  const text = value.replace(/[\u0000-\u001f\u007f]/g, " ").replace(/\s+/g, " ").trim();
  const length = Array.from(text).length;
  if (length === 0 || length > maxLength) throw invalid(code);
  return text;
}

function requireIndex(value: unknown, max: number, code: string): number {
  if (typeof value !== "number" || !Number.isInteger(value) || value < 0 || value > max) throw invalid(code);
  return value;
}

export function parseQuizQuestion(raw: unknown): QuizQuestion {
  if (!raw || typeof raw !== "object") throw invalid("QUIZ_QUESTION_INVALID");
  const input = raw as Record<string, unknown>;
  const type = input.type;
  if (!QUESTION_TYPES.includes(type as QuestionType)) throw invalid("QUIZ_QUESTION_TYPE_INVALID");
  const text = cleanText(input.text, QUIZ_LIMITS.maxQuestionLength, "QUIZ_QUESTION_TEXT_INVALID");
  let options: string[];
  if (type === "true_false") {
    // Labels are localised by the client; the server only stores the two fixed slots.
    options = ["true", "false"];
  } else {
    if (!Array.isArray(input.options) || input.options.length < 2 || input.options.length > 4) {
      throw invalid("QUIZ_OPTIONS_INVALID");
    }
    options = input.options.map((option) => cleanText(option, QUIZ_LIMITS.maxOptionLength, "QUIZ_OPTIONS_INVALID"));
    if (new Set(options.map((option) => option.toLocaleLowerCase("tr-TR"))).size !== options.length) {
      throw invalid("QUIZ_OPTIONS_DUPLICATE");
    }
  }
  const correctIndex = requireIndex(input.correctIndex, options.length - 1, "QUIZ_CORRECT_INDEX_INVALID");
  const timeLimitSec = input.timeLimitSec;
  if (typeof timeLimitSec !== "number" || !(QUIZ_LIMITS.timeLimitsSec as readonly number[]).includes(timeLimitSec)) {
    throw invalid("QUIZ_TIME_LIMIT_INVALID");
  }
  return { type: type as QuestionType, text, options, correctIndex, timeLimitSec };
}

/** Speed-based score: instant correct answer 1000, last second ~500, wrong or missing 0. */
export function scoreAnswer(correct: boolean, elapsedMs: number, timeLimitMs: number): number {
  if (!correct) return 0;
  const clamped = Math.min(Math.max(elapsedMs, 0), timeLimitMs);
  return Math.floor((1 - clamped / timeLimitMs / 2) * QUIZ_LIMITS.maxScore);
}

// --- nickname ---------------------------------------------------------------

const NICKNAME_PATTERN = /^[\p{L}\p{N} _.\-]+$/u;
const BLOCKED_EXACT = new Set(["amk", "aq", "sik", "got", "pic", "mk", "sg", "oc", "yarak", "meme", "am", "taşak"
  .normalize("NFKD").replace(/[̀-ͯ]/g, "")]);
const BLOCKED_CONTAINS = [
  "orospu", "yarrak", "sikik", "sikeyim", "sikerim", "siktir", "gotveren", "pezevenk", "kahpe",
  "ibne", "puşt", "pust", "amcik", "fuck", "shit", "bitch", "nigg", "faggot", "hitler",
].map((term) => normalizeForMatching(term).replace(/\s+/g, ""));

export function parseNickname(value: unknown): { display: string; key: string } {
  if (typeof value !== "string") throw invalid("QUIZ_NICKNAME_INVALID");
  const display = value.replace(/\s+/g, " ").trim();
  const length = Array.from(display).length;
  if (length < QUIZ_LIMITS.minNicknameLength || length > QUIZ_LIMITS.maxNicknameLength || !NICKNAME_PATTERN.test(display)) {
    throw invalid("QUIZ_NICKNAME_INVALID");
  }
  const normalized = normalizeForMatching(display);
  const words = normalized.split(/[^a-z]+/).filter(Boolean);
  const joined = words.join("");
  if (words.some((word) => BLOCKED_EXACT.has(word)) || BLOCKED_CONTAINS.some((term) => joined.includes(term))) {
    throw invalid("QUIZ_NICKNAME_BLOCKED");
  }
  const key = createHash("sha256").update(display.toLocaleLowerCase("tr-TR").replace(/\s+/g, "")).digest("hex");
  return { display, key };
}

// --- refs and access --------------------------------------------------------

const quizzesOf = (database: Firestore, organizationId: Id) => database.collection(`organizations/${organizationId}/quizzes`);
const sessionRef = (database: Firestore, sessionId: Id) => database.doc(`quizSessions/${sessionId}`);
const lockRef = (database: Firestore, organizationId: Id) => database.doc(`quizActive/${organizationId}`);

function shardRef(database: Firestore, sessionId: Id, uid: string): DocumentReference {
  const shard = createHash("sha256").update(uid).digest()[0]! % QUIZ_LIMITS.counterShards;
  return sessionRef(database, sessionId).collection("counters").doc(`s${shard}`);
}

async function requireCommunityManager(
  database: Firestore, transaction: Transaction, uid: string, organizationId: Id,
): Promise<void> {
  await requireActiveActor(database, transaction, uid);
  const [organization, member] = await Promise.all([
    transaction.get(database.doc(`organizations/${organizationId}`)),
    transaction.get(database.doc(`organizations/${organizationId}/members/${uid}`)),
  ]);
  if (!organization.exists || organization.get("type") !== "community" || organization.get("status") !== "active") {
    throw new HttpsError("failed-precondition", "COMMUNITY_NOT_ACTIVE");
  }
  if (!member.exists || member.get("status") !== "active" || !["manager", "staff"].includes(member.get("role"))) {
    throw new HttpsError("permission-denied", "QUIZ_MANAGER_REQUIRED");
  }
  if (organization.get("listed") !== false) {
    throw new HttpsError("permission-denied", "QUIZ_RESTRICTED_TO_TEST_COMMUNITIES");
  }
}

function requireHost(session: FirebaseFirestore.DocumentSnapshot, uid: string): void {
  if (!session.exists) throw new HttpsError("not-found", "QUIZ_SESSION_NOT_FOUND");
  if (session.get("hostUid") !== uid) throw new HttpsError("permission-denied", "QUIZ_HOST_ONLY");
}

const millis = (ms: number) => Timestamp.fromMillis(ms);

// --- quiz CRUD (community managers, used by the web portal) -----------------

export async function saveQuizService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ quizId: string; questionCount: number }> {
  const organizationId = requireId(input.organizationId, "COMMUNITY_ID_INVALID");
  const title = cleanText(input.title, QUIZ_LIMITS.maxTitleLength, "QUIZ_TITLE_INVALID");
  if (input.status !== "draft" && input.status !== "ready") throw invalid("QUIZ_STATUS_INVALID");
  const status = input.status;
  if (!Array.isArray(input.questions) || input.questions.length > QUIZ_LIMITS.maxQuestions) {
    throw invalid("QUIZ_QUESTIONS_INVALID");
  }
  if (status === "ready" && input.questions.length === 0) throw invalid("QUIZ_QUESTIONS_REQUIRED");
  const questions = input.questions.map(parseQuizQuestion);
  const existingId = input.quizId === undefined || input.quizId === null ? null : requireId(input.quizId, "QUIZ_ID_INVALID");

  return database.runTransaction(async (transaction) => {
    await requireCommunityManager(database, transaction, uid, organizationId);
    const collection = quizzesOf(database, organizationId);
    let quizRef: DocumentReference;
    let createdAt = millis(nowMs);
    let createdBy = uid;
    let oldQuestions: FirebaseFirestore.QueryDocumentSnapshot[] = [];
    if (existingId) {
      quizRef = collection.doc(existingId);
      const existing = await transaction.get(quizRef);
      if (!existing.exists) throw new HttpsError("not-found", "QUIZ_NOT_FOUND");
      createdAt = existing.get("createdAt") ?? createdAt;
      createdBy = existing.get("createdBy") ?? uid;
      oldQuestions = (await transaction.get(quizRef.collection("questions"))).docs;
    } else {
      const all = await transaction.get(collection.limit(QUIZ_LIMITS.maxQuizzesPerCommunity));
      if (all.size >= QUIZ_LIMITS.maxQuizzesPerCommunity) {
        throw new HttpsError("resource-exhausted", "QUIZ_LIMIT_REACHED");
      }
      quizRef = collection.doc();
    }
    const questionId = (index: number) => String(index).padStart(3, "0");
    for (const old of oldQuestions) {
      if (Number(old.id) >= questions.length) transaction.delete(old.ref);
    }
    questions.forEach((question, index) => {
      transaction.set(quizRef.collection("questions").doc(questionId(index)), { order: index, ...question });
    });
    transaction.set(quizRef, {
      organizationId, title, status, questionCount: questions.length, createdBy, createdAt, updatedAt: millis(nowMs),
    });
    return { quizId: quizRef.id, questionCount: questions.length };
  });
}

export async function listQuizzesService(database: Firestore, uid: string, input: Record<string, unknown>) {
  const organizationId = requireId(input.organizationId, "COMMUNITY_ID_INVALID");
  await database.runTransaction((transaction) => requireCommunityManager(database, transaction, uid, organizationId));
  const snapshot = await quizzesOf(database, organizationId)
    .orderBy("updatedAt", "desc").limit(QUIZ_LIMITS.maxQuizzesPerCommunity).get();
  return {
    quizzes: snapshot.docs.map((doc) => ({
      quizId: doc.id,
      title: String(doc.get("title") ?? ""),
      status: String(doc.get("status") ?? "draft"),
      questionCount: Number(doc.get("questionCount") ?? 0),
      updatedAtMs: (doc.get("updatedAt") as Timestamp | undefined)?.toMillis() ?? 0,
    })),
  };
}

export async function getQuizService(database: Firestore, uid: string, input: Record<string, unknown>) {
  const organizationId = requireId(input.organizationId, "COMMUNITY_ID_INVALID");
  const quizId = requireId(input.quizId, "QUIZ_ID_INVALID");
  await database.runTransaction((transaction) => requireCommunityManager(database, transaction, uid, organizationId));
  const quizRef = quizzesOf(database, organizationId).doc(quizId);
  const [quiz, questions] = await Promise.all([quizRef.get(), quizRef.collection("questions").orderBy("order").get()]);
  if (!quiz.exists) throw new HttpsError("not-found", "QUIZ_NOT_FOUND");
  return {
    quizId,
    title: String(quiz.get("title") ?? ""),
    status: String(quiz.get("status") ?? "draft"),
    questions: questions.docs.map((doc) => ({
      type: doc.get("type"), text: doc.get("text"), options: doc.get("options"),
      correctIndex: doc.get("correctIndex"), timeLimitSec: doc.get("timeLimitSec"),
    })),
  };
}

export async function deleteQuizService(database: Firestore, uid: string, input: Record<string, unknown>) {
  const organizationId = requireId(input.organizationId, "COMMUNITY_ID_INVALID");
  const quizId = requireId(input.quizId, "QUIZ_ID_INVALID");
  await database.runTransaction((transaction) => requireCommunityManager(database, transaction, uid, organizationId));
  await database.recursiveDelete(quizzesOf(database, organizationId).doc(quizId));
  return { quizId };
}

// --- session lifecycle ------------------------------------------------------

function questionPhase(question: FirebaseFirestore.DocumentSnapshot, index: number, nowMs: number) {
  const startsAt = nowMs + QUIZ_LIMITS.startDelayMs;
  const timeLimitSec = Number(question.get("timeLimitSec"));
  return {
    status: "question",
    currentIndex: index,
    questionStartsAt: millis(startsAt),
    questionEndsAt: millis(startsAt + timeLimitSec * 1000),
    // The correct answer is deliberately absent; it is published with `reveal`.
    currentQuestion: {
      index, type: question.get("type"), text: question.get("text"),
      options: question.get("options"), timeLimitSec,
    },
    reveal: null,
    lastActivityAt: millis(nowMs),
  };
}

export async function startQuizSessionService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ sessionId: string }> {
  const organizationId = requireId(input.organizationId, "COMMUNITY_ID_INVALID");
  const quizId = requireId(input.quizId, "QUIZ_ID_INVALID");
  return database.runTransaction(async (transaction) => {
    await requireCommunityManager(database, transaction, uid, organizationId);
    const quizRef = quizzesOf(database, organizationId).doc(quizId);
    const [quiz, questions, lock] = await Promise.all([
      transaction.get(quizRef),
      transaction.get(quizRef.collection("questions").orderBy("order")),
      transaction.get(lockRef(database, organizationId)),
    ]);
    if (!quiz.exists) throw new HttpsError("not-found", "QUIZ_NOT_FOUND");
    if (quiz.get("status") !== "ready" || questions.empty) throw new HttpsError("failed-precondition", "QUIZ_NOT_READY");
    if (lock.exists) {
      const current = await transaction.get(sessionRef(database, String(lock.get("sessionId"))));
      if (current.exists && (ACTIVE_SESSION_STATUSES as readonly string[]).includes(current.get("status"))) {
        throw new HttpsError("failed-precondition", "QUIZ_SESSION_ACTIVE");
      }
    }
    const ref = database.collection("quizSessions").doc();
    transaction.set(ref, {
      organizationId, quizId, hostUid: uid, title: String(quiz.get("title") ?? ""),
      status: "lobby", locked: false, totalQuestions: questions.size, currentIndex: -1,
      questionStartsAt: null, questionEndsAt: null, currentQuestion: null, reveal: null, podium: null,
      createdAt: millis(nowMs), lastActivityAt: millis(nowMs), finishedAt: null, expiresAt: null,
    });
    // Frozen copy: later edits in the portal cannot change a game in progress.
    questions.docs.forEach((doc, index) => {
      transaction.set(ref.collection("questions").doc(String(index)), {
        index, type: doc.get("type"), text: doc.get("text"), options: doc.get("options"),
        correctIndex: doc.get("correctIndex"), timeLimitSec: doc.get("timeLimitSec"),
      });
    });
    transaction.set(ref.collection("counters").doc("lobby"), { playerCount: 0 });
    transaction.set(lockRef(database, organizationId), { sessionId: ref.id, hostUid: uid, createdAt: millis(nowMs) });
    return { sessionId: ref.id };
  });
}

export async function joinQuizSessionService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ sessionId: string; nickname: string; serverTimeMs: number }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  const nickname = parseNickname(input.nickname);
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const ref = sessionRef(database, sessionId);
    const session = await transaction.get(ref);
    if (!session.exists) throw new HttpsError("not-found", "QUIZ_SESSION_NOT_FOUND");
    const status = String(session.get("status"));
    if (!(ACTIVE_SESSION_STATUSES as readonly string[]).includes(status)) {
      throw new HttpsError("failed-precondition", "QUIZ_SESSION_ENDED");
    }
    // Following the community is not required: the session id shared on the host screen (QR) is the join code.
    const playerRef = ref.collection("players").doc(uid);
    const nicknameRef = ref.collection("nicknames").doc(nickname.key);
    const lobbyRef = ref.collection("counters").doc("lobby");
    const [player, taken, lobby] = await Promise.all([
      transaction.get(playerRef),
      transaction.get(nicknameRef),
      transaction.get(lobbyRef),
    ]);
    if (session.get("hostUid") === uid) throw new HttpsError("failed-precondition", "QUIZ_HOST_CANNOT_PLAY");
    if (player.exists) {
      // Reconnect: keeps the original nickname and never takes another slot.
      if (player.get("status") === "kicked") throw new HttpsError("permission-denied", "QUIZ_KICKED");
      return { sessionId, nickname: String(player.get("nickname")), serverTimeMs: nowMs };
    }
    if (status !== "lobby" || session.get("locked") === true) throw new HttpsError("failed-precondition", "QUIZ_LOCKED");
    const count = Number(lobby.get("playerCount") ?? 0);
    if (count >= QUIZ_LIMITS.maxPlayers) throw new HttpsError("resource-exhausted", "QUIZ_FULL");
    if (taken.exists) throw new HttpsError("already-exists", "QUIZ_NICKNAME_TAKEN");
    transaction.create(playerRef, {
      userId: uid, nickname: nickname.display, status: "active", score: 0, rank: 0, streak: 0,
      correctCount: 0, lastResult: null, joinedAt: millis(nowMs),
    });
    transaction.create(nicknameRef, { userId: uid });
    transaction.set(lobbyRef, { playerCount: count + 1 });
    return { sessionId, nickname: nickname.display, serverTimeMs: nowMs };
  });
}

export async function setQuizLockService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ locked: boolean }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  if (typeof input.locked !== "boolean") throw invalid("QUIZ_LOCK_INVALID");
  const locked = input.locked;
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const ref = sessionRef(database, sessionId);
    const session = await transaction.get(ref);
    requireHost(session, uid);
    if (session.get("status") !== "lobby") throw new HttpsError("failed-precondition", "QUIZ_INVALID_PHASE");
    transaction.update(ref, { locked, lastActivityAt: millis(nowMs) });
    return { locked };
  });
}

export async function startQuizGameService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ status: string }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const ref = sessionRef(database, sessionId);
    const [session, lobby, question] = await Promise.all([
      transaction.get(ref),
      transaction.get(ref.collection("counters").doc("lobby")),
      transaction.get(ref.collection("questions").doc("0")),
    ]);
    requireHost(session, uid);
    if (session.get("status") !== "lobby") throw new HttpsError("failed-precondition", "QUIZ_INVALID_PHASE");
    if (Number(lobby.get("playerCount") ?? 0) < 1) throw new HttpsError("failed-precondition", "QUIZ_NO_PLAYERS");
    if (!question.exists) throw new HttpsError("failed-precondition", "QUIZ_NOT_READY");
    transaction.update(ref, { ...questionPhase(question, 0, nowMs), locked: true });
    return { status: "question" };
  });
}

export async function submitQuizAnswerService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ accepted: true }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  const index = requireIndex(input.index, QUIZ_LIMITS.maxQuestions - 1, "QUIZ_INDEX_INVALID");
  const option = requireIndex(input.option, 3, "QUIZ_OPTION_INVALID");
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const ref = sessionRef(database, sessionId);
    const answerRef = ref.collection("answers").doc(`${index}_${uid}`);
    const [session, player, question, existing] = await Promise.all([
      transaction.get(ref),
      transaction.get(ref.collection("players").doc(uid)),
      transaction.get(ref.collection("questions").doc(String(index))),
      transaction.get(answerRef),
    ]);
    if (!session.exists) throw new HttpsError("not-found", "QUIZ_SESSION_NOT_FOUND");
    if (!player.exists || player.get("status") !== "active") throw new HttpsError("permission-denied", "QUIZ_NOT_PLAYER");
    if (session.get("status") !== "question" || session.get("currentIndex") !== index || !question.exists) {
      throw new HttpsError("failed-precondition", "QUIZ_NOT_ACCEPTING_ANSWERS");
    }
    if (existing.exists) throw new HttpsError("already-exists", "QUIZ_ALREADY_ANSWERED");
    const startsMs = (session.get("questionStartsAt") as Timestamp).toMillis();
    const endsMs = (session.get("questionEndsAt") as Timestamp).toMillis();
    if (nowMs < startsMs - QUIZ_LIMITS.earlyAnswerToleranceMs) throw new HttpsError("failed-precondition", "QUIZ_QUESTION_NOT_STARTED");
    if (nowMs > endsMs + QUIZ_LIMITS.answerGraceMs) throw new HttpsError("failed-precondition", "QUIZ_TIME_UP");
    const options = question.get("options") as string[];
    if (option >= options.length) throw invalid("QUIZ_OPTION_INVALID");
    const timeLimitMs = Number(question.get("timeLimitSec")) * 1000;
    const elapsedMs = Math.min(Math.max(nowMs - startsMs, 0), timeLimitMs);
    const correct = option === question.get("correctIndex");
    transaction.create(answerRef, {
      index, userId: uid, option, elapsedMs, correct,
      points: scoreAnswer(correct, elapsedMs, timeLimitMs), answeredAt: millis(nowMs),
    });
    transaction.set(shardRef(database, sessionId, uid), { [`a${index}`]: FieldValue.increment(1) }, { merge: true });
    // Correctness is intentionally not returned; players learn it at reveal.
    return { accepted: true };
  });
}

interface RankedPlayer { uid: string; nickname: string; score: number; correctCount: number; rank: number }

function rankPlayers(players: Array<Omit<RankedPlayer, "rank">>): RankedPlayer[] {
  const sorted = [...players].sort((a, b) => b.score - a.score || a.nickname.localeCompare(b.nickname, "tr"));
  return sorted.map((player) => ({
    ...player,
    // Equal scores share the better rank (1, 1, 3).
    rank: 1 + sorted.filter((other) => other.score > player.score).length,
  }));
}

export async function revealQuizQuestionService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ status: string; alreadyRevealed: boolean }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  const index = requireIndex(input.index, QUIZ_LIMITS.maxQuestions - 1, "QUIZ_INDEX_INVALID");
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const ref = sessionRef(database, sessionId);
    const [session, question, answers, players] = await Promise.all([
      transaction.get(ref),
      transaction.get(ref.collection("questions").doc(String(index))),
      transaction.get(ref.collection("answers").where("index", "==", index).limit(QUIZ_LIMITS.maxPlayers + 10)),
      transaction.get(ref.collection("players").limit(QUIZ_LIMITS.maxPlayers + 1)),
    ]);
    if (!session.exists) throw new HttpsError("not-found", "QUIZ_SESSION_NOT_FOUND");
    // Idempotent: the host and a backup player may both ask for the same reveal.
    if (session.get("status") === "reveal" && session.get("currentIndex") === index) {
      return { status: "reveal", alreadyRevealed: true };
    }
    if (session.get("status") !== "question" || session.get("currentIndex") !== index || !question.exists) {
      throw new HttpsError("failed-precondition", "QUIZ_INVALID_PHASE");
    }
    if (session.get("hostUid") !== uid) {
      const caller = players.docs.find((doc) => doc.id === uid);
      if (!caller || caller.get("status") !== "active") throw new HttpsError("permission-denied", "QUIZ_NOT_PLAYER");
      const endsMs = (session.get("questionEndsAt") as Timestamp).toMillis();
      if (nowMs < endsMs + QUIZ_LIMITS.playerRevealGraceMs) throw new HttpsError("failed-precondition", "QUIZ_REVEAL_TOO_EARLY");
    }

    const optionCount = (question.get("options") as string[]).length;
    const correctIndex = Number(question.get("correctIndex"));
    const answerByUser = new Map(answers.docs.map((doc) => [String(doc.get("userId")), doc]));
    const activePlayers = players.docs.filter((doc) => doc.get("status") === "active");
    const distribution = Array<number>(optionCount).fill(0);
    let answered = 0;
    let correctAnswers = 0;
    let elapsedTotal = 0;
    const scored = activePlayers.map((doc) => {
      const answer = answerByUser.get(doc.id);
      const points = Number(answer?.get("points") ?? 0);
      const correct = answer?.get("correct") === true;
      if (answer) {
        answered += 1;
        elapsedTotal += Number(answer.get("elapsedMs") ?? 0);
        distribution[Number(answer.get("option"))] = (distribution[Number(answer.get("option"))] ?? 0) + 1;
        if (correct) correctAnswers += 1;
      }
      return {
        doc, answer, points, correct,
        streak: correct ? Number(doc.get("streak") ?? 0) + 1 : 0,
        base: {
          uid: doc.id,
          nickname: String(doc.get("nickname")),
          score: Number(doc.get("score") ?? 0) + points,
          correctCount: Number(doc.get("correctCount") ?? 0) + (correct ? 1 : 0),
        },
      };
    });
    const ranked = rankPlayers(scored.map((item) => item.base));
    const rankOf = new Map(ranked.map((player) => [player.uid, player.rank]));
    for (const item of scored) {
      transaction.update(item.doc.ref, {
        score: item.base.score,
        correctCount: item.base.correctCount,
        rank: rankOf.get(item.doc.id) ?? 0,
        streak: item.streak,
        lastResult: { index, correct: item.correct, points: item.points, selectedOption: item.answer ? Number(item.answer.get("option")) : -1 },
      });
    }
    transaction.set(ref.collection("results").doc(String(index)), {
      index, text: question.get("text"), correctIndex, distribution, answered, correctAnswers,
      playerCount: activePlayers.length,
      avgElapsedMs: answered === 0 ? 0 : Math.round(elapsedTotal / answered),
    });
    transaction.update(ref, {
      status: "reveal",
      reveal: {
        index, correctIndex, distribution, unanswered: activePlayers.length - answered,
        top5: ranked.slice(0, 5).map(({ nickname, score, rank }) => ({ nickname, score, rank })),
      },
      lastActivityAt: millis(nowMs),
    });
    return { status: "reveal", alreadyRevealed: false };
  });
}

export async function nextQuizQuestionService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ status: string }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const ref = sessionRef(database, sessionId);
    const session = await transaction.get(ref);
    requireHost(session, uid);
    if (session.get("status") !== "reveal") throw new HttpsError("failed-precondition", "QUIZ_INVALID_PHASE");
    const nextIndex = Number(session.get("currentIndex")) + 1;
    const total = Number(session.get("totalQuestions"));
    if (nextIndex < total) {
      const question = await transaction.get(ref.collection("questions").doc(String(nextIndex)));
      if (!question.exists) throw new HttpsError("failed-precondition", "QUIZ_NOT_READY");
      transaction.update(ref, questionPhase(question, nextIndex, nowMs));
      return { status: "question" };
    }

    const organizationId = String(session.get("organizationId"));
    const [results, players, lock] = await Promise.all([
      transaction.get(ref.collection("results").orderBy("index").limit(QUIZ_LIMITS.maxQuestions + 1)),
      transaction.get(ref.collection("players").limit(QUIZ_LIMITS.maxPlayers + 1)),
      transaction.get(lockRef(database, organizationId)),
    ]);
    const ranked = rankPlayers(players.docs.filter((doc) => doc.get("status") === "active").map((doc) => ({
      uid: doc.id, nickname: String(doc.get("nickname")),
      score: Number(doc.get("score") ?? 0), correctCount: Number(doc.get("correctCount") ?? 0),
    })));
    const questions = results.docs.map((doc) => {
      const playerCount = Number(doc.get("playerCount") ?? 0);
      return {
        index: Number(doc.get("index")),
        text: String(doc.get("text")),
        answered: Number(doc.get("answered") ?? 0),
        correctPercent: playerCount === 0 ? 0 : Math.round((Number(doc.get("correctAnswers") ?? 0) / playerCount) * 100),
        avgElapsedMs: Number(doc.get("avgElapsedMs") ?? 0),
      };
    });
    // The report carries nicknames only (no UIDs) and expires on its own.
    transaction.set(database.doc(`quizReports/${sessionId}`), {
      sessionId, organizationId, hostUid: uid, title: session.get("title") ?? "",
      playerCount: ranked.length, totalQuestions: total, finishedAt: millis(nowMs),
      expiresAt: millis(nowMs + QUIZ_LIMITS.reportRetentionMs),
      ranking: ranked.map(({ rank, nickname, score, correctCount }) => ({ rank, nickname, score, correctCount })),
      questions,
    });
    transaction.update(ref, {
      status: "finished", locked: true,
      podium: ranked.slice(0, 3).map(({ nickname, score, rank }) => ({ nickname, score, rank })),
      finishedAt: millis(nowMs), expiresAt: millis(nowMs + QUIZ_LIMITS.sessionRetentionMs), lastActivityAt: millis(nowMs),
    });
    if (lock.exists && lock.get("sessionId") === sessionId) transaction.delete(lock.ref);
    return { status: "finished" };
  });
}

async function releaseLock(database: Firestore, transaction: Transaction, organizationId: Id, sessionId: Id): Promise<void> {
  const lock = await transaction.get(lockRef(database, organizationId));
  if (lock.exists && lock.get("sessionId") === sessionId) transaction.delete(lock.ref);
}

export async function kickQuizPlayerService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ kicked: boolean }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  const playerId = requireId(input.playerId, "QUIZ_PLAYER_ID_INVALID");
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const ref = sessionRef(database, sessionId);
    const [session, player] = await Promise.all([transaction.get(ref), transaction.get(ref.collection("players").doc(playerId))]);
    requireHost(session, uid);
    if (!player.exists) throw new HttpsError("not-found", "QUIZ_PLAYER_NOT_FOUND");
    // A kicked player keeps the slot and nickname reserved so the same account cannot return.
    transaction.update(player.ref, { status: "kicked" });
    transaction.update(ref, { lastActivityAt: millis(nowMs) });
    return { kicked: true };
  });
}

export async function cancelQuizSessionService(
  database: Firestore, uid: string, input: Record<string, unknown>, nowMs = Date.now(),
): Promise<{ status: string }> {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, uid);
    const session = await transaction.get(sessionRef(database, sessionId));
    requireHost(session, uid);
    return cancelSession(database, transaction, session, nowMs);
  });
}

async function cancelSession(
  database: Firestore, transaction: Transaction, session: FirebaseFirestore.DocumentSnapshot, nowMs: number,
): Promise<{ status: string }> {
  const status = String(session.get("status"));
  if (!(ACTIVE_SESSION_STATUSES as readonly string[]).includes(status)) return { status };
  await releaseLock(database, transaction, String(session.get("organizationId")), session.id);
  transaction.update(session.ref, {
    status: "cancelled", locked: true,
    expiresAt: millis(nowMs + QUIZ_LIMITS.sessionRetentionMs), lastActivityAt: millis(nowMs),
  });
  return { status: "cancelled" };
}

export async function getQuizReportService(database: Firestore, uid: string, input: Record<string, unknown>) {
  const sessionId = requireId(input.sessionId, "QUIZ_SESSION_ID_INVALID");
  const report = await database.doc(`quizReports/${sessionId}`).get();
  if (!report.exists) throw new HttpsError("not-found", "QUIZ_REPORT_NOT_FOUND");
  await database.runTransaction((transaction) => (
    requireCommunityManager(database, transaction, uid, String(report.get("organizationId")))
  ));
  return {
    sessionId, title: report.get("title"), playerCount: report.get("playerCount"),
    totalQuestions: report.get("totalQuestions"), ranking: report.get("ranking"), questions: report.get("questions"),
    finishedAtMs: (report.get("finishedAt") as Timestamp).toMillis(),
  };
}

// --- scheduled cleanup ------------------------------------------------------

/** Cancels idle sessions, deletes ended sessions after 3 days and reports after 10 days. */
export async function cleanupQuizDataService(
  database: Firestore, nowMs = Date.now(), pageSize = 25,
): Promise<{ cancelled: number; sessionsDeleted: number; reportsDeleted: number }> {
  const idle = await database.collection("quizSessions")
    .where("status", "in", [...ACTIVE_SESSION_STATUSES])
    .where("lastActivityAt", "<=", millis(nowMs - QUIZ_LIMITS.idleCancelMs))
    .limit(pageSize).get();
  for (const doc of idle.docs) {
    await database.runTransaction(async (transaction) => {
      const fresh = await transaction.get(doc.ref);
      if (fresh.exists) await cancelSession(database, transaction, fresh, nowMs);
    });
  }
  const expired = await database.collection("quizSessions")
    .where("expiresAt", "<=", millis(nowMs)).limit(pageSize).get();
  for (const doc of expired.docs) await database.recursiveDelete(doc.ref);
  const reports = await database.collection("quizReports")
    .where("expiresAt", "<=", millis(nowMs)).limit(pageSize * 4).get();
  const batch = database.batch();
  reports.docs.forEach((doc) => batch.delete(doc.ref));
  if (!reports.empty) await batch.commit();
  return { cancelled: idle.size, sessionsDeleted: expired.size, reportsDeleted: reports.size };
}

/** Removes a deleted account's quiz traces from sessions that have not expired yet. */
export async function eraseQuizDataForUser(database: Firestore, uid: string): Promise<void> {
  for (const group of ["players", "answers", "nicknames"] as const) {
    while (true) {
      const page = await database.collectionGroup(group).where("userId", "==", uid).limit(400).get();
      if (page.empty) break;
      const batch = database.batch();
      page.docs.forEach((doc) => batch.delete(doc.ref));
      await batch.commit();
    }
  }
}

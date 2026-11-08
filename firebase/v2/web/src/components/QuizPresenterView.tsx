import { useCallback, useEffect, useMemo, useState } from "react";
import { httpsCallable } from "firebase/functions";
import { doc, collection, onSnapshot, type Unsubscribe } from "firebase/firestore";
import { functions, getRealtimeFirestoreDb } from "../firebase";
import { quizErrorMessage, quizStrings as s } from "../quizStrings";
import { QuizPresenterLobby } from "./QuizPresenterLobby";
import { QuizPresenterQuestion } from "./QuizPresenterQuestion";
import { QuizPresenterReveal } from "./QuizPresenterReveal";
import { QuizPresenterPodium } from "./QuizPresenterPodium";
import "./QuizPresenter.css";

export type QuizSessionStatus = "lobby" | "question" | "reveal" | "finished" | "cancelled";

export interface QuizSessionDoc {
  organizationId: string;
  quizId: string;
  hostUid: string;
  title: string;
  status: QuizSessionStatus;
  locked: boolean;
  totalQuestions: number;
  currentIndex: number;
  questionStartsAt?: unknown;
  questionEndsAt?: unknown;
  currentQuestion?: {
    index: number;
    type: "multiple_choice" | "true_false";
    text: string;
    options: string[];
    timeLimitSec: number;
  } | null;
  reveal?: {
    index: number;
    correctIndex: number;
    distribution: number[];
    unanswered: number;
    top5?: Array<{ nickname: string; score: number; rank: number }>;
  } | null;
  podium?: Array<{ nickname: string; score: number; rank: number }> | null;
  finishedAt?: unknown;
  expiresAt?: unknown;
}

export interface QuizPlayerItem {
  userId: string;
  nickname: string;
  score: number;
  rank: number;
  correctCount: number;
  status: string;
}

export interface QuizReportView {
  sessionId: string;
  title: string;
  playerCount: number;
  totalQuestions: number;
  finishedAt: unknown;
  ranking: Array<{ rank: number; nickname: string; score: number; correctCount: number }>;
  questions: Array<{
    index: number;
    text: string;
    answered: number;
    correctPercent: number;
    avgElapsedMs: number;
  }>;
}

export interface QuizPresenterViewProps {
  sessionId: string;
  organizationId: string;
  organizationName?: string;
  onClose: () => void;
}

const setQuizLockCallable = httpsCallable<{ sessionId: string; locked: boolean }, { locked: boolean }>(
  functions,
  "setQuizLock",
);
const startQuizGameCallable = httpsCallable<{ sessionId: string }, { status: string }>(
  functions,
  "startQuizGame",
);
const revealQuizQuestionCallable = httpsCallable<
  { sessionId: string; index: number },
  { status: string; alreadyRevealed: boolean }
>(functions, "revealQuizQuestion");
const nextQuizQuestionCallable = httpsCallable<{ sessionId: string }, { status: string }>(
  functions,
  "nextQuizQuestion",
);
const kickQuizPlayerCallable = httpsCallable<{ sessionId: string; playerId: string }, { kicked: boolean }>(
  functions,
  "kickQuizPlayer",
);
const cancelQuizSessionCallable = httpsCallable<{ sessionId: string }, { status: string }>(
  functions,
  "cancelQuizSession",
);
const getQuizReportCallable = httpsCallable<{ sessionId: string }, QuizReportView>(
  functions,
  "getQuizReport",
);

function toMillis(val: unknown): number {
  if (!val) return 0;
  if (typeof val === "object" && val !== null) {
    if ("toMillis" in val && typeof (val as { toMillis: () => number }).toMillis === "function") {
      return (val as { toMillis: () => number }).toMillis();
    }
    if ("seconds" in val && typeof (val as { seconds: number }).seconds === "number") {
      return (val as { seconds: number }).seconds * 1000;
    }
  }
  if (typeof val === "number") return val;
  if (val instanceof Date) return val.getTime();
  const parsed = new Date(String(val)).getTime();
  return isNaN(parsed) ? 0 : parsed;
}

export function QuizPresenterView({
  sessionId,
  organizationId: _organizationId,
  organizationName,
  onClose,
}: QuizPresenterViewProps) {
  const [session, setSession] = useState<QuizSessionDoc | null>(null);
  const [players, setPlayers] = useState<QuizPlayerItem[]>([]);
  const [counterShards, setCounterShards] = useState<Record<string, unknown>[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");
  const [actionError, setActionError] = useState("");
  const [toastNotice, setToastNotice] = useState("");
  const [busyAction, setBusyAction] = useState(false);

  // Countdown timer state
  const [remainingSeconds, setRemainingSeconds] = useState(20);
  const [totalSeconds, setTotalSeconds] = useState(20);

  // Dialogs & Modals
  const [exitConfirmOpen, setExitConfirmOpen] = useState(false);
  const [cancelConfirmOpen, setCancelConfirmOpen] = useState(false);
  const [reportData, setReportData] = useState<QuizReportView | null>(null);
  const [loadingReport, setLoadingReport] = useState(false);

  // Realtime Firestore listeners
  useEffect(() => {
    let unsubSession: Unsubscribe | null = null;
    let unsubPlayers: Unsubscribe | null = null;
    let unsubCounters: Unsubscribe | null = null;
    let active = true;

    void getRealtimeFirestoreDb()
      .then((db) => {
        if (!active) return;
        const sessionRef = doc(db, "quizSessions", sessionId);
        unsubSession = onSnapshot(
          sessionRef,
          (snap) => {
            if (!snap.exists()) {
              setError(quizErrorMessage("QUIZ_SESSION_NOT_FOUND"));
              setLoading(false);
              return;
            }
            setSession(snap.data() as QuizSessionDoc);
            setLoading(false);
          },
          (err) => {
            console.error("Quiz session listener error:", err);
            setError(quizErrorMessage(err));
            setLoading(false);
          },
        );

        const playersRef = collection(db, "quizSessions", sessionId, "players");
        unsubPlayers = onSnapshot(
          playersRef,
          (snap) => {
            const list: QuizPlayerItem[] = [];
            snap.forEach((d) => {
              const data = d.data();
              list.push({
                userId: d.id,
                nickname: String(data.nickname || ""),
                score: Number(data.score || 0),
                rank: Number(data.rank || 0),
                correctCount: Number(data.correctCount || 0),
                status: String(data.status || "active"),
              });
            });
            setPlayers(list);
          },
          (err) => {
            console.error("Quiz players listener error:", err);
          },
        );

        const countersRef = collection(db, "quizSessions", sessionId, "counters");
        unsubCounters = onSnapshot(
          countersRef,
          (snap) => {
            const shards: Record<string, unknown>[] = [];
            snap.forEach((d) => shards.push(d.data()));
            setCounterShards(shards);
          },
          (err) => {
            console.error("Quiz counters listener error:", err);
          },
        );
      })
      .catch((err) => {
        setError(quizErrorMessage(err));
        setLoading(false);
      });

    return () => {
      active = false;
      unsubSession?.();
      unsubPlayers?.();
      unsubCounters?.();
    };
  }, [sessionId]);

  const activePlayers = useMemo(() => {
    return players.filter((p) => p.status === "active");
  }, [players]);

  // Aggregate answers for the current question
  const answeredCount = useMemo(() => {
    if (!session || session.currentIndex < 0) return 0;
    const key = `a${session.currentIndex}`;
    let total = 0;
    for (const shard of counterShards) {
      const count = shard[key];
      if (typeof count === "number") total += count;
    }
    return total;
  }, [session, counterShards]);

  // Synchronized countdown calculation
  useEffect(() => {
    if (!session || session.status !== "question") return;

    const startsMs = toMillis(session.questionStartsAt);
    const endsMs = toMillis(session.questionEndsAt);
    const calculatedTotal =
      startsMs && endsMs
        ? Math.max(1, Math.round((endsMs - startsMs) / 1000))
        : session.currentQuestion?.timeLimitSec || 20;

    setTotalSeconds(calculatedTotal);

    const updateTimer = () => {
      if (!endsMs) {
        setRemainingSeconds(0);
        return;
      }
      const rem = Math.max(0, Math.ceil((endsMs - Date.now()) / 1000));
      setRemainingSeconds(rem);
    };

    updateTimer();
    const interval = setInterval(updateTimer, 250);
    return () => clearInterval(interval);
  }, [
    session?.status,
    session?.currentIndex,
    session?.questionStartsAt,
    session?.questionEndsAt,
    session?.currentQuestion?.timeLimitSec,
  ]);

  // Action handlers
  const handleToggleLock = useCallback(async () => {
    if (!session || busyAction) return;
    setBusyAction(true);
    setActionError("");
    try {
      await setQuizLockCallable({ sessionId, locked: !session.locked });
    } catch (err) {
      setActionError(quizErrorMessage(err));
    } finally {
      setBusyAction(false);
    }
  }, [session, busyAction, sessionId]);

  const handleStartGame = useCallback(async () => {
    if (!session || busyAction || activePlayers.length === 0) return;
    setBusyAction(true);
    setActionError("");
    try {
      await startQuizGameCallable({ sessionId });
    } catch (err) {
      setActionError(quizErrorMessage(err));
    } finally {
      setBusyAction(false);
    }
  }, [session, busyAction, activePlayers.length, sessionId]);

  const handleEarlyReveal = useCallback(async () => {
    if (!session || busyAction || session.currentIndex < 0) return;
    setBusyAction(true);
    setActionError("");
    try {
      await revealQuizQuestionCallable({ sessionId, index: session.currentIndex });
    } catch (err) {
      setActionError(quizErrorMessage(err));
    } finally {
      setBusyAction(false);
    }
  }, [session, busyAction, sessionId]);

  const handleNextQuestion = useCallback(async () => {
    if (!session || busyAction) return;
    setBusyAction(true);
    setActionError("");
    try {
      await nextQuizQuestionCallable({ sessionId });
    } catch (err) {
      setActionError(quizErrorMessage(err));
    } finally {
      setBusyAction(false);
    }
  }, [session, busyAction, sessionId]);

  const handleKickPlayer = useCallback(
    async (playerId: string) => {
      if (busyAction) return;
      setBusyAction(true);
      setActionError("");
      try {
        await kickQuizPlayerCallable({ sessionId, playerId });
        setToastNotice(s.kickedNotice);
        setTimeout(() => setToastNotice(""), 3000);
      } catch (err) {
        setActionError(quizErrorMessage(err));
      } finally {
        setBusyAction(false);
      }
    },
    [busyAction, sessionId],
  );

  const handleCancelSession = useCallback(async () => {
    if (busyAction) return;
    setBusyAction(true);
    setActionError("");
    try {
      await cancelQuizSessionCallable({ sessionId });
      setToastNotice(s.sessionCancelledNotice);
      setTimeout(() => {
        onClose();
      }, 1000);
    } catch (err) {
      setActionError(quizErrorMessage(err));
      setBusyAction(false);
    }
  }, [busyAction, sessionId, onClose]);

  const handleOpenNewTab = useCallback(() => {
    const url = `${window.location.origin}${window.location.pathname}?quizSessionId=${encodeURIComponent(sessionId)}`;
    window.open(url, "_blank", "noopener,noreferrer");
  }, [sessionId]);

  const handleViewReport = useCallback(async () => {
    setLoadingReport(true);
    setActionError("");
    try {
      const res = await getQuizReportCallable({ sessionId });
      setReportData(res.data);
    } catch (err) {
      setActionError(quizErrorMessage(err));
    } finally {
      setLoadingReport(false);
    }
  }, [sessionId]);

  // Global Keyboard shortcuts: Escape, F, Enter
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      const activeEl = document.activeElement;
      if (activeEl && (activeEl.tagName === "INPUT" || activeEl.tagName === "TEXTAREA")) {
        return;
      }

      if (e.key === "Escape") {
        e.preventDefault();
        setExitConfirmOpen((prev) => !prev);
        return;
      }

      if (e.key === "f" || e.key === "F") {
        e.preventDefault();
        if (typeof document !== "undefined") {
          if (!document.fullscreenElement) {
            void document.documentElement.requestFullscreen().catch(() => {});
          } else {
            void document.exitFullscreen().catch(() => {});
          }
        }
        return;
      }

      if (e.key === "Enter" || e.code === "Enter") {
        e.preventDefault();
        if (busyAction) return;
        if (session?.status === "lobby") {
          void handleStartGame();
        } else if (session?.status === "question") {
          void handleEarlyReveal();
        } else if (session?.status === "reveal") {
          void handleNextQuestion();
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [busyAction, session?.status, handleStartGame, handleEarlyReveal, handleNextQuestion]);

  // Render Loading
  if (loading) {
    return (
      <div className="quiz-presenter" style={{ display: "grid", placeItems: "center" }}>
        <div style={{ textAlign: "center", color: "#ffffff" }}>
          <div className="pulse-dot" style={{ width: 24, height: 24, margin: "0 auto 16px" }} />
          <h2 style={{ fontSize: 22, fontWeight: 700 }}>{s.presenterTitle} hazırlanıyor…</h2>
          <p style={{ color: "#9aa0a6" }}>Oturum verileri ve katılımcılar yükleniyor</p>
        </div>
      </div>
    );
  }

  // Render Error
  if (error || !session) {
    return (
      <div className="quiz-presenter" style={{ display: "grid", placeItems: "center" }}>
        <div
          style={{
            maxWidth: 480,
            padding: 32,
            background: "#181b20",
            borderRadius: 16,
            border: "1px solid rgba(255, 255, 255, 0.1)",
            textAlign: "center",
          }}
        >
          <div style={{ fontSize: 48, marginBottom: 16 }}>⚠️</div>
          <h2 style={{ fontSize: 20, fontWeight: 700, color: "#ffffff", marginBottom: 8 }}>
            {error || quizErrorMessage("QUIZ_SESSION_NOT_FOUND")}
          </h2>
          <button
            type="button"
            className="podium-exit-btn"
            style={{ marginTop: 24 }}
            onClick={onClose}
          >
            {s.exitPresentation}
          </button>
        </div>
      </div>
    );
  }

  // Render Cancelled Phase
  if (session.status === "cancelled") {
    return (
      <div className="quiz-presenter" style={{ display: "grid", placeItems: "center" }}>
        <div
          style={{
            maxWidth: 480,
            padding: 36,
            background: "#181b20",
            borderRadius: 16,
            border: "1px solid rgba(211, 47, 47, 0.3)",
            textAlign: "center",
          }}
        >
          <div style={{ fontSize: 52, marginBottom: 16 }}>🚫</div>
          <h2 style={{ fontSize: 24, fontWeight: 700, color: "#ef5350", marginBottom: 12 }}>
            {s.sessionCancelledNotice}
          </h2>
          <p style={{ color: "#9aa0a6", marginBottom: 24 }}>
            Bu quiz oturumu sonlandırılmıştır. Dilediğiniz zaman panodan yeni bir oturum başlatabilirsiniz.
          </p>
          <button
            type="button"
            className="podium-exit-btn"
            onClick={onClose}
          >
            {s.exitPresentation}
          </button>
        </div>
      </div>
    );
  }

  const quizTitle = session.title || s.presenterTitle;
  const isLastQuestion =
    session.currentIndex >= 0 && session.currentIndex >= session.totalQuestions - 1;

  return (
    <>
      {/* Toast Notice Banner */}
      {toastNotice ? (
        <div
          style={{
            position: "fixed",
            top: 80,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 10001,
            background: "rgba(46, 125, 50, 0.95)",
            color: "#ffffff",
            padding: "10px 24px",
            borderRadius: 999,
            fontWeight: 600,
            boxShadow: "0 8px 24px rgba(0,0,0,0.4)",
            backdropFilter: "blur(8px)",
          }}
        >
          {toastNotice}
        </div>
      ) : null}

      {/* Action Error Banner */}
      {actionError ? (
        <div
          style={{
            position: "fixed",
            top: 80,
            left: "50%",
            transform: "translateX(-50%)",
            zIndex: 10001,
            background: "rgba(211, 47, 47, 0.95)",
            color: "#ffffff",
            padding: "10px 24px",
            borderRadius: 999,
            fontWeight: 600,
            boxShadow: "0 8px 24px rgba(0,0,0,0.4)",
            display: "flex",
            alignItems: "center",
            gap: 12,
            backdropFilter: "blur(8px)",
          }}
        >
          <span>{actionError}</span>
          <button
            type="button"
            onClick={() => setActionError("")}
            style={{
              background: "transparent",
              border: "none",
              color: "#ffffff",
              cursor: "pointer",
              fontWeight: 700,
            }}
          >
            ✕
          </button>
        </div>
      ) : null}

      {/* Phase 1: Lobby */}
      {session.status === "lobby" ? (
        <QuizPresenterLobby
          title={quizTitle}
          organizationName={organizationName || ""}
          sessionId={sessionId}
          players={activePlayers}
          locked={session.locked}
          onToggleLock={() => void handleToggleLock()}
          onStartGame={() => void handleStartGame()}
          onKickPlayer={(id) => void handleKickPlayer(id)}
          busyAction={busyAction}
          onClose={() => setExitConfirmOpen(true)}
          onNewTab={handleOpenNewTab}
        />
      ) : null}

      {/* Phase 2: Question */}
      {session.status === "question" && session.currentQuestion ? (
        <QuizPresenterQuestion
          question={session.currentQuestion}
          currentIndex={session.currentIndex + 1}
          totalQuestions={session.totalQuestions}
          answeredCount={answeredCount}
          totalPlayers={activePlayers.length}
          remainingSeconds={remainingSeconds}
          totalSeconds={totalSeconds}
          onEarlyReveal={() => void handleEarlyReveal()}
          busyAction={busyAction}
          title={quizTitle}
          organizationName={organizationName || ""}
          onClose={() => setExitConfirmOpen(true)}
          onNewTab={handleOpenNewTab}
        />
      ) : null}

      {/* Phase 3: Reveal */}
      {session.status === "reveal" && session.reveal ? (
        <QuizPresenterReveal
          question={
            session.currentQuestion || {
              index: session.currentIndex,
              type: "multiple_choice",
              text: "",
              options: [],
              timeLimitSec: 20,
            }
          }
          reveal={session.reveal}
          totalPlayers={activePlayers.length}
          isLastQuestion={isLastQuestion}
          onNext={() => void handleNextQuestion()}
          busyAction={busyAction}
          title={quizTitle}
          organizationName={organizationName || ""}
          onClose={() => setExitConfirmOpen(true)}
          onNewTab={handleOpenNewTab}
        />
      ) : null}

      {/* Phase 4: Podium / Finished */}
      {session.status === "finished" ? (
        <QuizPresenterPodium
          podium={session.podium || []}
          players={activePlayers.map((p) => ({
            rank: p.rank,
            nickname: p.nickname,
            score: p.score,
            correctCount: p.correctCount,
          }))}
          onExit={() => setExitConfirmOpen(true)}
          onViewReport={() => void handleViewReport()}
          title={quizTitle}
          organizationName={organizationName || ""}
          onClose={() => setExitConfirmOpen(true)}
          onNewTab={handleOpenNewTab}
        />
      ) : null}

      {/* Exit Confirmation Dialog */}
      {exitConfirmOpen ? (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.75)",
            backdropFilter: "blur(12px)",
            zIndex: 10002,
            display: "grid",
            placeItems: "center",
            padding: 24,
          }}
          onClick={() => setExitConfirmOpen(false)}
        >
          <div
            style={{
              width: "100%",
              maxWidth: 480,
              background: "#181b20",
              border: "1px solid rgba(255, 255, 255, 0.15)",
              borderRadius: 16,
              padding: 28,
              boxShadow: "0 24px 60px rgba(0,0,0,0.6)",
              color: "#ffffff",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ fontSize: 20, fontWeight: 700, margin: "0 0 12px" }}>
              {s.exitConfirmTitle}
            </h3>
            <p style={{ color: "#9aa0a6", fontSize: 14, lineHeight: 1.5, margin: "0 0 24px" }}>
              {s.exitConfirmDesc}
            </p>

            <div style={{ display: "flex", flexDirection: "column", gap: 10 }}>
              <button
                type="button"
                style={{
                  height: 44,
                  borderRadius: 10,
                  border: "none",
                  background: "#008556",
                  color: "#ffffff",
                  fontWeight: 600,
                  cursor: "pointer",
                }}
                onClick={onClose}
              >
                {s.exitPresentation}
              </button>

              <button
                type="button"
                style={{
                  height: 44,
                  borderRadius: 10,
                  border: "1px solid rgba(211, 47, 47, 0.5)",
                  background: "rgba(211, 47, 47, 0.15)",
                  color: "#ef5350",
                  fontWeight: 600,
                  cursor: "pointer",
                }}
                onClick={() => {
                  setExitConfirmOpen(false);
                  setCancelConfirmOpen(true);
                }}
              >
                {s.cancelSession}
              </button>

              <button
                type="button"
                style={{
                  height: 44,
                  borderRadius: 10,
                  border: "1px solid rgba(255, 255, 255, 0.15)",
                  background: "transparent",
                  color: "#ffffff",
                  fontWeight: 500,
                  cursor: "pointer",
                }}
                onClick={() => setExitConfirmOpen(false)}
              >
                {s.cancelButton}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {/* Cancel Session Confirmation Dialog */}
      {cancelConfirmOpen ? (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.8)",
            backdropFilter: "blur(12px)",
            zIndex: 10003,
            display: "grid",
            placeItems: "center",
            padding: 24,
          }}
          onClick={() => setCancelConfirmOpen(false)}
        >
          <div
            style={{
              width: "100%",
              maxWidth: 480,
              background: "#181b20",
              border: "1px solid rgba(211, 47, 47, 0.5)",
              borderRadius: 16,
              padding: 28,
              boxShadow: "0 24px 60px rgba(0,0,0,0.7)",
              color: "#ffffff",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <h3 style={{ fontSize: 20, fontWeight: 700, margin: "0 0 12px", color: "#ef5350" }}>
              {s.cancelSession}
            </h3>
            <p style={{ color: "#9aa0a6", fontSize: 14, lineHeight: 1.5, margin: "0 0 24px" }}>
              {s.cancelConfirmDesc}
            </p>

            <div style={{ display: "flex", gap: 12 }}>
              <button
                type="button"
                style={{
                  flex: 1,
                  height: 44,
                  borderRadius: 10,
                  border: "none",
                  background: "#d32f2f",
                  color: "#ffffff",
                  fontWeight: 600,
                  cursor: "pointer",
                }}
                disabled={busyAction}
                onClick={() => void handleCancelSession()}
              >
                {busyAction ? "İptal Ediliyor…" : s.cancelSession}
              </button>

              <button
                type="button"
                style={{
                  flex: 1,
                  height: 44,
                  borderRadius: 10,
                  border: "1px solid rgba(255, 255, 255, 0.15)",
                  background: "transparent",
                  color: "#ffffff",
                  fontWeight: 500,
                  cursor: "pointer",
                }}
                disabled={busyAction}
                onClick={() => setCancelConfirmOpen(false)}
              >
                {s.cancelButton}
              </button>
            </div>
          </div>
        </div>
      ) : null}

      {/* Detailed Report Modal */}
      {reportData ? (
        <div
          style={{
            position: "fixed",
            inset: 0,
            background: "rgba(0,0,0,0.8)",
            backdropFilter: "blur(12px)",
            zIndex: 10004,
            display: "grid",
            placeItems: "center",
            padding: 24,
          }}
          onClick={() => setReportData(null)}
        >
          <div
            style={{
              width: "100%",
              maxWidth: 720,
              maxHeight: "85vh",
              overflowY: "auto",
              background: "#181b20",
              border: "1px solid rgba(255, 255, 255, 0.15)",
              borderRadius: 16,
              padding: 28,
              boxShadow: "0 24px 60px rgba(0,0,0,0.8)",
              color: "#ffffff",
            }}
            onClick={(e) => e.stopPropagation()}
          >
            <div style={{ display: "flex", justifyContent: "space-between", alignItems: "center", marginBottom: 20 }}>
              <h3 style={{ fontSize: 22, fontWeight: 700, margin: 0 }}>
                📊 {reportData.title || s.presenterTitle} — Rapor
              </h3>
              <button
                type="button"
                onClick={() => setReportData(null)}
                style={{
                  background: "transparent",
                  border: "none",
                  color: "#9aa0a6",
                  fontSize: 22,
                  cursor: "pointer",
                }}
              >
                ✕
              </button>
            </div>

            <div style={{ display: "grid", gridTemplateColumns: "1fr 1fr", gap: 12, marginBottom: 24 }}>
              <div style={{ padding: 14, background: "#20242c", borderRadius: 10 }}>
                <div style={{ fontSize: 13, color: "#9aa0a6" }}>Toplam Katılımcı</div>
                <div style={{ fontSize: 24, fontWeight: 700 }}>{reportData.playerCount}</div>
              </div>
              <div style={{ padding: 14, background: "#20242c", borderRadius: 10 }}>
                <div style={{ fontSize: 13, color: "#9aa0a6" }}>Toplam Soru</div>
                <div style={{ fontSize: 24, fontWeight: 700 }}>{reportData.totalQuestions}</div>
              </div>
            </div>

            <h4 style={{ fontSize: 16, fontWeight: 600, marginBottom: 12 }}>Soru Başarı Oranları</h4>
            <div style={{ display: "flex", flexDirection: "column", gap: 10, marginBottom: 24 }}>
              {reportData.questions.map((q) => (
                <div
                  key={q.index}
                  style={{
                    padding: 12,
                    background: "#20242c",
                    borderRadius: 10,
                    display: "flex",
                    justifyContent: "space-between",
                    alignItems: "center",
                  }}
                >
                  <div style={{ flex: 1, paddingRight: 16 }}>
                    <div style={{ fontWeight: 600, fontSize: 14 }}>
                      Soru {q.index + 1}: {q.text}
                    </div>
                    <div style={{ fontSize: 12, color: "#9aa0a6" }}>
                      {q.answered} cevap · Ort. {Math.round(q.avgElapsedMs / 1000)} sn
                    </div>
                  </div>
                  <div style={{ textAlign: "right" }}>
                    <div style={{ fontWeight: 700, color: q.correctPercent >= 50 ? "#81c784" : "#ef5350" }}>
                      %{q.correctPercent}
                    </div>
                    <div style={{ fontSize: 11, color: "#9aa0a6" }}>Başarı</div>
                  </div>
                </div>
              ))}
            </div>

            <button
              type="button"
              className="podium-exit-btn"
              onClick={() => setReportData(null)}
              style={{ width: "100%" }}
            >
              Kapat
            </button>
          </div>
        </div>
      ) : null}
    </>
  );
}

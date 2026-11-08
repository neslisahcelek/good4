import { useEffect } from "react";
import { quizStrings as s } from "../quizStrings";
import { QuizPresenterTopBar } from "./QuizPresenterTopBar";
import "./QuizPresenter.css";

export interface QuizPresenterRevealProps {
  question: {
    index: number;
    type: "multiple_choice" | "true_false";
    text: string;
    options: string[];
    timeLimitSec: number;
  };
  reveal: {
    index: number;
    correctIndex: number;
    distribution: number[];
    unanswered: number;
  };
  totalPlayers: number;
  isLastQuestion: boolean;
  onNext: () => void;
  busyAction?: boolean;
  title?: string;
  organizationName?: string;
  onFullscreen?: () => void;
  onClose?: () => void;
  isFullscreen?: boolean;
  onNewTab?: () => void;
}

const OPTION_STYLES = [
  {
    className: "answer-card--red",
    barClass: "distribution-bar--red",
    shape: "▲",
    shapeName: "Üçgen",
    svg: (
      <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 3L22 20H2L12 3Z" />
      </svg>
    ),
  },
  {
    className: "answer-card--blue",
    barClass: "distribution-bar--blue",
    shape: "◆",
    shapeName: "Baklava",
    svg: (
      <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2L22 12L12 22L2 12L12 2Z" />
      </svg>
    ),
  },
  {
    className: "answer-card--yellow",
    barClass: "distribution-bar--yellow",
    shape: "●",
    shapeName: "Daire",
    svg: (
      <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
        <circle cx="12" cy="12" r="10" />
      </svg>
    ),
  },
  {
    className: "answer-card--green",
    barClass: "distribution-bar--green",
    shape: "■",
    shapeName: "Kare",
    svg: (
      <svg width="28" height="28" viewBox="0 0 24 24" fill="currentColor">
        <rect x="3" y="3" width="18" height="18" rx="2" />
      </svg>
    ),
  },
] as const;

export function QuizPresenterReveal({
  question,
  reveal,
  totalPlayers,
  isLastQuestion,
  onNext,
  busyAction = false,
  title,
  organizationName,
  onFullscreen,
  onClose,
  isFullscreen,
  onNewTab,
}: QuizPresenterRevealProps) {
  // Space key advances to Next Question or Final Podium
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === "Space" || e.key === " ") {
        const activeEl = document.activeElement;
        if (activeEl && (activeEl.tagName === "INPUT" || activeEl.tagName === "TEXTAREA")) {
          return;
        }
        e.preventDefault();
        if (!busyAction) {
          onNext();
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [busyAction, onNext]);

  const isTrueFalse = question.type === "true_false";
  const optionsList = isTrueFalse
    ? [s.trueOptionLabel, s.falseOptionLabel]
    : question.options;

  const distribution = reveal.distribution || [];
  const maxVote = Math.max(1, ...distribution);

  return (
    <div className="quiz-presenter" role="region" aria-label="Soru Sonuçları">
      {/* Top Bar */}
      <QuizPresenterTopBar
        title={title}
        organizationName={organizationName}
        totalPlayers={totalPlayers}
        onFullscreen={onFullscreen}
        onNewTab={onNewTab}
        onClose={onClose}
        isFullscreen={isFullscreen}
      />

      {/* Main Body */}
      <main className="quiz-presenter__body">
        <div className="presenter-reveal">
          {/* Question Summary Header */}
          <section className="presenter-question-card" style={{ padding: "20px 28px" }}>
            <div className="presenter-question-content">
              <div className="presenter-question-meta">
                <span className="presenter-question-badge">
                  {s.questionNumber(question.index + 1)}
                </span>
                <span className="presenter-question-type">
                  {s.presenterCorrectAnswer}
                </span>
              </div>
              <h2 className="presenter-question-text" style={{ fontSize: "clamp(20px, 2.4vw, 32px)" }}>
                {question.text}
              </h2>
            </div>
          </section>

          {/* Reveal Distribution & Options Grid */}
          <section
            className={`reveal-columns-area ${
              isTrueFalse ? "reveal-columns-area--tf" : "reveal-columns-area--mc"
            }`}
            aria-label="Cevap Dağılım Grafiği"
          >
            {optionsList.map((optText, idx) => {
              const isCorrect = idx === reveal.correctIndex;
              const count = distribution[idx] ?? 0;
              const percent = totalPlayers > 0 ? Math.round((count / totalPlayers) * 100) : 0;
              const barHeightPercent = count > 0 ? Math.max(12, Math.round((count / maxVote) * 100)) : 4;

              // Style mapping:
              // For True/False: 0 is Blue Diamond, 1 is Red Triangle
              const style = isTrueFalse
                ? idx === 0
                  ? OPTION_STYLES[1]
                  : OPTION_STYLES[0]
                : OPTION_STYLES[idx % OPTION_STYLES.length];

              return (
                <div key={idx} className="reveal-column">
                  {/* Vertical Rising Distribution Bar */}
                  <div className="distribution-graph-slot">
                    <div className="distribution-meta">
                      {count} Kişi (%{percent})
                    </div>
                    <div
                      className={`distribution-bar ${style.barClass}`}
                      style={{
                        height: `${barHeightPercent}%`,
                        maxHeight: "220px",
                      }}
                      title={`${count} kişi (${percent}%)`}
                    />
                  </div>

                  {/* Option Card */}
                  <div
                    className={`reveal-card ${style.className} ${
                      isCorrect ? "is-correct" : "is-wrong"
                    }`}
                  >
                    <div className="answer-shape" aria-label={style.shapeName}>
                      {style.svg}
                    </div>

                    <div className="answer-text" style={{ fontSize: "clamp(17px, 1.8vw, 24px)" }}>
                      {optText}
                    </div>

                    <div
                      className={`reveal-status-icon ${
                        isCorrect ? "reveal-status-icon--check" : "reveal-status-icon--cross"
                      }`}
                      aria-label={isCorrect ? "Doğru Seçenek" : "Yanlış Seçenek"}
                    >
                      {isCorrect ? "✓" : "✕"}
                    </div>
                  </div>
                </div>
              );
            })}
          </section>

          {/* Footer with Unanswered Notice & Next CTA */}
          <footer className="reveal-footer">
            <div className="unanswered-banner">
              {reveal.unanswered > 0 ? (
                <>
                  <span>⚠️</span>
                  <span>{s.presenterUnansweredCount(reveal.unanswered)}</span>
                </>
              ) : (
                <>
                  <span>✓</span>
                  <span>Tüm katılımcılar yanıt verdi</span>
                </>
              )}
            </div>

            <button
              type="button"
              className="presenter-next-btn"
              onClick={onNext}
              disabled={busyAction}
              aria-label={isLastQuestion ? s.presenterShowPodium : s.presenterNextQuestion}
            >
              <span>{isLastQuestion ? s.presenterShowPodium : s.presenterNextQuestion}</span>
              <kbd className="kbd-badge">Space</kbd>
            </button>
          </footer>
        </div>
      </main>
    </div>
  );
}

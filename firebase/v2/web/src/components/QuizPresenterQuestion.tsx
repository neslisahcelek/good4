import { useEffect } from "react";
import { quizStrings as s } from "../quizStrings";
import { QuizPresenterTopBar } from "./QuizPresenterTopBar";
import "./QuizPresenter.css";

export interface QuizPresenterQuestionProps {
  question: {
    index: number;
    type: "multiple_choice" | "true_false";
    text: string;
    options: string[];
    timeLimitSec: number;
  };
  currentIndex: number;
  totalQuestions: number;
  answeredCount: number;
  totalPlayers: number;
  remainingSeconds: number;
  totalSeconds: number;
  onEarlyReveal: () => void;
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
    shape: "▲",
    shapeName: "Üçgen",
    svg: (
      <svg width="34" height="34" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 3L22 20H2L12 3Z" />
      </svg>
    ),
  },
  {
    className: "answer-card--blue",
    shape: "◆",
    shapeName: "Baklava",
    svg: (
      <svg width="34" height="34" viewBox="0 0 24 24" fill="currentColor">
        <path d="M12 2L22 12L12 22L2 12L12 2Z" />
      </svg>
    ),
  },
  {
    className: "answer-card--yellow",
    shape: "●",
    shapeName: "Daire",
    svg: (
      <svg width="34" height="34" viewBox="0 0 24 24" fill="currentColor">
        <circle cx="12" cy="12" r="10" />
      </svg>
    ),
  },
  {
    className: "answer-card--green",
    shape: "■",
    shapeName: "Kare",
    svg: (
      <svg width="34" height="34" viewBox="0 0 24 24" fill="currentColor">
        <rect x="3" y="3" width="18" height="18" rx="2" />
      </svg>
    ),
  },
] as const;

export function QuizPresenterQuestion({
  question,
  currentIndex,
  totalQuestions,
  answeredCount,
  totalPlayers,
  remainingSeconds,
  totalSeconds,
  onEarlyReveal,
  busyAction = false,
  title,
  organizationName,
  onFullscreen,
  onClose,
  isFullscreen,
  onNewTab,
}: QuizPresenterQuestionProps) {
  // Space triggers Early Reveal
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === "Space" || e.key === " ") {
        const activeEl = document.activeElement;
        if (activeEl && (activeEl.tagName === "INPUT" || activeEl.tagName === "TEXTAREA")) {
          return;
        }
        e.preventDefault();
        if (!busyAction) {
          onEarlyReveal();
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [busyAction, onEarlyReveal]);

  const isUrgent = remainingSeconds <= 5 && remainingSeconds > 0;
  const radius = 38;
  const circumference = 2 * Math.PI * radius;
  const safeTotal = Math.max(1, totalSeconds || question.timeLimitSec || 20);
  const progressRatio = Math.max(0, Math.min(1, remainingSeconds / safeTotal));
  const strokeDashoffset = circumference * (1 - progressRatio);

  const isTrueFalse = question.type === "true_false";

  // For True/False: Option 0 (Doğru) is Blue Diamond, Option 1 (Yanlış) is Red Triangle
  const renderOptions = () => {
    if (isTrueFalse) {
      const tfOptions = [
        {
          text: s.trueOptionLabel,
          style: OPTION_STYLES[1], // Blue Diamond
        },
        {
          text: s.falseOptionLabel,
          style: OPTION_STYLES[0], // Red Triangle
        },
      ];

      return (
        <div className="answer-grid answer-grid--tf">
          {tfOptions.map((opt, idx) => (
            <div key={idx} className={`answer-card ${opt.style.className}`}>
              <div className="answer-shape" aria-label={opt.style.shapeName}>
                {opt.style.svg}
              </div>
              <div className="answer-text">{opt.text}</div>
            </div>
          ))}
        </div>
      );
    }

    return (
      <div className="answer-grid answer-grid--mc">
        {question.options.map((optText, idx) => {
          const style = OPTION_STYLES[idx % OPTION_STYLES.length];
          return (
            <div key={idx} className={`answer-card ${style.className}`}>
              <div className="answer-shape" aria-label={style.shapeName}>
                {style.svg}
              </div>
              <div className="answer-text">{optText}</div>
            </div>
          );
        })}
      </div>
    );
  };

  return (
    <div className="quiz-presenter" role="region" aria-label={`Soru ${currentIndex} / ${totalQuestions}`}>
      {/* Top Bar */}
      <QuizPresenterTopBar
        title={title}
        organizationName={organizationName}
        questionProgress={{ current: currentIndex, total: totalQuestions }}
        answersProgress={{ answered: answeredCount, total: totalPlayers }}
        onFullscreen={onFullscreen}
        onNewTab={onNewTab}
        onClose={onClose}
        isFullscreen={isFullscreen}
      />

      {/* Main Body */}
      <main className="quiz-presenter__body">
        <div className="presenter-question">
          {/* Question Text & Timer */}
          <section className="presenter-question-card">
            <div className="presenter-question-content">
              <div className="presenter-question-meta">
                <span className="presenter-question-badge">
                  {s.questionNumber(currentIndex)}
                </span>
                <span className="presenter-question-type">
                  {isTrueFalse ? s.typeTrueFalse : s.typeMultipleChoice}
                </span>
              </div>
              <h2 className="presenter-question-text">{question.text}</h2>
            </div>

            {/* Circular Timer */}
            <div className="presenter-timer-wrap" aria-label={`Kalan süre: ${remainingSeconds} saniye`}>
              <svg className="presenter-timer-svg" viewBox="0 0 96 96">
                <circle className="presenter-timer-bg" cx="48" cy="48" r={radius} />
                <circle
                  className={`presenter-timer-stroke ${isUrgent ? "presenter-timer-stroke--urgent" : ""}`}
                  cx="48"
                  cy="48"
                  r={radius}
                  strokeDasharray={circumference}
                  strokeDashoffset={strokeDashoffset}
                />
              </svg>
              <div className={`presenter-timer-val ${isUrgent ? "presenter-timer-val--urgent" : ""}`}>
                {remainingSeconds}
              </div>
            </div>
          </section>

          {/* Options Grid */}
          <section className="presenter-answers-section" aria-label="Seçenekler">
            {renderOptions()}
          </section>

          {/* Bottom Control Bar */}
          <footer className="presenter-control-bar">
            <div className="answered-counter-pill">
              <span className="pulse-dot" />
              <span>{s.presenterAnswersCount(answeredCount, totalPlayers)}</span>
            </div>

            <button
              type="button"
              className="presenter-early-btn"
              onClick={onEarlyReveal}
              disabled={busyAction}
              aria-label={s.presenterEarlyReveal}
            >
              <span>{s.presenterEarlyReveal}</span>
              <kbd className="kbd-badge">Space</kbd>
            </button>
          </footer>
        </div>
      </main>
    </div>
  );
}

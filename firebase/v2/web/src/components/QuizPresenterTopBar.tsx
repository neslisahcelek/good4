import { useEffect, useState } from "react";
import { quizStrings as s } from "../quizStrings";

export interface QuizPresenterTopBarProps {
  title?: string;
  organizationName?: string;
  totalPlayers?: number;
  locked?: boolean;
  onToggleLock?: () => void;
  questionProgress?: { current: number; total: number };
  answersProgress?: { answered: number; total: number };
  onFullscreen?: () => void;
  onNewTab?: () => void;
  onClose?: () => void;
  isFullscreen?: boolean;
}

export function QuizPresenterTopBar({
  title = s.presenterTitle,
  organizationName,
  totalPlayers,
  locked,
  onToggleLock,
  questionProgress,
  answersProgress,
  onFullscreen,
  onNewTab,
  onClose,
  isFullscreen: externalFullscreen,
}: QuizPresenterTopBarProps) {
  const [internalFullscreen, setInternalFullscreen] = useState(
    typeof document !== "undefined" ? Boolean(document.fullscreenElement) : false
  );

  useEffect(() => {
    if (typeof document === "undefined") return;
    const handleFullscreenChange = () => {
      setInternalFullscreen(Boolean(document.fullscreenElement));
    };
    document.addEventListener("fullscreenchange", handleFullscreenChange);
    return () => {
      document.removeEventListener("fullscreenchange", handleFullscreenChange);
    };
  }, []);

  const isFullscreen = externalFullscreen !== undefined ? externalFullscreen : internalFullscreen;

  const handleToggleFullscreen = () => {
    if (onFullscreen) {
      onFullscreen();
      return;
    }
    if (typeof document === "undefined") return;
    if (!document.fullscreenElement) {
      void document.documentElement.requestFullscreen().catch(() => {});
    } else {
      void document.exitFullscreen().catch(() => {});
    }
  };

  const handleOpenNewTab = () => {
    if (onNewTab) {
      onNewTab();
      return;
    }
    if (typeof window !== "undefined") {
      window.open(window.location.href, "_blank", "noopener,noreferrer");
    }
  };

  return (
    <header className="quiz-presenter__topbar">
      {/* Brand & Titles */}
      <div className="quiz-presenter__brand-wrap">
        <div className="quiz-presenter__logo" title="Good4 Live Quiz">
          G4
        </div>
        <div className="quiz-presenter__title-group">
          <h1 className="quiz-presenter__main-title">{title}</h1>
          {organizationName ? (
            <span className="quiz-presenter__sub-title">{organizationName}</span>
          ) : (
            <span className="quiz-presenter__sub-title">{s.presenterTitle}</span>
          )}
        </div>
      </div>

      {/* Center Live Badges */}
      <div className="quiz-presenter__center-status">
        {questionProgress ? (
          <div className="presenter-status-pill presenter-status-pill--active">
            <span>{s.presenterQuestionProgress(questionProgress.current, questionProgress.total)}</span>
          </div>
        ) : null}

        {answersProgress ? (
          <div className="presenter-status-pill">
            <span>{s.presenterAnswersCount(answersProgress.answered, answersProgress.total)}</span>
          </div>
        ) : null}

        {totalPlayers !== undefined ? (
          <div className="presenter-status-pill">
            <span className="pulse-dot" />
            <span>{s.presenterPlayersCount(totalPlayers)}</span>
          </div>
        ) : null}

        {locked !== undefined ? (
          <div
            className={`presenter-status-pill ${
              locked ? "presenter-status-pill--locked" : "presenter-status-pill--active"
            }`}
          >
            <span className={`pulse-dot ${locked ? "pulse-dot--red" : ""}`} />
            <span>{locked ? s.presenterLobbyLocked : s.presenterLobbyUnlocked}</span>
          </div>
        ) : null}
      </div>

      {/* Actions */}
      <div className="quiz-presenter__actions">
        {onToggleLock ? (
          <button
            type="button"
            className="presenter-icon-btn"
            onClick={onToggleLock}
            title={locked ? s.presenterUnlockLobby : s.presenterLockLobby}
            aria-label={locked ? s.presenterUnlockLobby : s.presenterLockLobby}
          >
            {locked ? (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 10 0v4" />
              </svg>
            ) : (
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                <path d="M7 11V7a5 5 0 0 1 9.9-1" />
              </svg>
            )}
          </button>
        ) : null}

        <button
          type="button"
          className="presenter-icon-btn"
          onClick={handleToggleFullscreen}
          title={isFullscreen ? s.presenterExitFullscreen : s.presenterFullscreen}
          aria-label={isFullscreen ? s.presenterExitFullscreen : s.presenterFullscreen}
        >
          {isFullscreen ? (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M8 3v3a2 2 0 0 1-2 2H3m18 0h-3a2 2 0 0 1-2-2V3m0 18v-3a2 2 0 0 1 2-2h3M3 16h3a2 2 0 0 1 2 2v3" />
            </svg>
          ) : (
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <path d="M8 3H5a2 2 0 0 0-2 2v3m18 0V5a2 2 0 0 0-2-2h-3m0 18h3a2 2 0 0 0 2-2v-3M3 16v3a2 2 0 0 0 2 2h3" />
            </svg>
          )}
        </button>

        <button
          type="button"
          className="presenter-icon-btn"
          onClick={handleOpenNewTab}
          title={s.presenterNewTab}
          aria-label={s.presenterNewTab}
        >
          <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
            <path d="M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6" />
            <polyline points="15 3 21 3 21 9" />
            <line x1="10" y1="14" x2="21" y2="3" />
          </svg>
        </button>

        {onClose ? (
          <button
            type="button"
            className="presenter-icon-btn presenter-icon-btn--danger"
            onClick={onClose}
            title={s.presenterClose}
            aria-label={s.presenterClose}
          >
            <svg width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
              <line x1="18" y1="6" x2="6" y2="18" />
              <line x1="6" y1="6" x2="18" y2="18" />
            </svg>
          </button>
        ) : null}
      </div>
    </header>
  );
}

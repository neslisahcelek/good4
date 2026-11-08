import { useEffect, useState } from "react";
import QRCode from "qrcode";
import { quizStrings as s } from "../quizStrings";
import { QuizPresenterTopBar } from "./QuizPresenterTopBar";
import "./QuizPresenter.css";

export interface QuizPresenterLobbyProps {
  title: string;
  organizationName: string;
  sessionId: string;
  players: Array<{ userId: string; nickname: string }>;
  locked: boolean;
  onToggleLock: () => void;
  onStartGame: () => void;
  onKickPlayer: (userId: string) => void;
  busyAction?: boolean;
  onFullscreen?: () => void;
  onClose?: () => void;
  isFullscreen?: boolean;
  onNewTab?: () => void;
}

const AVATAR_COLORS = [
  "#e53935",
  "#d81b60",
  "#8e24aa",
  "#5e35b1",
  "#3949ab",
  "#1e88e5",
  "#039be5",
  "#00acc1",
  "#00897b",
  "#43a047",
  "#7cb342",
  "#f4511e",
];

function getAvatarColor(key: string): string {
  let hash = 0;
  for (let i = 0; i < key.length; i++) {
    hash = (hash << 5) - hash + key.charCodeAt(i);
    hash |= 0;
  }
  const index = Math.abs(hash) % AVATAR_COLORS.length;
  return AVATAR_COLORS[index];
}

export function QuizPresenterLobby({
  title,
  organizationName,
  sessionId,
  players,
  locked,
  onToggleLock,
  onStartGame,
  onKickPlayer,
  busyAction = false,
  onFullscreen,
  onClose,
  isFullscreen,
  onNewTab,
}: QuizPresenterLobbyProps) {
  const [qrDataUrl, setQrDataUrl] = useState<string>("");
  const [copied, setCopied] = useState(false);

  // Generate QR code for the session
  useEffect(() => {
    let active = true;
    const joinUrl = `https://good4tr-v2.web.app/quiz?session=${encodeURIComponent(sessionId)}`;

    QRCode.toDataURL(joinUrl, {
      width: 280,
      margin: 1,
      color: {
        dark: "#0F1115",
        light: "#FFFFFF",
      },
      errorCorrectionLevel: "M",
    })
      .then((url) => {
        if (active) setQrDataUrl(url);
      })
      .catch((err) => {
        console.error("QR Code generation error:", err);
      });

    return () => {
      active = false;
    };
  }, [sessionId]);

  // Keyboard shortcut: Space starts the game
  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.code === "Space" || e.key === " ") {
        const activeEl = document.activeElement;
        if (activeEl && (activeEl.tagName === "INPUT" || activeEl.tagName === "TEXTAREA")) {
          return;
        }
        e.preventDefault();
        if (!busyAction && players.length > 0) {
          onStartGame();
        }
      }
    };

    window.addEventListener("keydown", handleKeyDown);
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [busyAction, onStartGame, players.length]);

  const handleCopySessionCode = () => {
    if (typeof navigator !== "undefined" && navigator.clipboard) {
      void navigator.clipboard.writeText(sessionId).then(() => {
        setCopied(true);
        setTimeout(() => setCopied(false), 2000);
      });
    }
  };

  return (
    <div className="quiz-presenter" role="region" aria-label="Canlı Quiz Lobisi">
      {/* Top Bar */}
      <QuizPresenterTopBar
        title={title}
        organizationName={organizationName}
        totalPlayers={players.length}
        locked={locked}
        onToggleLock={onToggleLock}
        onFullscreen={onFullscreen}
        onNewTab={onNewTab}
        onClose={onClose}
        isFullscreen={isFullscreen}
      />

      {/* Main Body */}
      <main className="quiz-presenter__body">
        <div className="presenter-lobby">
          <div className="presenter-lobby__grid">
            {/* Sol Kolon: QR Kod ve Katılım Yönergeleri */}
            <section className="lobby-join-panel" aria-label="Katılım Bilgileri">
              <div className="lobby-qr-card">
                {qrDataUrl ? (
                  <img
                    src={qrDataUrl}
                    alt={`Good4 Canlı Quiz QR Kod - Oturum: ${sessionId}`}
                    className="lobby-qr-image"
                  />
                ) : (
                  <div
                    style={{
                      width: 230,
                      height: 230,
                      display: "grid",
                      placeItems: "center",
                      color: "#64748b",
                      fontSize: 14,
                    }}
                  >
                    QR Yükleniyor…
                  </div>
                )}
              </div>

              <div className="lobby-session-box">
                <div className="lobby-session-label">{s.presenterSessionCode}</div>
                <button
                  type="button"
                  className="lobby-session-code"
                  onClick={handleCopySessionCode}
                  title="Oturum kodunu kopyala"
                  style={{ background: "transparent", border: "1px dashed rgba(255, 255, 255, 0.3)", cursor: "pointer" }}
                >
                  {sessionId}
                  {copied ? (
                    <span style={{ fontSize: "12px", marginLeft: "8px", color: "#81c784" }}>Kopyalandı!</span>
                  ) : null}
                </button>
              </div>

              <div className="lobby-instructions">
                <div style={{ fontWeight: 700, fontSize: "14px", color: "#ffffff", marginBottom: "4px" }}>
                  {s.presenterHowToJoin}
                </div>
                <div className="lobby-step-item">
                  <span className="lobby-step-badge">1</span>
                  <span>{s.presenterStep1}</span>
                </div>
                <div className="lobby-step-item">
                  <span className="lobby-step-badge">2</span>
                  <span>{s.presenterStep2}</span>
                </div>
              </div>
            </section>

            {/* Sağ Kolon: Katılımcı Çipleri ve Yönetim */}
            <section className="lobby-players-panel" aria-label="Katılımcı Listesi">
              <div className="lobby-players-header">
                <h2 className="lobby-players-title">
                  <span>{s.presenterLobbyHeading}</span>
                  <span className="lobby-players-count">{s.presenterPlayersCount(players.length)}</span>
                </h2>

                <button
                  type="button"
                  className="lobby-lock-button"
                  onClick={onToggleLock}
                  disabled={busyAction}
                  title={locked ? s.presenterUnlockLobby : s.presenterLockLobby}
                >
                  {locked ? "🔒 " + s.presenterUnlockLobby : "🔓 " + s.presenterLockLobby}
                </button>
              </div>

              {players.length === 0 ? (
                <div className="lobby-empty-players">
                  <div className="lobby-empty-pulse">👥</div>
                  <div className="lobby-empty-text">{s.presenterWaitingForPlayers}</div>
                </div>
              ) : (
                <div className="lobby-players-grid">
                  {players.map((player) => {
                    const initial = (player.nickname || "?").charAt(0).toUpperCase();
                    const color = getAvatarColor(player.userId || player.nickname);

                    return (
                      <div
                        key={player.userId}
                        className="player-chip"
                        title={`${player.nickname} — Çıkarmak için '✕' butonuna tıklayın`}
                      >
                        <span className="player-chip__avatar" style={{ backgroundColor: color }}>
                          {initial}
                        </span>
                        <span className="player-chip__name">{player.nickname}</span>
                        <button
                          type="button"
                          className="kick-btn"
                          onClick={(e) => {
                            e.stopPropagation();
                            onKickPlayer(player.userId);
                          }}
                          aria-label={`${player.nickname} ${s.presenterKickPlayer}`}
                          title={`${player.nickname} isimli katılımcıyı çıkar`}
                        >
                          ✕
                        </button>
                      </div>
                    );
                  })}
                </div>
              )}
            </section>
          </div>

          {/* Alt Kısım: Oyunu Başlat Butonu */}
          <footer className="lobby-footer">
            <button
              type="button"
              className="presenter-start-btn"
              onClick={onStartGame}
              disabled={busyAction || players.length === 0}
              aria-label={s.presenterStartGame}
            >
              <span>{s.presenterStartGame}</span>
              <kbd className="kbd-badge">Space</kbd>
            </button>
          </footer>
        </div>
      </main>
    </div>
  );
}

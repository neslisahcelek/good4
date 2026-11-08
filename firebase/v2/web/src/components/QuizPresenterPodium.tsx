import { quizStrings as s } from "../quizStrings";
import { QuizPresenterTopBar } from "./QuizPresenterTopBar";
import "./QuizPresenter.css";

export interface QuizPresenterPodiumProps {
  podium: Array<{ rank: number; nickname: string; score: number }>;
  players: Array<{
    rank: number;
    nickname: string;
    score: number;
    correctCount: number;
  }>;
  onExit: () => void;
  onViewReport?: () => void;
  title?: string;
  organizationName?: string;
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

function formatScore(score: number): string {
  return new Intl.NumberFormat("tr-TR").format(score);
}

export function QuizPresenterPodium({
  podium,
  players,
  onExit,
  onViewReport,
  title,
  organizationName,
  onFullscreen,
  onClose,
  isFullscreen,
  onNewTab,
}: QuizPresenterPodiumProps) {
  // Find top 3 winners for the Olympic podium
  const rank1 = podium.find((p) => p.rank === 1) || podium[0];
  const rank2 = podium.find((p) => p.rank === 2) || podium[1];
  const rank3 = podium.find((p) => p.rank === 3) || podium[2];

  return (
    <div className="quiz-presenter" role="region" aria-label="Final Podyumu ve Sıralama">
      {/* Top Bar */}
      <QuizPresenterTopBar
        title={title}
        organizationName={organizationName}
        totalPlayers={players.length}
        onFullscreen={onFullscreen}
        onNewTab={onNewTab}
        onClose={onClose || onExit}
        isFullscreen={isFullscreen}
      />

      {/* Main Body */}
      <main className="quiz-presenter__body">
        <div className="presenter-podium">
          {/* Hero Celebration Heading */}
          <div className="podium-hero">
            <h1 className="podium-hero-title">🏆 {s.presenterPodiumHeading}</h1>
            <p className="podium-hero-subtitle">
              Yarışmayı başarıyla tamamlayan tüm katılımcıları tebrik ederiz!
            </p>
          </div>

          {/* 3-Step Olympic Podium: Rank 2 (Left), Rank 1 (Center), Rank 3 (Right) */}
          <section className="podium-stage" aria-label="İlk 3 Podyumu">
            {/* 2. Sıra: Gümüş (Solda) */}
            <div className="podium-column podium-column--rank2">
              {rank2 ? (
                <>
                  <div className="podium-user-bubble">
                    <div className="podium-trophy" role="img" aria-label="Gümüş Madalya">
                      🥈
                    </div>
                    <div className="podium-player-name">{rank2.nickname}</div>
                    <div className="podium-player-score">{formatScore(rank2.score)} Puan</div>
                  </div>
                  <div className="podium-pedestal podium-pedestal--silver">
                    <span className="podium-step-number">2</span>
                  </div>
                </>
              ) : (
                <div className="podium-pedestal podium-pedestal--silver" style={{ opacity: 0.3 }}>
                  <span className="podium-step-number">2</span>
                </div>
              )}
            </div>

            {/* 1. Sıra: Altın (Ortada, En Yüksek) */}
            <div className="podium-column podium-column--rank1">
              {rank1 ? (
                <>
                  <div className="podium-user-bubble">
                    <div className="podium-trophy podium-trophy--gold" role="img" aria-label="Altın Taç / Kupa">
                      👑
                    </div>
                    <div className="podium-player-name">{rank1.nickname}</div>
                    <div className="podium-player-score" style={{ color: "#ffd54f" }}>
                      {formatScore(rank1.score)} Puan
                    </div>
                  </div>
                  <div className="podium-pedestal podium-pedestal--gold">
                    <span className="podium-step-number">1</span>
                  </div>
                </>
              ) : (
                <div className="podium-pedestal podium-pedestal--gold" style={{ opacity: 0.3 }}>
                  <span className="podium-step-number">1</span>
                </div>
              )}
            </div>

            {/* 3. Sıra: Bronz (Sağda) */}
            <div className="podium-column podium-column--rank3">
              {rank3 ? (
                <>
                  <div className="podium-user-bubble">
                    <div className="podium-trophy" role="img" aria-label="Bronz Madalya">
                      🥉
                    </div>
                    <div className="podium-player-name">{rank3.nickname}</div>
                    <div className="podium-player-score">{formatScore(rank3.score)} Puan</div>
                  </div>
                  <div className="podium-pedestal podium-pedestal--bronze">
                    <span className="podium-step-number">3</span>
                  </div>
                </>
              ) : (
                <div className="podium-pedestal podium-pedestal--bronze" style={{ opacity: 0.3 }}>
                  <span className="podium-step-number">3</span>
                </div>
              )}
            </div>
          </section>

          {/* Tam Katılımcı Sıralama Tablosu */}
          <section className="presenter-leaderboard-card" aria-label={s.presenterLeaderboardHeading}>
            <div className="presenter-leaderboard-title">
              <span>{s.presenterLeaderboardHeading}</span>
              <span style={{ fontSize: "14px", fontWeight: 600, color: "var(--qp-text-secondary)" }}>
                {s.presenterPlayersCount(players.length)}
              </span>
            </div>

            <div className="presenter-table-wrap">
              <table className="presenter-table">
                <thead>
                  <tr>
                    <th style={{ width: "80px" }}>{s.presenterRankHeader}</th>
                    <th>{s.presenterPlayerHeader}</th>
                    <th style={{ width: "160px" }}>{s.presenterCorrectCountHeader}</th>
                    <th style={{ width: "160px", textAlign: "right" }}>{s.presenterScoreHeader}</th>
                  </tr>
                </thead>
                <tbody>
                  {players.map((p, idx) => {
                    const rank = p.rank || idx + 1;
                    const initial = (p.nickname || "?").charAt(0).toUpperCase();
                    const color = getAvatarColor(p.nickname);

                    let badgeClass = "presenter-rank-badge--normal";
                    let rankPrefix = `#${rank}`;
                    if (rank === 1) {
                      badgeClass = "presenter-rank-badge--1";
                      rankPrefix = "🥇 1";
                    } else if (rank === 2) {
                      badgeClass = "presenter-rank-badge--2";
                      rankPrefix = "🥈 2";
                    } else if (rank === 3) {
                      badgeClass = "presenter-rank-badge--3";
                      rankPrefix = "🥉 3";
                    }

                    return (
                      <tr key={idx}>
                        <td>
                          <span className={`presenter-rank-badge ${badgeClass}`}>{rankPrefix}</span>
                        </td>
                        <td>
                          <div className="presenter-player-cell">
                            <span className="player-chip__avatar" style={{ backgroundColor: color }}>
                              {initial}
                            </span>
                            <span>{p.nickname}</span>
                          </div>
                        </td>
                        <td>
                          <span className="presenter-correct-cell">{p.correctCount} Doğru</span>
                        </td>
                        <td style={{ textAlign: "right" }}>
                          <span className="presenter-score-cell">{formatScore(p.score)} P</span>
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          {/* Alt Aksiyon Butonları */}
          <footer className="podium-actions">
            <button type="button" className="podium-exit-btn" onClick={onExit}>
              {s.presenterExit}
            </button>
            {onViewReport ? (
              <button type="button" className="podium-report-btn" onClick={onViewReport}>
                {s.presenterReport}
              </button>
            ) : null}
          </footer>
        </div>
      </main>
    </div>
  );
}

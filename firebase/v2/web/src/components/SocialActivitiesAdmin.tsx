import { useCallback, useEffect, useState } from "react";
import { httpsCallable } from "firebase/functions";
import { functions } from "../firebase";
import { socialText as t } from "../socialStrings";

type Activity = { id: string; title: string; note: string; kind: string; type: string; organizerName: string; startsAt: string | null; status: string };
type Report = { id: string; targetType: "activity" | "conversation"; reportedProfile: { name: string; photoUrl: string | null; photoThumbUrl: string | null } | null; reason: string; note: string; reporterUid: string; reportedUid: string; createdAt: string | null; activity: Activity | null };
type Violation = { id: string; uid: string; term: string; context: string; excerpt: string; suspended: boolean; createdAt: string | null };
type Queue = { reports: Report[]; violations: Violation[] };
type Conversation = {
  organizerUid: string | null; participantUid: string | null; organizerName: string; participantName: string; activityTitle: string;
  messages: { senderUid: string | null; type: string; text: string; createdAt: string | null }[];
};
type ReportAction = "dismiss" | "removeActivity" | "suspendUser";
const listQueue = httpsCallable<void, Queue>(functions, "listSocialModerationQueue");
const resolveReport = httpsCallable<{ reportId: string; action: ReportAction; suspendDays?: number }, { resolved: true }>(functions, "resolveSocialReport");
const removePhoto = httpsCallable<{ reportId: string }, { removed: true }>(functions, "removeSocialReportedProfilePhoto");
const getConversation = httpsCallable<{ reportId: string }, Conversation>(functions, "getSocialReportConversation");

function formatDate(value: string | null): string {
  return value ? new Intl.DateTimeFormat("tr-TR", { day: "numeric", month: "short", hour: "2-digit", minute: "2-digit", timeZone: "Europe/Istanbul" }).format(new Date(value)) : "";
}
function reasonLabel(reason: string) { return t(`report_${["harassment", "inappropriate", "spam", "safety", "other"].includes(reason) ? reason : "other"}`); }
function typeLabel(type: string) {
  const key = `type_${type.replaceAll("-", "_")}`;
  try { return t(key); } catch { return t("type_other"); }
}

export function SocialActivitiesAdmin({ onPendingCountChange }: { onPendingCountChange?: (count: number) => void }) {
  const [queue, setQueue] = useState<Queue | null>(null);
  const [loading, setLoading] = useState(true);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [notice, setNotice] = useState("");
  const [conversations, setConversations] = useState<Record<string, Conversation>>({});
  const [suspending, setSuspending] = useState<string | null>(null);
  const [days, setDays] = useState("7");
  const validDays = /^\d+$/.test(days) && Number(days) >= 1 && Number(days) <= 3650;
  const load = useCallback(async () => {
    setLoading(true);
    setError("");
    try {
      const result = await listQueue();
      setQueue(result.data);
      onPendingCountChange?.(result.data.reports.length);
    } catch { setError(t("admin_load_error")); }
    finally { setLoading(false); }
  }, [onPendingCountChange]);
  useEffect(() => { void load(); }, [load]);

  async function resolve(reportId: string, action: ReportAction) {
    setBusy(true); setError(""); setNotice("");
    try {
      await resolveReport({ reportId, action, ...(action === "suspendUser" ? { suspendDays: Number(days) } : {}) });
      setSuspending(null);
      setNotice(t(`admin_success_${action}`));
      await load();
    } catch { setError(t("admin_action_error")); }
    finally { setBusy(false); }
  }
  async function removeProfilePhoto(reportId: string) {
    setBusy(true); setError(""); setNotice("");
    try {
      await removePhoto({ reportId });
      setNotice(t("admin_photo_removed"));
      await load();
    } catch { setError(t("admin_action_error")); }
    finally { setBusy(false); }
  }
  async function showConversation(reportId: string) {
    setBusy(true); setError("");
    try {
      const result = await getConversation({ reportId });
      setConversations((previous) => ({ ...previous, [reportId]: result.data }));
    } catch { setError(t("admin_conversation_error")); }
    finally { setBusy(false); }
  }
  return <>
    {error && <div className="inline-message inline-message--error admin-message" role="alert">{error}</div>}
    {notice && <div className="inline-message inline-message--success admin-message" role="status">{notice}</div>}
    <section className="admin-section" aria-labelledby="social-reports-title">
      <div className="section-title-row">
        <div><h2 id="social-reports-title">{t("admin_reports_title")}</h2><p>{t("admin_reports_hint")}</p></div>
        <button className="quiet-button" onClick={() => void load()} disabled={loading || busy}>{t("admin_refresh")}</button>
      </div>
      {loading && !queue ? <div className="admin-card admin-loading"><span className="spinner spinner--green" /><p>{t("admin_loading")}</p></div> : queue && queue.reports.length === 0 ?
        <div className="admin-card empty-state"><h3>{t("admin_no_reports")}</h3><p>{t("admin_no_reports_hint")}</p></div> :
        <div className="coupon-review-list">{queue?.reports.map((report) => {
          const conversation = conversations[report.id];
          return <article className="admin-card coupon-review-card" key={report.id}>
            <div className="coupon-review-main">
              <div className="coupon-tags"><span>{t(report.targetType === "activity" ? "admin_activity_report" : "admin_conversation_report")}</span><span>{reasonLabel(report.reason)}</span><span>{formatDate(report.createdAt)}</span></div>
              {report.note && <p className="coupon-description">{report.note}</p>}
              {report.activity ? <>
                <div className="coupon-review-title"><div><h3>{report.activity.title}</h3><p>{t(report.activity.kind === "sport" ? "kind_sport" : "kind_social")} · {typeLabel(report.activity.type)}</p></div></div>
                <div className="coupon-tags"><span>{report.activity.organizerName}</span><span>{formatDate(report.activity.startsAt)}</span></div>
                {report.activity.note && <p className="coupon-description">{report.activity.note}</p>}
              </> : <p>{t("admin_activity_missing")}</p>}
              <p><small>{t("admin_reported_user")}: {report.reportedUid}</small></p>
              {report.reportedProfile && <div className="coupon-tags">
                {report.reportedProfile.photoUrl && <img src={report.reportedProfile.photoThumbUrl || report.reportedProfile.photoUrl} alt={t("admin_profile_photo")} width="80" height="80" style={{ objectFit: "cover", borderRadius: "50%" }} />}
                <span>{report.reportedProfile.name}</span>
              </div>}

              {report.targetType === "conversation" && !conversation && <button className="quiet-button" disabled={busy || loading} onClick={() => void showConversation(report.id)}>{t("admin_show_conversation")}</button>}
              {conversation && <div className="market-conversation">
                {conversation.messages.length === 0 && <p>{t("admin_no_messages")}</p>}
                {conversation.messages.map((message, index) => <p key={index}>
                  <strong>{message.type === "system" ? t("admin_system") : message.senderUid === conversation.organizerUid ? `${conversation.organizerName} (${t("admin_organizer")})` : `${conversation.participantName} (${t("admin_participant")})`}</strong>{" "}<small>{formatDate(message.createdAt)}</small><br />{message.text}
                </p>)}
              </div>}
              {suspending === report.id && <div className="market-reject-box">
                <label>{t("admin_suspend_days")}<input type="number" min="1" max="3650" step="1" value={days} onChange={(event) => setDays(event.target.value)} /></label>
                <p>{t("admin_suspend_hint")}</p>
                {!validDays && <p role="alert">{t("admin_suspend_invalid")}</p>}
              </div>}
            </div>
            <div className="review-actions">
              {suspending === report.id ? <>
                <button className="quiet-button" disabled={busy} onClick={() => setSuspending(null)}>{t("cancel")}</button>
                <button className="danger-button" disabled={busy || loading || !validDays} onClick={() => void resolve(report.id, "suspendUser")}>{t("admin_suspend_confirm")}</button>
              </> : <>
                <button className="quiet-button" disabled={busy || loading} onClick={() => void resolve(report.id, "dismiss")}>{t("admin_dismiss")}</button>
                {report.activity && <button className="danger-button" disabled={busy || loading} onClick={() => void resolve(report.id, "removeActivity")}>{t("admin_remove_activity")}</button>}
                {report.reportedProfile?.photoUrl && <button className="danger-button" disabled={busy || loading} onClick={() => void removeProfilePhoto(report.id)}>{t("admin_remove_photo")}</button>}
                <button className="danger-button" disabled={busy || loading} onClick={() => { setSuspending(report.id); setDays("7"); }}>{t("admin_suspend_user")}</button>
              </>}
            </div>
          </article>;
        })}</div>}
    </section>
    <section className="admin-section" aria-labelledby="social-violations-title">
      <div className="section-title-row"><div><h2 id="social-violations-title">{t("admin_violations_title")}</h2><p>{t("admin_violations_hint")}</p></div></div>
      <div className="admin-card audit-card">{queue && queue.violations.length === 0 ? <div className="empty-state compact-empty"><h3>{t("admin_no_records")}</h3></div> :
        <div className="audit-list">{queue?.violations.map((violation) => <div className="market-violation-row" key={violation.id}>
          <div><strong>{violation.term}</strong> · {t(violation.context === "socialActivity" ? "admin_context_activity" : violation.context === "socialRequest" ? "admin_context_request" : "admin_context_message")}{violation.suspended && ` · ${t("admin_suspended")}`}<br /><small>{violation.excerpt}</small></div>
          <small>{formatDate(violation.createdAt)}<br />{violation.uid}</small>
        </div>)}</div>}</div>
    </section>
  </>;
}

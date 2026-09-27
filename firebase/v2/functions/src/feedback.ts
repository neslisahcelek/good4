import { FieldValue, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";

export async function submitFeedbackService(
  database: Firestore,
  actorUid: string,
  input: {
    subject: unknown;
    message: unknown;
    reportCommunityId?: unknown;
    reportEntryId?: unknown;
    reportEntryKind?: unknown;
  },
): Promise<{ feedbackId: string }> {
  const subject = requireNonEmptyString(input.subject, "subject", 120);
  const message = requireNonEmptyString(input.message, "message", 2000);
  if (subject.length < 3) throw new HttpsError("invalid-argument", "SUBJECT_INVALID");
  if (message.length < 10) throw new HttpsError("invalid-argument", "MESSAGE_INVALID");
  const hasReport = [input.reportCommunityId, input.reportEntryId, input.reportEntryKind]
    .some((value) => value !== undefined);
  const reportId = (value: unknown, field: string): string => {
    if (typeof value !== "string" || !/^[A-Za-z0-9_-]{1,128}$/.test(value)) {
      throw new HttpsError("invalid-argument", `${field}_INVALID`);
    }
    return value;
  };
  const report = hasReport ? {
    communityId: reportId(input.reportCommunityId, "REPORT_COMMUNITY_ID"),
    entryId: reportId(input.reportEntryId, "REPORT_ENTRY_ID"),
    kind: input.reportEntryKind,
  } : null;
  if (report && report.kind !== "event" && report.kind !== "coupon") {
    throw new HttpsError("invalid-argument", "REPORT_ENTRY_KIND_INVALID");
  }

  await database.runTransaction((transaction) => requireActiveActor(database, transaction, actorUid));
  const user = await database.doc(`users/${actorUid}`).get();
  const feedback = database.collection("feedbackSubmissions").doc();
  await feedback.set({
    userId: actorUid,
    userEmail: String(user.get("email") ?? ""),
    userDisplayName: String(user.get("displayName") ?? ""),
    subject,
    message,
    source: report ? "contentReport" : "mobile",
    environment: "v2",
    status: "new",
    ...(report ? { report } : {}),
    createdAt: FieldValue.serverTimestamp(),
  });
  return { feedbackId: feedback.id };
}

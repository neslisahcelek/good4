import { FieldValue, Timestamp, type DocumentData, type DocumentSnapshot, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";

// Communities apply on the web panel with their own Google account; a Good4 admin approves
// the application, which creates the community and makes that account its manager.
// Applications live in communityApplications/{applicantUid}, so each account has at most one.

export type CommunityApplicationStatus = "pending" | "approved" | "rejected";

export interface ApplicantIdentity {
  uid: string;
  email?: string;
  emailVerified: boolean;
  providers: string[];
}

export interface CommunityApplicationSummary {
  id: string;
  communityName: string;
  university: string;
  applicantName: string;
  applicantEmail: string;
  description: string;
  socialUrl: string;
  status: CommunityApplicationStatus;
  rejectionReason: string;
  createdAt: string | null;
}

function optionalText(value: unknown, maxLength: number): string {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

function optionalHttpsUrl(value: unknown): string {
  const text = optionalText(value, 300);
  if (!text) return "";
  let url: URL;
  try {
    url = new URL(text);
  } catch {
    throw new HttpsError("invalid-argument", "SOCIAL_URL_INVALID");
  }
  if (url.protocol !== "https:") throw new HttpsError("invalid-argument", "SOCIAL_URL_INVALID");
  return url.toString();
}

function applicationFromDocument(id: string, data: DocumentData | undefined): CommunityApplicationSummary {
  const status = data?.status === "approved" || data?.status === "rejected" ? data.status : "pending";
  return {
    id,
    communityName: String(data?.communityName ?? ""),
    university: String(data?.university ?? ""),
    applicantName: String(data?.applicantName ?? ""),
    applicantEmail: String(data?.applicantEmail ?? ""),
    description: String(data?.description ?? ""),
    socialUrl: String(data?.socialUrl ?? ""),
    status,
    rejectionReason: String(data?.rejectionReason ?? ""),
    createdAt: data?.createdAt instanceof Timestamp ? data.createdAt.toDate().toISOString() : null,
  };
}

/** Accounts carry a single role: a student account must stay a student, a portal account already has an organization. */
function requireAccountWithoutRole(user: DocumentSnapshot): void {
  if (!user.exists) return;
  if (user.get("role") === "student") throw new HttpsError("failed-precondition", "STUDENT_ACCOUNT_NOT_ALLOWED");
  throw new HttpsError("failed-precondition", "ACCOUNT_ALREADY_HAS_ROLE");
}

export async function getMyCommunityApplicationService(
  database: Firestore,
  uid: string,
): Promise<{ application: CommunityApplicationSummary | null; accountRole: string }> {
  const [application, user] = await Promise.all([
    database.doc(`communityApplications/${uid}`).get(),
    database.doc(`users/${uid}`).get(),
  ]);
  return {
    application: application.exists ? applicationFromDocument(application.id, application.data()) : null,
    accountRole: user.exists ? String(user.get("role") ?? "") : "",
  };
}

export async function submitCommunityApplicationService(
  database: Firestore,
  identity: ApplicantIdentity,
  input: Record<string, unknown>,
): Promise<{ status: "pending" }> {
  if (!identity.providers.includes("google.com")) {
    throw new HttpsError("permission-denied", "GOOGLE_SIGN_IN_REQUIRED");
  }
  const email = identity.email?.trim().toLowerCase() ?? "";
  if (!email || !identity.emailVerified) {
    throw new HttpsError("failed-precondition", "VERIFIED_EMAIL_REQUIRED");
  }
  const communityName = requireNonEmptyString(input.communityName, "communityName", 160);
  const university = requireNonEmptyString(input.university, "university", 160);
  const applicantName = requireNonEmptyString(input.applicantName, "applicantName", 120);
  const description = optionalText(input.description, 1000);
  const socialUrl = optionalHttpsUrl(input.socialUrl);

  const applicationRef = database.doc(`communityApplications/${identity.uid}`);
  await database.runTransaction(async (transaction) => {
    const [user, existing] = await Promise.all([
      transaction.get(database.doc(`users/${identity.uid}`)),
      transaction.get(applicationRef),
    ]);
    requireAccountWithoutRole(user);
    if (existing.get("status") === "approved") throw new HttpsError("already-exists", "APPLICATION_ALREADY_APPROVED");
    // Editing a pending application keeps its place in the queue; a rejected one re-enters it.
    const keepCreatedAt = existing.get("status") === "pending" && existing.get("createdAt");
    transaction.set(applicationRef, {
      applicantUid: identity.uid,
      applicantEmail: email,
      applicantName,
      communityName,
      university,
      description,
      socialUrl,
      status: "pending",
      rejectionReason: "",
      createdAt: keepCreatedAt || FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
  return { status: "pending" };
}

export async function listCommunityApplicationsService(
  database: Firestore,
  actorUid: string,
): Promise<{ applications: CommunityApplicationSummary[] }> {
  await database.runTransaction((transaction) => requireActiveActor(database, transaction, actorUid, ["good4Admin"]));
  const snapshot = await database.collection("communityApplications").where("status", "==", "pending").limit(200).get();
  const applications = snapshot.docs
    .map((document) => applicationFromDocument(document.id, document.data()))
    .sort((left, right) => (left.createdAt ?? "").localeCompare(right.createdAt ?? ""));
  return { applications };
}

export async function reviewCommunityApplicationService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
): Promise<{ status: "approved" | "rejected"; organizationId?: string }> {
  const applicationId = requireNonEmptyString(input.applicationId, "applicationId", 128);
  if (!/^[A-Za-z0-9_-]+$/.test(applicationId)) throw new HttpsError("invalid-argument", "APPLICATIONID_INVALID");
  if (input.decision !== "approve" && input.decision !== "reject") {
    throw new HttpsError("invalid-argument", "DECISION_INVALID");
  }
  const approve = input.decision === "approve";

  const applicationRef = database.doc(`communityApplications/${applicationId}`);
  const userRef = database.doc(`users/${applicationId}`);
  const organizationRef = database.collection("organizations").doc();
  const auditRef = database.collection("auditLogs").doc();

  await database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const [application, user] = await Promise.all([transaction.get(applicationRef), transaction.get(userRef)]);
    if (!application.exists) throw new HttpsError("not-found", "APPLICATION_NOT_FOUND");
    if (application.get("status") !== "pending") {
      throw new HttpsError("failed-precondition", "APPLICATION_ALREADY_REVIEWED");
    }
    const name = String(application.get("communityName") ?? "");
    const university = String(application.get("university") ?? "");

    if (!approve) {
      transaction.update(applicationRef, {
        status: "rejected",
        rejectionReason: optionalText(input.reason, 500),
        reviewedBy: actorUid,
        reviewedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      transaction.create(auditRef, {
        action: "communityApplication.rejected",
        actorUid,
        targetType: "communityApplication",
        targetId: applicationId,
        metadata: { name, university },
        createdAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    // The applicant may have signed in to the student app after applying.
    requireAccountWithoutRole(user);
    transaction.create(organizationRef, {
      name,
      type: "community",
      university,
      status: "active",
      followerCount: 0,
      createdAt: FieldValue.serverTimestamp(),
      createdBy: actorUid,
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(userRef, {
      email: String(application.get("applicantEmail") ?? ""),
      displayName: String(application.get("applicantName") ?? ""),
      role: "communityManager",
      status: "active",
      university,
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(organizationRef.collection("members").doc(applicationId), {
      userId: applicationId,
      role: "manager",
      status: "active",
      assignedBy: actorUid,
      assignedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.update(applicationRef, {
      status: "approved",
      organizationId: organizationRef.id,
      reviewedBy: actorUid,
      reviewedAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(auditRef, {
      action: "communityApplication.approved",
      actorUid,
      targetType: "communityApplication",
      targetId: applicationId,
      metadata: { organizationId: organizationRef.id, name, university },
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  return approve ? { status: "approved", organizationId: organizationRef.id } : { status: "rejected" };
}

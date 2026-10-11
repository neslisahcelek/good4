import { FieldValue, Timestamp, type DocumentData, type Firestore } from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";
import { listPendingApplicationPage, requireAccountWithoutRole, requireVerifiedGoogleApplicant,
  type ApplicantIdentity, type ApplicationPageInput } from "./applicationSupport.js";

// Businesses apply on the staff panel with their own Google account: business name, contact
// person and phone. Until a Good4 admin approves, the account has no users/{uid} document, so
// every business-only callable (code redemption included) keeps rejecting it. Approval creates
// the business, the owner account and the membership in one transaction.
// Applications live in businessApplications/{applicantUid}: one per account, server-only.

export type BusinessApplicationStatus = "pending" | "approved" | "rejected";

export interface BusinessApplicationSummary {
  id: string;
  businessName: string;
  contactName: string;
  phone: string;
  applicantEmail: string;
  status: BusinessApplicationStatus;
  rejectionReason: string;
  createdAt: string | null;
}

function optionalText(value: unknown, maxLength: number): string {
  return typeof value === "string" ? value.trim().slice(0, maxLength) : "";
}

/** Turkish mobile or landline, with or without +90/0 and spacing: 10 national digits. */
export function normalizeBusinessPhone(value: unknown): string {
  const raw = typeof value === "string" ? value.trim() : "";
  if (!raw || raw.length > 24 || !/^[+0-9 ()-]+$/.test(raw)) throw new HttpsError("invalid-argument", "PHONE_INVALID");
  let digits = raw.replace(/\D/g, "");
  if (digits.startsWith("90") && digits.length === 12) digits = digits.slice(2);
  if (digits.startsWith("0") && digits.length === 11) digits = digits.slice(1);
  if (!/^[2-5][0-9]{9}$/.test(digits)) throw new HttpsError("invalid-argument", "PHONE_INVALID");
  return `+90 ${digits.slice(0, 3)} ${digits.slice(3, 6)} ${digits.slice(6, 8)} ${digits.slice(8)}`;
}

export function businessApplicationFromDocument(id: string, data: DocumentData | undefined): BusinessApplicationSummary {
  const status = data?.status === "approved" || data?.status === "rejected" ? data.status : "pending";
  return {
    id,
    businessName: String(data?.businessName ?? ""),
    contactName: String(data?.contactName ?? ""),
    phone: String(data?.phone ?? ""),
    applicantEmail: String(data?.applicantEmail ?? ""),
    status,
    rejectionReason: String(data?.rejectionReason ?? ""),
    createdAt: data?.createdAt instanceof Timestamp ? data.createdAt.toDate().toISOString() : null,
  };
}

export async function getMyBusinessApplication(database: Firestore, uid: string): Promise<BusinessApplicationSummary | null> {
  const application = await database.doc(`businessApplications/${uid}`).get();
  return application.exists ? businessApplicationFromDocument(application.id, application.data()) : null;
}

export async function submitBusinessApplicationService(
  database: Firestore,
  identity: ApplicantIdentity,
  input: Record<string, unknown>,
): Promise<{ application: BusinessApplicationSummary }> {
  const email = requireVerifiedGoogleApplicant(identity);
  const businessName = requireNonEmptyString(input.businessName, "businessName", 160);
  const contactName = requireNonEmptyString(input.contactName, "contactName", 120);
  const phone = normalizeBusinessPhone(input.phone);

  const applicationRef = database.doc(`businessApplications/${identity.uid}`);
  await database.runTransaction(async (transaction) => {
    const [user, existing, communityApplication] = await Promise.all([
      transaction.get(database.doc(`users/${identity.uid}`)),
      transaction.get(applicationRef),
      transaction.get(database.doc(`communityApplications/${identity.uid}`)),
    ]);
    requireAccountWithoutRole(user);
    if (existing.get("status") === "approved") throw new HttpsError("already-exists", "APPLICATION_ALREADY_APPROVED");
    // One account, one role: an account waiting as a community cannot also become a business.
    if (communityApplication.get("status") === "pending") {
      throw new HttpsError("failed-precondition", "COMMUNITY_APPLICATION_PENDING");
    }
    // Editing a pending application keeps its place in the queue; a rejected one re-enters it.
    const keepCreatedAt = existing.get("status") === "pending" && existing.get("createdAt");
    transaction.set(applicationRef, {
      applicantUid: identity.uid,
      applicantEmail: email,
      businessName,
      contactName,
      phone,
      status: "pending",
      rejectionReason: "",
      createdAt: keepCreatedAt || FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
  });
  const saved = await applicationRef.get();
  return { application: businessApplicationFromDocument(saved.id, saved.data()) };
}

export async function listBusinessApplicationsService(
  database: Firestore,
  actorUid: string,
  input: ApplicationPageInput = {},
): Promise<{ applications: BusinessApplicationSummary[]; nextCursor: string | null }> {
  const page = await listPendingApplicationPage(database, actorUid, "businessApplications", input);
  return { applications: page.documents.map((document) => businessApplicationFromDocument(document.id, document.data())),
    nextCursor: page.nextCursor };
}

export async function reviewBusinessApplicationService(
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

  const applicationRef = database.doc(`businessApplications/${applicationId}`);
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
    const name = String(application.get("businessName") ?? "");

    if (!approve) {
      transaction.update(applicationRef, {
        status: "rejected",
        rejectionReason: optionalText(input.reason, 500),
        reviewedBy: actorUid,
        reviewedAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      transaction.create(auditRef, {
        action: "businessApplication.rejected",
        actorUid,
        targetType: "businessApplication",
        targetId: applicationId,
        metadata: { name },
        createdAt: FieldValue.serverTimestamp(),
      });
      return;
    }

    // The applicant may have signed in to the student app after applying.
    requireAccountWithoutRole(user);
    transaction.create(organizationRef, {
      name,
      type: "business",
      status: "active",
      contactName: String(application.get("contactName") ?? ""),
      phone: String(application.get("phone") ?? ""),
      createdAt: FieldValue.serverTimestamp(),
      createdBy: actorUid,
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(userRef, {
      email: String(application.get("applicantEmail") ?? ""),
      displayName: String(application.get("contactName") ?? ""),
      role: "businessOwner",
      status: "active",
      university: "",
      createdAt: FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(organizationRef.collection("members").doc(applicationId), {
      userId: applicationId,
      role: "owner",
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
      action: "businessApplication.approved",
      actorUid,
      targetType: "businessApplication",
      targetId: applicationId,
      metadata: { organizationId: organizationRef.id, name },
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  return approve ? { status: "approved", organizationId: organizationRef.id } : { status: "rejected" };
}

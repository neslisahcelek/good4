import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { Timestamp } from "firebase-admin/firestore";
import {
  getMyCommunityApplicationService,
  listCommunityApplicationsService,
  reviewCommunityApplicationService,
  submitCommunityApplicationService,
} from "./communityApplications.js";
import { db } from "./firebase.js";

const googleApplicant = {
  uid: "applicant-1", email: "Kulup@Gmail.com", emailVerified: true, providers: ["google.com"],
};
const form = {
  communityName: "Akdeniz Satranç Topluluğu", university: "Akdeniz Üniversitesi",
  applicantName: "Ayşe Yılmaz", description: "Haftalık turnuvalar", socialUrl: "https://instagram.com/akdenizsatranc",
};

beforeEach(async () => {
  for (const collection of ["users", "organizations", "communityApplications", "auditLogs"]) {
    await db.recursiveDelete(db.collection(collection));
  }
  await db.doc("users/admin-1").set({ role: "good4Admin", status: "active" });
});

after(async () => {
  await db.terminate();
});

test("a Google account without a role can apply and sees its pending application", async () => {
  await submitCommunityApplicationService(db, googleApplicant, form);
  const { application, accountRole } = await getMyCommunityApplicationService(db, "applicant-1");
  assert.equal(accountRole, "");
  assert.equal(application?.status, "pending");
  assert.equal(application?.applicantEmail, "kulup@gmail.com");
  assert.equal(application?.communityName, "Akdeniz Satranç Topluluğu");
});

test("applications require a verified Google sign-in", async () => {
  await assert.rejects(
    submitCommunityApplicationService(db, { ...googleApplicant, disabled: true }, form), /ACCOUNT_NOT_ACTIVE/,
  );
  await assert.rejects(
    submitCommunityApplicationService(db, { ...googleApplicant, providers: ["password"] }, form),
    /GOOGLE_SIGN_IN_REQUIRED/,
  );
  await assert.rejects(
    submitCommunityApplicationService(db, { ...googleApplicant, emailVerified: false }, form),
    /VERIFIED_EMAIL_REQUIRED/,
  );
});

test("student and portal accounts cannot apply", async () => {
  await db.doc("users/applicant-1").set({ role: "student", status: "active" });
  await assert.rejects(submitCommunityApplicationService(db, googleApplicant, form), /STUDENT_ACCOUNT_NOT_ALLOWED/);
  await db.doc("users/applicant-1").set({ role: "businessOwner", status: "active" });
  await assert.rejects(submitCommunityApplicationService(db, googleApplicant, form), /ACCOUNT_ALREADY_HAS_ROLE/);
});

test("social links must be https URLs", async () => {
  await assert.rejects(
    submitCommunityApplicationService(db, googleApplicant, { ...form, socialUrl: "javascript:alert(1)" }),
    /SOCIAL_URL_INVALID/,
  );
});

test("only Good4 admins can list and review applications", async () => {
  await submitCommunityApplicationService(db, googleApplicant, form);
  await db.doc("users/manager-1").set({ role: "communityManager", status: "active" });
  await assert.rejects(listCommunityApplicationsService(db, "manager-1"), /ROLE_NOT_ALLOWED/);
  await assert.rejects(
    reviewCommunityApplicationService(db, "manager-1", { applicationId: "applicant-1", decision: "approve" }),
    /ROLE_NOT_ALLOWED/,
  );
  const { applications } = await listCommunityApplicationsService(db, "admin-1");
  assert.deepEqual(applications.map((item) => item.id), ["applicant-1"]);
  await db.doc("communityApplications/applicant-1").update({ createdAt: Timestamp.fromMillis(2000) });
  for (const [id, milliseconds] of [["z-oldest", 1000], ["a-boundary", 1500], ["m-same-time", 1500]] as const) {
    await db.doc(`communityApplications/${id}`).set({ status: "pending", createdAt: Timestamp.fromMillis(milliseconds) });
  }
  const first = await listCommunityApplicationsService(db, "admin-1", { limit: 2 });
  assert.deepEqual(first.applications.map((item) => item.id), ["z-oldest", "a-boundary"]);
  assert.ok(first.nextCursor);
  await db.doc("communityApplications/a-boundary").update({ status: "rejected" });
  const next = await listCommunityApplicationsService(db, "admin-1", { limit: 2, cursor: first.nextCursor });
  assert.deepEqual(next.applications.map((item) => item.id), ["m-same-time", "applicant-1"]);
  await assert.rejects(listCommunityApplicationsService(db, "admin-1", { cursor: "invalid" }), /APPLICATION_CURSOR_INVALID/);
});

test("approval creates the community and makes the applicant its manager", async () => {
  await submitCommunityApplicationService(db, googleApplicant, form);
  const result = await reviewCommunityApplicationService(db, "admin-1", { applicationId: "applicant-1", decision: "approve" });
  assert.equal(result.status, "approved");

  const organization = await db.doc(`organizations/${result.organizationId}`).get();
  assert.equal(organization.get("type"), "community");
  assert.equal(organization.get("status"), "active");
  assert.equal(organization.get("name"), "Akdeniz Satranç Topluluğu");
  const member = await db.doc(`organizations/${result.organizationId}/members/applicant-1`).get();
  assert.equal(member.get("role"), "manager");
  const user = await db.doc("users/applicant-1").get();
  assert.equal(user.get("role"), "communityManager");
  assert.equal(user.get("email"), "kulup@gmail.com");

  await assert.rejects(
    reviewCommunityApplicationService(db, "admin-1", { applicationId: "applicant-1", decision: "approve" }),
    /APPLICATION_ALREADY_REVIEWED/,
  );
  await assert.rejects(submitCommunityApplicationService(db, googleApplicant, form), /ACCOUNT_ALREADY_HAS_ROLE/);
  assert.equal((await listCommunityApplicationsService(db, "admin-1")).applications.length, 0);
});

test("approval fails if the applicant became a student in the meantime", async () => {
  await submitCommunityApplicationService(db, googleApplicant, form);
  await db.doc("users/applicant-1").set({ role: "student", status: "active" });
  await assert.rejects(
    reviewCommunityApplicationService(db, "admin-1", { applicationId: "applicant-1", decision: "approve" }),
    /STUDENT_ACCOUNT_NOT_ALLOWED/,
  );
  const organizations = await db.collection("organizations").get();
  assert.equal(organizations.size, 0);
  assert.equal((await db.doc("users/applicant-1").get()).get("role"), "student");
});

test("a rejected application keeps its reason and can be resubmitted", async () => {
  await submitCommunityApplicationService(db, googleApplicant, form);
  await reviewCommunityApplicationService(db, "admin-1", {
    applicationId: "applicant-1", decision: "reject", reason: "Topluluk belgesi eksik",
  });
  let { application } = await getMyCommunityApplicationService(db, "applicant-1");
  assert.equal(application?.status, "rejected");
  assert.equal(application?.rejectionReason, "Topluluk belgesi eksik");

  await submitCommunityApplicationService(db, googleApplicant, form);
  ({ application } = await getMyCommunityApplicationService(db, "applicant-1"));
  assert.equal(application?.status, "pending");
  assert.equal(application?.rejectionReason, "");
});

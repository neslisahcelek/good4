import assert from "node:assert/strict";
import { after, beforeEach, test } from "node:test";
import { getPortalContextService } from "./adminPortal.js";
import {
  listBusinessApplicationsService,
  normalizeBusinessPhone,
  reviewBusinessApplicationService,
  submitBusinessApplicationService,
} from "./businessApplications.js";
import { submitCommunityApplicationService } from "./communityApplications.js";
import { redeemCampaignCodeService } from "./campaigns.js";
import { db } from "./firebase.js";

const googleApplicant = {
  uid: "business-applicant", email: "Kafe@Gmail.com", emailVerified: true, providers: ["google.com"],
};
const form = { businessName: "Kampüs Kafe", contactName: "Mehmet Demir", phone: "0532 123 45 67" };

beforeEach(async () => {
  for (const collection of ["users", "organizations", "businessApplications", "communityApplications", "auditLogs", "rateLimits"]) {
    await db.recursiveDelete(db.collection(collection));
  }
  await db.doc("users/admin-1").set({ role: "good4Admin", status: "active" });
});

after(async () => {
  await db.terminate();
});

test("phone numbers are normalized to one Turkish format", () => {
  for (const input of ["0532 123 45 67", "+90 532 123 4567", "5321234567", "(0242) 310 00 00"]) {
    assert.match(normalizeBusinessPhone(input), /^\+90 \d{3} \d{3} \d{2} \d{2}$/);
  }
  for (const input of ["", "123", "0532 123 45 6", "abc", "+1 415 555 0100", 5321234567]) {
    assert.throws(() => normalizeBusinessPhone(input), /PHONE_INVALID/);
  }
});

test("a Google account without a role applies and the panel shows only its own application", async () => {
  const { application } = await submitBusinessApplicationService(db, googleApplicant, form);
  assert.equal(application.status, "pending");
  assert.equal(application.applicantEmail, "kafe@gmail.com");
  assert.equal(application.phone, "+90 532 123 45 67");

  const context = await getPortalContextService(db, "business-applicant");
  assert.equal(context.portalRole, "applicant");
  assert.equal(context.portalRole === "applicant" ? context.businessApplication?.businessName : "", "Kampüs Kafe");

  const fresh = await getPortalContextService(db, "nobody-yet");
  assert.deepEqual(fresh, { portalRole: "applicant", businessApplication: null });
});

test("applications require Google, a verified e-mail and the three fields", async () => {
  await assert.rejects(submitBusinessApplicationService(db, { ...googleApplicant, providers: ["password"] }, form), /GOOGLE_SIGN_IN_REQUIRED/);
  await assert.rejects(submitBusinessApplicationService(db, { ...googleApplicant, emailVerified: false }, form), /VERIFIED_EMAIL_REQUIRED/);
  await assert.rejects(submitBusinessApplicationService(db, googleApplicant, { ...form, businessName: " " }), /BUSINESSNAME/);
  await assert.rejects(submitBusinessApplicationService(db, googleApplicant, { ...form, contactName: "" }), /CONTACTNAME/);
  await assert.rejects(submitBusinessApplicationService(db, googleApplicant, { ...form, phone: "12" }), /PHONE_INVALID/);
});

test("students and accounts that already have a role cannot apply", async () => {
  await db.doc("users/business-applicant").set({ role: "student", status: "active" });
  await assert.rejects(submitBusinessApplicationService(db, googleApplicant, form), /STUDENT_ACCOUNT_NOT_ALLOWED/);
  await db.doc("users/business-applicant").set({ role: "communityManager", status: "active" });
  await assert.rejects(submitBusinessApplicationService(db, googleApplicant, form), /ACCOUNT_ALREADY_HAS_ROLE/);
});

test("one account waits for one role at a time", async () => {
  await submitBusinessApplicationService(db, googleApplicant, form);
  await assert.rejects(
    submitCommunityApplicationService(db, googleApplicant, {
      communityName: "Kulüp", university: "Akdeniz Üniversitesi", applicantName: "Mehmet Demir",
    }),
    /BUSINESS_APPLICATION_PENDING/,
  );
  await db.recursiveDelete(db.collection("businessApplications"));
  await submitCommunityApplicationService(db, googleApplicant, {
    communityName: "Kulüp", university: "Akdeniz Üniversitesi", applicantName: "Mehmet Demir",
  });
  await assert.rejects(submitBusinessApplicationService(db, googleApplicant, form), /COMMUNITY_APPLICATION_PENDING/);
});

test("a pending business cannot redeem codes", async () => {
  await submitBusinessApplicationService(db, googleApplicant, form);
  await assert.rejects(redeemCampaignCodeService(db, "business-applicant", { code: "ABCD1234" }), /ACCOUNT_NOT_ACTIVE/);
});

test("only admins list and review; approval creates the business, owner and membership", async () => {
  await submitBusinessApplicationService(db, googleApplicant, form);
  await assert.rejects(listBusinessApplicationsService(db, "business-applicant"), /ACCOUNT_NOT_ACTIVE/);
  await assert.rejects(reviewBusinessApplicationService(db, "business-applicant", { applicationId: "business-applicant", decision: "approve" }), /ACCOUNT_NOT_ACTIVE/);

  const { applications } = await listBusinessApplicationsService(db, "admin-1");
  assert.deepEqual(applications.map((item) => item.id), ["business-applicant"]);

  const result = await reviewBusinessApplicationService(db, "admin-1", { applicationId: "business-applicant", decision: "approve" });
  assert.equal(result.status, "approved");
  const organization = await db.doc(`organizations/${result.organizationId}`).get();
  assert.equal(organization.get("type"), "business");
  assert.equal(organization.get("status"), "active");
  assert.equal(organization.get("name"), "Kampüs Kafe");
  assert.equal(organization.get("phone"), "+90 532 123 45 67");
  const user = await db.doc("users/business-applicant").get();
  assert.equal(user.get("role"), "businessOwner");
  assert.equal(user.get("email"), "kafe@gmail.com");
  const member = await db.doc(`organizations/${result.organizationId}/members/business-applicant`).get();
  assert.equal(member.get("role"), "owner");

  const context = await getPortalContextService(db, "business-applicant");
  assert.equal(context.portalRole, "business");
  await assert.rejects(
    reviewBusinessApplicationService(db, "admin-1", { applicationId: "business-applicant", decision: "reject" }),
    /APPLICATION_ALREADY_REVIEWED/,
  );
  await assert.rejects(submitBusinessApplicationService(db, googleApplicant, form), /ACCOUNT_ALREADY_HAS_ROLE/);
});

test("a rejected business sees the reason and can apply again", async () => {
  await submitBusinessApplicationService(db, googleApplicant, form);
  await reviewBusinessApplicationService(db, "admin-1", { applicationId: "business-applicant", decision: "reject", reason: "Telefon ulaşılamıyor" });
  const context = await getPortalContextService(db, "business-applicant");
  assert.equal(context.portalRole === "applicant" ? context.businessApplication?.rejectionReason : "", "Telefon ulaşılamıyor");
  const { application } = await submitBusinessApplicationService(db, googleApplicant, { ...form, phone: "0532 765 43 21" });
  assert.equal(application.status, "pending");
  assert.equal(application.rejectionReason, "");
});

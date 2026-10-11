import { randomInt } from "node:crypto";
import {
  FieldPath,
  FieldValue,
  Timestamp,
  type Firestore,
  type Transaction,
  type DocumentSnapshot,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";
import {
  assertNotBlocked,
  assertRateLimit,
  recordFailedAttempt,
  clearFailedAttempts,
} from "./rateLimit.js";
const TTL = 600000;
export function communityCouponDay(nowMillis: number): string {
  const parts = new Intl.DateTimeFormat("en-CA", {
    timeZone: "Europe/Istanbul",
    year: "numeric",
    month: "2-digit",
    day: "2-digit",
  }).formatToParts(nowMillis);
  const partValue = (type: string) => parts.find((item) => item.type === type)!.value;
  return `${partValue("year")}-${partValue("month")}-${partValue("day")}`;
}
function id(value: unknown): string {
  const result = requireNonEmptyString(value, "id", 128);
  if (!/^[A-Za-z0-9_-]+$/.test(result)) {
    throw new HttpsError("invalid-argument", "ID_INVALID");
  }
  return result;
}
function positiveInteger(value: unknown, field: string, max = 1000000): number {
  if (typeof value !== "number" || !Number.isInteger(value) || value < 1 || value > max) {
    throw new HttpsError("invalid-argument", `${field}_INVALID`);
  }
  return value;
}
function timestampInput(value: unknown): Timestamp {
  if (
    typeof value !== "string" ||
    value.length > 30 ||
    !/^\d{4}-\d{2}-\d{2}T/.test(value) ||
    !Number.isFinite(Date.parse(value))
  ) {
    throw new HttpsError("invalid-argument", "DATE_INVALID");
  }
  const calendarDate = value.slice(0, 10);
  if (new Date(`${calendarDate}T00:00:00Z`).toISOString().slice(0, 10) !== calendarDate) {
    throw new HttpsError("invalid-argument", "DATE_INVALID");
  }
  return Timestamp.fromMillis(Date.parse(value));
}
async function requireOrganizationContext(
  database: Firestore,
  transaction: Transaction,
  actorUid: string,
  kind: "community" | "business",
  readOnly = false,
) {
  await requireActiveActor(
    database,
    transaction,
    actorUid,
    kind === "community"
      ? readOnly
        ? ["communityManager", "communityStaff"]
        : ["communityManager"]
      : ["businessOwner", "businessStaff"],
  );
  const members = await transaction.get(
    database.collectionGroup("members").where("userId", "==", actorUid).limit(10),
  );
  for (const member of members.docs) {
    const ref = member.ref.parent.parent;
    if (
      !ref ||
      ref.parent.id !== "organizations" ||
      member.id !== actorUid ||
      member.get("status") !== "active" ||
      !(
        kind === "community" ? (readOnly ? ["manager", "staff"] : ["manager"]) : ["owner", "staff"]
      ).includes(member.get("role"))
    ) {
      continue;
    }
    const org = await transaction.get(ref);
    if (org.get("type") === kind && org.get("status") === "active") {
      return org;
    }
  }
  throw new HttpsError("permission-denied", "ORGANIZATION_MEMBERSHIP_REQUIRED");
}
function recordAudit(
  database: Firestore,
  transaction: Transaction,
  actorUid: string,
  couponId: string,
  action: string,
) {
  transaction.create(database.collection("auditLogs").doc(), {
    actorUid: actorUid,
    targetType: "communityCoupon",
    targetId: couponId,
    action,
    createdAt: FieldValue.serverTimestamp(),
  });
}
export function communityCouponSummary(doc: DocumentSnapshot) {
  const data = doc.data()!;
  return {
    id: doc.id,
    communityId: data.communityId,
    communityName: data.communityName,
    businessId: data.businessId,
    businessName: data.businessName,
    title: data.title,
    description: data.description,
    discountType: data.discountType,
    discountValue: data.discountValue,
    freeItem: data.freeItem ?? "",
    startsAt: (data.startsAt as Timestamp).toDate().toISOString(),
    endsAt: (data.endsAt as Timestamp).toDate().toISOString(),
    totalLimit: data.totalLimit,
    dailyLimit: data.dailyLimit ?? null,
    redemptionCount: data.redemptionCount ?? 0,
    dailyRedemptionCount: data.dailyRedemptionCount ?? 0,
    dailyRedemptionDay: data.dailyRedemptionDay ?? "",
    status: data.status,
    rejectionReason: data.rejectionReason ?? "",
  };
}
export function communityCouponDiscount(data: Record<string, unknown>): string {
  return data.discountType === "freeItem"
    ? `Ücretsiz: ${data.freeItem}`
    : data.discountType === "fixed"
      ? `${data.discountValue} TL indirim`
      : `%${data.discountValue} indirim`;
}
export async function saveCommunityCouponService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
) {
  const title = requireNonEmptyString(input.title, "title", 120);
  const description = requireNonEmptyString(input.description, "description", 2000);
  const businessId = id(input.businessId);
  const discountType = input.discountType;
  if (
    typeof discountType !== "string" ||
    !["percentage", "fixed", "freeItem"].includes(discountType)
  ) {
    throw new HttpsError("invalid-argument", "DISCOUNT_INVALID");
  }
  const discountValue =
    discountType === "freeItem"
      ? 0
      : positiveInteger(
          input.discountValue,
          "DISCOUNT",
          discountType === "percentage" ? 100 : 100000,
        );
  const freeItem =
    discountType === "freeItem" ? requireNonEmptyString(input.freeItem, "freeItem", 80) : "";
  const startsAt = timestampInput(input.startsAt);
  const endsAt = timestampInput(input.endsAt);
  if (endsAt.toMillis() <= startsAt.toMillis()) {
    throw new HttpsError("invalid-argument", "DATE_RANGE_INVALID");
  }
  const totalLimit = positiveInteger(input.totalLimit, "TOTAL_LIMIT");
  const dailyLimit =
    input.dailyLimit == null ? null : positiveInteger(input.dailyLimit, "DAILY_LIMIT");
  if (dailyLimit !== null && dailyLimit > totalLimit) {
    throw new HttpsError("invalid-argument", "DAILY_LIMIT_INVALID");
  }
  const ref =
    input.couponId == null
      ? database.collection("communityCoupons").doc()
      : database.doc(`communityCoupons/${id(input.couponId)}`);
  return database.runTransaction(async (transaction) => {
    const community = await requireOrganizationContext(
      database,
      transaction,
      actorUid,
      "community",
    );
    const [old, business] = await Promise.all([
      transaction.get(ref),
      transaction.get(database.doc(`organizations/${businessId}`)),
    ]);
    if (input.couponId != null && !old.exists) {
      throw new HttpsError("not-found", "COUPON_NOT_FOUND");
    }
    if (
      old.exists &&
      (old.get("communityId") !== community.id ||
        !["pending", "rejected"].includes(old.get("status")))
    ) {
      throw new HttpsError("permission-denied", "COUPON_NOT_EDITABLE");
    }
    if (business.get("type") !== "business" || business.get("status") !== "active") {
      throw new HttpsError("failed-precondition", "BUSINESS_NOT_ACTIVE");
    }
    transaction.set(
      ref,
      {
        communityId: community.id,
        communityName: String(community.get("name") ?? ""),
        businessId,
        businessName: String(business.get("name") ?? ""),
        title,
        description,
        discountType,
        discountValue,
        freeItem,
        startsAt,
        endsAt,
        totalLimit,
        dailyLimit,
        status: "pending",
        rejectionReason: "",
        ...(old.exists
          ? {}
          : {
              redemptionCount: 0,
              dailyRedemptionCount: 0,
              dailyRedemptionDay: "",
              createdAt: FieldValue.serverTimestamp(),
            }),
        createdBy: FieldValue.delete(),
        updatedAt: FieldValue.serverTimestamp(),
      },
      { merge: true },
    );
    if (!old.exists) {
      recordAudit(database, transaction, actorUid, ref.id, "communityCoupon.created");
    } else if (typeof old.get("createdBy") === "string") {
      recordAudit(database, transaction, old.get("createdBy"), ref.id, "communityCoupon.created");
    }
    return { couponId: ref.id, status: "pending" };
  });
}
export async function endCommunityCouponService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
) {
  const ref = database.doc(`communityCoupons/${id(input.couponId)}`);
  return database.runTransaction(async (transaction) => {
    const org = await requireOrganizationContext(database, transaction, actorUid, "community");
    const coupon = await transaction.get(ref);
    if (!coupon.exists || coupon.get("communityId") !== org.id) {
      throw new HttpsError("permission-denied", "COUPON_NOT_OWNED");
    }
    if (typeof coupon.get("createdBy") === "string") {
      recordAudit(
        database,
        transaction,
        coupon.get("createdBy"),
        ref.id,
        "communityCoupon.created",
      );
    }
    recordAudit(database, transaction, actorUid, ref.id, "communityCoupon.ended");
    transaction.update(ref, {
      status: "ended",
      createdBy: FieldValue.delete(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    return { status: "ended" };
  });
}
export async function listCommunityCouponsService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown> = {},
) {
  const cursor = input.cursor == null ? null : id(input.cursor);
  return database.runTransaction(async (transaction) => {
    const org = await requireOrganizationContext(
      database,
      transaction,
      actorUid,
      "community",
      true,
    );
    let query = database
      .collection("communityCoupons")
      .where("communityId", "==", org.id)
      .orderBy(FieldPath.documentId())
      .limit(100);
    if (cursor) {
      query = query.startAfter(cursor);
    }
    const [coupons, businesses] = await Promise.all([
      transaction.get(query),
      transaction.get(
        database
          .collection("organizations")
          .where("type", "==", "business")
          .where("status", "==", "active"),
      ),
    ]);
    return {
      coupons: coupons.docs.map(communityCouponSummary),
      nextCursor: coupons.size === 100 ? coupons.docs.at(-1)!.id : null,
      businesses: businesses.docs.map((businessDocument) => ({
        id: businessDocument.id,
        name: String(businessDocument.get("name") ?? ""),
      })),
    };
  });
}
export async function reviewCommunityCouponService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
) {
  const ref = database.doc(`communityCoupons/${id(input.couponId)}`);
  if (typeof input.decision !== "string" || !["approve", "reject"].includes(input.decision)) {
    throw new HttpsError("invalid-argument", "DECISION_INVALID");
  }
  const reason =
    input.decision === "reject" ? requireNonEmptyString(input.reason, "reason", 500) : "";
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const coupon = await transaction.get(ref);
    if (!coupon.exists || coupon.get("status") !== "pending") {
      throw new HttpsError("failed-precondition", "COUPON_NOT_PENDING");
    }
    const [business, community] = await Promise.all([
      transaction.get(database.doc(`organizations/${coupon.get("businessId")}`)),
      transaction.get(database.doc(`organizations/${coupon.get("communityId")}`)),
    ]);
    if (
      input.decision === "approve" &&
      (business.get("status") !== "active" ||
        business.get("type") !== "business" ||
        community.get("status") !== "active" ||
        community.get("type") !== "community")
    ) {
      throw new HttpsError("failed-precondition", "ORGANIZATION_NOT_ACTIVE");
    }
    const status = input.decision === "approve" ? "published" : "rejected";
    if (typeof coupon.get("createdBy") === "string") {
      recordAudit(
        database,
        transaction,
        coupon.get("createdBy"),
        ref.id,
        "communityCoupon.created",
      );
    }
    transaction.update(ref, {
      status,
      rejectionReason: reason,
      createdBy: FieldValue.delete(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    recordAudit(database, transaction, actorUid, ref.id, `communityCoupon.${status}`);
    return { status };
  });
}
function couponUnavailableOutcome(coupon: DocumentSnapshot, nowMillis: number): string | null {
  if (
    !coupon.exists ||
    coupon.get("status") !== "published" ||
    nowMillis < coupon.get("startsAt").toMillis() ||
    nowMillis >= coupon.get("endsAt").toMillis()
  ) {
    return "coupon_inactive";
  }
  if (Number(coupon.get("redemptionCount") ?? 0) >= coupon.get("totalLimit")) {
    return "limit_reached";
  }
  const daily =
    coupon.get("dailyRedemptionDay") === communityCouponDay(nowMillis)
      ? Number(coupon.get("dailyRedemptionCount") ?? 0)
      : 0;
  if (coupon.get("dailyLimit") != null && daily >= coupon.get("dailyLimit")) {
    return "daily_limit_reached";
  }
  return null;
}
async function hasActiveCouponOrganizations(
  database: Firestore,
  transaction: Transaction,
  coupon: DocumentSnapshot,
) {
  const [business, community] = await Promise.all([
    transaction.get(database.doc(`organizations/${coupon.get("businessId")}`)),
    transaction.get(database.doc(`organizations/${coupon.get("communityId")}`)),
  ]);
  return (
    business.get("type") === "business" &&
    business.get("status") === "active" &&
    community.get("type") === "community" &&
    community.get("status") === "active"
  );
}
export async function issueCommunityCouponCodeService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
  nowMillis = Date.now(),
  generateCode = () => String(randomInt(0, 10000)).padStart(4, "0"),
) {
  const couponId = id(input.couponId);
  const day = communityCouponDay(nowMillis);
  const claimRef = database.doc(`communityCouponClaims/${couponId}_${actorUid}_${day}`);
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["student"]);
    const [coupon, claim] = await Promise.all([
      transaction.get(database.doc(`communityCoupons/${couponId}`)),
      transaction.get(claimRef),
    ]);
    const outcome = couponUnavailableOutcome(coupon, nowMillis);
    if (outcome === "coupon_inactive") {
      return { outcome };
    }
    if (!(await hasActiveCouponOrganizations(database, transaction, coupon))) {
      return { outcome: "coupon_inactive" };
    }
    if (claim.get("status") === "redeemed") {
      return { outcome: "already_used_today" };
    }
    if (outcome) {
      return { outcome };
    }
    const businessId = coupon.get("businessId") as string;
    if (claim.get("status") === "issued" && claim.get("expiresAt")?.toMillis() > nowMillis) {
      const old = await transaction.get(
        database.doc(`communityCouponCodes/${businessId}_${claim.get("code")}`),
      );
      if (
        old.get("studentId") === actorUid &&
        old.get("couponId") === couponId &&
        old.get("status") === "issued" &&
        old.get("expiresAt")?.toMillis() > nowMillis
      ) {
        return {
          outcome: "already_issued",
          code: claim.get("code") as string,
          expiresAt: claim.get("expiresAt").toDate().toISOString() as string,
        };
      }
    }
    // Read collision candidates before any writes. Only a live issued code blocks reuse.
    let chosen: {
      code: string;
      ref: FirebaseFirestore.DocumentReference;
    } | null = null;
    for (let attempt = 0; attempt < 40; attempt++) {
      const code = generateCode();
      if (!/^\d{4}$/.test(code)) {
        throw new HttpsError("internal", "CODE_GENERATION_FAILED");
      }
      const ref = database.doc(`communityCouponCodes/${businessId}_${code}`);
      const existing = await transaction.get(ref);
      if (
        existing.get("status") !== "issued" ||
        existing.get("expiresAt")?.toMillis() <= nowMillis
      ) {
        chosen = { code, ref };
        break;
      }
    }
    if (!chosen) {
      throw new HttpsError("resource-exhausted", "CODE_SPACE_BUSY");
    }
    await assertRateLimit(
      database,
      transaction,
      { key: `communityCouponIssue:${actorUid}`, limit: 5, windowSeconds: 60 },
      nowMillis,
    );
    const expiresAt = Timestamp.fromMillis(nowMillis + TTL);
    const payload = {
      couponId,
      communityId: coupon.get("communityId"),
      businessId,
      studentId: actorUid,
      day,
      code: chosen.code,
      status: "issued",
      issuedAt: Timestamp.fromMillis(nowMillis),
      expiresAt,
    };
    transaction.set(chosen.ref, payload);
    transaction.set(claimRef, payload);
    return { outcome: "issued", code: chosen.code, expiresAt: expiresAt.toDate().toISOString() };
  });
}
export async function redeemCommunityCouponCodeService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
  nowMillis = Date.now(),
) {
  if (typeof input.code !== "string" || !/^\d{4}$/.test(input.code)) {
    throw new HttpsError("invalid-argument", "CODE_INVALID");
  }
  const result = await database.runTransaction(async (transaction) => {
    const business = await requireOrganizationContext(database, transaction, actorUid, "business");
    const key = `communityCouponFail:${business.id}`;
    await assertNotBlocked(database, transaction, key, nowMillis);
    const codeRef = database.doc(`communityCouponCodes/${business.id}_${input.code}`);
    const code = await transaction.get(codeRef);
    async function fail(outcome: string) {
      const failure = await recordFailedAttempt(
        database,
        transaction,
        { key, maxAttempts: 5, windowSeconds: 60, blockDurationSeconds: 300 },
        nowMillis,
      );
      return failure.error ? { outcome, error: failure.error } : { outcome };
    }
    if (!code.exists) {
      return fail("not_found");
    }
    if (code.get("status") === "redeemed") {
      return { outcome: "already_redeemed" };
    }
    if (code.get("expiresAt").toMillis() <= nowMillis || code.get("status") !== "issued") {
      return { outcome: "expired" };
    }
    const couponId = code.get("couponId");
    const studentId = code.get("studentId");
    const day = communityCouponDay(nowMillis);
    const claimRef = database.doc(`communityCouponClaims/${couponId}_${studentId}_${day}`);
    const [coupon, claim, student] = await Promise.all([
      transaction.get(database.doc(`communityCoupons/${couponId}`)),
      transaction.get(claimRef),
      transaction.get(database.doc(`users/${studentId}`)),
    ]);
    const outcome = couponUnavailableOutcome(coupon, nowMillis);
    if (outcome) {
      return { outcome };
    }
    if (
      coupon.get("businessId") !== business.id ||
      !(await hasActiveCouponOrganizations(database, transaction, coupon))
    ) {
      return { outcome: "coupon_inactive" };
    }
    if (student.get("status") !== "active" || student.get("role") !== "student") {
      return { outcome: "student_inactive" };
    }
    if (claim.get("status") === "redeemed") {
      return { outcome: "already_used_today" };
    }
    const daily =
      coupon.get("dailyRedemptionDay") === day
        ? Number(coupon.get("dailyRedemptionCount") ?? 0)
        : 0;
    transaction.update(codeRef, {
      status: "redeemed",
      redeemedAt: Timestamp.fromMillis(nowMillis),
      redeemedBy: actorUid,
    });
    transaction.set(claimRef, {
      couponId,
      studentId,
      day,
      code: input.code,
      status: "redeemed",
      redeemedAt: Timestamp.fromMillis(nowMillis),
    });
    transaction.update(coupon.ref, {
      redemptionCount: FieldValue.increment(1),
      dailyRedemptionDay: day,
      dailyRedemptionCount: daily + 1,
      updatedAt: FieldValue.serverTimestamp(),
    });
    const discountLabel = communityCouponDiscount(coupon.data()!);
    transaction.create(database.collection("communityCouponRedemptions").doc(), {
      couponId,
      organizationId: business.id,
      communityId: coupon.get("communityId"),
      studentId,
      redeemedBy: actorUid,
      redeemedAt: Timestamp.fromMillis(nowMillis),
      title: coupon.get("title"),
      communityName: coupon.get("communityName"),
      discountLabel,
    });
    await clearFailedAttempts(database, transaction, key);
    return {
      outcome: "redeemed",
      couponTitle: String(coupon.get("title")),
      communityName: String(coupon.get("communityName")),
      discountLabel,
    };
  });
  if ("error" in result && result.error) {
    throw result.error;
  }
  return result;
}
export async function listBusinessRedemptionsService(database: Firestore, actorUid: string) {
  return database.runTransaction(async (transaction) => {
    const org = await requireOrganizationContext(database, transaction, actorUid, "business");
    const [meals, coupons] = await Promise.all([
      transaction.get(
        database
          .collection("redemptions")
          .where("organizationId", "==", org.id)
          .orderBy("redeemedAt", "desc")
          .limit(100),
      ),
      transaction.get(
        database
          .collection("communityCouponRedemptions")
          .where("organizationId", "==", org.id)
          .orderBy("redeemedAt", "desc")
          .limit(100),
      ),
    ]);
    const titles = new Map<string, string>();
    for (const item of meals.docs) {
      const campaignId = String(item.get("campaignId"));
      if (!titles.has(campaignId)) {
        const campaign = await transaction.get(database.doc(`campaigns/${campaignId}`));
        titles.set(campaignId, String(campaign.get("title") ?? ""));
      }
    }
    return {
      redemptions: [
        ...meals.docs.map((item) => ({
          id: item.id,
          source: "campaign",
          code: String(item.get("code") ?? item.id),
          title: titles.get(String(item.get("campaignId"))) ?? "",
          communityName: "",
          discountLabel: "",
          redeemedAt: item.get("redeemedAt").toDate().toISOString(),
        })),
        ...coupons.docs.map((item) => ({
          id: item.id,
          source: "communityCoupon",
          code: "",
          title: String(item.get("title") ?? ""),
          communityName: String(item.get("communityName") ?? ""),
          discountLabel: String(item.get("discountLabel") ?? ""),
          redeemedAt: item.get("redeemedAt").toDate().toISOString(),
        })),
      ].sort((a, businessDocument) => businessDocument.redeemedAt.localeCompare(a.redeemedAt)),
    };
  });
}

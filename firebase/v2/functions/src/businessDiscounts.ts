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
import { assertRateLimit, clearFailedAttempts } from "./rateLimit.js";
import { couponDay } from "./couponClock.js";
import { normalizeGoogleMapsUrl } from "./businessDiscountMaps.js";

const PAGE_SIZE = 100;
function identifier(value: unknown): string {
  const result = requireNonEmptyString(value, "id", 128);
  if (!/^[A-Za-z0-9_-]+$/.test(result))
    throw new HttpsError("invalid-argument", "ID_INVALID");
  return result;
}
function dateInput(value: unknown): Timestamp {
  if (
    typeof value !== "string" ||
    value.length > 30 ||
    !/^\d{4}-\d{2}-\d{2}T.*(?:Z|[+-]\d{2}:\d{2})$/.test(value) ||
    !Number.isFinite(Date.parse(value))
  ) {
    throw new HttpsError("invalid-argument", "DATE_INVALID");
  }
  const date = value.slice(0, 10);
  if (new Date(`${date}T00:00:00Z`).toISOString().slice(0, 10) !== date)
    throw new HttpsError("invalid-argument", "DATE_INVALID");
  return Timestamp.fromMillis(Date.parse(value));
}
function audit(
  database: Firestore,
  transaction: Transaction,
  actorUid: string,
  targetId: string,
  action: string,
) {
  transaction.create(database.collection("auditLogs").doc(), {
    actorUid,
    targetType: "businessDiscount",
    targetId,
    action,
    createdAt: FieldValue.serverTimestamp(),
  });
}
export async function saveBusinessDiscountService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
) {
  const businessId = identifier(input.businessId);
  const title = requireNonEmptyString(input.title, "title", 100);
  const conditions = requireNonEmptyString(
    input.conditions,
    "conditions",
    2000,
  );
  let mapsUrl: string | undefined;
  if (input.mapsUrl !== undefined) {
    try {
      mapsUrl = normalizeGoogleMapsUrl(input.mapsUrl);
    } catch {
      throw new HttpsError("invalid-argument", "MAPS_URL_INVALID");
    }
  }
  const discountValue = input.discountValue;
  const totalLimit = input.totalLimit ?? null;
  if (
    typeof discountValue !== "number" ||
    !Number.isInteger(discountValue) ||
    discountValue < 1 ||
    discountValue > 100
  )
    throw new HttpsError("invalid-argument", "DISCOUNT_INVALID");
  if (
    totalLimit !== null &&
    (typeof totalLimit !== "number" ||
      !Number.isInteger(totalLimit) ||
      totalLimit < 1 ||
      totalLimit > 1000000)
  )
    throw new HttpsError("invalid-argument", "LIMIT_INVALID");
  const startsAt = dateInput(input.startsAt);
  const endsAt = dateInput(input.endsAt);
  if (startsAt.toMillis() >= endsAt.toMillis())
    throw new HttpsError("invalid-argument", "DATE_ORDER_INVALID");
  const ref =
    input.discountId == null
      ? database.collection("businessDiscounts").doc()
      : database.doc(`businessDiscounts/${identifier(input.discountId)}`);
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const [old, business] = await Promise.all([
      transaction.get(ref),
      transaction.get(database.doc(`organizations/${businessId}`)),
    ]);
    if (input.discountId != null && !old.exists)
      throw new HttpsError("not-found", "DISCOUNT_NOT_FOUND");
    if (
      business.get("type") !== "business" ||
      business.get("status") !== "active"
    )
      throw new HttpsError("failed-precondition", "BUSINESS_NOT_ACTIVE");
    if (old.exists && old.get("businessId") !== businessId)
      throw new HttpsError("failed-precondition", "BUSINESS_NOT_EDITABLE");
    const redemptionCount = Number(old.get("redemptionCount") ?? 0);
    if (totalLimit !== null && totalLimit < redemptionCount)
      throw new HttpsError("failed-precondition", "LIMIT_BELOW_USAGE");
    transaction.set(ref, {
      businessId,
      businessName: String(business.get("name") ?? ""),
      title,
      conditions,
      mapsUrl: mapsUrl ?? String(old.get("mapsUrl") ?? ""),
      discountValue,
      totalLimit,
      startsAt,
      endsAt,
      redemptionCount,
      status: "published",
      version: Number(old.get("version") ?? 0) + 1,
      createdAt: old.get("createdAt") ?? FieldValue.serverTimestamp(),
      updatedAt: FieldValue.serverTimestamp(),
    });
    audit(
      database,
      transaction,
      actorUid,
      ref.id,
      old.exists ? "businessDiscount.updated" : "businessDiscount.created",
    );
    return { discountId: ref.id, status: "published" };
  });
}
export async function endBusinessDiscountService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
) {
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    const discount = await transaction.get(
      database.doc(`businessDiscounts/${identifier(input.discountId)}`),
    );
    if (!discount.exists)
      throw new HttpsError("not-found", "DISCOUNT_NOT_FOUND");
    transaction.update(discount.ref, {
      status: "ended",
      version: Number(discount.get("version")) + 1,
      updatedAt: FieldValue.serverTimestamp(),
    });
    audit(
      database,
      transaction,
      actorUid,
      discount.id,
      "businessDiscount.ended",
    );
    return { status: "ended" };
  });
}
export async function listBusinessDiscountsService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown> = {},
) {
  const cursor = input.cursor == null ? null : identifier(input.cursor);
  const businessesCursor =
    input.businessesCursor == null ? null : identifier(input.businessesCursor);
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["good4Admin"]);
    let query = database
      .collection("businessDiscounts")
      .orderBy(FieldPath.documentId())
      .limit(PAGE_SIZE);
    let businessesQuery = database
      .collection("organizations")
      .where("type", "==", "business")
      .where("status", "==", "active")
      .orderBy(FieldPath.documentId())
      .limit(PAGE_SIZE);
    if (cursor) query = query.startAfter(cursor);
    if (businessesCursor)
      businessesQuery = businessesQuery.startAfter(businessesCursor);
    const [discounts, businesses] = await Promise.all([
      input.businessesOnly === true
        ? Promise.resolve(null)
        : transaction.get(query),
      input.includeBusinesses === false
        ? Promise.resolve(null)
        : transaction.get(businessesQuery),
    ]);
    return {
      discounts:
        discounts?.docs.map((discount) => ({
          id: discount.id,
          ...discount.data(),
          mapsUrl: String(discount.get("mapsUrl") ?? ""),
          startsAt: discount.get("startsAt").toDate().toISOString(),
          endsAt: discount.get("endsAt").toDate().toISOString(),
        })) ?? [],
      nextCursor:
        discounts?.size === PAGE_SIZE ? discounts.docs.at(-1)!.id : null,
      businesses:
        businesses?.docs.map((business) => ({
          id: business.id,
          name: String(business.get("name") ?? ""),
        })) ?? [],
      businessesNextCursor:
        businesses?.size === PAGE_SIZE ? businesses.docs.at(-1)!.id : null,
    };
  });
}
function unavailable(
  discount: DocumentSnapshot,
  nowMillis: number,
): string | null {
  if (
    !discount.exists ||
    discount.get("status") !== "published" ||
    nowMillis < discount.get("startsAt")?.toMillis() ||
    nowMillis >= discount.get("endsAt")?.toMillis()
  )
    return "coupon_inactive";
  if (
    discount.get("totalLimit") != null &&
    Number(discount.get("redemptionCount") ?? 0) >= discount.get("totalLimit")
  )
    return "limit_reached";
  return null;
}
export async function issueBusinessDiscountCodeService(
  database: Firestore,
  actorUid: string,
  input: Record<string, unknown>,
  nowMillis = Date.now(),
  generateCode = () => String(randomInt(0, 10000)).padStart(4, "0"),
) {
  const discountId = identifier(input.discountId);
  const day = couponDay(nowMillis);
  const claimRef = database.doc(
    `businessDiscountClaims/${discountId}_${actorUid}_${day}`,
  );
  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["student"]);
    const [discount, claim] = await Promise.all([
      transaction.get(database.doc(`businessDiscounts/${discountId}`)),
      transaction.get(claimRef),
    ]);
    const outcome = unavailable(discount, nowMillis);
    if (
      outcome === "coupon_inactive" ||
      discount.get("version") !== input.version
    )
      return { outcome: "coupon_inactive" };
    const businessId = String(discount.get("businessId"));
    const business = await transaction.get(
      database.doc(`organizations/${businessId}`),
    );
    if (
      business.get("type") !== "business" ||
      business.get("status") !== "active"
    )
      return { outcome: "coupon_inactive" };
    if (claim.get("status") === "redeemed")
      return { outcome: "already_used_today" };
    if (outcome) return { outcome };
    if (
      claim.get("status") === "issued" &&
      claim.get("expiresAt")?.toMillis() > nowMillis
    ) {
      const old = await transaction.get(
        database.doc(`communityCouponCodes/${businessId}_${claim.get("code")}`),
      );
      if (
        old.get("source") === "businessDiscount" &&
        old.get("discountId") === discountId &&
        old.get("version") === discount.get("version") &&
        old.get("studentId") === actorUid &&
        old.get("status") === "issued" &&
        old.get("expiresAt")?.toMillis() > nowMillis
      ) {
        return {
          outcome: "already_issued",
          code: String(claim.get("code")),
          expiresAt: old.get("expiresAt").toDate().toISOString() as string,
        };
      }
    }
    let chosen: {
      code: string;
      ref: FirebaseFirestore.DocumentReference;
    } | null = null;
    for (let attempt = 0; attempt < 40; attempt++) {
      const code = generateCode();
      if (!/^\d{4}$/.test(code))
        throw new HttpsError("internal", "CODE_GENERATION_FAILED");
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
    if (!chosen) throw new HttpsError("resource-exhausted", "CODE_SPACE_BUSY");
    await assertRateLimit(
      database,
      transaction,
      { key: `communityCouponIssue:${actorUid}`, limit: 5, windowSeconds: 60 },
      nowMillis,
    );
    const expiresAt = Timestamp.fromMillis(nowMillis + 600000);
    const payload = {
      source: "businessDiscount",
      discountId,
      version: discount.get("version"),
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
    return {
      outcome: "issued",
      code: chosen.code,
      expiresAt: expiresAt.toDate().toISOString(),
    };
  });
}
/** Called inside the shared four-digit redemption transaction, after business authorization. */
export async function redeemBusinessDiscountInTransaction(
  database: Firestore,
  transaction: Transaction,
  business: DocumentSnapshot,
  code: DocumentSnapshot,
  actorUid: string,
  nowMillis: number,
) {
  const discountId = String(code.get("discountId"));
  const studentId = String(code.get("studentId"));
  const day = couponDay(nowMillis);
  const claimRef = database.doc(
    `businessDiscountClaims/${discountId}_${studentId}_${day}`,
  );
  const [discount, claim, student] = await Promise.all([
    transaction.get(database.doc(`businessDiscounts/${discountId}`)),
    transaction.get(claimRef),
    transaction.get(database.doc(`users/${studentId}`)),
  ]);
  const outcome = unavailable(discount, nowMillis);
  if (outcome) return { outcome };
  if (
    discount.get("businessId") !== business.id ||
    code.get("businessId") !== business.id ||
    code.get("version") !== discount.get("version")
  )
    return { outcome: "coupon_inactive" };
  if (student.get("status") !== "active" || student.get("role") !== "student")
    return { outcome: "student_inactive" };
  if (claim.get("status") === "redeemed")
    return { outcome: "already_used_today" };
  transaction.update(code.ref, {
    status: "redeemed",
    redeemedBy: actorUid,
    redeemedAt: Timestamp.fromMillis(nowMillis),
  });
  transaction.set(claimRef, {
    discountId,
    studentId,
    day,
    code: code.get("code"),
    status: "redeemed",
    redeemedAt: Timestamp.fromMillis(nowMillis),
  });
  transaction.update(discount.ref, {
    redemptionCount: FieldValue.increment(1),
    updatedAt: FieldValue.serverTimestamp(),
  });
  const discountLabel = `%${discount.get("discountValue")} indirim`;
  transaction.create(database.collection("businessDiscountRedemptions").doc(), {
    discountId,
    organizationId: business.id,
    studentId,
    redeemedBy: actorUid,
    redeemedAt: Timestamp.fromMillis(nowMillis),
    title: discount.get("title"),
    conditions: discount.get("conditions"),
    discountLabel,
    version: discount.get("version"),
  });
  await clearFailedAttempts(
    database,
    transaction,
    `communityCouponFail:${business.id}`,
  );
  return {
    outcome: "redeemed",
    couponTitle: String(discount.get("title")),
    communityName: "",
    discountLabel,
    discountConditions: String(discount.get("conditions")),
  };
}

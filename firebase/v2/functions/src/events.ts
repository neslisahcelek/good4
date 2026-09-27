import { randomUUID } from "node:crypto";
import {
  FieldValue,
  Timestamp,
  type DocumentSnapshot,
  type Firestore,
  type Transaction,
} from "firebase-admin/firestore";
import { HttpsError } from "firebase-functions/v2/https";
import { requireActiveActor, requireNonEmptyString } from "./shared.js";
import { EVENT_CATEGORIES, type EventCategoryId } from "./eventCategories.js";

export const EVENT_STATUSES = ["draft", "published", "cancelled", "completed"] as const;
export type EventStatus = (typeof EVENT_STATUSES)[number];

function safeId(value: unknown, fieldName: string): string {
  const id = requireNonEmptyString(value, fieldName, 128);
  if (!/^[A-Za-z0-9_-]+$/.test(id)) {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  }
  return id;
}

function nonNegativeInteger(value: unknown, fieldName: string): number {
  if (!Number.isInteger(value) || Number(value) < 0) {
    throw new HttpsError("invalid-argument", `${fieldName.toUpperCase()}_INVALID`);
  }
  return Number(value);
}

function eventStatus(value: unknown): EventStatus {
  if (typeof value === "string" && EVENT_STATUSES.includes(value as EventStatus)) {
    return value as EventStatus;
  }
  return "published";
}

function istanbulTimestamp(dateValue: unknown, timeValue: unknown): Timestamp {
  const date = requireNonEmptyString(dateValue, "date", 10);
  const time = requireNonEmptyString(timeValue, "time", 5);
  if (!/^\d{4}-\d{2}-\d{2}$/.test(date) || !/^([01]\d|2[0-3]):[0-5]\d$/.test(time)) {
    throw new HttpsError("invalid-argument", "EVENT_DATE_TIME_INVALID");
  }
  const parsed = new Date(`${date}T${time}:00+03:00`);
  if (Number.isNaN(parsed.getTime())) {
    throw new HttpsError("invalid-argument", "EVENT_DATE_TIME_INVALID");
  }
  return Timestamp.fromDate(parsed);
}

async function requireCommunityMembership(
  database: Firestore,
  transaction: Transaction,
  actorUid: string,
  organizationId: string,
): Promise<"manager" | "staff"> {
  await requireActiveActor(database, transaction, actorUid, ["communityManager", "communityStaff"]);
  const membership = await transaction.get(
    database.doc(`organizations/${organizationId}/members/${actorUid}`),
  );
  const role = membership.get("role");
  if (!membership.exists || membership.get("status") !== "active" || !["manager", "staff"].includes(role)) {
    throw new HttpsError("permission-denied", "COMMUNITY_MEMBERSHIP_REQUIRED");
  }
  return role as "manager" | "staff";
}

export interface SaveEventInput {
  eventId?: unknown;
  title: unknown;
  description: unknown;
  date: unknown;
  time: unknown;
  location: unknown;
  capacity?: unknown;
  status?: unknown;
  imageUrl?: unknown;
  categoryId?: unknown;
}

export async function saveEventService(
  database: Firestore,
  actorUid: string,
  organizationId: string,
  input: SaveEventInput,
): Promise<{ eventId: string; status: EventStatus }> {
  const eventId = typeof input.eventId === "string" && input.eventId.trim()
    ? safeId(input.eventId, "eventId")
    : "";
  const title = requireNonEmptyString(input.title, "title", 120);
  const description = requireNonEmptyString(input.description, "description", 4000);
  const startsAt = istanbulTimestamp(input.date, input.time);
  const endsAt = Timestamp.fromMillis(startsAt.toMillis() + 2 * 60 * 60 * 1000);
  const location = requireNonEmptyString(input.location, "location", 200);
  const capacity = nonNegativeInteger(input.capacity ?? 0, "capacity");
  const status = eventStatus(input.status);
  // Absence is accepted for older clients. Explicit empty/unknown values are invalid.
  const categoryId = input.categoryId === undefined ? undefined : input.categoryId;
  if (categoryId !== undefined && !EVENT_CATEGORIES.some((category) => category.id === categoryId)) {
    throw new HttpsError("invalid-argument", "EVENT_CATEGORY_INVALID");
  }
  const requestedImageUrl = typeof input.imageUrl === "string" ? input.imageUrl.trim() : null;
  if (requestedImageUrl && !requestedImageUrl.startsWith("https://")) {
    throw new HttpsError("invalid-argument", "IMAGE_URL_INVALID");
  }
  const eventRef = eventId ? database.doc(`events/${eventId}`) : database.collection("events").doc();

  await database.runTransaction(async (transaction) => {
    const membershipRole = await requireCommunityMembership(database, transaction, actorUid, organizationId);
    if (membershipRole !== "manager") {
      throw new HttpsError("permission-denied", "COMMUNITY_MANAGER_REQUIRED");
    }
    const organization = await transaction.get(database.doc(`organizations/${organizationId}`));
    if (!organization.exists || organization.get("type") !== "community" || organization.get("status") !== "active") {
      throw new HttpsError("failed-precondition", "COMMUNITY_NOT_ACTIVE");
    }
    const current = eventId ? await transaction.get(eventRef) : null;
    if (eventId && (!current?.exists || current.get("organizationId") !== organizationId)) {
      throw new HttpsError("not-found", "EVENT_NOT_FOUND");
    }
    if (current?.get("status") === "cancelled") {
      throw new HttpsError("failed-precondition", "EVENT_NOT_EDITABLE");
    }
    const imageUrl = requestedImageUrl ?? String(current?.get("imageUrl") ?? "");
    const savedCategoryId = categoryId ?? current?.get("categoryId");
    const auditRef = database.collection("auditLogs").doc();
    transaction.set(eventRef, {
      organizationId,
      title,
      description,
      startsAt,
      endsAt,
      timezone: "Europe/Istanbul",
      location,
      imageUrl,
      ...(savedCategoryId !== undefined ? { categoryId: savedCategoryId as EventCategoryId } : {}),
      capacity,
      registrationCount: Number(current?.get("registrationCount") ?? 0),
      attendanceCount: Number(current?.get("attendanceCount") ?? 0),
      status,
      createdAt: current?.get("createdAt") ?? FieldValue.serverTimestamp(),
      createdBy: current?.get("createdBy") ?? actorUid,
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(auditRef, {
      action: eventId ? "event.updated" : "event.created",
      actorUid,
      targetType: "event",
      targetId: eventRef.id,
      metadata: { organizationId, status },
      createdAt: FieldValue.serverTimestamp(),
    });
  });

  return { eventId: eventRef.id, status };
}

export async function cancelEventService(
  database: Firestore,
  actorUid: string,
  organizationId: string,
  eventIdValue: unknown,
): Promise<{ status: "cancelled" }> {
  const eventId = safeId(eventIdValue, "eventId");
  const eventRef = database.doc(`events/${eventId}`);
  await database.runTransaction(async (transaction) => {
    const membershipRole = await requireCommunityMembership(database, transaction, actorUid, organizationId);
    if (membershipRole !== "manager") {
      throw new HttpsError("permission-denied", "COMMUNITY_MANAGER_REQUIRED");
    }
    const event = await transaction.get(eventRef);
    if (!event.exists || event.get("organizationId") !== organizationId) {
      throw new HttpsError("not-found", "EVENT_NOT_FOUND");
    }
    if (event.get("status") !== "cancelled") {
      transaction.update(eventRef, {
        status: "cancelled",
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    transaction.create(database.collection("auditLogs").doc(), {
      action: "event.cancelled",
      actorUid,
      targetType: "event",
      targetId: eventId,
      metadata: { organizationId },
      createdAt: FieldValue.serverTimestamp(),
    });
  });
  return { status: "cancelled" };
}

function registrationResult(snapshot: DocumentSnapshot) {
  const registeredAt = snapshot.get("registeredAt");
  return {
    registrationId: snapshot.id,
    userId: String(snapshot.get("userId") ?? ""),
    displayName: String(snapshot.get("displayName") ?? "Good4 öğrencisi"),
    registeredAt: registeredAt instanceof Timestamp ? registeredAt.toDate().toISOString() : null,
  };
}

export async function setEventRegistrationService(
  database: Firestore,
  actorUid: string,
  input: { eventId: unknown; registered: unknown },
): Promise<{
  outcome: "registered" | "already_registered" | "unregistered" | "not_registered";
  registrationId?: string;
}> {
  const eventId = safeId(input.eventId, "eventId");
  if (typeof input.registered !== "boolean") {
    throw new HttpsError("invalid-argument", "REGISTERED_REQUIRED");
  }
  const eventRef = database.doc(`events/${eventId}`);

  return database.runTransaction(async (transaction) => {
    await requireActiveActor(database, transaction, actorUid, ["student"]);
    const event = await transaction.get(eventRef);
    if (!event.exists || event.get("status") !== "published") {
      throw new HttpsError("failed-precondition", "EVENT_NOT_OPEN");
    }
    const endsAt = event.get("endsAt");
    if (!(endsAt instanceof Timestamp) || Timestamp.now().toMillis() >= endsAt.toMillis()) {
      throw new HttpsError("failed-precondition", "EVENT_ENDED");
    }
    const existingQuery = eventRef.collection("registrations")
      .where("userId", "==", actorUid)
      .limit(1);
    const existing = await transaction.get(existingQuery);
    const current = existing.docs[0];

    if (input.registered) {
      if (current) {
        return { outcome: "already_registered" as const, registrationId: current.id };
      }
      const registrationCount = Number(event.get("registrationCount") ?? 0);
      const capacity = Number(event.get("capacity") ?? 0);
      if (capacity > 0 && registrationCount >= capacity) {
        throw new HttpsError("resource-exhausted", "EVENT_CAPACITY_REACHED");
      }
      const profile = await transaction.get(database.doc(`users/${actorUid}`));
      const registrationRef = eventRef.collection("registrations").doc(randomUUID());
      transaction.create(registrationRef, {
        eventId,
        organizationId: event.get("organizationId"),
        userId: actorUid,
        displayName: String(profile.get("displayName") ?? profile.get("fullName") ?? "Good4 öğrencisi"),
        status: "registered",
        registeredAt: FieldValue.serverTimestamp(),
        updatedAt: FieldValue.serverTimestamp(),
      });
      transaction.update(eventRef, {
        registrationCount: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
      transaction.create(database.collection("auditLogs").doc(), {
        action: "event.registered",
        actorUid,
        targetType: "eventRegistration",
        targetId: `${eventId}:${registrationRef.id}`,
        metadata: { eventId, organizationId: event.get("organizationId") },
        createdAt: FieldValue.serverTimestamp(),
      });
      return { outcome: "registered" as const, registrationId: registrationRef.id };
    }

    if (!current) return { outcome: "not_registered" as const };
    const checkin = await transaction.get(eventRef.collection("checkins").doc(current.id));
    if (checkin.exists) {
      throw new HttpsError("failed-precondition", "CHECKED_IN_REGISTRATION_LOCKED");
    }
    transaction.delete(current.ref);
    transaction.update(eventRef, {
      registrationCount: FieldValue.increment(-1),
      updatedAt: FieldValue.serverTimestamp(),
    });
    transaction.create(database.collection("auditLogs").doc(), {
      action: "event.unregistered",
      actorUid,
      targetType: "eventRegistration",
      targetId: `${eventId}:${current.id}`,
      metadata: { eventId, organizationId: event.get("organizationId") },
      createdAt: FieldValue.serverTimestamp(),
    });
    return { outcome: "unregistered" as const };
  });
}

export async function recordEventAttendanceService(
  database: Firestore,
  actorUid: string,
  input: { eventId: unknown; registrationId?: unknown; userId?: unknown; undo?: unknown },
): Promise<{ changed: boolean; registrationId: string; userId: string }> {
  const eventId = safeId(input.eventId, "eventId");
  const requestedRegistrationId = typeof input.registrationId === "string" && input.registrationId.trim()
    ? safeId(input.registrationId, "registrationId")
    : "";
  const requestedUserId = typeof input.userId === "string" && input.userId.trim()
    ? safeId(input.userId, "userId")
    : "";
  if (!requestedRegistrationId && !requestedUserId) {
    throw new HttpsError("invalid-argument", "REGISTRATION_ID_OR_USER_ID_REQUIRED");
  }
  const undo = input.undo === true;
  const eventRef = database.doc(`events/${eventId}`);

  return database.runTransaction(async (transaction) => {
    const event = await transaction.get(eventRef);
    if (!event.exists || event.get("status") !== "published") {
      throw new HttpsError("failed-precondition", "EVENT_NOT_OPEN");
    }
    const organizationId = String(event.get("organizationId") ?? "");
    await requireCommunityMembership(database, transaction, actorUid, organizationId);
    let registration: DocumentSnapshot | undefined;
    if (requestedRegistrationId) {
      const snapshot = await transaction.get(eventRef.collection("registrations").doc(requestedRegistrationId));
      if (snapshot.exists) registration = snapshot;
    } else {
      const query = eventRef.collection("registrations").where("userId", "==", requestedUserId).limit(1);
      registration = (await transaction.get(query)).docs[0];
    }
    if (!registration || registration.get("status") !== "registered") {
      throw new HttpsError("not-found", "REGISTRATION_NOT_FOUND");
    }
    const checkinRef = eventRef.collection("checkins").doc(registration.id);
    const checkin = await transaction.get(checkinRef);
    if (undo) {
      if (!checkin.exists) {
        return { changed: false, registrationId: registration.id, userId: String(registration.get("userId")) };
      }
      transaction.delete(checkinRef);
      transaction.update(eventRef, {
        attendanceCount: FieldValue.increment(-1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    } else {
      if (checkin.exists) {
        return { changed: false, registrationId: registration.id, userId: String(registration.get("userId")) };
      }
      transaction.create(checkinRef, {
        eventId,
        organizationId,
        registrationId: registration.id,
        userId: registration.get("userId"),
        checkedInBy: actorUid,
        checkedInAt: FieldValue.serverTimestamp(),
        method: requestedRegistrationId ? "qr" : "manual",
      });
      transaction.update(eventRef, {
        attendanceCount: FieldValue.increment(1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
    transaction.create(database.collection("auditLogs").doc(), {
      action: undo ? "event.checkinUndone" : "event.checkedIn",
      actorUid,
      targetType: "eventCheckin",
      targetId: `${eventId}:${registration.id}`,
      metadata: { eventId, organizationId, method: requestedRegistrationId ? "qr" : "manual" },
      createdAt: FieldValue.serverTimestamp(),
    });
    return { changed: true, registrationId: registration.id, userId: String(registration.get("userId")) };
  });
}

export function eventRegistrationFromSnapshot(snapshot: DocumentSnapshot) {
  return registrationResult(snapshot);
}

import {
  FieldValue,
  type DocumentReference,
  type Firestore,
  type Query,
} from "firebase-admin/firestore";

const DELETED_ACCOUNT_MARKER = "deleted-account";
const DELETE_PAGE_SIZE = 400;

async function deleteMatchingDocuments(database: Firestore, query: Query): Promise<void> {
  while (true) {
    const page = await query.limit(DELETE_PAGE_SIZE).get();
    if (page.empty) return;

    const batch = database.batch();
    page.docs.forEach((document) => batch.delete(document.ref));
    await batch.commit();
  }
}

async function replaceMatchingField(
  database: Firestore,
  query: Query,
  field: string,
  replacement = DELETED_ACCOUNT_MARKER,
): Promise<void> {
  while (true) {
    const page = await query.limit(DELETE_PAGE_SIZE).get();
    if (page.empty) return;

    const batch = database.batch();
    page.docs.forEach((document) => batch.update(document.ref, field, replacement));
    await batch.commit();
  }
}

async function removeFollower(database: Firestore, followerRef: DocumentReference, uid: string) {
  const organizationRef = followerRef.parent.parent;
  await database.runTransaction(async (transaction) => {
    const follower = await transaction.get(followerRef);
    if (!follower.exists || follower.get("userId") !== uid) return;

    const organization = organizationRef ? await transaction.get(organizationRef) : null;
    transaction.delete(followerRef);
    if (organization?.exists) {
      transaction.update(organizationRef!, {
        followerCount: Math.max(0, Number(organization.get("followerCount") ?? 0) - 1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
  });
}

async function removeRegistration(database: Firestore, registrationRef: DocumentReference, uid: string) {
  const eventRef = registrationRef.parent.parent;
  if (!eventRef) {
    await registrationRef.delete();
    return;
  }
  const isV2Event = eventRef.parent.id === "events";
  const attendanceRef = eventRef.collection(isV2Event ? "checkins" : "attendance")
    .doc(isV2Event ? registrationRef.id : uid);

  await database.runTransaction(async (transaction) => {
    const registration = await transaction.get(registrationRef);
    if (!registration.exists || registration.get("userId") !== uid) return;

    const attendance = await transaction.get(attendanceRef);
    const event = isV2Event ? await transaction.get(eventRef) : null;
    transaction.delete(registrationRef);
    if (attendance.exists) transaction.delete(attendanceRef);
    if (event?.exists) {
      transaction.update(eventRef, {
        registrationCount: Math.max(
          0,
          Number(event.get("registrationCount") ?? 0)
            - (registration.get("status") === "registered" ? 1 : 0),
        ),
        attendanceCount: Math.max(
          0,
          Number(event.get("attendanceCount") ?? 0) - (attendance.exists ? 1 : 0),
        ),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
  });
}

async function removeOrphanCheckin(database: Firestore, checkinRef: DocumentReference, uid: string) {
  const eventRef = checkinRef.parent.parent;
  await database.runTransaction(async (transaction) => {
    const checkin = await transaction.get(checkinRef);
    if (!checkin.exists || checkin.get("userId") !== uid) return;

    const event = eventRef ? await transaction.get(eventRef) : null;
    transaction.delete(checkinRef);
    if (event?.exists) {
      transaction.update(eventRef!, {
        attendanceCount: Math.max(0, Number(event.get("attendanceCount") ?? 0) - 1),
        updatedAt: FieldValue.serverTimestamp(),
      });
    }
  });
}

async function removeAccountAuditReferences(database: Firestore, uid: string): Promise<void> {
  await replaceMatchingField(database,
    database.collection("auditLogs").where("actorUid", "==", uid), "actorUid");
  await replaceMatchingField(database,
    database.collection("auditLogs").where("targetId", "==", uid), "targetId");

  while (true) {
    const page = await database.collection("auditLogs")
      .where("metadata.userId", "==", uid)
      .limit(DELETE_PAGE_SIZE)
      .get();
    if (page.empty) return;

    const batch = database.batch();
    page.docs.forEach((document) => {
      const organizationId = document.get("metadata.organizationId");
      const targetType = document.get("targetType");
      batch.update(document.ref, {
        "metadata.userId": DELETED_ACCOUNT_MARKER,
        ...(targetType === "organizationMember" && typeof organizationId === "string"
          ? { targetId: `${organizationId}:${DELETED_ACCOUNT_MARKER}` }
          : {}),
      });
    });
    await batch.commit();
  }
}

async function scrubLegacyCouponData(legacyDatabase: Firestore, uid: string): Promise<void> {
  await deleteMatchingDocuments(legacyDatabase,
    legacyDatabase.collectionGroup("claims").where("userId", "==", uid),
  );

  while (true) {
    const page = await legacyDatabase.collection("community_coupon_codes")
      .where("userId", "==", uid)
      .limit(DELETE_PAGE_SIZE)
      .get();
    if (page.empty) return;

    const batch = legacyDatabase.batch();
    page.docs.forEach((document) => {
      if (document.get("status") === "pending") batch.delete(document.ref);
      else batch.update(document.ref, "userId", DELETED_ACCOUNT_MARKER);
    });
    await batch.commit();
  }
}

async function scrubLegacyCodes(database: Firestore, uid: string): Promise<void> {
  while (true) {
    const page = await database.collection("codes").where("userId", "==", uid)
      .limit(DELETE_PAGE_SIZE).get();
    if (page.empty) return;

    const batch = database.batch();
    page.docs.forEach((document) => {
      if (document.get("status") === "pending") batch.delete(document.ref);
      else batch.update(document.ref, "userId", DELETED_ACCOUNT_MARKER);
    });
    await batch.commit();
  }
}

async function scrubSupporterOrders(database: Firestore, uid: string): Promise<void> {
  while (true) {
    const page = await database.collection("orders").where("supporterId", "==", uid)
      .limit(DELETE_PAGE_SIZE).get();
    if (page.empty) break;

    const batch = database.batch();
    page.docs.forEach((document) => batch.update(document.ref, {
      supporterId: DELETED_ACCOUNT_MARKER,
      supporterName: "Silinmiş kullanıcı",
    }));
    await batch.commit();
  }
  await replaceMatchingField(database, database.collection("orders").where("userId", "==", uid), "userId");
}

async function removeEmailKeyedAccess(database: Firestore, email: string | null): Promise<void> {
  const normalizedEmail = email?.trim();
  if (!normalizedEmail) return;
  const candidates = new Set([normalizedEmail, normalizedEmail.toLowerCase()]);
  await Promise.all([...candidates].map((candidate) =>
    database.doc(`community_access/${candidate}`).delete()));
}

/**
 * Erases or de-identifies the V2 records linked to an account. Shared event,
 * campaign, organization, and audit history is retained with the account UID
 * removed. The caller deletes the Firebase Authentication identity last.
 */
export async function eraseAccountData(
  database: Firestore,
  legacyTestDatabase: Firestore,
  uid: string,
  email: string | null = null,
): Promise<void> {
  const userRef = database.doc(`users/${uid}`);
  const user = await userRef.get();
  if (user.exists) {
    await userRef.update({ status: "deleting", updatedAt: FieldValue.serverTimestamp() });
  }

  const registrations = database.collectionGroup("registrations").where("userId", "==", uid);
  while (true) {
    const page = await registrations.limit(DELETE_PAGE_SIZE).get();
    if (page.empty) break;
    for (const registration of page.docs) {
      await removeRegistration(database, registration.ref, uid);
    }
  }
  const orphanCheckins = database.collectionGroup("checkins").where("userId", "==", uid);
  while (true) {
    const page = await orphanCheckins.limit(DELETE_PAGE_SIZE).get();
    if (page.empty) break;
    for (const checkin of page.docs) await removeOrphanCheckin(database, checkin.ref, uid);
  }

  const followers = database.collectionGroup("followers").where("userId", "==", uid);
  while (true) {
    const page = await followers.limit(DELETE_PAGE_SIZE).get();
    if (page.empty) break;
    for (const follower of page.docs) await removeFollower(database, follower.ref, uid);
  }
  await deleteMatchingDocuments(database,
    database.collectionGroup("members").where("userId", "==", uid),
  );
  await deleteMatchingDocuments(database,
    database.collectionGroup("attendance").where("userId", "==", uid),
  );

  await deleteMatchingDocuments(database, database.collection("campaignClaims").where("studentId", "==", uid));
  await replaceMatchingField(database, database.collection("campaignCodes").where("studentId", "==", uid), "studentId");
  await replaceMatchingField(database, database.collection("redemptions").where("studentId", "==", uid), "studentId");
  await replaceMatchingField(database, database.collection("redemptions").where("redeemedBy", "==", uid), "redeemedBy");
  await deleteMatchingDocuments(database, database.collection("feedbackSubmissions").where("userId", "==", uid));
  await deleteMatchingDocuments(database, database.collection("eduEmailClaims").where("uid", "==", uid));
  await deleteMatchingDocuments(database, database.collection("mail").where("uid", "==", uid));
  await database.doc(`eduVerifications/${uid}`).delete();

  await replaceMatchingField(database, database.collection("events").where("createdBy", "==", uid), "createdBy");
  await replaceMatchingField(database, database.collection("campaigns").where("createdBy", "==", uid), "createdBy");
  await replaceMatchingField(database, database.collection("organizations").where("createdBy", "==", uid), "createdBy");
  await replaceMatchingField(database, database.collectionGroup("checkins").where("checkedInBy", "==", uid), "checkedInBy");
  await replaceMatchingField(database, database.collectionGroup("attendance").where("checkedInBy", "==", uid), "checkedInBy");
  await replaceMatchingField(database, database.collection("businesses").where("ownerId", "==", uid), "ownerId");
  await scrubLegacyCodes(database, uid);
  await scrubSupporterOrders(database, uid);
  await replaceMatchingField(database, database.collection("legacyTestRedemptions").where("legacyStudentId", "==", uid), "legacyStudentId");
  await replaceMatchingField(database, database.collection("legacyTestRedemptions").where("redeemedBy", "==", uid), "redeemedBy");
  await removeAccountAuditReferences(database, uid);
  await scrubLegacyCouponData(legacyTestDatabase, uid);
  await scrubLegacyCouponData(database, uid);
  await Promise.all([
    removeEmailKeyedAccess(database, email),
    removeEmailKeyedAccess(legacyTestDatabase, email),
    legacyTestDatabase.recursiveDelete(legacyTestDatabase.doc(`users/${uid}`)),
  ]);

  await database.recursiveDelete(userRef);
}

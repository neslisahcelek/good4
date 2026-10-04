// Seeds a community-manager demo account into the local Firebase emulators only.
// Usage (from firebase/v2, with the emulators running):
//   FIRESTORE_EMULATOR_HOST=127.0.0.1:8285 FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9199 \
//     node scripts/seed-community-demo.mjs
import { createRequire } from "node:module";

const require = createRequire(new URL("../functions/package.json", import.meta.url));
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, FieldValue, Timestamp } = require("firebase-admin/firestore");

const localHost = /^(127\.0\.0\.1|localhost):\d+$/;
if (!localHost.test(process.env.FIRESTORE_EMULATOR_HOST ?? "") || !localHost.test(process.env.FIREBASE_AUTH_EMULATOR_HOST ?? "")) {
  console.error("Durduruldu: bu betik yalnızca yerel emülatörlerde çalışır.");
  process.exit(1);
}

// Emulator-only demo credentials; never valid on the live project.
export const DEMO_EMAIL = "demo-topluluk@good4.test";
export const DEMO_PASSWORD = "Good4Demo!2026";
const ORGANIZATION_ID = "demo-topluluk";
const UNIVERSITY = "Akdeniz Üniversitesi";

initializeApp({ projectId: "good4tr-v2" });
const auth = getAuth();
const database = getFirestore();

let user;
try {
  user = await auth.getUserByEmail(DEMO_EMAIL);
  await auth.updateUser(user.uid, { password: DEMO_PASSWORD, emailVerified: true });
} catch {
  user = await auth.createUser({
    email: DEMO_EMAIL, password: DEMO_PASSWORD, emailVerified: true, displayName: "Demo Yönetici",
  });
}

const now = FieldValue.serverTimestamp();
const day = 24 * 60 * 60 * 1000;
const event = (offsetDays, hour, fields) => {
  const start = new Date(Date.now() + offsetDays * day);
  start.setUTCHours(hour - 3, 0, 0, 0);
  return {
    organizationId: ORGANIZATION_ID,
    timezone: "Europe/Istanbul",
    startsAt: Timestamp.fromDate(start),
    endsAt: Timestamp.fromMillis(start.getTime() + 2 * 60 * 60 * 1000),
    imageUrl: "",
    registrationCount: 0,
    attendanceCount: 0,
    status: "published",
    createdAt: now,
    createdBy: user.uid,
    updatedAt: now,
    ...fields,
  };
};

const batch = database.batch();
batch.set(database.doc(`organizations/${ORGANIZATION_ID}`), {
  name: "Good4 Demo Topluluğu",
  type: "community",
  university: UNIVERSITY,
  description: "Topluluk yöneticisi arayüzünü denemek için yerel demo topluluğu.",
  status: "active",
  followerCount: 0,
  createdAt: now,
  createdBy: user.uid,
  updatedAt: now,
});
batch.set(database.doc(`users/${user.uid}`), {
  email: DEMO_EMAIL,
  displayName: "Demo Yönetici",
  role: "communityManager",
  status: "active",
  university: UNIVERSITY,
  updatedAt: now,
}, { merge: true });
batch.set(database.doc(`organizations/${ORGANIZATION_ID}/members/${user.uid}`), {
  userId: user.uid,
  role: "manager",
  status: "active",
  assignedBy: user.uid,
  assignedAt: now,
  updatedAt: now,
});
batch.set(database.doc("events/demo-tanisma"), event(3, 18, {
  title: "Tanışma Buluşması",
  description: "Yeni dönemin ilk buluşması: tanışma, sohbet ve dönem planı.",
  location: "Merkezi Kafeterya",
  categoryId: "career-entrepreneurship",
  capacity: 60,
}));
batch.set(database.doc("events/demo-atolye"), event(9, 14, {
  title: "Girişimcilik Atölyesi",
  description: "Fikirden ürüne: kısa sunumlar ve grup çalışması.",
  location: "İİBF Konferans Salonu",
  categoryId: "career-entrepreneurship",
  capacity: 40,
}));
// A few registered students, one already checked in, so the attendee list has something to show.
const students = [
  ["demo-ogrenci-1", "Ayşe Yılmaz", "6f1c2a8e-0b1d-4c3e-9a51-1a2b3c4d5e01"],
  ["demo-ogrenci-2", "Mert Kaya", "6f1c2a8e-0b1d-4c3e-9a51-1a2b3c4d5e02"],
  ["demo-ogrenci-3", "Zeynep Ak", "6f1c2a8e-0b1d-4c3e-9a51-1a2b3c4d5e03"],
  ["demo-ogrenci-4", "Can Demir", "6f1c2a8e-0b1d-4c3e-9a51-1a2b3c4d5e04"],
];
for (const [userId, displayName, registrationId] of students) {
  batch.set(database.doc(`events/demo-tanisma/registrations/${registrationId}`), {
    eventId: "demo-tanisma", organizationId: ORGANIZATION_ID, userId, displayName,
    status: "registered", registeredAt: now, updatedAt: now,
  });
}
batch.set(database.doc(`events/demo-tanisma/checkins/${students[0][2]}`), {
  userId: students[0][0], checkedInBy: user.uid, checkedInAt: now, method: "qr",
});
batch.update(database.doc("events/demo-tanisma"), { registrationCount: students.length, attendanceCount: 1 });
await batch.commit();

// A plain student account to see the student-facing community screens.
const STUDENT_EMAIL = "demo-ogrenci@good4.test";
let student;
try {
  student = await auth.getUserByEmail(STUDENT_EMAIL);
  await auth.updateUser(student.uid, { password: DEMO_PASSWORD, emailVerified: true });
} catch {
  student = await auth.createUser({ email: STUDENT_EMAIL, password: DEMO_PASSWORD, emailVerified: true, displayName: "Demo Öğrenci" });
}
await database.doc(`users/${student.uid}`).set({
  email: STUDENT_EMAIL, displayName: "Demo Öğrenci", role: "student", status: "active",
  university: UNIVERSITY, userAgreementAccepted: true, kvkkNoticeAcknowledged: true, updatedAt: now,
}, { merge: true });

console.log(`Demo topluluk yöneticisi hazır: ${DEMO_EMAIL} (uid ${user.uid})`);

// Loads the fictional demo content (demo-data.mjs) into the local landing-demo emulators.
// Refuses to run against anything but the demo project on loopback ports.
//   node tools/landing-demo/seed.mjs            # image URLs for the iOS Simulator (127.0.0.1)
//   node tools/landing-demo/seed.mjs 10.0.2.2   # image URLs for the Android emulator
import { createHash, randomUUID } from "node:crypto";
import { readFileSync } from "node:fs";
import { createRequire } from "node:module";
import { dirname, join } from "node:path";
import { fileURLToPath } from "node:url";

import {
  BUSINESSES, COMMUNITIES, DEMO_STUDENT, EDU_DOMAIN, EVENTS, LISTINGS, MEALS, SELLERS, UNIVERSITY,
} from "./demo-data.mjs";

const PROJECT = "demo-good4-v2";
const BUCKET = `${PROJECT}.appspot.com`;
const PORTS = { auth: 9399, firestore: 8385, functions: 5305, storage: 9495 };
const imageHost = process.argv[2] ?? "127.0.0.1";

process.env.GCLOUD_PROJECT = PROJECT;
process.env.FIRESTORE_EMULATOR_HOST = `127.0.0.1:${PORTS.firestore}`;
process.env.FIREBASE_AUTH_EMULATOR_HOST = `127.0.0.1:${PORTS.auth}`;
process.env.FIREBASE_STORAGE_EMULATOR_HOST = `127.0.0.1:${PORTS.storage}`;

const here = dirname(fileURLToPath(import.meta.url));
const require = createRequire(join(here, "../../firebase/v2/functions/package.json"));
const { initializeApp } = require("firebase-admin/app");
const { getAuth } = require("firebase-admin/auth");
const { getFirestore, Timestamp, FieldValue } = require("firebase-admin/firestore");
const { getStorage } = require("firebase-admin/storage");

initializeApp({ projectId: PROJECT, storageBucket: BUCKET });
const db = getFirestore();
const auth = getAuth();
const bucket = getStorage().bucket();

const HOUR = 60 * 60 * 1000;
const now = Date.now();
const ts = (millis) => Timestamp.fromMillis(millis);
const istanbulDate = (millis) => new Date(millis + 3 * HOUR).toISOString().slice(0, 10);

/** Midnight in Istanbul `day` days from today, plus "HH:MM". */
function istanbulTime(day, hhmm) {
  const [h, m] = hhmm.split(":").map(Number);
  const midnight = Date.parse(`${istanbulDate(now)}T00:00:00+03:00`);
  return midnight + day * 24 * HOUR + (h * 60 + m) * 60 * 1000;
}

async function clearEmulators() {
  const base = `http://127.0.0.1`;
  await fetch(`${base}:${PORTS.firestore}/emulator/v1/projects/${PROJECT}/databases/(default)/documents`, { method: "DELETE" });
  await fetch(`${base}:${PORTS.auth}/emulator/v1/projects/${PROJECT}/accounts`, { method: "DELETE" });
  await bucket.deleteFiles({ force: true }).catch(() => undefined);
}

async function upload(name) {
  const token = randomUUID();
  const path = `demo/${name}.png`;
  await bucket.file(path).save(readFileSync(join(here, "assets", `${name}.png`)), {
    contentType: "image/png",
    metadata: { metadata: { firebaseStorageDownloadTokens: token } },
  });
  return `http://${imageHost}:${PORTS.storage}/v0/b/${BUCKET}/o/${encodeURIComponent(path)}?alt=media&token=${token}`;
}

async function callFunction(name, idToken, data) {
  const response = await fetch(`http://127.0.0.1:${PORTS.functions}/${PROJECT}/europe-west1/${name}`, {
    method: "POST",
    headers: { "Content-Type": "application/json", Authorization: `Bearer ${idToken}` },
    body: JSON.stringify({ data }),
  });
  const body = await response.json();
  if (body.error) throw new Error(`${name}: ${body.error.message}`);
  return body.result;
}

async function signIn(email, password) {
  const response = await fetch(
    `http://127.0.0.1:${PORTS.auth}/identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=demo`,
    { method: "POST", headers: { "Content-Type": "application/json" }, body: JSON.stringify({ email, password, returnSecureToken: true }) },
  );
  const body = await response.json();
  if (!body.idToken) throw new Error(`sign-in failed for ${email}: ${JSON.stringify(body)}`);
  return body.idToken;
}

async function verifyCampusEmail(uid, eduEmail) {
  const claim = `eduEmailClaims/${createHash("sha256").update(eduEmail).digest("hex")}`;
  await db.doc(claim).set({ uid, verifiedAt: FieldValue.serverTimestamp() });
  await db.doc(`users/${uid}`).set({
    eduEmail, eduVerified: true, eduVerifiedAt: FieldValue.serverTimestamp(), updatedAt: FieldValue.serverTimestamp(),
  }, { merge: true });
}

async function seedStudent() {
  const user = await auth.createUser({
    uid: "demo-elif", email: DEMO_STUDENT.email, password: DEMO_STUDENT.password,
    displayName: DEMO_STUDENT.displayName, emailVerified: true,
  });
  // Same profile path the app uses on first sign-in, so every required field is present.
  await callFunction("ensureStudentProfile", await signIn(DEMO_STUDENT.email, DEMO_STUDENT.password), {
    displayName: DEMO_STUDENT.displayName, university: UNIVERSITY,
    userAgreementAccepted: true, kvkkNoticeAcknowledged: true,
  });
  await verifyCampusEmail(user.uid, DEMO_STUDENT.eduEmail);
  return user.uid;
}

async function seedSellers() {
  const uids = {};
  for (const seller of SELLERS) {
    const uid = `demo-${seller.key}`;
    await auth.createUser({ uid, email: seller.email, displayName: seller.displayName, emailVerified: true });
    await db.doc(`users/${uid}`).set({
      email: seller.email, displayName: seller.displayName, role: "student", status: "active",
      university: UNIVERSITY, createdAt: ts(now - 90 * 24 * HOUR), updatedAt: ts(now),
    });
    await verifyCampusEmail(uid, seller.email.replace("@demo.edu.tr", "@ogr.akdeniz.edu.tr"));
    uids[seller.key] = uid;
  }
  return uids;
}

async function seedCommunities(studentUid) {
  for (const c of COMMUNITIES) {
    const id = `demo-${c.id}`;
    await db.doc(`organizations/${id}`).set({
      name: c.name, type: "community", status: "active", university: UNIVERSITY, description: c.description,
      logoUrl: await upload(`logo-${c.id}`), coverUrl: await upload(`cover-${c.id}`),
      followerCount: c.followers + (c.followed ? 1 : 0),
      createdAt: ts(now - 200 * 24 * HOUR), createdBy: "demo-admin", updatedAt: ts(now),
    });
    if (c.followed) {
      await db.doc(`organizations/${id}/followers/${studentUid}`).set({ userId: studentUid, followedAt: ts(now - 20 * 24 * HOUR) });
    }
  }
  for (const e of EVENTS) {
    const startsAt = istanbulTime(e.day, e.start);
    await db.doc(`events/demo-${e.id}`).set({
      organizationId: `demo-${e.community}`, title: e.title, description: e.description,
      startsAt: ts(startsAt), endsAt: ts(startsAt + e.hours * HOUR), timezone: "Europe/Istanbul",
      location: e.location, capacity: e.capacity, registrationCount: e.registered, attendanceCount: 0,
      status: "published", categoryId: e.categoryId, imageUrl: await upload(`event-${e.id}`),
      createdAt: ts(now - 7 * 24 * HOUR), createdBy: "demo-admin", updatedAt: ts(now),
    });
  }
}

async function seedSuspendedMeals() {
  for (const b of BUSINESSES) {
    await db.doc(`organizations/demo-${b.id}`).set({
      name: b.name, type: "business", status: "active", university: UNIVERSITY,
      createdAt: ts(now - 120 * 24 * HOUR), createdBy: "demo-admin", updatedAt: ts(now),
    });
  }
  for (const m of MEALS) {
    await db.doc(`campaigns/demo-${m.id}`).set({
      organizationId: `demo-${m.business}`, title: m.title, description: m.description,
      startsAt: ts(now - 24 * HOUR), endsAt: ts(istanbulTime(m.days, "23:00")), status: "published",
      totalLimit: m.total, redemptionCount: m.used,
      createdAt: ts(now - 24 * HOUR), createdBy: "demo-admin", updatedAt: ts(now),
    });
  }
}

function searchTokens(title) {
  const fold = { ç: "c", ğ: "g", ı: "i", ö: "o", ş: "s", ü: "u", â: "a", î: "i", û: "u" };
  const words = title.toLocaleLowerCase("tr-TR").replace(/[çğıöşüâîû]/g, (l) => fold[l] ?? l)
    .normalize("NFKD").replace(/[̀-ͯ]/g, "").split(/[^a-z0-9]+/).filter((w) => w.length >= 2);
  const tokens = new Set();
  for (const w of words) for (let n = 2; n <= Math.min(w.length, 15); n += 1) tokens.add(w.slice(0, n));
  return [...tokens];
}

async function seedCloset(studentUid, sellerUids) {
  await db.doc("app_config/campus_closet").set({ enabled: true });
  await db.doc(`marketUserState/${studentUid}`).set({ termsVersion: 1, unreadCount: 0 });
  for (const l of LISTINGS) {
    const seller = SELLERS.find((s) => s.key === l.seller);
    const photo = await upload(`item-${l.id}`);
    const publishedAt = now - l.hoursAgo * HOUR;
    await db.doc(`marketListings/demo-${l.id}`).set({
      sellerUid: sellerUids[l.seller], sellerName: seller.displayName, eduDomain: EDU_DOMAIN, universityName: UNIVERSITY,
      category: l.category, condition: l.condition, title: l.title, description: l.description, price: l.price,
      searchTokens: searchTokens(l.title), photos: [{ url: photo, thumbUrl: photo }],
      status: "published", moderationFlags: [], rejectReason: null, renewCount: 0,
      createdAt: ts(publishedAt - HOUR), updatedAt: ts(publishedAt), publishedAt: ts(publishedAt),
      expiresAt: ts(publishedAt + 30 * 24 * HOUR),
    });
  }
}

async function seedHome() {
  await db.doc("app_config/campus_weather").set({
    temperature: 24, label: "Parçalı bulutlu", source: "MET Norway", updatedAtMillis: now,
  });
  const kyk = [
    { breakfast: ["Kaşarlı omlet", "Domates, salatalık", "Beyaz peynir", "Siyah zeytin", "Bal, tereyağı", "Çay"],
      dinner: ["Mercimek çorbası", "Tavuk sote", "Bulgur pilavı", "Cacık"] },
    { breakfast: ["Haşlanmış yumurta", "Kaşar peyniri", "Yeşil zeytin", "Domates", "Reçel", "Çay"],
      dinner: ["Ezogelin çorbası", "Etli kuru fasulye", "Pirinç pilavı", "Turşu"] },
  ];
  for (let day = 0; day < 7; day += 1) {
    await db.doc(`kyk_menu_days/${istanbulDate(now + day * 24 * HOUR)}`).set(kyk[day % 2]);
  }
  const names = ["Pazar", "Pazartesi", "Salı", "Çarşamba", "Perşembe", "Cuma", "Cumartesi"];
  const meals = [
    ["Yayla çorbası", "Izgara köfte", "Şehriyeli pilav", "Mevsim salata", "Sütlaç"],
    ["Domates çorbası", "Fırın tavuk", "Bulgur pilavı", "Yoğurt", "Elma"],
    ["Mercimek çorbası", "Etli nohut", "Pirinç pilavı", "Turşu", "Revani"],
    ["Ezogelin çorbası", "Karnıyarık", "Makarna", "Ayran", "Portakal"],
    ["Tarhana çorbası", "Tavuk şiş", "Sebzeli bulgur", "Cacık", "Kemalpaşa"],
  ];
  const weekdays = [...Array(7).keys()].map((offset) => now + offset * 24 * HOUR)
    .filter((t) => ![0, 6].includes(new Date(t + 3 * HOUR).getUTCDay())).slice(0, 5);
  await db.doc("app_config/akdeniz_dining_menu").set({
    weekLabel: "Bu hafta", weekStart: istanbulDate(weekdays[0]), weekEnd: istanbulDate(weekdays.at(-1)),
    days: weekdays.map((t, i) => ({
      date: istanbulDate(t), dayName: names[new Date(t + 3 * HOUR).getUTCDay()], meals: meals[i], calories: 950 + i * 35,
    })),
  });
}

await clearEmulators();
const studentUid = await seedStudent();
const sellerUids = await seedSellers();
await seedCommunities(studentUid);
await seedSuspendedMeals();
await seedCloset(studentUid, sellerUids);
await seedHome();
console.log(`Demo data loaded (images on ${imageHost}). Sign in as ${DEMO_STUDENT.email} (password in demo-data.mjs).`);

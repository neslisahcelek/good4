import { createRequire } from 'node:module';
const require = createRequire(new URL('../functions/package.json', import.meta.url));
const { initializeApp } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, Timestamp } = require('firebase-admin/firestore');
for (const key of ['FIRESTORE_EMULATOR_HOST', 'FIREBASE_AUTH_EMULATOR_HOST']) {
  if (!/^(127\.0\.0\.1|localhost):\d+$/.test(process.env[key] ?? '')) throw new Error(`${key} must point to a local emulator`);
}
const app = initializeApp({ projectId: 'demo-good4-v2' });
const auth = getAuth(app), db = getFirestore(app);
for (const [uid, role] of [['local-student', 'student'], ['local-manager', 'communityManager'], ['local-admin', 'good4Admin']]) {
  const email = `${uid}@akdeniz.edu.tr`;
  try { await auth.createUser({ uid, email, emailVerified: true, password: 'LocalTest123!', displayName: uid }); }
  catch (error) { if (error.code !== 'auth/uid-already-exists' && error.code !== 'auth/email-already-exists') throw error; }
  await db.doc(`users/${uid}`).set({ email, displayName: uid, fullName: uid, role, status: 'active', university: 'Akdeniz Üniversitesi', mobileNotifications: false });
}
await db.doc('organizations/local-community').set({ name: 'Yerel test topluluğu', type: 'community', status: 'active', university: 'Akdeniz Üniversitesi', followerCount: 0 });
await db.doc('organizations/local-community/members/local-manager').set({ userId: 'local-manager', role: 'manager', status: 'active' });
const large = process.argv.includes('--large');
for (let i = 0; i < (large ? 125 : 3); i++) {
  await db.doc(`events/local-event-${String(i).padStart(3, '0')}`).set({ organizationId: 'local-community', title: `Test etkinliği ${i + 1}`, description: 'Sentetik test verisi',
    startsAt: Timestamp.fromMillis(Date.now() + (i + 1) * 86400000), timezone: 'Europe/Istanbul', location: 'Test kampüsü', status: 'published', registrationCount: 0, attendanceCount: 0, imageUrl: '' });
}
if (large) {
  const batch = db.batch();
  for (let i = 0; i < 125; i++) batch.set(db.doc(`events/local-event-000/registrations/participant-${String(i).padStart(3, '0')}`), {
    userId: `synthetic-${i}`, displayName: `Test katılımcısı ${i + 1}`, status: 'registered', registeredAt: Timestamp.now(),
  });
  batch.update(db.doc('events/local-event-000'), { registrationCount: 125 }); await batch.commit();
}
console.log('Seeded demo-good4-v2 only. Accounts: local-student/local-manager/local-admin @akdeniz.edu.tr; password: LocalTest123!');
await db.terminate();

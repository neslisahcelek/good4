// Yerel örnek veri: Sosyal Etkinlikler. YALNIZCA emülatör (demo-good4-v2) içindir.
//
//   npm --prefix functions run build
//   npm run emulators:local            # ayrı bir terminalde
//   npm run seed:local                 # temel hesaplar
//   FIRESTORE_EMULATOR_HOST=127.0.0.1:8285 FIREBASE_AUTH_EMULATOR_HOST=127.0.0.1:9199 node scripts/seed-social-local.mjs
//
// Giriş: denizdemo@akdeniz.edu.tr / LocalTest123!  (kuralları kabul etmiş)
//        yenidemo@akdeniz.edu.tr / LocalTest123!   (kuralları henüz kabul etmemiş: ilk kullanım ekranı)
for (const key of ['FIRESTORE_EMULATOR_HOST', 'FIREBASE_AUTH_EMULATOR_HOST']) {
  if (!/^(127\.0\.0\.1|localhost):\d+$/.test(process.env[key] ?? '')) throw new Error(`${key} must point to a local emulator`);
}
process.env.GCLOUD_PROJECT = 'demo-good4-v2';
const lib = new URL('../functions/lib/', import.meta.url);
const { db } = await import(new URL('firebase.js', lib));
const social = await import(new URL('social.js', lib));
const { createRequire } = await import('node:module');
const { getAuth } = createRequire(new URL('../functions/package.json', import.meta.url))('firebase-admin/auth');

const deps = { notify: async () => undefined };
// Gerçek Firebase Auth UID'leri alfasayısaldır; sosyal fonksiyonlar yalnızca bunları kabul eder.
const students = {
  denizdemo: 'Deniz Aydın', sayse: 'Ayşe Yılmaz', smehmet: 'Mehmet Kaya',
  szeynep: 'Zeynep Kara', semre: 'Emre Demir', selif: 'Elif Şahin',
};

for (const collection of ['socialActivities', 'socialConversations', 'socialUserState', 'socialReports']) {
  await db.recursiveDelete(db.collection(collection));
}
await db.doc('app_config/social_activities').set({ enabled: true });

async function account(uid, name, termsAccepted) {
  try {
    await getAuth().createUser({ uid, email: `${uid}@akdeniz.edu.tr`, emailVerified: true, password: 'LocalTest123!', displayName: name });
  } catch (error) {
    if (error.code !== 'auth/uid-already-exists' && error.code !== 'auth/email-already-exists') throw error;
  }
  await db.doc(`users/${uid}`).set({
    email: `${uid}@akdeniz.edu.tr`, displayName: name, fullName: name, role: 'student', status: 'active',
    university: 'Akdeniz Üniversitesi', mobileNotifications: false,
    eduEmail: `${uid}@ogr.akdeniz.edu.tr`, eduVerified: true,
  }, { merge: true });
  if (termsAccepted) await db.doc(`socialUserState/${uid}`).set({ termsVersion: social.SOCIAL_TERMS_VERSION }, { merge: true });
}
for (const [uid, name] of Object.entries(students)) await account(uid, name, true);
await account('yenidemo', 'Ayşe Yılmaz', false);

/** Yerel saatle: bugünden `days` gün sonra hh:mm. */
function at(days, hour, minute = 0) {
  const date = new Date();
  date.setDate(date.getDate() + days);
  date.setHours(hour, minute, 0, 0);
  return date.toISOString();
}

const open = async (uid, input) => (await social.createSocialActivityService(db, uid, input, deps)).activityId;
const join = (uid, activityId, note) => social.requestToJoinSocialActivityService(db, uid, { activityId, note }, deps);
const accept = (organizer, activityId, requesterUid) =>
  social.respondToSocialRequestService(db, organizer, { activityId, requesterUid, accept: true }, deps);

await open('sayse', { kind: 'social', type: 'coffee', title: 'Ders sonrası kahve', note: 'Biraz sohbet edip kafa dağıtalım.', startsAt: at(1, 15), capacity: 3 });
const basket = await open('smehmet', { kind: 'sport', type: 'basketball', title: "3'e 3 basket", note: 'İki kişi daha lazım.', startsAt: at(1, 18, 30), level: 'intermediate', capacity: 2 });
await join('semre', basket, 'Ben varım.'); await accept('smehmet', basket, 'semre');
await open('szeynep', { kind: 'sport', type: 'cycling', title: 'Konyaaltı sahil turu', note: 'Sakin tempo, sahil boyunca gidip dönüyoruz.', startsAt: at(1, 8), level: 'intermediate', capacity: 4 });
const games = await open('selif', { kind: 'social', type: 'board-games', game: 'okey', title: 'Okey akşamı', note: 'Dört kişi olunca başlıyoruz.', startsAt: at(1, 20), capacity: 5 });
const tennis = await open('sayse', { kind: 'sport', type: 'tennis', title: 'Akşam tenisi', note: 'Keyifli bir maç olsun.', startsAt: at(2, 18), level: 'beginner', capacity: 1 });
await open('smehmet', { kind: 'social', type: 'language-exchange', title: 'İngilizce pratik', note: 'B1 seviyesindeyim, konuşma pratiği yapalım.', startsAt: at(3, 15), capacity: 3 });
await open('szeynep', { kind: 'social', type: 'movie', title: 'Film gecesi', startsAt: at(4, 20, 30), capacity: 4 });
await open('selif', { kind: 'sport', type: 'sup-kayak', title: 'Gün batımında SUP', note: 'Ekipmanı sahilden kiralıyoruz.', startsAt: at(5, 17, 30), level: 'any', capacity: 3 });
await open('semre', { kind: 'sport', type: 'running', title: 'Sabah koşusu', startsAt: at(6, 7), level: 'any', capacity: 5 });

// Deniz'in kendi etkinliği: biri kabul edilmiş, biri bekliyor.
const football = await open('denizdemo', { kind: 'sport', type: 'football', title: 'Halı saha maçı', note: 'Kaleci arıyoruz, 5e 5 oynayacağız.', startsAt: at(2, 19), level: 'any', capacity: 4 });
await join('szeynep', football, 'Kaleciyim, gelirim.'); await accept('denizdemo', football, 'szeynep');
await join('semre', football, 'Haftada iki oynuyorum.');

// Deniz okey akşamına kabul edilmiş (sohbet açık), tenis için yanıt bekliyor.
await join('denizdemo', games, 'Okeyi çok severim.');
const { conversationId } = await accept('selif', games, 'denizdemo');
await social.sendSocialMessageService(db, 'selif', { conversationId, text: 'Merhaba! Buluşma yerini birlikte belirleyelim mi?' }, deps);
await join('denizdemo', tennis, 'Yeni başladım, birlikte oynayalım.');

console.log('Sosyal etkinlik örnek verisi yüklendi (demo-good4-v2).');
await db.terminate();
process.exit(0);

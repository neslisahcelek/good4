import { readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, doc, setDoc, getDoc, getDocs, query, where, limit, updateDoc, serverTimestamp } from 'firebase/firestore';
test('second-phase rules reject unbounded lists while preserving individual reads', async () => {
  const environment = await initializeTestEnvironment({ projectId: 'demo-good4-cost-rules', firestore: { host: '127.0.0.1', port: 8285, rules: await readFile(new URL('./firestore.bounded.rules', import.meta.url), 'utf8') } });
  try {
    await environment.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, 'users/student'), { role: 'student', status: 'active' });
      await setDoc(doc(db, 'events/one'), { status: 'published', organizationId: 'community' });
      await setDoc(doc(db, 'communityCoupons/published'), { status: 'published' });
      await setDoc(doc(db, 'communityCoupons/pending'), { status: 'pending' });
      await setDoc(doc(db, 'communityCouponCodes/business_1234'), { studentId: 'student' });
    });
    const db = environment.authenticatedContext('student').firestore();
    await assertSucceeds(getDoc(doc(db, 'events/one')));
    await assertSucceeds(getDocs(query(collection(db, 'events'), where('status', '==', 'published'), limit(50))));
    await assertFails(getDocs(query(collection(db, 'events'), where('status', '==', 'published'))));
    await assertFails(getDocs(query(collection(db, 'events'), where('status', '==', 'published'), limit(101))));
    await assertSucceeds(getDoc(doc(db, 'communityCoupons/published')));
    await assertSucceeds(getDocs(query(collection(db, 'communityCoupons'), where('status', '==', 'published'), limit(100))));
    await assertFails(getDocs(query(collection(db, 'communityCoupons'), where('status', '==', 'published'))));
    await assertFails(getDocs(query(collection(db, 'communityCoupons'), where('status', '==', 'published'), limit(101))));
    await assertFails(getDoc(doc(db, 'communityCoupons/pending')));
    await assertFails(getDoc(doc(db, 'communityCouponCodes/business_1234')));
  } finally { await environment.cleanup(); }
});


test('bounded rules keep campus test grant and proof fields server-only', async () => {
  const environment = await initializeTestEnvironment({ projectId: 'demo-good4-cost-rules', firestore: {
    host: '127.0.0.1', port: 8285, rules: await readFile(new URL('./firestore.bounded.rules', import.meta.url), 'utf8') } });
  try {
    for (const role of ['student', 'good4Admin']) {
      const uid = `campus-test-${role}`;
      await environment.withSecurityRulesDisabled(async (context) => {
        await setDoc(doc(context.firestore(), `users/${uid}`), { role, status: 'active', displayName: 'Test', updatedAt: 1 });
      });
      const db = environment.authenticatedContext(uid).firestore();
      for (const data of [
        { adminEduTest: { enabled: true, email: 'self@example.com', grantId: 'forged' } },
        { adminEduTestVerification: { email: 'self@example.com', grantId: 'forged', verifiedAt: serverTimestamp() } },
        { adminEduTestGeneration: 0 },
      ]) await assertFails(updateDoc(doc(db, `users/${uid}`), data));
      await assertFails(setDoc(doc(db, 'app_config/admin_edu_test'), { uid }));
    }
  } finally { await environment.cleanup(); }
});

import { readFile } from 'node:fs/promises';
import { test } from 'node:test';
import { initializeTestEnvironment, assertFails, assertSucceeds } from '@firebase/rules-unit-testing';
import { collection, doc, setDoc, getDoc, getDocs, query, where, limit } from 'firebase/firestore';
test('second-phase rules reject unbounded lists while preserving individual reads', async () => {
  const environment = await initializeTestEnvironment({ projectId: 'demo-good4-cost-rules', firestore: { host: '127.0.0.1', port: 8285, rules: await readFile(new URL('./firestore.bounded.rules', import.meta.url), 'utf8') } });
  try {
    await environment.withSecurityRulesDisabled(async (context) => {
      const db = context.firestore();
      await setDoc(doc(db, 'users/student'), { role: 'student', status: 'active' });
      await setDoc(doc(db, 'events/one'), { status: 'published', organizationId: 'community' });
    });
    const db = environment.authenticatedContext('student').firestore();
    await assertSucceeds(getDoc(doc(db, 'events/one')));
    await assertSucceeds(getDocs(query(collection(db, 'events'), where('status', '==', 'published'), limit(50))));
    await assertFails(getDocs(query(collection(db, 'events'), where('status', '==', 'published'))));
    await assertFails(getDocs(query(collection(db, 'events'), where('status', '==', 'published'), limit(101))));
  } finally { await environment.cleanup(); }
});

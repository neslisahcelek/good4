// Uses operator Application Default Credentials, never shipped in the mobile app.
import { initializeApp, applicationDefault } from 'firebase-admin/app';
import { getFirestore, FieldValue } from 'firebase-admin/firestore';
import { getAuth } from 'firebase-admin/auth';
import { readFile } from 'node:fs/promises';

const [command, project, ...args] = process.argv.slice(2);
if (!project || !['create', 'assign', 'revoke', 'approve', 'unpublish'].includes(command)) {
  throw new Error('Usage: node manage.mjs <create|assign|revoke|approve|unpublish> <project-id> <arguments>. See README.md.');
}
initializeApp({ credential: applicationDefault(), projectId: project });
const db = getFirestore();
const cleanId = value => { if (!value || !/^[a-zA-Z0-9_-]+$/.test(value)) throw new Error('Invalid document ID'); return value; };
const id = cleanId(args[0]);
if (command === 'create') {
  const data = JSON.parse(await readFile(args[1], 'utf8'));
  if (!data.name?.trim() || data.name.length > 120 || (data.description || '').length > 1000) throw new Error('Invalid community profile');
  if (!data.university?.trim() || data.university.length > 160) throw new Error('Invalid university');
  await db.doc(`communities/${id}`).create({ name: data.name.trim(), university: data.university.trim(), description: data.description || '', logoUrl: data.logoUrl || '', coverUrl: data.coverUrl || '' });
} else if (command === 'assign' || command === 'revoke') {
  const email = args[1]?.trim().toLowerCase();
  if (!email || !/^[^/\s@]+@[^/\s@]+\.[^/\s@]+$/.test(email)) throw new Error('Invalid email');
  if (!(await db.doc(`communities/${id}`).get()).exists) throw new Error('Community does not exist');
  if (command === 'assign') {
    let user;
    try { user = await getAuth().getUserByEmail(email); }
    catch (e) { if (e.code !== 'auth/user-not-found') throw e; user = await getAuth().createUser({ email }); }
    // Pre-create the existing Good4 profile; Google must still verify ownership.
    // Do not overwrite an existing student's profile, role or credit.
    const ref = db.doc(`users/${user.uid}`);
    await db.runTransaction(async tx => {
      if (!(await tx.get(ref)).exists) tx.create(ref, { email, fullName: user.displayName || '', role: 'student', verified: true, credit: 0, weeklyCreditOverride: 0, createdAt: Date.now() });
    });
    await db.doc(`community_access/${email}`).set({ active: true, communityIds: FieldValue.arrayUnion(id) }, { merge: true });
  } else {
    await db.doc(`community_access/${email}`).update({ communityIds: FieldValue.arrayRemove(id) });
  }
} else {
  const entryId = cleanId(args[1]);
  const ref = db.doc(`communities/${id}/entries/${entryId}`);
  await db.runTransaction(async tx => {
    const snapshot = await tx.get(ref);
    if (!snapshot.exists) throw new Error('Entry not found');
    if (command === 'approve' && (snapshot.data().kind !== 'coupon' || snapshot.data().status !== 'pending')) throw new Error('Only pending coupons can be approved');
    tx.update(ref, { status: command === 'approve' ? 'published' : 'cancelled' });
  });
}
console.log('Completed:', command, 'project:', project, 'community:', id);

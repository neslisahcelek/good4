// Explicit operator action; preview by default. Never imported by deployed functions.
import { randomUUID } from 'node:crypto';
import { createRequire } from 'node:module';
const require = createRequire(new URL('../functions/package.json', import.meta.url));
const { initializeApp, applicationDefault } = require('firebase-admin/app');
const { getAuth } = require('firebase-admin/auth');
const { getFirestore, FieldValue } = require('firebase-admin/firestore');
const args = process.argv.slice(2);
const argument = (name) => args[args.indexOf(name) + 1];
const project = argument('--project');
const uid = argument('--uid');
const action = argument('--action');
if (!args.includes('--project') || !args.includes('--uid') || !args.includes('--action')
  || !['good4tr-v2', 'good4tr-test', 'demo-good4-v2'].includes(project)
  || !/^[A-Za-z0-9_-]{1,128}$/.test(uid ?? '') || !['grant', 'revoke'].includes(action)) {
  throw new Error('Usage: node scripts/manage-campus-test.mjs --project PROJECT --uid UID --action grant|revoke [--apply]');
}
if ((process.env.FIRESTORE_EMULATOR_HOST || process.env.FIREBASE_AUTH_EMULATOR_HOST) && project !== 'demo-good4-v2') {
  throw new Error('Emulator connections require demo-good4-v2');
}
if (project === 'demo-good4-v2' && (!process.env.FIRESTORE_EMULATOR_HOST || !process.env.FIREBASE_AUTH_EMULATOR_HOST)) {
  throw new Error('Both Auth and Firestore emulators are required for demo-good4-v2');
}
const app = initializeApp({ projectId: project, ...(project === 'demo-good4-v2' ? {} : { credential: applicationDefault() }) });
const db = getFirestore(app);
try {
  const identity = await getAuth(app).getUser(uid);
  const email = identity.email?.trim().toLowerCase();
  const userRef = db.doc(`users/${uid}`);
  const configRef = db.doc('app_config/admin_edu_test');
  const user = await userRef.get();
  if (action === 'grant' && (identity.disabled || !identity.emailVerified || !email
      || user.get('role') !== 'good4Admin' || user.get('status') !== 'active')) {
    throw new Error('Grant requires an active good4Admin with a verified Firebase Auth email');
  }
  const existing = await configRef.get();
  console.log(JSON.stringify({ project, uid, action, apply: args.includes('--apply'),
    replacesAnotherAccount: action === 'grant' && !!existing.get('uid') && existing.get('uid') !== uid }));
  if (args.includes('--apply')) {
    const grantId = randomUUID();
    await db.runTransaction(async (tx) => {
      const [current, config] = await Promise.all([tx.get(userRef), tx.get(configRef)]);
      const previousUid = config.get('uid');
      const previousRef = typeof previousUid === 'string' && previousUid !== uid ? db.doc(`users/${previousUid}`) : null;
      const previous = previousRef ? await tx.get(previousRef) : null;
      if (action === 'grant' && (current.get('role') !== 'good4Admin' || current.get('status') !== 'active')) {
        throw new Error('Target account is no longer an active admin');
      }
      if (!current.exists) throw new Error('User profile does not exist');
      const clear = { adminEduTest: FieldValue.delete(), adminEduTestVerification: FieldValue.delete(), updatedAt: FieldValue.serverTimestamp() };
      if (action === 'grant' && previous?.exists) tx.update(previous.ref, clear);
      tx.update(userRef, action === 'grant' ? {
        adminEduTest: { enabled: true, email, grantId, grantedAt: FieldValue.serverTimestamp() },
        adminEduTestVerification: FieldValue.delete(), updatedAt: FieldValue.serverTimestamp(),
      } : clear);
      if (action === 'grant') tx.set(configRef, { uid, updatedAt: FieldValue.serverTimestamp() });
      else if (previousUid === uid) tx.delete(configRef);
      tx.create(db.collection('auditLogs').doc(), { action: `campusTest.${action}`, actorUid: 'operator', targetType: 'user', targetId: uid,
        metadata: { replacedAnotherAccount: action === 'grant' && typeof previousUid === 'string' && previousUid !== uid }, createdAt: FieldValue.serverTimestamp() });
    });
    console.log('Applied. A grant does not complete verification. Existing quotas remain unchanged.');
  }
} finally { await db.terminate(); }

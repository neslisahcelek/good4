// Dry run by default. Writes are permanently restricted to good4tr-test.
import { createRequire } from 'node:module';
import { mkdir, writeFile } from 'node:fs/promises';
import { fileURLToPath } from 'node:url';
import { resolve } from 'node:path';
import { buildTestSchemaPlan } from './test-schema-model.mjs';

const args = process.argv.slice(2);
if (args.some(arg => arg !== '--apply') || args.length > 1) throw new Error('Usage: node scripts/align-test-schema.mjs [--apply]');
const apply = args.includes('--apply');
const testProject = 'good4tr-test';
const productionProject = 'good4tr-v2';
const root = fileURLToPath(new URL('../', import.meta.url));
const require = createRequire(resolve(root, 'package.json'));
const auth = require('firebase-tools/lib/auth.js');
const account = auth.getProjectDefaultAccount(root);
if (!account) throw new Error('Run firebase login first.');
const { access_token: token } = await auth.getAccessToken(account.tokens.refresh_token, []);
const headers = { Authorization: `Bearer ${token}`, 'Content-Type': 'application/json' };
const base = project => `https://firestore.googleapis.com/v1/projects/${project}/databases/(default)`;

async function api(url, body, { missing = false } = {}) {
  const response = await fetch(url, {
    headers, ...(body ? { method: 'POST', body: JSON.stringify(body) } : {}), signal: AbortSignal.timeout(45_000),
  });
  if (missing && response.status === 404) return {};
  const result = await response.json();
  if (!response.ok) throw new Error(`${response.status}: ${result.error?.message ?? 'Google API request failed'}`);
  return result;
}

function decode(value) {
  if ('timestampValue' in value) return new Date(value.timestampValue);
  if ('integerValue' in value) return Number(value.integerValue);
  if ('arrayValue' in value) return (value.arrayValue.values ?? []).map(decode);
  if ('mapValue' in value) return Object.fromEntries(Object.entries(value.mapValue.fields ?? {}).map(([k, v]) => [k, decode(v)]));
  if ('nullValue' in value) return null;
  if ('stringValue' in value) return value.stringValue;
  if ('booleanValue' in value) return value.booleanValue;
  if ('doubleValue' in value) return value.doubleValue;
  // Refuse unhandled values instead of silently changing their type during migration.
  throw new Error(`Unsupported Firestore value type: ${Object.keys(value)[0]}`);
}

function encode(value) {
  if (value instanceof Date) return { timestampValue: value.toISOString() };
  if (value === null) return { nullValue: null };
  if (typeof value === 'string') return { stringValue: value };
  if (typeof value === 'boolean') return { booleanValue: value };
  if (typeof value === 'number') return Number.isInteger(value) ? { integerValue: String(value) } : { doubleValue: value };
  if (Array.isArray(value)) return { arrayValue: { values: value.map(encode) } };
  if (value && typeof value === 'object') return { mapValue: { fields: fields(value) } };
  throw new Error(`Cannot encode ${typeof value}`);
}
const fields = data => Object.fromEntries(Object.entries(data).map(([key, value]) => [key, encode(value)]));

async function list(project, collection) {
  const result = [];
  let pageToken;
  do {
    const url = new URL(`${base(project)}/documents/${collection}`);
    url.searchParams.set('pageSize', '1000');
    if (pageToken) url.searchParams.set('pageToken', pageToken);
    const page = await api(url, null, { missing: true });
    result.push(...(page.documents ?? []));
    pageToken = page.nextPageToken;
  } while (pageToken);
  return result;
}
const snapshots = [];
for (const collection of ['users', 'businesses', 'communities', 'community_access', 'campaigns', 'legacyCampaigns', 'organizations', 'events', 'legacyTestBusinessLinks', 'app_config', 'academic_calendar_events', 'kyk_menu_days']) {
  snapshots.push(...await list(testProject, collection));
}
for (const document of [...snapshots]) {
  const path = document.name.split('/documents/')[1];
  const collection = path.split('/')[0];
  const subcollections = collection === 'communities' ? ['entries', 'followers']
    : collection === 'organizations' ? ['members', 'followers']
      : collection === 'events' ? ['registrations', 'checkins'] : [];
  for (const sub of subcollections) snapshots.push(...await list(testProject, `${path}/${sub}`));
}
for (const entry of snapshots.filter(d => d.name.includes('/communities/') && d.name.includes('/entries/'))) {
  const path = entry.name.split('/documents/')[1];
  for (const sub of ['registrations', 'attendance']) snapshots.push(...await list(testProject, `${path}/${sub}`));
}
const documents = snapshots.map(d => ({ path: d.name.split('/documents/')[1], updateTime: d.updateTime, data: decode({ mapValue: { fields: d.fields ?? {} } }) }));
const identities = [];
let nextPageToken;
do {
  const url = new URL(`https://identitytoolkit.googleapis.com/v1/projects/${testProject}/accounts:batchGet`);
  url.searchParams.set('maxResults', '1000');
  if (nextPageToken) url.searchParams.set('nextPageToken', nextPageToken);
  const page = await api(url);
  // Do not retain or print password hashes, salts or OAuth tokens from Auth responses.
  identities.push(...(page.users ?? []).map(({ localId, email, displayName, createdAt, emailVerified, disabled }) => ({ localId, email, displayName, createdAt, emailVerified, disabled })));
  nextPageToken = page.nextPageToken;
} while (nextPageToken);
const plan = buildTestSchemaPlan(documents, identities);

// Only shared public campus content is seeded. Production identities, roles,
// memberships, codes, attendance, feedback and consent are never copied.
const paths = new Set(documents.map(d => d.path));
for (const collection of ['academic_calendar_events', 'kyk_menu_days', 'app_config']) {
  for (const source of await list(productionProject, collection)) {
    const path = source.name.split('/documents/')[1];
    if (paths.has(path)) continue;
    const data = decode({ mapValue: { fields: source.fields ?? {} } });
    if ('updatedBy' in data) data.updatedBy = 'test-schema-seed';
    plan.writes.push({ path, data, create: true });
  }
}
const counts = {};
for (const write of plan.writes) {
  const collection = write.path.split('/')[0];
  counts[collection] = (counts[collection] ?? 0) + 1;
}
console.log(JSON.stringify({ mode: apply ? 'apply' : 'dry-run', target: testProject, writes: counts, archivedAdvertisingCampaigns: plan.archives.length, warnings: plan.warnings }, null, 2));
if (!apply) process.exit(0);

// Do not break the legacy app by changing records before V2 callables are ready.
const functions = await api(`https://cloudfunctions.googleapis.com/v2/projects/${testProject}/locations/europe-west1/functions?pageSize=1000`);
const deployed = new Set((functions.functions ?? []).filter(f => f.state === 'ACTIVE').map(f => f.name.split('/').at(-1)));
const required = ['ensureStudentProfile', 'recordLegalAcknowledgements', 'setEventRegistration', 'recordEventAttendance', 'setCommunityFollowing', 'getFollowingCommunityIds', 'getPortalContext', 'getCommunityPortalDashboard', 'submitFeedback', 'deleteMyAccount'];
if (required.some(name => !deployed.has(name))) throw new Error(`Deploy test V2 functions first. Missing: ${required.filter(name => !deployed.has(name)).join(', ')}`);

const backupPath = resolve(root, '../../output/firebase-test-schema', new Date().toISOString().replaceAll(':', '-'));
await mkdir(backupPath, { recursive: true, mode: 0o700 });
await writeFile(resolve(backupPath, 'before.json'), JSON.stringify({ project: testProject, documents: snapshots }), { mode: 0o600 });
await writeFile(resolve(backupPath, 'plan.json'), JSON.stringify(plan), { mode: 0o600 });
console.log(`Backup saved: ${backupPath}`);
const archivePaths = new Set(plan.archives.map(item => item.archivePath));
const writes = plan.writes.filter(w => !archivePaths.has(w.path)).map(w => ({
  update: { name: `projects/${testProject}/databases/(default)/documents/${w.path}`, fields: fields(w.data) },
  currentDocument: w.create ? { exists: false } : { updateTime: w.updateTime },
}));
for (const archive of plan.archives) {
  const create = plan.writes.find(w => w.path === archive.archivePath);
  // Archive and remove the incompatible original in the same atomic commit.
  writes.push([
    ...(create ? [{ update: { name: `projects/${testProject}/databases/(default)/documents/${create.path}`, fields: fields(create.data) }, currentDocument: { exists: false } }] : []),
    { delete: `projects/${testProject}/databases/(default)/documents/${archive.path}`, currentDocument: { updateTime: archive.updateTime } },
  ]);
}
let batch = [];
let applied = 0;
const commit = async () => {
  if (!batch.length) return;
  await api(`${base(testProject)}/documents:commit`, { writes: batch });
  applied += batch.length;
  console.log(`Applied ${applied} writes to ${testProject}.`);
  batch = [];
};
for (const write of writes) {
  const group = Array.isArray(write) ? write : [write];
  if (batch.length + group.length > 400) await commit();
  batch.push(...group);
}
await commit();
console.log('Test schema migration complete. Legacy collections and Auth accounts were retained.');

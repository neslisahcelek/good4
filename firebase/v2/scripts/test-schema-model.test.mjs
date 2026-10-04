import assert from 'node:assert/strict';
import { test } from 'node:test';
import { buildTestSchemaPlan, canonicalUser, timestamp } from './test-schema-model.mjs';

const now = new Date('2026-10-02T12:00:00Z');
const doc = (path, data) => ({ path, data, updateTime: '2026-10-01T00:00:00Z' });

test('legacy dates become timestamps and user history and real consent are retained', () => {
  for (const input of [1700000000, 1700000000000, { seconds: 1700000000 }, { _seconds: 1700000000 }]) {
    assert.equal(timestamp(input, now).toISOString(), '2023-11-14T22:13:20.000Z');
  }
  const user = canonicalUser({ role: 'student', email: 'TEST@example.com', fullName: 'Test', credit: 4 }, now);
  assert.equal(user.email, 'test@example.com');
  assert.equal(user.displayName, 'Test');
  assert.equal(user.credit, 4);
  assert.equal(user.status, 'active');
  assert.ok(user.createdAt instanceof Date);
  assert.equal(user.legalAcknowledgements, undefined);
  const existing = { userAgreement: { version: 'real-consent' } };
  assert.equal(canonicalUser({ ...user, legalAcknowledgements: existing }, now).legalAcknowledgements, existing);
});

test('unknown and disabled accounts do not receive active or administrative permissions', () => {
  assert.equal(canonicalUser({ role: 'unknown', email: 'u@example.com' }, now).status, 'disabled');
  assert.equal(canonicalUser({ role: 'constructor', email: 'u@example.com' }, now).role, 'student');
  assert.equal(canonicalUser({ role: 'student', status: 'unexpected', email: 'u@example.com' }, now).status, 'disabled');
  assert.equal(canonicalUser({ role: 'supporter', email: 'u@example.com' }, now).role, 'student');
  assert.equal(canonicalUser({ role: 'admin', email: 'u@example.com' }, now, { disabled: true }).status, 'disabled');
  const source = [doc('communities/c', { name: 'Community' }), doc('community_access/u@example.com', { active: true, communityIds: ['c'] })];
  const plan = buildTestSchemaPlan(source, [{ localId: 'u', email: 'u@example.com', emailVerified: false }], now);
  assert.equal(plan.writes.some(w => w.path.includes('/members/')), false);
});

test('verified legacy manager access becomes UID membership and deterministic event relationships', () => {
  const source = [
    doc('communities/c', { name: 'Community' }),
    doc('community_access/u@example.com', { active: true, communityIds: ['c'] }),
    doc('communities/c/entries/e', { kind: 'event', date: '2026-10-03', time: '14:00', title: 'Event' }),
    doc('communities/c/entries/e/registrations/student', { userId: 'student', registeredAt: 1700000000 }),
    doc('communities/c/entries/e/attendance/student', { userId: 'student', method: 'qr', checkedInBy: 'u' }),
  ];
  const identities = [{ localId: 'u', email: 'u@example.com', emailVerified: true }];
  const plan = buildTestSchemaPlan(source, identities, now);
  const event = plan.writes.find(w => w.path.startsWith('events/') && w.path.split('/').length === 2);
  const registration = plan.writes.find(w => w.path.includes('/registrations/'));
  const checkin = plan.writes.find(w => w.path.includes('/checkins/'));
  assert.equal(event.data.startsAt.toISOString(), '2026-10-03T11:00:00.000Z');
  assert.equal(event.data.registrationCount, 1);
  assert.equal(event.data.attendanceCount, 1);
  assert.equal(registration.data.eventId, event.path.split('/')[1]);
  assert.equal(checkin.data.registrationId, registration.path.split('/').at(-1));
  assert.equal(plan.writes.find(w => w.path === 'users/u').data.role, 'communityManager');
  assert.equal(buildTestSchemaPlan(source, identities, now).writes.find(w => w.path === event.path).path, event.path);
  const migrated = [...source, ...plan.writes.map(w => doc(w.path, w.data))];
  assert.equal(buildTestSchemaPlan(migrated, identities, now).writes.length, 0);
});

test('incompatible advertising campaigns are archived and canonical campaigns are preserved', () => {
  const source = [doc('campaigns/advert', { image: 'https://example.com/image.png' }), doc('campaigns/canonical', { organizationId: 'b' })];
  const plan = buildTestSchemaPlan(source, [], now);
  assert.deepEqual(plan.archives.map(a => a.path), ['campaigns/advert']);
  assert.equal(plan.writes.find(w => w.path === 'legacyCampaigns/advert').data.image, source[0].data.image);
  assert.equal(plan.writes.some(w => w.path === 'campaigns/canonical'), false);
  assert.throws(() => buildTestSchemaPlan([...source, doc('legacyCampaigns/advert', { image: 'different' })], [], now), /refusing to overwrite/);
});

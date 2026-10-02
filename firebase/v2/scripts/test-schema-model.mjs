import { createHash } from 'node:crypto';

const roles = new Set(['student', 'businessOwner', 'businessStaff', 'communityManager', 'communityStaff', 'good4Admin']);
const legacyRoles = { student: 'student', supporter: 'student', business: 'businessOwner', admin: 'good4Admin' };
const marker = 'test-v2-schema-2026-10-02';
const text = value => typeof value === 'string' ? value : '';
const id = path => path.split('/').at(-1);
const stableId = path => createHash('sha256').update(path).digest('hex').slice(0, 32);

export function timestamp(value, fallback) {
  if (value instanceof Date && !Number.isNaN(value.getTime())) return value;
  if (value && typeof value === 'object') {
    const seconds = value.seconds ?? value._seconds;
    if (Number.isFinite(Number(seconds))) return new Date(Number(seconds) * 1000);
  }
  if (typeof value === 'number' && Number.isFinite(value)) {
    return new Date(value < 1e12 ? value * 1000 : value);
  }
  return fallback;
}

export function canonicalUser(user, now, identity) {
  const knownRole = roles.has(user.role) || Object.hasOwn(legacyRoles, user.role);
  const role = roles.has(user.role) ? user.role : Object.hasOwn(legacyRoles, user.role) ? legacyRoles[user.role] : 'student';
  const email = text(user.email || identity?.email).trim().toLowerCase();
  const requestedStatus = user.status ?? 'active';
  const status = ['active', 'disabled', 'pendingEmailVerification'].includes(requestedStatus) ? requestedStatus : 'disabled';
  return {
    ...user,
    email,
    displayName: text(user.displayName || user.fullName || identity?.displayName || email.split('@')[0]).slice(0, 120),
    role,
    status: identity?.disabled || !knownRole || !email ? 'disabled' : status,
    university: text(user.university),
    createdAt: timestamp(user.createdAt ?? user.registrationDate, timestamp(Number(identity?.createdAt), now)),
    updatedAt: timestamp(user.updatedAt, now),
    schemaMigration: marker,
  };
}

/** Add canonical records without touching legacy event/code history or inventing consent. */
export function buildTestSchemaPlan(documents, identities = [], now = new Date()) {
  const existing = new Map(documents.map(d => [d.path, d]));
  const writes = new Map();
  const warnings = [];
  const authByUid = new Map(identities.map(u => [u.localId, u]));
  const authByEmail = new Map(identities.filter(u => u.emailVerified && !u.disabled).map(u => [u.email?.toLowerCase(), u]));
  const inCollection = path => documents.filter(d => d.path.startsWith(`${path}/`) && d.path.split('/').length === path.split('/').length + 1);
  const put = (path, data, { replace = false } = {}) => {
    if (!replace && existing.has(path)) return;
    writes.set(path, { path, data, ...(existing.has(path) ? { updateTime: existing.get(path).updateTime } : { create: true }) });
  };
  const users = new Map();
  for (const doc of inCollection('users')) {
    const user = canonicalUser(doc.data, now, authByUid.get(id(doc.path)));
    users.set(id(doc.path), user);
    if (doc.data.schemaMigration !== marker) put(doc.path, user, { replace: true });
  }
  const actorUid = identities.find(identity => users.get(identity.localId)?.role === 'good4Admin' && !identity.disabled)?.localId
    ?? [...users].find(([, user]) => user.role === 'good4Admin')?.[0] ?? marker;
  const member = (organizationId, uid, role) => put(`organizations/${organizationId}/members/${uid}`, {
    userId: uid, role, status: users.get(uid)?.status === 'active' ? 'active' : 'disabled',
    assignedBy: actorUid, assignedAt: now, updatedAt: now,
  });
  for (const business of inCollection('businesses')) {
    if (!text(business.data.name).trim()) {
      warnings.push('An empty legacy business remains in its original collection.');
      continue;
    }
    const organizationId = `legacy-business-${id(business.path)}`;
    put(`organizations/${organizationId}`, {
      ...business.data, type: 'business', status: 'active', university: '', followerCount: 0,
      createdAt: now, createdBy: actorUid, updatedAt: now,
      legacyTestBusinessId: id(business.path),
    });
    put(`legacyTestBusinessLinks/${id(business.path)}`, { organizationId, createdBy: actorUid, createdAt: now });
    const ownerId = text(business.data.ownerId);
    if (ownerId && ['businessOwner', 'good4Admin'].includes(users.get(ownerId)?.role)) member(organizationId, ownerId, 'owner');
  }
  for (const community of inCollection('communities')) {
    const organizationId = `legacy-community-${id(community.path)}`;
    const followers = inCollection(`${community.path}/followers`);
    put(`organizations/${organizationId}`, {
      ...community.data, type: 'community', status: 'active', followerCount: followers.length,
      createdAt: now, createdBy: actorUid, updatedAt: now,
      legacyTestCommunityId: id(community.path),
    });
    for (const follower of followers) {
      put(`organizations/${organizationId}/followers/${id(follower.path)}`, {
        userId: text(follower.data.userId) || id(follower.path), followedAt: timestamp(follower.data.followedAt, now),
      });
    }
    for (const access of inCollection('community_access')) {
      if (!access.data.active || !access.data.communityIds?.includes(id(community.path))) continue;
      const identity = authByEmail.get(id(access.path).toLowerCase());
      if (!identity) {
        warnings.push('A community access record has no verified Auth identity; no manager privilege was granted.');
        continue;
      }
      const user = users.get(identity.localId);
      if (user && !['student', 'communityManager', 'good4Admin'].includes(user.role)) {
        warnings.push('A conflicting community manager role was preserved.');
        continue;
      }
      const profile = canonicalUser({ ...(user ?? {}), role: user?.role === 'good4Admin' ? 'good4Admin' : 'communityManager', status: user?.status ?? 'active' }, now, identity);
      users.set(identity.localId, profile);
      if (existing.get(`users/${identity.localId}`)?.data.role !== profile.role || !user) {
        put(`users/${identity.localId}`, profile, { replace: true });
      }
      member(organizationId, identity.localId, 'manager');
    }
    for (const entry of inCollection(`${community.path}/entries`)) {
      if (entry.data.kind !== 'event') continue;
      const eventId = `legacy-${stableId(entry.path)}`;
      const startsAt = new Date(`${entry.data.date}T${entry.data.time || '00:00'}:00+03:00`);
      if (Number.isNaN(startsAt.getTime())) {
        warnings.push('A legacy event with an invalid date remains in its original collection.');
        continue;
      }
      const registrations = inCollection(`${entry.path}/registrations`);
      const attendance = inCollection(`${entry.path}/attendance`);
      let attendanceCount = 0;
      for (const registration of registrations) {
        const registrationId = stableId(registration.path);
        const userId = text(registration.data.userId) || id(registration.path);
        put(`events/${eventId}/registrations/${registrationId}`, {
          eventId, organizationId, userId, displayName: text(registration.data.displayName),
          status: 'registered', registeredAt: timestamp(registration.data.registeredAt, now), updatedAt: now,
        });
        const arrived = attendance.find(a => (a.data.userId || id(a.path)) === userId);
        if (arrived) {
          attendanceCount++;
          put(`events/${eventId}/checkins/${registrationId}`, {
            eventId, organizationId, registrationId, userId, checkedInBy: text(arrived.data.checkedInBy),
            checkedInAt: timestamp(arrived.data.checkedInAt, now), method: arrived.data.method === 'manual' ? 'manual' : 'qr',
          });
        }
      }
      put(`events/${eventId}`, {
        organizationId, title: text(entry.data.title), description: text(entry.data.description),
        startsAt, endsAt: new Date(startsAt.getTime() + 2 * 60 * 60 * 1000), timezone: 'Europe/Istanbul',
        location: text(entry.data.location), imageUrl: text(entry.data.imageUrl), capacity: Number(entry.data.capacity) || 0,
        registrationCount: registrations.length, attendanceCount, status: entry.data.status === 'cancelled' ? 'cancelled' : 'published',
        createdAt: now, createdBy: actorUid, updatedAt: now,
        ...(entry.data.categoryId ? { categoryId: entry.data.categoryId } : {}),
        legacyTestEntryPath: entry.path,
      });
    }
  }
  const archives = inCollection('campaigns').filter(doc => !doc.data.organizationId && Object.keys(doc.data).every(key => key === 'image')).map(doc => {
    // Legacy campaigns contain only an advertising image and cannot be converted to meal campaigns.
    const archivePath = `legacyCampaigns/${id(doc.path)}`;
    if (existing.has(archivePath) && JSON.stringify(existing.get(archivePath).data) !== JSON.stringify(doc.data)) {
      throw new Error('An existing legacy campaign archive differs; refusing to overwrite it.');
    }
    put(archivePath, doc.data);
    return { path: doc.path, updateTime: doc.updateTime, archivePath };
  });
  return { writes: [...writes.values()], archives, warnings: [...new Set(warnings)] };
}

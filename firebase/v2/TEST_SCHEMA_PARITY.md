# Test schema transition status — 2 October 2026

Production is `good4tr-v2`; the separate test project is `good4tr-test`.
The legacy production project `good4tr` is outside this change.

## Live audit

| Area | Production | Test before transition |
| --- | --- | --- |
| Users | 111 V2 profiles; `status`, `displayName`, Timestamp dates | 782 legacy profiles; missing status/displayName; mixed date types |
| Organizations/events | `organizations`, canonical `events` contract | `businesses`, `communities/{id}/entries` |
| Rules | V2, critical mutations restricted to callables | Legacy client mutations |
| Composite indexes | 3 V2 indexes | 17 legacy indexes |
| Functions | 37 active functions | None; Cloud Functions API initially disabled |
| Billing | Enabled | Disabled |
| Auth | Google and Apple enabled | Google enabled; Apple initially missing |

## Completed

- Android/iOS test clients select the V2 model and their own Firebase callable project.
- Test and production use the same e-mail verification policy and feature gates.
- The backend resolves the correct test Storage bucket; the Firebase CLI has an explicit `test` alias.
- The existing test Android application's debug and local release SHA-1/SHA-256 certificates are registered; its SDK JSON was refreshed from Firebase, with the previous local file backed up under ignored `output/firebase-test-schema/config-before/`.
- The native Apple Auth provider is enabled in test.
- The dry-run-first migration is implemented in `scripts/align-test-schema.mjs`. It requires active test callables before writing, saves a local backup, uses update-time/create preconditions, preserves legacy history and actual consent, and archives the three image-only legacy campaign documents atomically.

The live dry run proposed 782 user writes, 38 organization/member/follower
writes, 17 legacy business link writes, 7 event/registration/check-in writes,
3 advertising archives, and missing shared public campus content (383 academic
calendar events, 35 KYK menu days, 2 configuration documents). No production
identity, membership, consent, feedback, code or attendance data is copied.
One empty legacy business is retained without creating a canonical organization.
A conflicting business/community manager role is preserved rather than replaced.

## Remaining external prerequisite

The owner approved linking the existing production billing account to test.
Google Cloud rejected the billing update with `403: The caller does not have
permission`. A test-only Functions deployment was attempted and stopped at
the Blaze requirement. **Live Firestore documents, rules and indexes have not
been migrated.** The updated test mobile build should be installed after the
backend transition is completed.

An account with permission to assign that billing account must enable billing
for [the test project](https://console.firebase.google.com/project/good4tr-test/usage/details).
Then follow the deployment/migration sequence in `mobile/README.md` and verify
active functions, ready indexes, matching rule contracts and a no-op dry run.

## Validation

- 4 migration-model tests passed, including role safety, history/consent retention, registration/check-in relationships and rerun idempotency.
- 32 staging Android unit tests passed.
- Production Android and iOS simulator Kotlin compilation passed.
- Firestore rule emulator tests and 97 backend tests passed; the web production build passed.
- Refreshed staging Firebase configuration processed successfully and staging Android Kotlin compilation passed.

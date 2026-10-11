---
name: good4-firebase-guardrails
description: Apply Good4 Firebase cost, abuse, data-access, storage, and operational guardrails whenever inspecting, designing, changing, or deploying Firebase-backed behavior in this repository.
---

# Good4 Firebase guardrails

Load this skill for every Good4 task that touches Firebase Auth, Firestore, Storage, Cloud Functions, App Check, Firebase configuration, rules, indexes, scheduled jobs, or Firebase deployment. Apply it together with `good4-architecture` for KMP or application architecture changes and `good4-firebase-release` for requested backend deployments.

## Before changing Firebase behavior

1. Inspect the actual Firebase project/configuration and the narrow code path being changed. Distinguish local source state from deployed production state; do not infer that a local rule or Function is live.
2. Trace the complete read/write path across client, callable/trigger, repository, Firestore or Storage rules, and relevant platform implementations.
3. Estimate the cost path: reads/writes/deletes per user action, listeners and refresh frequency, page sizes, response/document bytes, Storage uploads/downloads and retention, Function memory/runtime/concurrency/retries, scheduled frequency, and any paid external API/OCR calls.
4. Record the security boundary: authenticated identity, active-account and role/ownership checks, App Check status, client rules, server Admin SDK authorization, input size/type limits, and abuse/rate controls.

## Implementation decisions

- Prefer bounded queries with stable cursor pagination. Avoid full-collection/group reads when a page, count, aggregate, or maintained summary can meet the product need. Treat account deletion and cleanup as potentially large jobs: make them resumable and page through results.
- Avoid polling and duplicate fetches. Use lifecycle-aware listeners where real-time updates are needed; otherwise fetch on entry/refresh, cancel stale requests, and consider account-scoped caching with explicit invalidation.
- If a home screen prefetches a destination feed, share the in-flight request with that screen and reuse a fresh result. Bound the page and prefetch interval, including failed attempts; do not fetch every category speculatively. Scope caches to the account and filter, and invalidate them after relevant writes or campus-verification changes.
- Do not add a summary callable to every filter change when the feed already supplies the screen status. If the badge still needs an independent summary, throttle and deduplicate it separately, and prevent a late summary from overwriting newer profile/terms state.
- For scheduled or fan-out work, justify its cadence, bound per-run/per-user work, make retries idempotent, and define retention for job, receipt, log, and media records. Ensure pause/budget controls cover the intended optional work and state clearly what they do not stop.
- Protect every Admin SDK callable/trigger independently of client rules. Derive UID from verified auth; check active account, role and ownership; validate input sizes; add rate/quota controls to costly user-callable paths. App Check complements authentication and does not replace authorization or quotas.
- Keep Firestore and Storage access rules aligned with shipped clients. For list queries, require bounded limits where compatible with all supported clients. For uploads, enforce path ownership/admin scope, content type, byte and image-dimension limits; set cache headers intentionally and clean up replaced/orphaned files.
- Prefer counters/summary documents for repeated broad counts, but update them transactionally and reconcile if required. Avoid introducing high-write hot spots or redundant indexes without considering their write/storage cost.
- Keep platform Firebase SDKs platform-scoped in KMP, register iOS Firestore serializers for DTOs actually decoded there, and preserve project/flavor separation.

## Delivery, cleanup, and pagination

- For external delivery, distinguish a definite rejection before acceptance from timeout, transport, or server failure after possible acceptance. Refund delivery quota only when non-delivery is established. Keep a separate, non-refundable attempt limit shared by callable aliases; failed delivery must not make provider requests and Firestore work unbounded.
- When an ID can be reused (for example a short business coupon code), cleanup must re-read the current owner in the deletion transaction. Trace both ownership and actor UID fields, including account-scoped quota keys. Remove account links without resetting shared recipient/global quotas that prevent abuse.
- A query limit needs a usable continuation contract. Trace cursor order and all ordering values through the UI, including a boundary document being approved, removed, or reused. Keep lookup lists independently bounded; do not reload every business on each coupon page or label a bounded sample as a global total.
- Onboarding without a Firestore profile needs a current Firebase Auth account check. Reject disabled applicants at submission and missing/disabled targets at approval; do not treat default callable token checks or emulator success as proof of the production disabled-account boundary.

See [11 Ekim: kupon, başvuru ve teslimat sözleşmeleri](../../../docs/REPEATED_REVIEW_PATTERNS.md#11-ekim-kupon-başvuru-ve-teslimat-sözleşmeleri) when reviewing these paths.

## Live operations and reporting

- Use emulators and local tests for behavior checks when practical. Choose only targeted checks relevant to the request; do not add or run tests unless requested by the user or needed to validate the requested change.
- Live inspection is read-only by default. Deployments, billing-budget changes, IAM/App Check/rules changes, data migrations, and destructive cleanup require explicit user authorization for that action and the correct project/environment.
- Before an authorized live change, inspect current state and scope the smallest change. Never treat budget alerts as a hard billing cap. Report access failures and unverified deployment state as unknown; do not claim a safeguard is active based only on source code.
- In reviews, rank findings by severity and include concrete source/config evidence, triggering conditions, cost/security impact, confidence, and an actionable remediation. Separate confirmed code behavior from live usage or billing amounts that could not be verified.

## Good4 references

- Firebase V2 Functions: `firebase/v2/functions/src/`
- Project wiring and deployments: `firebase/v2/firebase.json`, `firebase/v2/firebase.bounded.json`
- Firestore and Storage rules: `firebase/v2/firestore.rules`, `firebase/v2/firestore.bounded.rules`, `firebase/v2/storage.rules`
- Data model: `firebase/v2/CANONICAL_DATA_MODEL.md`
- Cost-control and release state: `firebase/v2/COST_TESTING.md`
- KMP Firebase platform bindings: `composeApp/src/androidMain/`, `composeApp/src/iosMain/`

Read a reference only when it applies, and verify dated operational notes against current project state before relying on them.

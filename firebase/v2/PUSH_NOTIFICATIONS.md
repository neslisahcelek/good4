# Good4 push notifications

FCM delivery is **off by default**. Only `NOTIFICATIONS_ENABLED=true` enables
new event jobs, reminder scheduling and admin announcements. Missing/false is
safe, and workers cancel queued jobs while disabled. Keep the switch off until
both mobile releases and the APNs configuration have passed acceptance tests.
This first release is explicitly limited to `good4tr-v2`; the concurrent V2 test-environment migration does not automatically enable test push.

## Configuration and rollout

1. Enable the Cloud Tasks API on `good4tr-v2`. Deploy Firestore rules/indexes and
   the notification functions together with the updated event/account services.
   Functions retain the existing `europe-west1` region. Use a Firebase functions
   dotenv file with `NOTIFICATIONS_ENABLED=false` for the initial deployment.
2. In Apple Developer, enable Push Notifications for `com.good4.iosApp`, update
   the provisioning profile, and upload the APNs authentication key plus Key ID
   and Team ID to that iOS app's Firebase Cloud Messaging configuration. Never
   commit the APNs `.p8` key. The shared Xcode configurations select development
   vs production APNs entitlements; alerts do not depend on silent background
   execution. Android uses the existing production application `com.good4`.
3. Confirm the runtime service account can enqueue Cloud Tasks and the task
   service account can invoke `deliverNotification`. The Firebase task function
   config creates a queue with concurrency one and bounded retries.
4. Publish the mobile updates. Validate permissions and notification clicks on
   physical Android/iOS devices with controlled accounts. There is no Analytics
   or Dynamic Links integration.
5. Set `NOTIFICATIONS_ENABLED=true` and redeploy functions. Use an admin's own
   signed-in mobile account and the panel's test button first, then a controlled
   community. The test bypasses the announcement category toggle, but never
   bypasses OS permission or device-specific community blocks.
6. Inspect Cloud Logging, Cloud Tasks failed/retried tasks and the admin history.
   `accepted` means FCM accepted the request; it is not a delivery or read receipt.
   Set the switch false and redeploy to stop creation and sending. Failed queue
   tasks are retried up to five times within a 24-hour job lifetime; exhausted
   tasks require operator attention.

## Data and behavior

- `pushDevices/{installationId}`: owner UID, hashed random installation secret,
  platform, optional FCM registration token, current OS permission and device
  community blocks. The secret allows the same installation to change accounts;
  unrelated accounts cannot claim it. Endpoint documents are server-only.
  Mobile refreshes unchanged registrations once per day; account, token,
  permission and community-block changes update them immediately.
- `users/{uid}/notificationPreferences/default`: event updates/reminders default
  on, announcements default off. These preferences control phone notifications;
  the authenticated inbox remains available without OS permission.
- `users/{uid}/notifications/{jobId}`: numeric epoch-second `createdAt`/`readAt`
  values, type, text and event/community target. Opening the inbox does not mark
  it read. The mobile list shows the latest 100 entries. Blocked communities are
  filtered on the device, preserving existing device-local block semantics.
- `notificationJobs/{jobId}` and private delivery/device receipts: transactional
  outbox, persisted page cursor and accepted/invalid endpoint counters. Pages
  contain at most 50 candidate accounts. Successful endpoint receipts are reused
  on retry. FCM and Firestore cannot form one transaction: a crash after FCM
  accepts but before its receipt is stored can repeat a phone alert; stable
  Android tags/APNs collapse IDs reduce this window. Inbox IDs remain unique.
- `notificationDispatches/{dispatchId}` creation queues each page; updates to
  job counters do not enqueue another task. Page reservations are idempotent,
  with limits of 1,000 recipients per job and 2,000 per project per UTC day.
  Only one non-test bulk announcement is admitted per UTC day. Jobs exceeding
  quotas wait explicitly; administrators can resume eligible waiting jobs.
  Budget/manual pause also stops optional distribution. Local emulators and
  the cloud test project never dispatch production jobs.
- Published event creation/draft publication notifies followers. Only start
  time or location changes notify registered participants. Count, text and image
  edits do not broadcast. Cancellation notifies current participants and opens
  the stored notice without broadening cancelled-event Firestore permissions.
- Reminder enrollment occurs on post-rollout event saves only. A five-minute scheduler
  queues due events, then rechecks publication, start, registration and check-in
  at delivery. Rescheduling recalculates the deadline; old-start jobs are skipped.
  The reminder delay depends on scheduler/queue load and delivery is best effort.
- Authentication changes clear UI state. Logout serializes with registration,
  removes the device server-side and invalidates its endpoint where possible.
  If both fail, logout returns an error rather than leaving an account attached.
  Account deletion removes device records, inbox/preferences and private delivery
  receipts, and anonymizes the retained sender history.

See [COST_TESTING.md](COST_TESTING.md) for local test accounts, billing/IAM
blockers and the staged production rollout. Technical completed job records
are cleaned after 30 days with bounded batches; user inbox and audit history
are retained.

## Validation commands

Firebase emulator tests require JDK 21+ (the installed Android Studio JBR works):

```sh
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
PATH='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin':"$PATH" \
npm run test:functions
JAVA_HOME='/Applications/Android Studio.app/Contents/jbr/Contents/Home' \
PATH='/Applications/Android Studio.app/Contents/jbr/Contents/Home/bin':"$PATH" \
npm run test:rules
npm --prefix web test
npm --prefix web run build
```

From the repository root:

```sh
./gradlew :composeApp:assembleProdDebug :composeApp:testProdDebugUnitTest \
  :composeApp:compileStagingDebugKotlinAndroid :composeApp:compileKotlinIosSimulatorArm64
xcodebuild -quiet -project iosApp/iosApp.xcodeproj -scheme 'iosApp Prod' \
  -configuration Release -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' CODE_SIGNING_ALLOWED=NO build
```

Physical-device acceptance: allow/deny and settings changes; foreground,
background and terminated clicks; cancelled/unavailable target; unread/read-all
across two devices; token renewal, logout/account switch; first follow/register
permission education; general/community/test announcements; category opt-out;
blocked communities; unregister, reschedule and check-in before reminders.

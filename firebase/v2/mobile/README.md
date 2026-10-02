# Good4 mobile environments

For local acceptance testing, **Android `stagingDebug` and iOS `iosApp Test` / Debug now default to `demo-good4-v2` emulators**. They do not fall back to the cloud when emulators are unavailable. Follow [the local test and rollout handoff](../COST_TESTING.md). The table below describes the cloud registrations; the debug emulator override takes precedence. Android `prodDebug` still connects to production.

There are two supported environments: production (`good4tr-v2`) and test
(`good4tr-test`). Android debug/release build types control debugging, signing,
and optimization; the product flavor selects the Firebase project. On iOS the
shared scheme selects the environment through its build configuration.

`good4tr-v2` is the current production project. The `V2` name in the backend
code identifies its data model and callable functions, not another mobile app
or a test environment. Keep the project, functions, hosting URLs, and backend
code under `firebase/v2`.

## Supported applications

| Platform / environment | Build variant or scheme | Application identifier | Firebase project | Firebase app ID |
| --- | --- | --- | --- | --- |
| Android production | `prodDebug`, `prodRelease` | `com.good4` | `good4tr-v2` | `1:654697131931:android:b75fdd099636ce13603b3e` |
| Android test | `stagingDebug`, `stagingRelease` | `com.good4.test` | `good4tr-test` | `1:449563145023:android:fae75aa992ae4c5b19d98a` |
| iOS production | `iosApp Prod` / `Release` | `com.good4.iosApp` | `good4tr-v2` | `1:654697131931:ios:c7e28c28e33731d1603b3e` |
| iOS test | `iosApp Test` / `Debug` | `com.good4.iosApp.test` | `good4tr-test` | `1:449563145023:ios:dff46396477949a419d98a` |

Both environments use the V2 data model, role checks, and sign-in flow.
Cloud test builds read `good4tr-test` and call that project's `europe-west1`
functions. Local debug test builds use the emulator override described above;
`prodDebug` uses production data even though it is a debug build. Test Auth
identities and data remain separate from production.

### Provisioning the test backend

The live audit on 2 October 2026 found legacy records/rules/indexes in
`good4tr-test`, no Cloud Functions, and billing disabled. The new client
configuration requires completing this backend transition before using
a cloud-connected test build. Local debug tests do not require the transition.
Cloud Functions deployment requires Blaze billing;
linking a billing account must be approved by the project owner.

The billing link was approved during this audit, but Google Cloud returned
`403: The caller does not have permission` for the existing CLI session.
Consequently, the test Functions deployment stopped at the Blaze requirement;
the live Firestore migration and rule/index replacement have not run. An
account with billing-assignment permission must complete the link first.

The test Android SDK JSON was refreshed from Firebase after the audit. Debug
and local release SHA-1/SHA-256 certificates are registered on its existing
`com.good4.test` application. The test Auth Apple provider is enabled for the
native iOS flow, matching production; provider credentials remain separate.

Use the same backend source and rules as production, with an explicit test
project. From `firebase/v2`:

```shell
node scripts/align-test-schema.mjs           # read-only migration preview
npm run test:schema
# After enabling Blaze for good4tr-test:
npx firebase deploy --only functions --project test
node scripts/align-test-schema.mjs --apply   # checks callable readiness, then backs up and migrates
npx firebase deploy --only firestore,storage --project test
node scripts/align-test-schema.mjs           # verify no further migration writes
```

The migration preserves Auth UIDs, account history, original legacy
community/event/coupon records, and actual consent. It converts legacy user
roles/dates, creates canonical organizations with UID memberships and events
with registrations/check-ins, and archives image-only advertisements in
`legacyCampaigns` instead of treating them as V2 meal campaigns. Missing shared
public campus content is seeded from production; private production data is
never copied. Backups under ignored `output/firebase-test-schema/` contain
private test data and must remain local. Concurrent changes abort the affected
batch through Firestore update-time preconditions; rerun the preview before retrying.

Firestore and Storage deployment must wait until the functions and data are
ready so existing test clients are not left with V2 rules and legacy data.
Extra legacy indexes can remain for historical queries; install all V2 indexes
from `firebase/v2/firestore.indexes.json` and wait for them to become ready.

## Local Firebase configuration files

| Environment | File |
| --- | --- |
| Android production | `composeApp/src/prod/google-services.json` |
| Android test | `composeApp/src/staging/google-services.json` |
| iOS production | `iosApp/GoogleService-Info-Prod.plist` |
| iOS test | `iosApp/GoogleService-Info-Test.plist` |

Download each file from the corresponding Firebase project/application. These
files are ignored by Git and must be installed on each developer machine and
in CI. Production JSON previously stored at `composeApp/google-services.json`
must be moved to `composeApp/src/prod/google-services.json`; do not retain a
module-root fallback. Do not use `composeApp/src/v2/google-services.json` or
`GoogleService-Info-V2.plist`.

Firebase can include multiple Android client entries in a downloaded JSON.
The Google Services plugin selects the one matching the variant's package
name. An obsolete `.v2` entry in an older downloaded file does not cause the
production app to use that registration. Download a fresh file after console
cleanup instead of inventing or manually changing OAuth client IDs.

On iOS, the production and test plist bundle IDs and reversed Google client
IDs must match their Xcode configurations. The schemes use different bundle
IDs and can be installed side by side.

## Running and checking the environments

```shell
./gradlew :composeApp:assembleProdDebug :composeApp:assembleStagingDebug
./gradlew :composeApp:assembleProdRelease
./gradlew :composeApp:signingReport
```

`prodRelease` requires the existing release keystore configuration. Register
both the shared debug certificate and local release certificate under the
same Firebase Android production application (`com.good4`). For Google Play
installs, also retain the Play App Signing certificate, which can differ from
the local release/upload key. A separate Firebase app for each certificate is
not required.

The local release certificate reported on 2 October 2026 is:

```text
SHA-1:   56:BC:AC:58:5A:77:68:46:30:98:50:29:BB:CB:BE:43:87:8B:DA:ED
SHA-256: EA:3C:68:0A:1E:FE:01:43:E1:83:48:07:ED:62:C5:75:47:4C:79:5C:F1:94:29:7F:D9:DB:E6:E9:62:02:7C:E7
```

Google sign-in needs the registered SHA-1 and the production Web OAuth client
ID. App Check with Play Integrity has its own SHA-256 and distribution
requirements; registering the SHA-1 alone does not verify App Check. Recheck
fingerprints with `signingReport` if signing changes.

Callable App Check enforcement is opt-in through `ENFORCE_APP_CHECK=true`.
Android, iOS, and the web panel initialize App Check and send tokens. Leave
enforcement off until the updated clients are deployed and each client provides
verified tokens, otherwise its callable requests will be rejected. Web setup
and verification are documented in the [web panel guide](../README.md#web-panel).

### iOS App Check setup and verification

The native Firebase SDK installs its provider before `FirebaseApp.configure()`:
real-device Release builds use App Attest; Debug builds and simulator builds
use the debug provider. Token auto-refresh is enabled. The Kotlin callable
client obtains the native SDK token through `NativeAppCheckBridge` and sends
it as `X-Firebase-AppCheck`. Token acquisition has a 10-second timeout; failures
are logged without token contents and requests proceed without the header,
matching Android's optional-token behavior while enforcement is off.

1. In `good4tr-v2`, register `com.good4.iosApp` under App Check with **App Attest**.
   The Apple Team ID must match the signing team (`NM79R577GW` in this project).
   Keep the default 1-hour token TTL.
2. The Xcode target includes FirebaseAppCheck and the App Attest capability.
   The entitlement `com.apple.developer.devicecheck.appattest-environment` is
   `production`, as required by Firebase (even for locally signed device builds).
   If device signing reports missing entitlements, enable App Attest for the
   matching App ID in Apple Developer and refresh its provisioning profile.
3. For `iosApp Test`, register `com.good4.iosApp.test` in `good4tr-test` separately.
   Run it with `-FIRDebugEnabled` in the scheme's Run arguments, find the Firebase
   App Check debug token in Xcode's console, and add it under **Manage debug
   tokens** for that test application. A production-scheme simulator uses the
   debug provider too; its token belongs in the production application's list.
   Keep debug tokens private.
4. After registering the debug token, restart and exercise Firestore reads and
   callable actions. Check App Check metrics for Firestore/Storage and callable
   verification logs for Functions; successful actions alone do not establish
   that attestation succeeded while enforcement is off.
5. Verify `iosApp Prod` on a real iPhone (or TestFlight) to exercise App Attest.
   Check sign-in/profile initialization, community following, event registration,
   and campaign code issuance. Look for verified requests and investigate any
   `[App Check] Token request failed` entries before enabling enforcement.

References: [App Attest setup](https://firebase.google.com/docs/app-check/ios/app-attest-provider),
[iOS debug provider](https://firebase.google.com/docs/app-check/ios/debug-provider),
[Functions verification metrics](https://firebase.google.com/docs/app-check/monitor-functions-metrics).

The schedule audit uses `prodDebug`:

```shell
bash tools/schedule-audit/run.sh
```

In Xcode, choose `iosApp Prod` for production and App Store archives, or
`iosApp Test` for the test project. The production scheme currently runs a
Release build; debug-vs-release is not a separate Firebase registration.

## Retiring obsolete mobile registrations

Commit `88edd35f3af52134b49c98c170826c2adf6889c6` removed the iOS `DebugV2`
configuration, `iosApp V2` scheme, and V2 iPhone install script. It did not
remove Android's `v2` flavor, the schedule-audit references, or the Firebase
console registrations. The Android `v2` flavor is now removed as well.

The following registrations are no longer used by the current source tree:

| Registration in `good4tr-v2` | Application identifier | Firebase app ID |
| --- | --- | --- |
| Good4 V2 Android | `com.good4.v2` | `1:654697131931:android:92249864df761520603b3e` |
| Good4 V2 iOS | `com.good4.iosApp.v2` | `1:654697131931:ios:a224075631fa42e3603b3e` |

Before using **Remove this app** in Firebase, check that these old package IDs
are not still distributed through Google Play, TestFlight, App Distribution,
or used by installed development builds. This repository audit cannot inspect
those external deployments. Replace old development installs with the
production or test packages above. App Check and other app-ID-specific
services stop using a removed registration; Firebase allows restoration for
30 days before permanent deletion.

Keep the Android/iOS production registrations, both test registrations, the
business Web app, and the `good4tr-v2` Firebase project. Do not delete shared
Authentication users, Firestore data, or Storage as part of app-registration
cleanup.

API keys and OAuth clients are not automatically deleted with the Firebase
app. Review only clients explicitly bound to the obsolete `.v2` package/bundle
IDs. The Web OAuth client is shared by production Google sign-in; retain it
and the production iOS client, API keys, and production certificate entries.
Do not delete clients based only on a nickname containing `V2`.

After console cleanup, download a fresh production Android JSON to
`composeApp/src/prod/google-services.json`, sync Gradle, and verify Google login
on a locally signed `prodRelease` build and a Play-distributed build.

References: [Google Services config selection](https://firebase.google.com/docs/android/google-services-plugin-and-file),
[Firebase app removal and restoration](https://support.google.com/firebase/answer/7047853),
[Google sign-in setup](https://firebase.google.com/docs/auth/android/google-signin),
[Play Integrity App Check](https://firebase.google.com/docs/app-check/android/play-integrity-provider).

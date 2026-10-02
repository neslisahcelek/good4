# Good4 mobile environments

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

The test project uses the legacy data model. Running `stagingDebug` does not
exercise production V2 callable functions or its Google-only login flow.
Running `prodDebug` uses production data, even though it is a debug build.

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

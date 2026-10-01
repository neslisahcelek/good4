# Good4 mobile environments

Production and test clients use separate Firebase projects. This prevents test
builds from overwriting or silently reading production data.

## Firebase applications

| Platform | Application identifier | Firebase app ID |
| --- | --- | --- |
| Android | `com.good4.v2` | `1:654697131931:android:92249864df761520603b3e` |
| iOS production | `com.good4.iosApp` | `1:654697131931:ios:c7e28c28e33731d1603b3e` |
| iOS test | `com.good4.iosApp.test` | `1:449563145023:ios:dff46396477949a419d98a` |

Local Firebase configuration files:

- Android: `composeApp/src/v2/google-services.json`
- iOS production: `iosApp/GoogleService-Info-Prod.plist`
- iOS test: `iosApp/GoogleService-Info-Test.plist`

These client configuration files are intentionally ignored by Git with the
other Firebase app configurations. Keep them in the local `Good4 dev` folder.

## Running each environment

### Android

- Choose the `v2Debug` build variant for `good4tr-v2`.
- Choose `stagingDebug` to continue using `good4tr-test`.

Command-line checks:

```shell
./gradlew :composeApp:assembleV2Debug
./gradlew :composeApp:assembleStagingDebug
```

### iOS

- Choose `iosApp Prod` for `good4tr-v2` and App Store archives.
- Choose `iosApp Test` for `good4tr-test` and test archives.

The two schemes use separate bundle identifiers, Firebase plist files and
Google sign-in client IDs, so they can be installed side by side.

## Compatibility boundary

The mobile runtime now recognizes both legacy roles and the V2 roles:

- `good4Admin`
- `businessOwner`, `businessStaff`
- `communityManager`, `communityStaff`
- `student`

Legacy profile reads remain supported. New V2 student self-registration is not
enabled yet because it must create the new `displayName/status/role` profile
only after email verification. Assigned V2 community and business accounts can
be used for the next development phase.

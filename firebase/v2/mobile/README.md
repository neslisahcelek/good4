# Good4 V2 mobile environment

The V2 mobile clients are deliberately separate from the existing Good4Test
clients. This prevents development against `good4tr-v2` from overwriting or
silently reading `good4tr-test` data.

## Firebase applications

| Platform | Application identifier | Firebase app ID |
| --- | --- | --- |
| Android | `com.good4.v2` | `1:654697131931:android:92249864df761520603b3e` |
| iOS | `com.good4.iosApp.v2` | `1:654697131931:ios:a224075631fa42e3603b3e` |

Local Firebase configuration files:

- Android: `composeApp/src/v2/google-services.json`
- iOS: `iosApp/GoogleService-Info-V2.plist`

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

- Choose the shared `iosApp V2` scheme for `good4tr-v2`.
- Choose `iosApp Staging` to continue using `good4tr-test`.

The V2 configuration builds a separate `Good4 V2` app with bundle identifier
`com.good4.iosApp.v2`, so it can be installed alongside Good4Test.

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

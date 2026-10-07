# Firestore symbols in iOS archives

Firebase 12.19.2's default Swift package graph uses prebuilt Firestore, abseil,
gRPC and OpenSSL frameworks without bundled dSYMs. These cause Organizer's
`Upload Symbols Failed` warnings. Unlike MapLibre, no matching vendor symbols
are available in the resolved packages.

Good4 device archives use Firebase's supported source distribution instead.
Firestore and its C++ dependencies are linked into the app; Xcode includes their
debug information in the app dSYM. Firebase is pinned to the already-resolved
12.19.2 version. No generated placeholder dSYMs are used.

## Local Xcode archive

1. Quit Xcode completely. The variable must reach the Xcode process that resolves
   packages; adding it to a scheme's Run environment or an xcconfig is too late.
2. From the repository root, run `bash iosApp/scripts/open-xcode.sh`.
3. Let Swift packages resolve. The source graph uses `abseil-cpp-SwiftPM`,
   `grpc-ios` and `boringSSL-SwiftPM` instead of the binary packages.
4. Choose **Product > Clean Build Folder**, then archive **iosApp Prod**.
   The first source build takes longer than the prebuilt distribution.
5. Distribute the new archive. The existing 1.1.3 (14) archive remains unchanged.
   If that build was already uploaded, use a new build number for a new upload.

Archive preflight fails early if the environment variable is missing. The final
archive check rejects the five prebuilt frameworks if an old package graph is
still being used. Regular and simulator builds skip these checks.

## Command line and Xcode Cloud

Prefix both package resolution and archive commands with
`FIREBASE_SOURCE_FIRESTORE=1`. For Xcode Cloud, add that variable with value `1`
in the workflow's environment settings; exporting it in a CI hook alone does not
set the environment of the later Xcode Cloud archive process.

## Verify the archive

The app's `Frameworks` directory must not contain `FirebaseFirestoreInternal`,
`absl`, `grpc`, `grpcpp` or `openssl_grpc` frameworks. The archive must contain
`dSYMs/iosApp.app.dSYM`, with the same arm64 UUID as the app executable. MapLibre
continues to use the separate UUID-verified vendor dSYM script.

Reference: [Firebase 12.19.2 source distribution](https://github.com/firebase/firebase-ios-sdk/blob/12.19.2/docs/FirestoreSPM.md).

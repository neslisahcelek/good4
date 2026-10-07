#!/bin/bash
set -euo pipefail

# Device archives need source-built Firestore; regular/simulator builds may use binaries.
if [[ "${ACTION:-}" != "install" || "${PLATFORM_NAME:-}" != "iphoneos" ]]; then
  exit 0
fi

case "${1:?Expected preflight or verify}" in
  preflight)
    if [[ -z "${FIREBASE_SOURCE_FIRESTORE:-}" ]]; then
      echo "error: Quit Xcode and reopen with bash iosApp/scripts/open-xcode.sh. Firestore's prebuilt frameworks have no vendor dSYMs. For CLI/Cloud builds, set FIREBASE_SOURCE_FIRESTORE=1 before resolving packages and archiving." >&2
      exit 1
    fi
    ;;
  verify)
    # Source-built C++ libraries are linked into the app and covered by its dSYM.
    # Reject a stale binary package graph instead of shipping another symbol warning.
    frameworks="${TARGET_BUILD_DIR:?}/${FRAMEWORKS_FOLDER_PATH:?}"
    for name in FirebaseFirestoreInternal absl grpc grpcpp openssl_grpc; do
      if [[ -d "$frameworks/$name.framework" ]]; then
        echo "error: $name.framework is still embedded. Resolve Swift packages with FIREBASE_SOURCE_FIRESTORE=1, clean the build folder, and archive again." >&2
        exit 1
      fi
    done
    echo "Firestore archive uses source-built libraries; no unsymbolicated vendor frameworks are embedded."
    ;;
  *) echo "error: Expected preflight or verify." >&2; exit 1 ;;
esac

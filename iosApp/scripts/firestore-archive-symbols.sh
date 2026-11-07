#!/bin/bash
set -euo pipefail

# Device archives check
if [[ "${ACTION:-}" != "install" || "${PLATFORM_NAME:-}" != "iphoneos" ]]; then
  exit 0
fi

case "${1:?Expected preflight or verify}" in
  preflight)
    if [[ -n "${FIREBASE_SOURCE_FIRESTORE:-}" ]]; then
      echo "Firestore archive: source-build mode enabled."
    else
      echo "Firestore archive: using standard official binary distribution."
    fi
    ;;
  verify)
    if [[ -n "${FIREBASE_SOURCE_FIRESTORE:-}" ]]; then
      frameworks="${TARGET_BUILD_DIR:?}/${FRAMEWORKS_FOLDER_PATH:?}"
      for name in FirebaseFirestoreInternal absl grpc grpcpp openssl_grpc; do
        if [[ -d "$frameworks/$name.framework" ]]; then
          echo "error: $name.framework is still embedded while FIREBASE_SOURCE_FIRESTORE=1." >&2
          exit 1
        fi
      done
      echo "Firestore archive uses source-built libraries."
    else
      echo "Firestore archive uses standard prebuilt binary frameworks; verification passed."
    fi
    ;;
  *) echo "error: Expected preflight or verify." >&2; exit 1 ;;
esac

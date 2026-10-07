#!/bin/bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
if /usr/bin/pgrep -x Xcode >/dev/null; then
  echo "Quit Xcode first, then run this script again. Swift package resolution must inherit FIREBASE_SOURCE_FIRESTORE." >&2
  exit 1
fi

# Firebase reads this in Package.swift, before any target or scheme runs.
/usr/bin/open --env FIREBASE_SOURCE_FIRESTORE=1 "$script_dir/../iosApp.xcodeproj"

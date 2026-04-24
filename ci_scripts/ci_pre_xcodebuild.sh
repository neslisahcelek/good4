#!/bin/zsh

set -e

echo "--- ci_pre_xcodebuild: Starting ---"

REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-.}"
IOS_DIR="$REPO_ROOT/iosApp"

# ── Firebase GoogleService-Info.plist ─────────────────────────────────────────
# File is gitignored. Provide Base64-encoded content as an Xcode Cloud env var:
#   GOOGLE_SERVICE_INFO_PROD_BASE64
#
# To encode: base64 -i GoogleService-Info.plist | pbcopy

if [[ -n "$GOOGLE_SERVICE_INFO_PROD_BASE64" ]]; then
    echo "Writing GoogleService-Info-Prod.plist..."
    echo "$GOOGLE_SERVICE_INFO_PROD_BASE64" | base64 --decode > "$IOS_DIR/GoogleService-Info-Prod.plist"
else
    echo "ERROR: GOOGLE_SERVICE_INFO_PROD_BASE64 is not set"
    exit 1
fi

# ── Build environment info ─────────────────────────────────────────────────────

echo "Xcode version: $(xcodebuild -version | head -1)"
echo "macOS version: $(sw_vers -productVersion)"
echo "Ruby version:  $(ruby -v)"
echo "CocoaPods:     $(pod --version)"

echo "--- ci_pre_xcodebuild: Done ---"

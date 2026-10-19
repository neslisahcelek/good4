#!/bin/bash
set -euo pipefail

echo "--- ci_pre_xcodebuild: Starting ---"

REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-.}"
IOS_DIR="$REPO_ROOT/iosApp"

# ── 1. Firebase GoogleService-Info.plist ─────────────────────────────────────────
# Files are gitignored. Provide Base64-encoded content as an Xcode Cloud env var:
#   GOOGLE_SERVICE_INFO_PROD_BASE64
#   GOOGLE_SERVICE_INFO_TEST_BASE64 (optional)
#
# To encode: base64 -i iosApp/GoogleService-Info-Prod.plist | pbcopy

if [[ -n "${GOOGLE_SERVICE_INFO_PROD_BASE64:-}" ]]; then
    echo "Writing GoogleService-Info-Prod.plist from environment variable..."
    echo "$GOOGLE_SERVICE_INFO_PROD_BASE64" | base64 --decode > "$IOS_DIR/GoogleService-Info-Prod.plist"
elif [[ -f "$IOS_DIR/GoogleService-Info-Prod.plist" ]]; then
    echo "Using existing GoogleService-Info-Prod.plist"
else
    echo "ERROR: GOOGLE_SERVICE_INFO_PROD_BASE64 is not set and $IOS_DIR/GoogleService-Info-Prod.plist is missing" >&2
    exit 1
fi

if [[ -n "${GOOGLE_SERVICE_INFO_TEST_BASE64:-}" ]]; then
    echo "Writing GoogleService-Info-Test.plist from environment variable..."
    echo "$GOOGLE_SERVICE_INFO_TEST_BASE64" | base64 --decode > "$IOS_DIR/GoogleService-Info-Test.plist"
fi

# ── 2. Environment Variables ────────────────────────────────────────────────────
# Ensure Firestore source-build flag is set for subsequent steps
export FIREBASE_SOURCE_FIRESTORE=1

# Ensure JAVA_HOME is set in case xcodebuild invokes gradlew
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
    if [ -x "/usr/libexec/java_home" ] && /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
    elif [ -d "$HOME/.jdk/Contents/Home" ]; then
        export JAVA_HOME="$HOME/.jdk/Contents/Home"
    fi
fi
if [ -n "${JAVA_HOME:-}" ]; then
    export PATH="$JAVA_HOME/bin:$PATH"
fi

# ── 3. Build environment info ───────────────────────────────────────────────────
echo "Xcode version: $(xcodebuild -version | head -1)"
echo "macOS version: $(sw_vers -productVersion)"
if command -v java >/dev/null 2>&1; then
    echo "Java version:  $(java -version 2>&1 | head -1)"
fi

echo "--- ci_pre_xcodebuild: Done ---"

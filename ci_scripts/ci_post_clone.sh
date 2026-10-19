#!/bin/bash
set -euo pipefail

echo "--- ci_post_clone: Starting ---"

REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-.}"
IOS_DIR="$REPO_ROOT/iosApp"

echo "Repo root: $REPO_ROOT"
echo "Architecture: $(uname -m)"

# ── 1. Setup Java 17 for Kotlin Multiplatform ─────────────────────────────────
# Xcode Cloud macOS runners do not include a JDK by default.
if [ -z "${JAVA_HOME:-}" ] || [ ! -x "${JAVA_HOME:-}/bin/java" ]; then
    if [ -x "/usr/libexec/java_home" ] && /usr/libexec/java_home -v 17 >/dev/null 2>&1; then
        export JAVA_HOME="$(/usr/libexec/java_home -v 17)"
    elif [ -d "$HOME/.jdk/Contents/Home" ]; then
        export JAVA_HOME="$HOME/.jdk/Contents/Home"
    else
        echo "Installing OpenJDK 17 for Xcode Cloud runner..."
        ARCH="$(uname -m)"
        if [ "$ARCH" = "x86_64" ]; then
            JDK_ARCH="x64"
        else
            JDK_ARCH="aarch64"
        fi
        JDK_DIR="$HOME/.jdk"
        mkdir -p "$JDK_DIR"
        curl -fsSL "https://api.adoptium.net/v3/binary/latest/17/ga/mac/${JDK_ARCH}/jdk/hotspot/normal/eclipse" -o /tmp/openjdk17.tar.gz
        tar -xzf /tmp/openjdk17.tar.gz -C "$JDK_DIR" --strip-components=1
        rm -f /tmp/openjdk17.tar.gz
        export JAVA_HOME="$JDK_DIR/Contents/Home"
    fi
fi

export PATH="$JAVA_HOME/bin:$PATH"
echo "Using Java: $(java -version 2>&1 | head -n 1)"

# Persist JAVA_HOME for Gradle daemon and subsequent builds
mkdir -p "$HOME/.gradle"
echo "org.gradle.java.home=$JAVA_HOME" >> "$HOME/.gradle/gradle.properties"

# ── 2. Pre-warm Kotlin/Native compilation ─────────────────────────────────────
# Pre-compile the iOS framework so xcodebuild can link it smoothly.
cd "$REPO_ROOT"
chmod +x gradlew
./gradlew :composeApp:compileKotlinIosArm64

# ── 3. Resolve Swift Package Manager dependencies ─────────────────────────────
# Force source-built Firestore so device archives include full dSYMs.
FIREBASE_SOURCE_FIRESTORE=1 xcodebuild \
    -resolvePackageDependencies \
    -project "$IOS_DIR/iosApp.xcodeproj" \
    -scheme "iosApp Prod" \
    -onlyUsePackageVersionsFromResolvedFile

echo "--- ci_post_clone: Done ---"

#!/bin/zsh
set -e

echo "--- ci_post_clone: Starting ---"

REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-.}"
IOS_DIR="$REPO_ROOT/iosApp"

echo "Repo root: $REPO_ROOT"

# Warm up the Kotlin/Native compiler. Xcode's build phase builds and embeds the
# framework for the selected configuration and SDK.
cd "$REPO_ROOT"
chmod +x gradlew
./gradlew :composeApp:compileKotlinIosArm64

# Firebase, Google Sign-In and MapLibre are managed by Swift Package Manager.
xcodebuild -resolvePackageDependencies -project "$IOS_DIR/iosApp.xcodeproj"

echo "--- ci_post_clone: Done ---"

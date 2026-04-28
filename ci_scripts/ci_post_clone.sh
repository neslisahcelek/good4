#!/bin/zsh
set -e

echo "--- ci_post_clone: Starting ---"

REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-.}"
IOS_DIR="$REPO_ROOT/iosApp"

echo "Repo root: $REPO_ROOT"

# 1. Gradle build
cd "$REPO_ROOT"
chmod +x gradlew
./gradlew :composeApp:podPublishReleaseXCFramework

# 2. CocoaPods
cd "$IOS_DIR"

echo "Podfile check:"
ls -la

echo "Installing pods..."
pod install --repo-update

echo "Workspace kontrol:"
ls -la
find . -name "*.xcworkspace"

echo "--- ci_post_clone: Done ---"
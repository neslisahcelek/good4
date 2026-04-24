#!/bin/zsh

set -e

echo "--- ci_post_clone: Starting ---"

REPO_ROOT="${CI_PRIMARY_REPOSITORY_PATH:-.}"
IOS_DIR="$REPO_ROOT/iosApp"

# CocoaPods — must run here so .xcworkspace exists before Xcode Cloud resolves it
if ! command -v pod &>/dev/null; then
    echo "pod not found, installing via gem..."
    sudo gem install cocoapods --no-document
fi

echo "Installing CocoaPods dependencies..."
cd "$IOS_DIR"
pod install --repo-update
cd "$REPO_ROOT"

echo "Searching for xcworkspace files:"
find "$REPO_ROOT" -name "*.xcworkspace" -not -path "*/Pods/*"

echo "--- ci_post_clone: Done ---"

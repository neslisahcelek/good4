#!/bin/bash
set -euo pipefail

script_dir="$(cd "$(dirname "$0")" && pwd)"
project_path="$script_dir/../iosApp.xcodeproj"

echo "Opening Good4 iOS project in Xcode..."
/usr/bin/open "$project_path"

#!/bin/bash
set -euo pipefail

# MapLibre distributes device symbols separately from its prebuilt XCFramework.
# Download during pod install, then copy them locally during Archive.
arm64_uuid() {
  /usr/bin/xcrun dwarfdump --uuid "$1" | /usr/bin/awk '$3 == "(arm64)" { print $2 }'
}

verify_symbols() {
  local binary_uuid symbols_uuid
  binary_uuid="$(arm64_uuid "$1")"
  symbols_uuid="$(arm64_uuid "$2")"
  if [[ -z "$binary_uuid" || "$binary_uuid" != "$symbols_uuid" ]]; then
    echo "error: MapLibre dSYM UUID mismatch (framework: $binary_uuid, symbols: $symbols_uuid). Run pod install to prepare matching symbols." >&2
    return 1
  fi
}

mode="${1:?Expected prepare or embed}"
version="${2:?Expected resolved MapLibre version}"
pods_root="${3:-${PODS_ROOT:-}}"
if [[ -z "$pods_root" ]]; then
  echo "error: PODS_ROOT is required for MapLibre symbols." >&2
  exit 1
fi
symbols_dir="$pods_root/MapLibre/dSYMs/$version/MapLibre.framework.dSYM"
symbols_file="$symbols_dir/Contents/Resources/DWARF/MapLibre"

case "$mode" in
  prepare)
    binary="$pods_root/MapLibre/MapLibre.xcframework/ios-arm64/MapLibre.framework/MapLibre"
    if [[ -f "$symbols_file" ]] && verify_symbols "$binary" "$symbols_file"; then
      echo "MapLibre $version: matching device dSYM already cached."
      exit 0
    fi

    temp_dir="$(/usr/bin/mktemp -d "${TMPDIR:-/tmp}/good4-maplibre-dsym.XXXXXX")"
    trap '/bin/rm -rf "$temp_dir"' EXIT
    url="https://github.com/maplibre/maplibre-native/releases/download/ios-v$version/MapLibre_ios_device.framework.dSYM.zip"
    echo "Downloading official MapLibre $version device symbols..."
    /usr/bin/curl --fail --location --silent --show-error --retry 2 --connect-timeout 15 --max-time 180 "$url" -o "$temp_dir/symbols.zip"
    /usr/bin/ditto -x -k "$temp_dir/symbols.zip" "$temp_dir/extracted"
    downloaded_dir="$temp_dir/extracted/MapLibre_ios_device.framework.dSYM"
    downloaded_file="$downloaded_dir/Contents/Resources/DWARF/MapLibre_ios_device"
    verify_symbols "$binary" "$downloaded_file"

    # Match the embedded framework's name, keeping the original DWARF data.
    /bin/mv "$downloaded_file" "$downloaded_dir/Contents/Resources/DWARF/MapLibre"
    /bin/mkdir -p "$(/usr/bin/dirname "$symbols_dir")"
    /usr/bin/ditto "$downloaded_dir" "$symbols_dir"
    echo "MapLibre $version: device dSYM prepared."
    ;;
  embed)
    # Xcode uses ACTION=install for Archive. Regular builds need no device dSYM.
    if [[ "${ACTION:-}" != "install" || "${PLATFORM_NAME:-}" != "iphoneos" ]]; then
      exit 0
    fi
    binary="${TARGET_BUILD_DIR:?}/${FRAMEWORKS_FOLDER_PATH:?}/MapLibre.framework/MapLibre"
    if [[ ! -f "$symbols_file" ]]; then
      echo "error: MapLibre device dSYM is missing. Run pod install before archiving." >&2
      exit 1
    fi
    verify_symbols "$binary" "$symbols_file"
    destination="${DWARF_DSYM_FOLDER_PATH:?}/MapLibre.framework.dSYM"
    /usr/bin/ditto "$symbols_dir" "$destination"
    echo "MapLibre $version: copied matching dSYM to $destination"
    ;;
  *)
    echo "error: Unknown MapLibre dSYM command: $mode" >&2
    exit 1
    ;;
esac

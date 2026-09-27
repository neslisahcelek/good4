#!/usr/bin/env bash
set -euo pipefail

expected_source='/Users/cankilinc/Desktop/Good4/Good4 dev'
# DebugV2 builds the store bundle id against good4tr-v2 (Xcode project settings, Sep 2026).
expected_bundle='com.good4.iosApp'
device_id="${1:-00008120-001831543C42601E}"
source_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"

if [[ "$source_dir" != "$expected_source" ]]; then
    echo "Kurulum durduruldu: güncel V2 kaynağı $expected_source olmalı." >&2
    exit 1
fi

connected_devices="$(xcrun devicectl list devices)"
if ! rg -q -F "$device_id" <<< "$connected_devices"; then
    echo "Kurulum durduruldu: beklenen iPhone bağlı değil ($device_id)." >&2
    exit 1
fi

workspace="$source_dir/iosApp/iosApp.xcworkspace"
build_args=(
    -workspace "$workspace"
    -scheme 'iosApp V2'
    -configuration DebugV2
    -destination "id=$device_id"
    -allowProvisioningUpdates
    -allowProvisioningDeviceRegistration
)

build_settings="$(xcodebuild "${build_args[@]}" -showBuildSettings -json)"
actual_bundle="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.PRODUCT_BUNDLE_IDENTIFIER' <<< "$build_settings")"
build_dir="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.TARGET_BUILD_DIR' <<< "$build_settings")"
product_name="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.FULL_PRODUCT_NAME' <<< "$build_settings")"

if [[ "$actual_bundle" != "$expected_bundle" || -z "$build_dir" || -z "$product_name" ]]; then
    echo "Kurulum durduruldu: V2 paket kimliği veya derleme yolu beklenen değerle eşleşmiyor." >&2
    exit 1
fi

xcodebuild "${build_args[@]}" -quiet build

app_path="$build_dir/$product_name"
built_bundle="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app_path/Info.plist")"
if [[ "$built_bundle" != "$expected_bundle" ]]; then
    echo "Kurulum durduruldu: üretilen uygulama $expected_bundle değil." >&2
    exit 1
fi

xcrun devicectl device install app --device "$device_id" "$app_path"
if xcrun devicectl device process launch --terminate-existing --device "$device_id" "$expected_bundle"; then
    echo 'Good4 V2 iPhone’a kuruldu ve açıldı.'
else
    echo 'Good4 V2 iPhone’a kuruldu. Telefon kilitliyse açtıktan sonra uygulamayı başlatın.' >&2
fi

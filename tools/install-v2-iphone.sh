#!/usr/bin/env bash
set -euo pipefail

expected_source='/Users/cankilinc/Desktop/Good4/Good4 dev'
expected_bundle="${GOOD4_IPHONE_BUNDLE_ID:-com.good4.iosApp}"
expected_firebase='good4tr-v2'
device_id="${1:-00008120-001831543C42601E}"
source_dir="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd -P)"

if [[ "$source_dir" != "$expected_source" ]]; then
    echo "Kurulum durduruldu: güncel V2 kaynağı $expected_source olmalı." >&2
    exit 1
fi

case "$expected_bundle" in
    com.good4.iosApp|com.good4.iosApp.v2) ;;
    *) echo 'Kurulum durduruldu: desteklenmeyen V2 paket kimliği.' >&2; exit 1 ;;
esac

connected_devices="$(xcrun devicectl list devices)"
if ! grep -Eq "$device_id.*connected.*physical" <<< "$connected_devices"; then
    # New devicectl versions list a connected phone as "available (paired)".
    # Confirm its actual connection, physical identity and UDID before building.
    if ! device_details="$(xcrun devicectl device info details --device "$device_id" --quiet --timeout 20 --json-output -)" \
        || ! /usr/bin/jq -e --arg device_id "$device_id" '
            (.info.outcome == "success") and
            ((.result.properties.connection.state // .result.connectionProperties.tunnelState) == "connected") and
            ((.result.properties.hardware.reality // .result.hardwareProperties.reality) == "physical") and
            ((.result.properties.hardware.udid // .result.hardwareProperties.udid) == $device_id)
        ' <<< "$device_details" >/dev/null; then
        echo "Kurulum durduruldu: beklenen iPhone bağlı değil ($device_id)." >&2
        exit 1
    fi
fi

build_args=(
    -project "$source_dir/iosApp/iosApp.xcodeproj"
    -scheme 'iosApp V2'
    -configuration DebugV2
    -derivedDataPath "$source_dir/iosApp/build/Good4V2-iPhone"
    -destination "id=$device_id"
    -allowProvisioningUpdates
    -allowProvisioningDeviceRegistration
)

# A local V2 identity can install the same current source without changing the
# production signing team or the standard V2 bundle in the Xcode project.
if [[ "$expected_bundle" == 'com.good4.iosApp.v2' ]]; then
    : "${GOOD4_IPHONE_TEAM:?Yerel V2 geliştirici takımı belirtilmeli.}"
    build_args+=(
        "DEVELOPMENT_TEAM=$GOOD4_IPHONE_TEAM"
        "PRODUCT_BUNDLE_IDENTIFIER=$expected_bundle"
        'FIREBASE_CONFIG_FILE=GoogleService-Info-V2.plist'
        'GOOGLE_REVERSED_CLIENT_ID=com.googleusercontent.apps.654697131931-l8cnp6cl49jmddt9a2bkffrps4ug04lr'
    )
fi

# Keep the V2 device configuration and signing profile while measuring the
# performance of optimized Kotlin and Swift code on the phone.
if [[ "${GOOD4_IPHONE_OPTIMIZED:-0}" == '1' ]]; then
    build_args+=(
        'KOTLIN_FRAMEWORK_BUILD_TYPE=RELEASE'
        'SWIFT_OPTIMIZATION_LEVEL=-O'
        'GCC_OPTIMIZATION_LEVEL=s'
    )
fi

echo 'Güncel Kampüs Dolabı sürümü için V2 ayarları kontrol ediliyor.'
build_settings="$(xcodebuild "${build_args[@]}" -showBuildSettings -json)"
actual_bundle="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.PRODUCT_BUNDLE_IDENTIFIER' <<< "$build_settings")"
build_dir="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.TARGET_BUILD_DIR' <<< "$build_settings")"
product_name="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.FULL_PRODUCT_NAME' <<< "$build_settings")"
firebase_config="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.FIREBASE_CONFIG_FILE' <<< "$build_settings")"
firebase_project="$(/usr/libexec/PlistBuddy -c 'Print :PROJECT_ID' "$source_dir/iosApp/$firebase_config")"
firebase_bundle="$(/usr/libexec/PlistBuddy -c 'Print :BUNDLE_ID' "$source_dir/iosApp/$firebase_config")"

if [[ "$actual_bundle" != "$expected_bundle" || "$firebase_project" != "$expected_firebase" || "$firebase_bundle" != "$expected_bundle" || -z "$build_dir" || -z "$product_name" ]]; then
    echo 'Kurulum durduruldu: V2 paket kimliği, Firebase projesi veya derleme yolu beklenen değerle eşleşmiyor.' >&2
    exit 1
fi

echo 'Kampüs Dolabı sürümü iPhone için derleniyor.'
app_path="$build_dir/$product_name"
build_log="$(mktemp "${TMPDIR:-/tmp/}good4-v2-build.XXXXXX")"
trap 'rm -f "$build_log"' EXIT
if xcodebuild "${build_args[@]}" -quiet build 2>&1 | tee "$build_log"; then
    :
elif grep -Eq 'resource fork, Finder information, or similar detritus not allowed' "$build_log" \
    && ! grep -Eq '^([^:]+:[0-9]+(:[0-9]+)?:[[:space:]]*)?(fatal[[:space:]]+)?error:' "$build_log"; then
    # Finder metadata on copied resource bundles can block the final signature.
    # Use Xcode's generated entitlements and its already embedded device profile.
    target_temp_dir="$(/usr/bin/jq -r '.[] | select(.target == "iosApp") | .buildSettings.TARGET_TEMP_DIR' <<< "$build_settings")"
    /usr/bin/xattr -cr "$build_dir"
    /usr/bin/codesign --force --sign "${GOOD4_IPHONE_SIGNING_IDENTITY:-Apple Development}" \
        --entitlements "$target_temp_dir/$product_name.xcent" \
        --generate-entitlement-der --timestamp=none "$app_path"
else
    exit 1
fi
/usr/bin/codesign --verify --deep --strict "$app_path"

built_bundle="$(/usr/libexec/PlistBuddy -c 'Print :CFBundleIdentifier' "$app_path/Info.plist")"
built_firebase="$(/usr/libexec/PlistBuddy -c 'Print :PROJECT_ID' "$app_path/GoogleService-Info.plist")"
if [[ "$built_bundle" != "$expected_bundle" || "$built_firebase" != "$expected_firebase" ]]; then
    echo 'Kurulum durduruldu: üretilen uygulama beklenen V2 paketi değil.' >&2
    exit 1
fi

xcrun devicectl device install app --device "$device_id" "$app_path"
if xcrun devicectl device process launch --terminate-existing --device "$device_id" "$expected_bundle"; then
    echo 'Kampüs Dolabı sürümü Good4Test adıyla iPhone’a kuruldu ve açıldı.'
else
    echo 'Kampüs Dolabı sürümü iPhone’a kuruldu. Telefon kilitliyse açtıktan sonra Good4Test’i başlatın.' >&2
fi

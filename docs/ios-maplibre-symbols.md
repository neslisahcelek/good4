# MapLibre symbols in iOS archives

MapLibre 6.17.1 ships its device dSYM separately from the CocoaPods XCFramework.
Without that dSYM, distribution reports `The archive did not include a dSYM for
the MapLibre.framework`. The app's Release configuration already uses
`dwarf-with-dsym`; changing that setting cannot create the vendor's symbols.

The Podfile now runs `iosApp/scripts/maplibre-dsym.sh prepare` with the resolved
MapLibre version. It downloads the official device symbols, verifies their arm64
UUID against the installed framework, and caches them under `iosApp/Pods/MapLibre`.
Existing matching symbols are reused. This also runs in Xcode Cloud's existing
`ci_post_clone.sh` CocoaPods installation step.

The app's `[MapLibre] Copy Archive dSYM` build phase runs after CocoaPods embeds
frameworks. During a device Archive it verifies the shipped framework's UUID and
copies `MapLibre.framework.dSYM` into `DWARF_DSYM_FOLDER_PATH`, which Xcode collects
into the archive's `dSYMs` folder. Regular builds and simulator builds skip this
step. Missing or mismatched symbols fail the archive with an actionable error.

After pulling this change:

```sh
cd iosApp
pod install
```

Create a new archive in Xcode and distribute it. Existing archives are unchanged.
In Organizer, use **Show in Finder**, then **Show Package Contents** to confirm
that `dSYMs/MapLibre.framework.dSYM` is present. To verify an archive manually:

```sh
xcrun dwarfdump --uuid "/path/to/Good4.xcarchive/Products/Applications/iosApp.app/Frameworks/MapLibre.framework/MapLibre"
xcrun dwarfdump --uuid "/path/to/Good4.xcarchive/dSYMs/MapLibre.framework.dSYM"
```

The arm64 UUIDs must match. The official 6.17.1 binary and dSYM both have UUID
`7F6D991E-F317-33B4-9978-0C3EE559C2B3`.

References:
- https://github.com/maplibre/maplibre-native/issues/3155
- https://github.com/maplibre/maplibre-native/releases/tag/ios-v6.17.1

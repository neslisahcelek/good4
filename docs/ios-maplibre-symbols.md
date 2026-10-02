# MapLibre symbols in iOS archives

MapLibre 6.17.1 ships its device dSYM separately from the prebuilt XCFramework.
The SPM dependency is pinned to 6.17.1 so the framework and symbols match.
Without that dSYM, distribution reports `The archive did not include a dSYM for
the MapLibre.framework`. The app's Release configuration already uses
`dwarf-with-dsym`; changing that setting cannot create the vendor's symbols.

The app's `[MapLibre] Copy Archive dSYM` build phase runs after Xcode embeds
frameworks. During a device Archive, `iosApp/scripts/maplibre-dsym.sh` downloads
the official device symbols, verifies their arm64 UUID against the embedded
framework, and caches them under `DERIVED_FILE_DIR/good4-maplibre-dsyms/6.17.1`.
Existing matching symbols are reused. The archive step does not infer the SPM
directory from `BUILD_DIR`, whose layout changes during Archive, and also works
with a custom `-clonedSourcePackagesDirPath`. It copies the symbols into
`DWARF_DSYM_FOLDER_PATH`, which Xcode
collects into the archive's `dSYMs` folder. Regular and simulator builds skip
this step. Missing or mismatched artifacts fail the archive with an error.
The first archive needs access to GitHub to download the symbols.

Open `iosApp/iosApp.xcodeproj`, resolve Swift packages and choose `iosApp Prod`.
Xcode Cloud resolves packages in `ci_post_clone.sh`; the archive build phase
prepares the symbols automatically. CocoaPods is no longer required.

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

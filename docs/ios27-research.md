# iOS 27 metadata research — RC8

Local user-supplied archive: ios27-carplay-metadata.tar(1).gz. Metadata only; do not redistribute the dump or infer implementations from declarations.

## Theme evidence

CarKit Objective-C dump contains CRVehicleAccessory.sendWiredCarPlayAvailable:usbIdentifier:wirelessCarPlayAvailable:bluetoothIdentifier:themeAssetsAvailable:, supportsThemeAssets and themeAssetIdentifier. CARThemeAssetVersion has separate iOSContentVersion and accessoryContentVersion properties. CARThemeAssetLibrarianObserving has notifyFoundNoMatchingAssetForVehicleIdentifier:nextRequiredCompatibilityVersion:requestDescription:. CarKit strings explicitly associate wallpaper lookup with themeData and a display identifier. These establish separate capability, compatibility and rendering concepts, not their wire encoding.

WCP pinned 3790d63d uses legacy transport group 24 parameters 5 and 6 with the full tuple; the alternate LIVI-style implementation uses parameter 6 with the asset identifier. WCP also handles 0x4300 availability and sends 0x4301 when armed. Its handler currently ignores availability payload contents. The Apple method declaration does not establish a particular iAP2 parameter number or type. Preserve the working legacy identity; do not infer an extra wire flag from a method name.

Catalog probe now reports compatibility-related metadata for all matching asset entries while retaining exact-version selection. Compare catalog versions against the registered tuple before proposing changes. Runtime phone acceptance remains unverified.

## Next steps

- Stage 4: correlate asset identity, compatibility and display-specific theme data; inspect actual availability message metadata and phone-side logs if available. No speculative identity changes.
- Stage 5: reproduce ordinary unprotected video; RC7 protected stream failed with unsupported DRM after decoder initialization. No attempt to bypass content protection.
- Then revisit radio and climate using Apple declarations and existing K5 evidence. Preserve verified passenger heating and FM controls.
- Stage 6: seven-package signed bundle including Standalone TBT, after acceptance of the development checkpoint.

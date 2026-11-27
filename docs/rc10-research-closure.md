# RC10 incremental evidence review

Development checkpoint: a9d0842e96cef4438d583a2f8bdb13211e7ac602. Runtime baseline 42594f784fe8afb7858606647e6310f3eb6e7f6d passed regression run 45931795153. This document does not authorize publication or WCP final audit.

## Incremental iOS 27.0.1 (24A446)

Reviewed the supplied split incremental archive's member inventory, decoded CAF identifier catalog, AirPlaySender/AirPlayReceiver declarations and the following extracted method bodies:

- disassembly/transport/AirPlayReceiverSessionTearDown.txt: entry 0x2517a429c; byte load at 0x2517a42cc and conditional branch at 0x2517a42d0, followed by further conditional teardown paths. The byte's meaning and initiating event are not established.
- disassembly/themes/CARThemeAssetVersion-compare.txt: entry 0x1c254c2e0; multiple conditional paths at 0x1c254c324, 0x1c254c338, 0x1c254c34c and 0x1c254c360. Unresolved calls prevent assigning exact comparison precedence.
- disassembly/themes/CARThemeAssetLibrary-queue-findCurrentAsset.txt: entry 0x1c254d61c; conditional paths at 0x1c254d698, 0x1c254d6e0 and 0x1c254d6e4. No proven alternate accessory identity or asset-selection wire field.
- disassembly/themes/CRAssetWallpaperData-asDictionary.txt: entry 0x1c255bfa4; null checks at 0x1c255bfd8, 0x1c255bfec and 0x1c255c010. This does not establish a head-unit wallpaper dictionary contract.
- disassembly/themes/CRVehicleAccessory-sendCarPlayAvailable.txt: entry 0x1c2558cb0; distinct argument registers are preserved. No proven iAP2 parameter number or encoding.
- AirPlaySender headers declare APCarPlayPolicyMonitor endpoint authentication and vehicle policy transitions, and APEndpointManagerCarPlayDelegate endpoint allowance. These declarations do not identify the cause of the K5 first-frame disconnect.
- constants/catalog/caf-identifiers.csv confirms CAFServiceTypeSeatFan 0x22000002. Retain previously verified seat identifiers and existing schema; see rc9-seat-evidence.md.

## Release disposition

No new runtime change follows from the above evidence. Keep current theme tuple and experimental gates, parked-only playback, and existing media lifecycle protections. Do not infer new wire encodings, send extra first-frame acknowledgments, bypass content protection, or change endpoint authorization.

HD multicast wire encoding, SXM display numbering and source activation, actual seat/passenger actuation, theme asset selection, wallpaper rendering and AirPlay disconnect causality remain unresolved runtime evidence questions. The existing test plan records these as unverified; they are not proof of defects in the new corrections. Physical testing is excluded from RC10 development readiness.

The remaining comprehensive source audit and companion compatibility review are separate gates. Do not publish, change companion pins, or authorize WCP's final audit before the CPUL candidate is frozen. No fresh archive extraction is requested on the basis of this review.

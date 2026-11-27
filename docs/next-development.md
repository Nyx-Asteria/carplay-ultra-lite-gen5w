# Next testing release — development notes

## Theme appearance (center display only)
- Goal: populate native Theme Selector with all compatible assets, Midnight initial default, user choice retained.
- First experiment: request the existing, source-backed Modular/blue layout during the virtual cluster handshake, only for Cluster_Display. This is a bootstrap hypothesis, not proof of center-screen palette support. Midnight identifier and center-display layout metadata are not verified; do not invent them or claim the selector is fixed.
- No physical cluster changes. Preserve current wireless provider, display IDs and DBX cabin model.

## Climate
- Current combined seat range -3..3 is advertised but single-stage heating and missing ventilation are reported; feedback reads -1. Need OEM dump and actual service metadata to determine correct getters and layout.
- Wheel control overlaps HVAC and does not actuate hardware. Initial defensive fix records a pending press only after the binder transaction returns successfully. This is not hardware confirmation.
- Investigate separate seat fan services and explicit layout metadata against original schema before editing production service types.

## Deferred / dependencies
- Session teardown crashes deferred at user's request; do not change working wireless negotiation.
- Native volume/media popup suppression and radio integration require firmware dump (SystemUI, Mobis_SystemPopup, HKMC_MediaGate, framework2, media_cmd and OEM USM classes if available).
- Parked video remains unverified; keep safety interlock.
- Do not publish until changes reviewed and tested. Keep current release version during development.

## Seat implementation checkpoint
- Upstream feature/climate-and-video at 70e3ba245987e5b9c7b9f1ae75a024a383e5fe08 provides Layout A: SeatFan service 0x22000002, FanLevel 0x31000012, front service bases 1041/1045. Restore front-only schema and feedback/command mapping, no rear services.
- Actual K5FirmwareDump.zip DEX method tables confirm IUSMService getFrontSeatHeat_DrvState/AstState ()I and setFrontSeatHeatDrvStep/AstStep (I)V. Service bytecode delegates setters to sendFrontSeatHeat(II)V with driver selector 0 and passenger selector 1. Getter implementations use selector values 0x114 and 0x115. This establishes the interface, not the hardware state/step semantics. BCM has distinct seat status listener interfaces; investigate callback values before substituting feedback.
- Separate UI is an experiment, not validated on iOS. Hardware feedback returning -1 and single-stage actuation still require investigation. Do not assume matching method signatures prove correct runtime behavior.
- Order: seats, wheel, then themes; Midnight initial default, retain user selection.

## Hardware control checkpoint — Comfort reference
- ComfortEngine delegates seat_heat, seat_vent and wheel_heat to HMG-API. HMG-API a1830a83 Hub.java / Commands.java use BCM getDrvSeatHeatState/getAstSeatHeatState, USM setFrontSeatHeatDrvStep/setFrontSeatHeatAstStep, and VR set_StrgWhlHeated_Cmd_AVN(int) with BCM onRemoteTeleHeatingStatusChanged feedback.
- Those symbol names and transaction field strings exist in the uploaded K5 automotive.odex. Reuse CPUL's existing Binder transport, checked method descriptors and listener fingerprint; do not assume Comfort app's daemon-client authentication works inside the DiPO hook process.
- Replace USM seat reads with BCM; filter unknown states outside 2..8. Replace wheel toggle with explicit VR 0/1, no guessed optimistic indicator; use existing verified BCM callback and retry registration after failure. Seat setter names unchanged (same as Comfort).
- No on-car validation yet. Wheel placement and theme selection still outstanding; Midnight remains required initial appearance.

## Remaining development order (owner confirmed)
- Themes first: native selector, all compatible layouts/palettes and wallpapers, Midnight initial theme and a matching initial wallpaper; preserve subsequent user choices. No physical cluster changes. The six UiSync definitions are cluster layout UUIDs with example palettes, not a complete center-display theme or wallpaper catalog. The original bundled CPUL source references the phone's theme settings and wallpapers following the active layout/palette; it does not include the asset package.json/theme.json or a verified Midnight/wallpaper identifier. Do not fabricate IDs or repeatedly force a default over the user's choice.
- Volume/media popup suppression next: use captured OEM binaries to locate pre-presentation path; preserve audio controls and stock behavior outside CarPlay.
- Radio last: investigate OEM tuner integration and source/state feedback.
- Climate layout minor tweaks deferred; seat and wheel implementation still requires parked hardware validation. Preserve working wireless negotiation and do not trigger a release during development.

## Consolidated Ultra profile
- Owner requested two default-on switches: CarPlay Ultra (identity, session start, virtual cluster, themed IDs, DBX, original full feature mask and enhanced RequestCarUI) and Ultra themes/UI synchronization (dependent on Ultra). Keep existing keys ultra_theme and ultra_ui_sync to preserve saved selections; an existing explicit off remains off. Retired individual experiment keys are no longer read. This is not proof the complete profile is safe on every firmware; validate on K5 while parked.
- Midnight and matching wallpaper remain requirements, not implemented defaults. Upstream tree at 70e3ba2 has UiSync.java, UiSyncChannel.java and UiSyncTest.java but no theme asset catalog files by name. Continue searching source, not asking owner to provide identifiers.

## Theme catalog diagnostic
- Standalone path-triggered workflow, no compilation or release. Reproduces LIVI f84638ed theme_catalog.rs request and pinned Apple root trust; selects only 44C9FA31EC version 985, reads the ZIP by range and inspects package.json/theme.json plus filenames. No graphical assets downloaded. Results are diagnostic artifacts, not bundled into CPUL. Do not assume Midnight or wallpaper identifiers until results are inspected.

- Initial catalog diagnostic failed in TLS validation during the POST (expired certificate in the pinned trust chain). Retry with the current runner OS trust store, retaining full TLS verification; never disable certificate or hostname checks. LIVI cluster.rs uses fetched metadata to select receiver-side layout/palette and map components; that alone does not establish that fetching metadata on the car is required for the phone's theme selector. Investigate negotiated theme identity, display IDs and UI sync before adding a runtime network dependency.

## Theme synchronization implementation checkpoint
- Successful diagnostic run 45543108686 artifact 14989418877: Apple 44C9FA31EC v985 package and theme manifests. Both displays 1920x720. Center Wallpaper primary UUID 9E19BC4E-5251-5821-A0BA-26479A79F722 has zero zones, NoStyle offers blue only. Midnight is valid for Modular, DigiRing, DigiBand and DigiBandWithTicks; not NoStyle, MapsMedia or Cobranded. Center wallpaper IDs include gradient-Midnight, landscape-Midnight and topography-Midnight. Apple NoStyle default wallpaper is black, not gradient-Midnight.
- Implement metadata-derived layout/palette constraints, initial virtual Modular/midnight, center Wallpaper/blue request with empty activeComponents, and locally persisted phone layout/palette choices. Center request is an experiment using LIVI request_layout format, not proven on K5. No physical cluster changes. LIVI ui_sync.rs does not define wallpaper selection messages, so gradient-Midnight remains desired default only; do not invent a wallpaper field. Phone manages actual wallpaper selection. Test protocol changes before release.

- Owner clarified center display is the priority, not virtual cluster customization. Keep cluster negotiation needed by Ultra; no physical cluster changes. Center wallpaper manifest has 28 entries but only 25 distinct IDs (vibrant aliases three existing wallpapers). NoStyle default palette blue and wallpaper black; desired gradient-Midnight selection is still not established by LIVI protocol. Development regression workflow compiles CPUL and runs ClimateMapTest/UiSyncTest without publishing. Release workflow now includes the same UiSyncTest.

## OEM volume presentation
- K5 automotive-service.odex MD5 04fd3c2e9bc7444c2ae812b57a83ac5d. Private void MicomVolumePanel.onShowVolumeChanged(int streamType,int flags) and onShowVolumeChangedNew(int volume,int imageResId,int imageType,int stringId,int minVol,int maxVol,int flags). Hook presentation only; no volume, mute, routing or general warning changes.
- Exact-firmware experimental low-level SDK Hooks helper outside HookPlan arbitration; proper mapped HMG replace points still needed for distribution. Protected DiPO system-UID readiness lease, OEM connected state and foreground required. Dedicated toggle; delayed fallback retained. No media popup changes. Needs car validation.

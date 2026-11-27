# K5 RC11 vehicle validation

Development candidate — not a claim of vehicle verification. See the mandatory [pairing and theme matrix P1–P8](theme-pairing-validation.md); include this document in the testing bundle. Target: 2022 Kia K5 EX, DL322 USA 260507, API 19, HMG-API RC10 and Nemesis 1.17.1. Confirm final companion versions against the signed release manifest/changelog before installation. Keep a known-good rollback configuration.

## Baseline and recovery
- Confirm native wireless projection is functional, with required Wi-Fi features enabled and no unresolved external-file conflicts. Do not bypass recovery safeguards.
- Preserve working settings and logs. Avoid the reported USB dongle/Ultra sidebar crash combination.
- While parked, confirm normal CarPlay, audio, touch, native applications, sidebars, disconnect/reconnect and standard CarPlay fallback. Check Park enforcement.
- Export bounded CPUL, WCP and STBT session logs to USB; record matching timestamps and build versions. Do not collect credentials.

## Climate and seats
- For each front seat independently, test off → heat 1/2/3 → off, off → ventilation 1/2/3 → off, heat ↔ ventilation, and physical button changes.
- Compare physical actuation, BCM readback and CarPlay rendering separately; test both immediate-feedback settings, one variable at a time. Include initial panel opening after a seat write, and reconnect during feedback reconciliation.
- If using the optional combined four-step layout, verify -4 reaches high ventilation, -3/-2 medium, -1 low, and positive heating steps remain unchanged.
- Test driver/passenger target temperature independently, sync on/off, Fahrenheit/Celsius, and reconnect. Record native callback state and raw passenger setpoint correlation.

## Radio, audio and Vehicle page
- Test FM activation from CarPlay and OEM-first, seek, rapid tuning, catalog recovery, HD1 metadata/artwork and source switching. HD multicast control remains unverified.
- Test SXM only with authorized subscription; compare catalog, display number/name, native tune, seek and song/artist/artwork. Do not bypass skip, lock or subscription restrictions.
- Verify volume knob, steering keys, mute, Siri, call, navigation, FM and SXM source levels; OEM volume panel fallback must remain available. While parked, reconnect during rapid slider changes and verify old queued volume/mute commands do not act on the replacement session.
- Compare read-only Bose status to OEM settings for CarPlay, FM and SXM; test source changes and reconnect. No setter is provided.
- Verify range, odometer, units, shortcuts, optional Comfort controls and Vehicle page replay on reconnect; record missing or stale values.

## Themes and parked AirPlay
- Run P1–P8 in the pairing/theme matrix; preserve explicit phone preference, existing pairing first, fresh pairing only with consent. Keep UI Sync off initially. Compare actual selector, center/cluster layout, wallpaper, native apps and standard fallback. Distinguish identity builder evidence from dispatch, transfer and installation.
- While parked only, test ordinary unprotected media and picture-only diagnostic separately. Correlate first frame, readiness, transport teardown and session finalization. Stop if projection becomes unstable. No DRM or Park bypass.

## Companion integration
- Keep one navigation publisher: disable WCP cluster/HUD TBT before enabling Standalone TBT, retain shared identity and wireless/audio functionality.
- Test Apple Maps, Google Maps and Waze individually; do not infer Waze support from other applications.
- Verify PPS scaling and touch alignment with CarPlay; Android Auto remains separately unverified.
- Verify collector USB export and compatibility check, using incremental collection only for newly requested files.

## Reporting and rollback
Record each test as pass, fail or untested with build versions, relevant setting values and session log timestamps. Do not equate CI success with vehicle success. Revert individual experiments first, then restore the known-good signed bundle if needed. RC11 release readiness excludes physical testing; this checklist is for evidence after the candidate is built.

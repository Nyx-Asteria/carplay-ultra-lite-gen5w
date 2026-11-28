# K5 RC11 vehicle validation

Development candidate — not a claim of vehicle verification. See the mandatory [pairing and theme matrix P1–P8](theme-pairing-validation.md); include this document in the testing bundle. Target: 2022 Kia K5 EX, DL322 USA 260507, API 19, HMG-API RC10 and Nemesis 1.17.1. Confirm final companion versions against the signed release manifest/changelog before installation. Keep a known-good rollback configuration.

## Baseline and recovery
- Confirm native wireless projection is functional, with required Wi-Fi features enabled and no unresolved external-file conflicts. Do not bypass recovery safeguards.
- Preserve working settings and logs. Avoid the reported USB dongle/Ultra sidebar crash combination.
- While parked, confirm normal CarPlay, audio, touch, native applications, sidebars, disconnect/reconnect and standard CarPlay fallback. Check Park enforcement.
- Export bounded CPUL, WCP and STBT session logs to USB; record matching timestamps and build versions. Do not collect credentials.

## Climate and seats
- Owner reports intermittent physical driver/passenger heating response in RC10 (no logs). Record a successful and failed request under the same settings while parked, then repeat with legacy heated-seat compatibility enabled after restart. Compare per-side arbitration input/output (`selectedRaw=none/2/6/7/8`), queued command and generation, USM dispatch and BCM feedback. Distinguish UI-only changes from actual seat heat indicator/physical actuation. Do not infer root cause from a single observation or disable session/vehicle guards.
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
- Baseline tests use FileTransfer disabled/unavailable. A future compatible WCP RC7 experiment is a separate owner-approved comparison; no requirement for CPUL release readiness or asset installation.
- Keep one navigation publisher: disable WCP cluster/HUD TBT before enabling Standalone TBT, retain shared identity and wireless/audio functionality.
- Test Apple Maps, Google Maps and Waze individually; do not infer Waze support from other applications.
- Verify PPS scaling and touch alignment with CarPlay; Android Auto remains separately unverified.
- Verify collector USB export and compatibility check, using incremental collection only for newly requested files.

## Reporting and rollback
Record each test as pass, fail or untested with build versions, relevant setting values and session log timestamps. Do not equate CI success with vehicle success. Revert individual experiments first, then restore the known-good signed bundle if needed. RC11 release readiness excludes physical testing; this checklist is for evidence after the candidate is built.

## Optional passive RemoteControl discovery (WCP Stage 1)
Enable RemoteControl SETUP Discovery only for a targeted parked test, restart DiPO, keep session logging and USB export enabled. Capture existing type-130 attempts and export `[projection-discovery]` records; do not force a new negotiation or enable FileTransfer. Compare baseline with setting off. Known handlers and unknown stock rejection remain unchanged. Maximum 32 unique sanitized metadata fingerprints per DiPO startup; duplicates/hash collisions, lock contention and I/O failure may omit observations. Only validated canonical clientTypeUUID is recorded; clientUUID, channelID, seed and allowlisted SETUP fields are type-only. Flags and controlType report bounded values when recognized; numeric suffixes and raw CF type IDs are hexadecimal. Unknown field names/values omitted. Unknown UUIDs are candidates only. No actual phone traffic verified in development. Disable and restart after testing.

## RC11 verification and next-cycle naming
Verify exact candidate development CI, actual stock firmware gates and regenerated recipe before signing. Owner-reported intermittent front seat response is evidence of occasional actuation, not a confirmed arbitration root cause. Record matching successful/failed commands and BCM feedback. The RC10 Maps prompt referenced LIVI; no plugin identity change in RC11. Next development series is 3.0.0-rc.1, with CPUL-authoritative versioning; this does not alter current acceptance or package versions.

## RC11 final matched-source audit
WCP RC7 3c3d2489b5bd4b8f4b8d37a5a451341701f78bce and STBT RC4 fac2ec367028a97334d548c53d5930d768ec2694 are the exact release pins. WCP final run [46090592763](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa/actions/runs/46090592763) all 3 jobs success; STBT [46090509762](https://github.com/Nyx-Asteria/Standalone_TBT/actions/runs/46090509762) both jobs success. Inspected actual API source blobs: CarPlayIdentification.java unchanged (dad090c4c8c313e8d4badccc9b1b418de2cada0e). Independently parsed final recipe directives: CPUL/WCP CarPlayCore 16 vs 6 and 3 stock ranges, WCP/STBT iAP2 4 vs 4, all verification/write pair intersections empty. All three AA recipe normalized directive sequences match. Not an on-car test or mapped-segment audit. WCP bridge default true, integrated tbt false, K5 gasoline identity explicit opt-in false by default; enable K5 identity on matching target for Ultra. Retain single STBT navigation publisher. All three manifest floors >=1.1.0-rc.10. WCP experimental quarantine components present but live FileTransfer and export inactive; Stage 2 not included. Final matched-source build repeats regression and signs all seven packages with permanent key. Owner authorized automatic publication only on successful gated build. No companion source edits.

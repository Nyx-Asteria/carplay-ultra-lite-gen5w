# RC10 development backlog

Baseline: RC9 vehicle Session A, HMG-API RC10 SDK migration. Comfort only adjusts seats at startup per K5 owner; not a primary suspect. No release until authorized.

## Logging and release assurance
- [x] IPv4/MAC/IPv6 redaction and missing theme setting metadata; development CI passed.
- [x] Seat Binder acceptance vs BCM observation, supersession and generation diagnostics (CI pending).
- [x] Bound pre-session projection events to 10 seconds, retain native timestamps and reject events before previous teardown (CI pending; no native session identifier available).
- [x] Capture allowlisted offered/enabled projection features and independent UI Sync outcome; native video/HEVC offer flags added (CI pending).
- [ ] Reduce navigation characteristic noise in climate logs; record actual firmware identity and CCNC/LivePatch environment.
- [ ] Surface writer/export failures; distinguish delivery queued, socket success and phone acknowledgement.
- [ ] Companion WCP/STBT legacy cmp compatibility, export dedup/rate limiting and legacy utility tests (coordinate separate repositories).
- [ ] Release regression parity with development CI; compile Comfort in nonpublishing bundle check; update companion pins after fixes.
- [ ] Incremental firmware dumper only collects new missing targets; avoid duplicate APK/ODEX collection.

## Climate and vehicle
- [ ] Resolve seat actuation regression, compare feedback experiments independently.
- [x] Guard queued seat commands by original session generation (CI pending).
- [ ] Resolve ventilation UI feedback using confirmed iOS identifiers; no speculative unsupported auto mode.
- [ ] Passenger temperature request/converted commands and fresh DATC callback correlation implemented (CI pending); verified K5 setter still needed.
- [ ] Fuel range validity and missing HMG-API/Nemesis shortcuts.
- [ ] Bose source-dependent setting; clear stale diagnostic sample on read failures, no forced setter.

## Radio
- [ ] FM catalog publication, seek, metadata/artwork and direct activation reliability.
- [ ] HD2–4 discovery and control only with verified protocol evidence.
- [ ] SXM channel identity mapping, source activation, delayed callback rejection, song/artist/artwork and seek validation.

## AirPlay and themes
- [ ] First-frame session collapse: correlate WCP/CPUL timestamps and playback state; fix generation race in Park-loss stop.
- [ ] Keep parked-only restriction, no DRM bypass.
- [ ] Center display negotiation, Midnight/wallpaper and theme catalog; isolate experiments.
- [ ] UI Sync interleaving/no-display tests and legacy compatibility; no physical cluster changes requested.

## Compatibility
- [ ] Vehicle test Nemesis 1.17.1 + RC10 and stock CCNC, especially OSD overlap, mounted framework and launcher shortcuts.
- [ ] No projection scaling retest required by owner; STBT navigation tests separate.

## RC11 stage 1 (owner requested; after RC10)
- Inventory and refactor source by subsystem; preserve package identities and runtime behavior.
- Consolidate development commits into meaningful reviewable units; preserve published release tags and create recovery refs before any history rewrite.
- Review build and test infrastructure; verify identical artifacts or explain intentional differences.
- Do not rewrite active RC10 history; agree migration scope before changing shared branch history.

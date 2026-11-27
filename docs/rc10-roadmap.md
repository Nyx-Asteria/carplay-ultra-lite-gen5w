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
- [ ] Bose source-dependent setting; no forced setter.
- [x] Clear cached Bose diagnostic sample and reset poll generation on audio read failure (CI pending).

## Radio
- [ ] FM catalog publication, seek, metadata/artwork and direct activation reliability.
- [x] Bind FM seek to latest source/station request and log unsupported seek return type (CI pending); physical seek response remains unverified.
- [x] Preserve nonempty FM cache on transient empty poll or list accessor failure while tuner remains available; log catalog subscription/notifier queue status (CI pending).
- [ ] HD2–4 discovery and control only with verified protocol evidence.
- [ ] SXM channel identity mapping, source activation, song/artist/artwork and seek validation.
- [x] Do not publish Smart Favorites track snapshots as live song/artist; log observed ID membership in eligible catalog (CI pending).
- [x] Reject live metadata callbacks for mismatched observed channel and out-of-order delivery (CI pending); early callbacks before tuner observation may be withheld, same-ID delayed callbacks remain ambiguous.

## AirPlay and themes
- [ ] First-frame session collapse: correlate WCP/CPUL timestamps and playback state; generation-aware queued stop protection implemented (CI pending).
- [ ] Keep parked-only restriction, no DRM bypass.
- [ ] Center display negotiation, Midnight/wallpaper and theme catalog; isolate experiments.
- [x] Test actual UI Sync panel router with interleaved center/cluster/legacy frames and independent reconnects (CI pending); no physical cluster changes requested.

## Compatibility
- [ ] Vehicle test Nemesis 1.17.1 + RC10 and stock CCNC, especially OSD overlap, mounted framework and launcher shortcuts.
- [ ] No projection scaling retest required by owner; STBT navigation tests separate.

## RC11 stage 1 (owner requested; after RC10)
- Inventory and refactor source by subsystem; preserve package identities and runtime behavior.
- Consolidate development commits into meaningful reviewable units; preserve published release tags and create recovery refs before any history rewrite.
- Review build and test infrastructure; verify identical artifacts or explain intentional differences.
- Do not rewrite active RC10 history; agree migration scope before changing shared branch history.

- [x] AirPlay player-specific listeners and generation-bound audio fallback; CI pending.

- [x] Preserve safe display registration and screen SETUP identities in native/session diagnostics; no arbitrary peer UUIDs (CI pending).

- [x] HMG-API vehicle shortcut: RC10 manifest confirms MAIN without LAUNCHER; explicit hmgapi.app.MainActivity fallback, resolve before launch. Nemesis entry point still needs evidence; vehicle verification pending.

- [x] Passenger temperature dispatch uses same session-generation guards as seat commands, including immediately before Binder transaction. Existing Celsius/Fahrenheit conversion confirmed against ClimateMap; OEM setter unchanged. CI and vehicle validation pending.

- [x] FM/SXM pending tunes require a live activation request; clear failed/expired/fingerprint/signature-mismatched activation requests before delayed tune. No new source IDs; CI and vehicle validation pending.

- [x] Expose writer and USB export failure/recovery transitions to HMG runtime log, deduplicated every five seconds; label Android build separately from actual firmware. Actual firmware/LivePatch identity still outstanding.
- [x] Extend source-request expiry, cancellation and stale-generation regression cases.

- [x] Shared host regression script used by both development and signed bundle; include video session policy and all logging/radio/seat tests. Signed bundle still gated; verify development CI before release.
- [x] Pin incremental collector 55e8de065f32776ab9eb466207364e0bb66bf29b (collector CI passed); other companion pins remain pending coordination.

- [x] Report supported firmware target, verified startup preflight, Android build, CCNC launcher package presence and exact-file mount evidence for four known LivePatch targets. Mount ownership and actual OEM firmware version are not independently inferred; CI pending.

- [x] Consolidate eight diagnostic switches into detailed session diagnostics (default on) and protocol metadata diagnostics (default off); keep all experimental behavior switches. Existing per-category preferences are superseded by these new defaults. Suppress DEBUG/TRACE in shared HMG log while persistent writer is enabled; CI pending.

- [x] Navigation characteristic changes use metadata-only protocol diagnostics, not ordinary climate logs; no route values emitted by these paths. Schema-based regression coverage; CI pending.

- [x] Theme contribution handles provider replacement and attempts rollback on registration callback failure (WCP IdentityRegistry inserts before notifying). Native activation still unverified; no theme identity or protocol change. CI pending.

- [x] Distinguish confirmed registry contribution from uncertain ownership after failed registration/withdrawal; uncertain state must be withdrawn successfully before re-registration. CI pending; native activation remains separate.

- [x] Extract production theme ownership state and regression-test confirmed, uncertain, replacement and withdrawal transitions in the shared CI suite. Reflective provider/native integration still needs runtime validation.

- [x] FM station name, artwork and individual HD tags reject older callback timestamps within the observed frequency/session scope; additional generation rechecks and host policy tests. Same-frequency delayed callbacks after a retune still need source-side evidence. CI pending.

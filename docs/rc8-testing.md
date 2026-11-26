# CarPlay Ultra-Lite K5 1.0.0-rc.8 — prepared testing release

**Development preparation only.** Target DL322 USA 260507, Gen5W_M, Android 4.4.2/API 19, HMG-API >=1.1.0-rc.8. RC7 remains the published baseline until the release is explicitly authorized. Host regression passed on 18101ed; subsequent packaging changes require the signed workflow. Perform interaction while parked; use a passenger for any road navigation observations.

## Full changelog since RC7

### Radio and audio
- Native IRadioService FM seek replaces injected keys; retain source/session checks. Route volume to confirmed FM/SXM audio streams.
- Diagnose OEM scan rejection counts and merge observed frequencies when cached scan results are empty. Bridge frequency-matched SIS station names, primary-program ID3 title/artist/album and bounded PNG/JPEG station logos.
- Experimental SiriusXM source activation, channel feedback, Smart Favorites metadata and catalog, native direct tuning with subscription/skip/lock checks, full decoded RadioNA catalog observer and cached artwork. Request OEM catalog asynchronously, share snapshots, retain full catalog when favorites are unavailable, clear session caches, trace getter and eligibility results. No entitlement changes.
- Read-only Bose Centerpoint availability/state diagnostics; no amplifier writes.
- Passive HD SPS method tracing in the existing two-minute radio diagnostic window; actual HD2/HD3 control and Apple multicast encoding remain unverified.
- Add radio policy regression coverage.

### Climate
- Preserve pre-write and actual BCM seat state; explicit power-off takes precedence over retained levels, and opposite-mode idle writes do not disable active seats. Resolve one native command per seat per request.
- Trace resolved commands and pre-dispatch BCM/USM values; withhold disconnected seat commands.
- Publish paired subscribed ventilation feedback for physical and phone changes; authoritative read/register values, deduplication, typed complete feedback validation and IID uniqueness checks.
- New opt-in Seat Ventilation Immediate Feedback experiment (default off), with later BCM reconciliation. Add regression assertions for malformed feedback and both timing paths.
- Driver heating reversion and ventilation visual state remain unresolved until vehicle testing.

### Ultra, themes and video
- Preserve legacy DBX identity and receiver 950.7.1; supplement existing center view-area metadata without changing geometry. Log unclaimed stream types, optional screen SETUP identifiers and alternate display registration.
- Record theme compatibility catalog metadata and research leads. Empty theme selector, black wallpaper and missing center handshake remain unresolved. No alternate identity encoding introduced.
- Distinguish DRM playback errors from transport/decoder errors. Hardware decoder initialization observed in RC7; first frame of ordinary unprotected media still unverified. Park and active-session gates retained.
- Retry transient Maven dependency downloads and cache checksum-pinned dependencies.

### Companion bundle
- WCP 2.2.1-rc.4 (23c1983): retain working wireless projection, shared identity and Now Playing; documentation/rollback updates. STBT 1.0.0-rc.1 (2533e82): lifecycle/subscription recovery, parser improvements and opt-in Waze recovery; no claim Waze works.
- PPS 1.0.0 (150962a), Comfort 1.1.8-cpul.rc.1 (2724ac5), firmware dumper RC4 development (f786403), CPUL Check. Dumper retains incremental navigation/iAP2 targets and adds opt-in CPUL log export.
- Seven packages, common permanent signing key, signature verification and checksums; normal non-draft release marked Latest when authorized. No signed RC8 bundle exists yet.

## Install, settings and rollback
Keep RC7 and known-good companion ZIPs and record saved settings. Verify author identity and checksum; old test-key packages may require removal/apply before replacement. Keep known-good HMG-API and wireless Wi-Fi configuration. Remove original Phone Projection TBT before installing the renamed fork. Disable WCP cluster/HUD TBT, apply/restart; leave wireless bridge, audio, shared identity and Now Playing on. Never run two navigation publishers. Start with CPUL integration, Ultra and Sidebars on; UI Sync, forced wallpaper, SXM, immediate seat feedback and parked video off. STBT Waze experiment off. Start PPS at 100%. Do not change unrelated settings during a comparison. Restore baseline on failure; if necessary remove experimental packages and reinstall known-good signed packages.

## Test groups
After each group, insert USB and toggle firmware dumper **Export CPUL runtime diagnostics** OFF then ON. Save the timestamped cpul-diagnostics folder, inventory, WCP logs and relevant TBT-LOGS. HMG-API rotates at 256 KB: export promptly. If zero matches or unreadable files, use HMG-API log export/problem recording and retain original logs. Record exact settings, times, physical observations and app actions. Redact identifiers, destinations and media details before sharing privately.

### A. Wireless baseline and scaling
- [ ] Cold connect, automatic opening, reconnect after disconnect; record latency without treating it as fixed.
- [ ] Normal phone audio, Siri, calls, Now Playing track changes, physical and on-screen volume, native apps and sidebars.
- [ ] PPS 100/85/75/50%, touch alignment, native climate/radio and return to baseline. Android Auto not tested here.
- [ ] Export CPUL and WCP logs.

### B. Climate baseline and seats
- [ ] Verify temperatures, sync, blower, airflow, auto, A/C, recirc, defrost, off/on and wheel heater.
- [ ] Temporarily disable Comfort automatic climate for isolated seat testing. Passenger and driver separately: heat high/medium/low/off with at least three seconds between steps; no physical input during phone-command sequence.
- [ ] Repeat using physical buttons; compare raw BCM/USM, CarPlay icon and physical indicator.
- [ ] Repeat for ventilation, then heat→vent→heat on each seat. Reconnect with ventilation active.
- [ ] Export logs with [seat], [seat-dispatch], [seat-fan], VS read/register and notification lines.
- [ ] Enable immediate feedback only, restart, repeat ventilation tests; export separately. Restore off and Comfort preference.

### C. FM and HD
- [ ] Native FM source, numeric tune, physical and onscreen seek, scan list and observed fallback, SIS/ID3/artwork; verify clearing and return to phone audio.
- [ ] Arm Vehicle > Radio > Collect radio diagnostics (2 min). In native radio, tune HD station, wait for HD1, select HD2 then HD3 if available, return HD1, tune away and back. Repeat window if necessary.
- [ ] Record [radio-meta] SPS callbacks, raw arguments, monotonic timestamps and missing hooks. No experimental multicast tuning command enabled.
- [ ] Export logs.

### D. SiriusXM and Bose
- [ ] Enable SiriusXM Discovery and restart; verify source appears, activation, catalog, channel and metadata, one subscribed unlocked direct tune, artwork, physical seek, volume and return to phone.
- [ ] Test onscreen seek separately; unimplemented behavior is not a failure of physical seek. No changes to entitlements or locks.
- [ ] Compare Centerpoint diagnostic with stock off/on setting across FM, SXM and phone audio where offered. Restore preference.
- [ ] Export logs; disable SXM and restart if unstable.

### E. Navigation (STBT)
- [ ] Confirm WCP navigation publisher disabled. Apple Maps, Google Maps, Waze with experiment off; end each route before switching. Repeat Apple Maps after Waze.
- [ ] Enable experimental Waze recovery, restart, repeat; record packet availability, support flags, maneuver, distance, lanes and recovery after reconnect. Do not rely on experimental cluster guidance alone.
- [ ] Confirm Now Playing after reconnect and collect TBT-LOGS plus WCP logs. Restore experiment off.

### F. Themes and standard fallback
- [ ] Capture baseline Ultra screenshot with UI Sync off; enable UI Sync/restart, compare center/cluster, themes, wallpaper, apps, sidebars.
- [ ] Enable forced wallpaper only for separate comparison; capture theme/SETUP/stream logs. Restore experiment off.
- [ ] Select standard CarPlay through phone if offered; check connect, audio and no crash; return to Ultra. Separate test with Ultra disabled in CPUL and restart. If necessary disable CPUL for true stock/PPS standalone.
- [ ] Export logs.

### G. Parked AirPlay
- [ ] Enable parked video and picture-only, restart; play short local unprotected H.264 media. Record surface, track, decoder, first frame, sustained playback, cleanup.
- [ ] If picture works, repeat with audio enabled. DRM errors on protected sources are separate; no DRM workaround.
- [ ] Verify feature withheld without confirmed Park; never play video while driving. Restore video off and export logs.

### H. Incremental firmware and final regression
- [ ] Collect navigation engine and iAP2 server files using Dump firmware now; keep inventory and prior captures. Do not expect old radio/audio binaries in this incremental capture.
- [ ] Repeat wireless reconnect, normal audio, native apps, radio return, climate and stock fallback with baseline settings. Record all failures, screenshots and logs.
- [ ] Retain OEM binaries privately and remove diagnostic collector when finished.

## Evidence and known limits
Host tests are not vehicle validation. HD multicast controls, Bose writes, Waze recovery, actual SXM catalog/tuning, ventilation UI, center themes, unprotected video first frame, standard fallback and full signed bundle remain unverified. No Android Auto development or physical cluster customization in CPUL. See [radio](k5-radio.md), [climate](rc8-climate.md), [iOS research](ios27-research.md).

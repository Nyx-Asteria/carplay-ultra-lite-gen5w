# CarPlay Ultra-Lite K5 1.0.0-rc.5

Experimental vehicle-testing prerelease for the 2022 Kia K5, DL322 USA 260507 (Gen5W_M). HMG-API 1.1.0-rc.8 or later required. The changes below are implemented in source; new hardware behavior is not yet verified.

## Highlights
- Independent three-stage front seat heating and ventilation, including the passenger side.
- Native FM source selection, station tuning, seeking and frequency feedback.
- Revised Ultra theme negotiation and phone-managed center appearance.
- Improved parked AirPlay video preparation, buffering and playback protocol.
- Optional Phone Projection Scaling integration and a six-package bundle signed with a permanent author identity.

## Full changelog since rc.4

### Climate and Comfort
- Coalesce paired level and power writes to a single OEM command per physical seat; prioritize an explicit level over a power-only write and avoid conflicting heating/ventilation commands.
- Expose independent front SeatFan characteristics with explicit unsigned levels 0–3 and on/off state.
- Map both seats' ventilation to OEM states 3/4/5 and heating to 6/7/8, with state 2 for off.
- Add raw BCM feedback and accessory command diagnostics; retain steering-wheel heater and Comfort integrations.
- Add regression coverage for driver and passenger seat command arbitration.

### FM radio
- Read OEM tuner and audio source state with firmware and interface checks.
- Publish FM availability, current frequency, source and basic Now Playing information.
- Support native FM source selection, seek up/down, frequency and station identifier commands on the North American 200 kHz grid.
- Wait for confirmed FM activation before deferred tuning; expire requests, invalidate superseded activation callbacks and cancel on alternate source requests.
- Remove European DAB service and associated characteristic registrations.
- Extend read-only diagnostics with OEM mode-manager observations.
- Retain the Kia radio shortcut and explicit two-minute diagnostic collection. RDS, HD metadata and station-list population are incomplete.
- Disabled-by-default SiriusXM schema discovery only; no native SiriusXM tuning or subscription changes.

### Ultra appearance
- Keep center layout selection phone-managed rather than forcing a center palette/layout request.
- Preserve cluster handshake, layout/palette validation and persisted selections; update synchronization tests.
- Companion wireless provider corrects theme identity wire encoding to advertise the bare asset identifier while preserving stock identification fields.
- No physical cluster changes. Native theme selector and center wallpaper appearance require testing; Midnight center wallpaper default remains unresolved.

### Parked CarPlay video
- Keep a black shutter over the video surface until the first rendered frame and attach the surface before preparation.
- Improve redirect URI propagation, HTTP range response validation, EOF handling and parallel-stream URL selection.
- Retain phone playback intent during preparation and screen borrowing.
- Recognize explicit HLS MIME hints in addition to playlist URL patterns.
- Reduce buffer targets from 30–90 seconds / 64 MiB to 15–45 seconds / 24 MiB; startup threshold 1.5 seconds, rebuffer threshold 5 seconds.
- Prefer AVC and up to 720p adaptive tracks, retaining compatible fallback; expand codec and playback diagnostics.
- Accept scalar timing and include LIVI-style playback status alongside existing response fields.
- Video remains opt-in and requires fresh confirmed Park feedback and an active CarPlay session. Local audio remains separately opt-in.
- Independent AirPlay receiver and YouTube discovery are deferred; ParkCast and LIVI are documented research references, not bundled receiver implementations.

### Projection scaling
- Remove hardcoded 80% scaling; consume enabled PPS CarPlay preference at startup, defaulting to stock dimensions.
- Retain CPUL native scaling adapter and withhold the standalone PPS CarPlay hook when CPUL owns the branch.
- PPS remains independent for Android Auto scaling; disable CPUL itself to use standalone PPS CarPlay scaling.
- Restart after settings changes. WCP debloating is deferred.

### Packaging and development
- Build CPUL, wireless CP/AA fork, Comfort, compatibility checker, firmware collector and PPS from pinned sources.
- Require permanent author signing identity d515981b-afc8-8230-ae0f-62d32de366bc and verify each of six package signatures.
- Publish non-draft prerelease with checksums, individual packages and combined testing bundle.
- Expand development regression to compile the video engine and test Park authorization, climate, popup policy and UI synchronization without publishing.
- Refresh README and consolidate development history while preserving the original source tree and backup branch.

## Installation and rollback

Back up settings and keep copies of the working wireless package and Wi-Fi setup. Prior test-signed packages may require removal before installing permanent-author-signed replacements. Install the compatible wireless provider if testing Ultra identity/session-start; do not install competing identification patches. Preserve the stock codec setup that previously eliminated the decoder-query error. Optional Comfort and PPS packages can be installed separately. Restart after settings changes.

## Parked vehicle checklist

1. Verify normal wireless CarPlay connection, reconnection, audio and rollback.
2. Exercise off/low/medium/high independently for driver heat, driver ventilation, passenger heat and passenger ventilation; verify physical operation and matching UI feedback. Test heat-to-vent transitions and steering-wheel heat.
3. Open native theme selector; check layouts, colors, wallpapers and persistence after reconnect. Report missing themes rather than assuming success.
4. Test volume and media popup suppression in foreground CarPlay and confirm stock behavior outside it. Media suppression is opt-in.
5. Select FM, tune a known station, seek both directions and compare OEM tuner frequency with CarPlay; verify leaving FM does not unexpectedly retune. Collect radio diagnostics if needed.
6. Enable parked video while parked, restart and test free non-DRM Apple TV trailers, Photos or Safari video if the receiver is offered. Record whether the item is inserted, tracks selected, buffering progresses and first frame appears. Test audio separately. Verify that loss of Park authorization stops video; never watch video while driving.
7. Check PPS scaling with CPUL enabled and after disabling CPUL itself; restart each time. Verify normal Android Auto behavior if available.
8. Export relevant logs after failures, omitting credentials and proprietary firmware from public reports.

## Known limitations

Hardware validation of the revised seats, native themes, FM and video is outstanding. Driver heat and steering-wheel heat were reported working previously. Standalone AirPlay, YouTube picker compatibility, SiriusXM operation, center default wallpaper, advanced station metadata, fuel range and WCP debloating are not release blockers. Host compilation and signature verification do not establish vehicle functionality.

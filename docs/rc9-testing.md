# K5 RC9 vehicle validation / RC10 evidence collection

This is a development test plan, not a statement that the candidate has been published or vehicle-verified. Target DL322 USA 260507, API 19. Retain the known-working RC8 rollback bundle and settings. Companion revisions must be confirmed before the coordinated signed release; do not install a competing identity or navigation publisher.

## First connection and logging
- Install the correctly signed CPUL candidate with compatible WCP shared identity, and retain EWF if required for the working wireless configuration. Test while parked.
- Confirm normal connection, audio, touch, sidebars, Climate and Radio before enabling experiments.
- Session logging and USB export default on; enable only relevant detailed categories. Capture a baseline connect/disconnect, inspect exported session files, and preserve corresponding WCP/STBT timestamps. Do not collect credentials.
- Standard CarPlay fallback, reconnect, and Park enforcement are mandatory checks. Wired comparison is optional only if WCP shared identity and normal operation can be retained without changing the test configuration.

## Seats and climate
- Driver/passenger independently: off → heat 1/2/3 → off; off → vent 1/2/3 → off; heat ↔ ventilation; physical button updates; steering-wheel heat and normal climate.
- Verify physical action and UI separately. Immediate heating feedback defaults on, ventilation feedback off. Repeat ventilation with its experiment on; reset between tests. Compare BCM readback and command arbitration. See rc9-seat-vehicle-tests.md.
- Do not mistake immediate UI feedback for confirmed physical state.

## Radio / audio
- From CarPlay media, request FM and SXM directly without OEM activation; compare with OEM-first control. Observe mode accepted/acquired/sustained/lost and source switching. Check seek and rapid station changes, metadata and station lists, HD1 primary playback/artwork. HD multicast selection remains diagnostic-only; no claimed HD2–4 control.
- With SXM discovery explicitly enabled, compare catalog on reconnect versus OEM-first, native tune, channel change, song/artist metadata. Record channel artwork and seek failures separately; no entitlement changes.
- Verify knob, steering keys, CarPlay volume/mute, navigation/phone/Siri and native FM/SXM routing; stock panel must remain available when integration not ready.
- Bose Centerpoint is read-only: compare OEM on/off and raw availability/setting for CarPlay, FM and SXM; no setter.
- Vehicle odometer/range, units, shortcuts, optional Comfort threshold adjustment and reconnect.

## Themes (one variable at a time)
Enable UI Synchronization and Ultra Sidebars. Baseline: Independent UI Sync Negotiation off, Phone-Managed Cluster Layout off, Force Theme Wallpaper off. Record phone offers, native enabled features, provider registration, center/cluster handshake, selector contents and wallpaper. Repeat with only independent negotiation on, then only phone-managed cluster on, then only Force Wallpaper on, then independent + phone-managed on with Force Wallpaper off. Restart after changes. The independent negotiation toggle has no effect when VSP is offered. No guarantee any experiment fixes theme eligibility.

## Parked AirPlay
Only while parked, enable parked video; preserve all Park enforcement. Use ordinary unprotected media. Test normal audio/video, then picture-only diagnostic. Record proxy connection, prepare, decoder, first picture, rate/readiness, sender stop and native session finalization; correlate WCP watchdog/recovery/transport logs. Distinguish proxy failures before versus after finalization. Stop testing if ordinary projection becomes unstable. No attempt to defeat DRM or parked-video gates. Wired optional if equivalent identity configuration becomes available.

## Pass criteria and rollback
Record pass/fail/untested for every item; do not promote a host regression pass to vehicle verification. Retain RC8, revert individual experiments before rolling back packages, and preserve logs before reboot. RC10 is for evidence-backed corrections, not a requirement to claim every RC9 experiment works. 

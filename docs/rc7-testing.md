# CarPlay Ultra-Lite K5 1.0.0-rc.7 — testing release

Target: Kia K5 DL322 USA 260507, Gen5W_M / Android 4.4.2. HMG-API 1.1.0-rc.8 or newer; use the known working HMG-API installation. RC7 is a regular release marked Latest, but is still an experimental vehicle testing build. Host checks are not a claim of successful on-car operation.

## Full changelog since RC6

### Ultra and settings
- Consolidate the RC6-tested Ultra profile under CarPlay Ultra: virtual cluster, DBX variant, extended features, enhanced RequestCarUI, original extended feature mask, LIVI display extras and receiver 950.7.1.
- Group theme identity, themed display IDs and CarPlayStartSession under Enable Ultra Sidebars. Require Ultra for Sidebars and Sidebars for UI Synchronization.
- Keep the legacy theme identity tuple `44C9FA31EC:985:19.0` through the WCP provider; preserve working native apps and sidebars configuration.
- Remove the old independent RC4 center-layout switch from the baseline profile. Provide a new opt-in Force Theme Wallpaper experiment requesting the catalog center Wallpaper UUID with its supported blue palette at initialization only.
- Isolate UI Sync handshake state per connection; add independent center/cluster handshake, reconnect and layout regression coverage. Log which display panels actually send traffic outside ordinary packet log limits.
- Only negotiate viewAreas when the phone offers it. This is a defensive standard-CarPlay fallback correction, not a confirmed crash fix.

### Climate, seats and popups
- Publish paired authoritative BCM-confirmed SeatFan level and on/off feedback after ventilation commands, suppressing premature optimistic level echoes on the real-car path.
- Add tests for both front seats cycling 3 → 2 → 1 → off, stale feedback suppression and duplicate feedback deduplication.
- Preserve previously working heating, steering-wheel heater and native volume popup suppression paths. Media popup suppression remains subject to vehicle validation.

### Native radio and Centerpoint
- Route active FM seek through permission-checked OEM SEEK_UP/SEEK_DOWN key injection using the physical daudio keyboard, with fresh source checks and no generic media-key fallback.
- Publish validated cached OEM FM scan frequencies as station items, without starting a scan or retuning.
- Retain North American FM tuning and remove unsupported European DAB service.
- Add opt-in disabled SiriusXM discovery schema with channel and image value/notifier registration; no unverified satellite semantic ID or tuning enabled.
- Inventory native radio/HD/SiriusXM interfaces during explicit diagnostics.
- Add a protected, time-limited OEM source-transition and physical seek-key trace, armed from the vehicle radio diagnostics.
- Collect native SiriusXM smart favorites and channel metadata; correlate audio mode, stream, current/ready mode and last channel ID.
- Observe HD Radio ID3 tags, SIS names, artwork byte counts, RDS scan arrays and Bose Centerpoint mode/state/availability methods where available.
- SiriusXM tuning, native metadata/artwork/favorites publication, radio source activation and Centerpoint control are not complete. Physical SiriusXM seek behavior was reported to match FM/HD; software SiriusXM seek is not yet wired.

### Parked AirPlay
- Retain fresh Park and active-session safety gates; never enable playback while Park is unconfirmed.
- Add optional AirPlay Picture Only diagnostic setting: omit local audio renderer from startup and select the lowest supported video bitrate with an AVC preference. Normal audio/video mode retains adaptive selection and existing audio fallback.
- Report actual enabled-audio and mute state instead of unconditional audio availability.
- Log real SurfaceHolder creation, dimensions and destruction; reset the loading shutter on surface destruction.
- Preserve prior HLS/proxy/redirect/range handling, first-frame shutter and playback-status compatibility paths.
- First rendered video frame and sustained video/audio playback are not verified for RC7. Standalone AirPlay mirroring and YouTube discovery remain future work.

### Bundle and release
- Pin WCP 2.2.1-rc.4 to `3790d63d9fd43bfe47e3b0d1ace65bdc3bcc1fa8`: includes K5 connection cleanup, automatic wireless CarPlay opening and legacy full Ultra identity tuple from the companion project.
- Pin stable Phone Projection Scaling 1.0.0 to `150962a2624f9b855ab6c41d3d5a631cdb7cbd30`; enforce exact version at packaging.
- Rebuild all six packages using the permanent author identity, run package tests, verify signatures and generate SHA256SUMS and a combined testing ZIP.
- Publish as a normal non-draft, non-prerelease release marked Latest with full notes.
- Refresh README settings, scope and known limitations.

## Bundle contents
CPUL 1.0.0-rc.7; WCP 2.2.1-rc.4; PPS 1.0.0; Comfort 1.1.8-cpul.rc.1; compatibility checker; firmware collector. Keep rollback copies of known working packages and settings. Existing saved toggles can override defaults.

## Testing guide — perform while parked

### 1. Baseline connection
- Install matching signed CPUL and WCP packages; restart. Verify the eight settings below. Preserve the known working Wi-Fi configuration.
- Start with K5 integration, Ultra and Sidebars on; UI Sync, forced wallpaper, SiriusXM and video off.
- Measure wireless connection/reconnection, automatic CarPlay opening, native apps, sidebars, climate, normal phone audio, Apple/Google Maps TBT and stock volume/media behavior.
- Verify PPS at 100% then a known working smaller scale. Check no competing CarPlay patch. Android Auto scaling remains unverified on the target vehicle.

### 2. Seats
- Driver and passenger separately: ventilation 3 → 2 → 1 → off; check both CarPlay and physical indicators, then reverse direction via native controls.
- Check heating 3 → 2 → 1 → off and wheel heating. Switch between heat and ventilation. Capture [seat-fan] confirmation/correction lines.

### 3. Radio
- Start the two-minute radio diagnostics from Vehicle > Radio. If needed, start another collection window for SiriusXM/Centerpoint.
- Switch from phone audio to native FM, tune two frequencies, use physical and CarPlay seek controls, browse station list, tune HD station and switch subchannels.
- Switch to native SiriusXM, use physical seek, browse favorites, observe channel/title/artist/artwork. Toggle Centerpoint on/off using native audio settings and return to CarPlay music; record whether the setting remains available or affects audio.
- Enable SiriusXM Discovery only for a separate parked test. Grey/disabled source is expected; no native SXM controls advertised as working.
- Capture [radio], [radio-probe], [radio-source], [radio-meta] and native trace lines. Record missing hooks as well as successful callbacks.

### 4. Themes and fallback
- Baseline screenshot with UI Sync off; then enable UI Synchronization, restart and compare theme selector, wallpaper, sidebars and native apps.
- Enable Force Theme Wallpaper with UI Sync on, restart and compare. Capture [theme-sync], SYNC phone/car, identity, /info and SETUP logs. Note whether Center_Display ever negotiates.
- In a separate parked test try standard CarPlay selection on the phone. Record disconnect/crash and whether reconnect succeeds. Restore Ultra if needed. If unstable, disable experiments and restart.
- Do not assume a downloaded theme or correct layout until the phone demonstrates it.

### 5. AirPlay
- Enable Parked AirPlay Video & Audio and AirPlay Picture Only (Diagnostic), restart, and test a short non-protected compatible H.264 video while parked. First frame is the priority.
- Record startup timing, selected tracks, surface creation, decoder initialization, first frame, playback duration and whether the phone ends the session.
- If video works, disable picture-only and repeat with audio. Capture [video-selection], [video-surface], [video-audio], decoder/first-frame logs and cpc-video-trace.txt if available.
- Stop if unstable. Verify video is unavailable without confirmed Park; never attempt playback while driving. Restore normal CarPlay after testing.

## Settings defaults

| Setting | Default |
| --- | --- |
| K5 Vehicle Integration | On |
| CarPlay Ultra | On |
| Enable Ultra Sidebars | On |
| UI Synchronization | Off |
| Force Theme Wallpaper (Experimental) | Off |
| SiriusXM Discovery | Off |
| Parked AirPlay Video & Audio | Off |
| AirPlay Picture Only (Diagnostic) | Off |

Restart after changes. Keep firmware dumps and logs containing device identifiers private. Share only diagnostic information needed for debugging.

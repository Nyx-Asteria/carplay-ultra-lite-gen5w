# K5 radio integration

## Firmware evidence (DL322 USA 260507)
The new K5FirmwareDump(1).zip includes HKMC_RadioNA.apk/odex.
- HKMC_RadioNA.odex MD5 a909ed44ac10909a83af0a1109e12f92
- automotive.odex MD5 7c99638e39c369c45fab9c485b103cf0
- automotive-service.odex MD5 04fd3c2e9bc7444c2ae812b57a83ac5d
- Stock package com.hkmc.av.app.radiona.

DEX method table evidence: com.hkmc.media.MicomRadioManager(Context) exposes directTune(IZ)Z, changeBand(I)V, seekUp/Down()V, tuneUp/Down(I)V, readPreset(I)V, presetUp/Down()V, getLastAMFrequency()I, getLastFMFrequency()I, getTuningValue()I, addListener(MicomRadioListener)V. The framework also contains additional directTune overloads. IRadioService exposes directTune(IZII)Z, changeBand(I)V and seekUp/Down()V. Actual implementation com.hkmc.micom.RadioService is in automotive-service.odex. Prefer OEM manager path if its constructor and permission requirements are verified; do not infer argument values from signatures.

Feedback: MicomRadioListener.onMicomRadioStatusChanged(MicomRadioStatus), onMicomRadioFreqInfoChanged(IIIIII), onMicomRadioPSChanged(ILjava/lang/String;), onMicomRadioTextChanged(ILjava/lang/String;), onMicomRadioPresetChanged(ZLcom/hkmc/media/MicomRadioPresetData;), onRdsStationListReceived([Lcom/hkmc/media/RdsStationInfo;). HDRadioManager exposes spsChannelTuneDirect(I)V and HDRadioListener exposes ID3 tags, artwork, SIS and status callbacks. These are method-table findings, not runtime proof.

SiriusXM uses com.hkmc.media.sxm65h.Manager, including tuneDirect(IZ)I, getLastTunedChannelID()I and requestAllAvailableChannels()V. The stock SXMRadioAppService exposes getCurrentChannelNumber()I, getCurrentChannel() and SiriusXM_fastSeekUp/Down(). Do not change subscription, lock or entitlement state.

## Apple protocol
Vendored LIVI vehicle_state/media.rs defines FM source semantic 2, service 4112, frequency 4117 (kHz), items 4118, now playing service 4353, source/title/artist/album/artwork/state 4354–4359, and tune/source controls 4364–4366. Its other radio source is DAB, not AM or SXM. Do not invent semantic IDs or label AM/SXM as DAB.

## Historical implementation plan (RC3)
1. Trace stock manager constructor, service acquisition, frequency units, band constants, source activation, permissions and listener payloads.
2. Implement read-only capability/feedback first. Do not advertise active station from last-frequency getters alone.
3. Implement FM controls using explicit user commands and verified OEM source ownership; no automatic source switch or muting of phone audio on connection.
4. Add AM/HD/SXM only when both OEM and Apple semantics are established.

Historical RC3 state: stock radio shortcut only; superseded by RC7 and RC8 development below.

## Manager delegation and constants checkpoint
Verified directly against the extracted automotive.odex (MD5 matches above): MicomRadioManager.changeBand delegates to IRadioService.changeBand; getLastFMFrequency delegates to IRadioService.getLastFMFrequency; directTune overloads delegate to IRadioService.directTune(IZII)Z. Static encoded MicomRadioConstants values: MICOM_RADIO_AM1=113, FM1=-127, FM2=-126 (byte patterns 0x71, 0x81, 0x82); these are not yet proven to be the arguments expected by changeBand. Frequency limit and step fields have zero encoded initial values; inspect runtime initialization rather than treating zero as valid. Stock RadioNA ODEX optimized invokes prevent the simple method-index cross-reference scan from reliably recovering call-site arguments. No write operations enabled on this evidence alone.

## RC4 vehicle diagnostic procedure
In Park, connect CarPlay, open Vehicle > Radio > Collect radio diagnostics (2 min), then open the stock Kia radio while retaining the CarPlay session. Manually tune known FM stations (e.g. 88.1 and 101.1 if available), change to AM, change preset, and optionally select an available SXM channel. Return to CarPlay and collect the CPUL log, including [radio-probe] lines. The explicit collector polls every 1.5 seconds and logs changed raw values and binder availability only, ending on session generation change, disable, or 120 seconds (a blocked OEM binder call can delay exit). It does not register listeners, tune, activate sources or change entitlements. RDS text, HD subchannel metadata and exact band callbacks are not yet collected; do not infer them from last-frequency getters. Compilation and runtime permissions remain unverified.

RC4 scope: Ultra-Lite K5 fixes and radio diagnostics; native radio controls and further AirPlay playback fixes deferred. Standalone scaling and wireless CP/AA debloat are outside this release. Keep RC3 version until release preparation is explicitly requested.

## RC8 development checkpoint

RC7 is the installed baseline; RC8 is not released. Passing a regression build is not proof of OEM runtime behavior.

### FM / HD Radio
- Verified on RC7: native source activation, manual numeric tuning, physical seek, frequency feedback, return to phone audio.
- RC8: native IRadioService seekUp/seekDown, observed-station fallback merged with OEM scan cache, live SIS and ID3 metadata, bounded station logo transport.
- HD metadata uses primary program only. Explicit HD2/HD3 selection is deferred; full RF scan discovery is unverified.
- Artwork is restricted to PNG/JPEG signatures and 128 KiB; actual image decoding and callback delivery remain unverified.
- Metadata received before a new frequency poll may be discarded; rapid tuning can expose remaining association races.

### SiriusXM
Experimental setting sxm_schema_test must be enabled to advertise the candidate Apple source schema. Its semantic value is not verified. The Smart Favorites path reads OEM channel records. The full catalog bridge observes RadioNA's decoded callback, gated by the RadioNA ODEX fingerprint and DEVICE_POWER permission. Requests are asynchronous and may require the OEM app to be running.
- OEM source mode 13, audio mode 4, stream 41 were observed in RC7 logs.
- Direct tune uses internal channel ID and override=false; subscription, skip and lock checks fail closed. Return values and method availability require validation.
- Full catalog and artwork bridges are implemented but unverified on the car. Catalog artwork currently refers to Now Playing, not images for every list entry.
- Physical seek already works; preserve it. CarPlay on-screen SiriusXM seek is not established.
- External datasets are research references only; no bundled external channel data.

### Bose Centerpoint
Read-only diagnostic entry in the Vehicle app. Candidate OEM sound setting ID 48, getSoundAvailability(int) and getSoundSetting(int), checked by exact descriptor. No amplifier write is implemented. Do not infer on/off or supported state from a single sample. The native Apple audio setting characteristic remains unknown.

### RC8 vehicle acceptance tests (Park)
1. With FM selected, verify numeric tuning, on-screen seek, physical seek, current frequency and return to CarPlay. Confirm that commands do not reach the phone while radio owns playback.
2. Tune multiple known HD stations. Check name, title, artist, album, station logo, and clearing on station/source changes. Compare OEM station list and the observed-station fallback.
3. Enable experimental SiriusXM discovery and reconnect. Check whether the source becomes selectable. Test native source activation, channel feedback, catalog count, names, one subscribed unlocked channel selection and return to phone. Do not change locks or subscriptions. Test on-screen seek separately from working physical buttons.
4. Compare SiriusXM Now Playing artwork with the current channel; check clearing on changes. Capture [radio-sxm] bridge/request/receiver diagnostics if missing.
5. Compare Centerpoint diagnostic values with the stock sound setting, first off then on, on FM, SiriusXM and CarPlay music where the OEM UI permits. Record raw availability and setting values and source. Restore the original preference.
6. Check media volume routing across FM, SiriusXM and CarPlay; verify stock popup suppression remains intact.
7. Disconnect and reconnect; verify no stale station names, catalog or artwork are published.
8. Record failures with [radio], [radio-meta], [radio-sxm], [audio-bose] and VS log lines. Do not treat missing experimental schema or unavailable methods as verified support.

RadioPolicyTest covers pure mapping/filtering rules, not OEM runtime behavior. RC8 release remains gated on development and signed bundle checks. Passenger seat heating is now reported working in both directions; ventilation remains a separate follow-up.

## RC8 final development additions
Full catalog remains usable without Smart Favorites; inactive-session artwork and eligibility caches clear. HD SPS candidate methods are traced during the existing two-minute window, with elapsed timestamps. Test native HD1→HD2→HD3→HD1 and frequency changes; capture hook availability and callback arguments. Native multicast tuning is not enabled. See [RC8 guide](rc8-testing.md).

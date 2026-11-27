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

## Implementation order
1. Trace stock manager constructor, service acquisition, frequency units, band constants, source activation, permissions and listener payloads.
2. Implement read-only capability/feedback first. Do not advertise active station from last-frequency getters alone.
3. Implement FM controls using explicit user commands and verified OEM source ownership; no automatic source switch or muting of phone audio on connection.
4. Add AM/HD/SXM only when both OEM and Apple semantics are established.

Current Vehicle page remains the explicit stock radio shortcut. No native tuner commands are implemented yet. Maintain RC3 version during development.

## Manager delegation and constants checkpoint
Verified directly against the extracted automotive.odex (MD5 matches above): MicomRadioManager.changeBand delegates to IRadioService.changeBand; getLastFMFrequency delegates to IRadioService.getLastFMFrequency; directTune overloads delegate to IRadioService.directTune(IZII)Z. Static encoded MicomRadioConstants values: MICOM_RADIO_AM1=113, FM1=-127, FM2=-126 (byte patterns 0x71, 0x81, 0x82); these are not yet proven to be the arguments expected by changeBand. Frequency limit and step fields have zero encoded initial values; inspect runtime initialization rather than treating zero as valid. Stock RadioNA ODEX optimized invokes prevent the simple method-index cross-reference scan from reliably recovering call-site arguments. No write operations enabled on this evidence alone.

## RC4 vehicle diagnostic procedure
In Park, connect CarPlay, open Vehicle > Radio > Collect radio diagnostics (2 min), then open the stock Kia radio while retaining the CarPlay session. Manually tune known FM stations (e.g. 88.1 and 101.1 if available), change to AM, change preset, and optionally select an available SXM channel. Return to CarPlay and collect the CPUL log, including [radio-probe] lines. The explicit collector polls every 1.5 seconds and logs changed raw values and binder availability only, ending on session generation change, disable, or 120 seconds (a blocked OEM binder call can delay exit). It does not register listeners, tune, activate sources or change entitlements. RDS text, HD subchannel metadata and exact band callbacks are not yet collected; do not infer them from last-frequency getters. Compilation and runtime permissions remain unverified.

RC4 scope: Ultra-Lite K5 fixes and radio diagnostics; native radio controls and further AirPlay playback fixes deferred. Standalone scaling and wireless CP/AA debloat are outside this release. Keep RC3 version until release preparation is explicitly requested.

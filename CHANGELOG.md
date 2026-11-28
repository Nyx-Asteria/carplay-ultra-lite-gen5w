# Changelog

## K5 1.0.0-rc.11 (development candidate; not released)

- Reworked bounded session diagnostics and USB evidence, including initial theme registration, native feature negotiation, per-panel sync and virtual cluster lifecycle; redacted companion configuration and phone choices.
- Evidence-backed climate, radio, media and vehicle-state corrections and optional legacy heated-seat compatibility. Existing Park restriction and physical-cluster boundaries retained.
- API v3 WCP theme-only registration, full legacy theme tuple, provider-classloader compatibility, generation-consistent observations and withdrawal. WCP exclusively owns gasoline identity and StartSession; FileTransfer remains separate and disabled.
- Phone-managed center and virtual cluster layouts; no forced wallpaper, no shared layout persistence in production; listener-gated display profiles, coherent rollback, guarded native stream routing and UI Sync state/key handling. Theme selector and wallpaper not yet proven fixed.
- Independent widescreen experiment, PPS setting delegation, STBT exclusive guidance with WCP integrated TBT off. Pinned companion source/recipe checks and RC10 loader audit.
- Existing-versus-fresh pairing comparison, explicit Classic/Ultra preferences, staged/current asset and acquisition/transfer distinctions from WCP research. No preference bypass or automatic re-pairing.
- Add opt-in passive RemoteControl SETUP discovery: canonical unknown client-type UUID, fixed field metadata, bounded values, per-process deduplication and USB session logs; no FileTransfer advertisement or unknown stream admission.
- Record intermittent heating arbitration inputs and the selected OEM command separately per front seat. Compare legacy compatibility off/on; no new vehicle-confirmed seat fix.
- Final companion pins: WCP 2.2.1-rc.7 (`3c3d248`), STBT 1.0.0-rc.4 (`fac2ec3`); both final companion CI runs passed, API v3 unchanged, independent native region and AA recipe comparison passed. Experimental FileTransfer remains inactive. Final combined CI regenerates the native recipe and verifies seven signed packages before automatic prerelease publication; no separate approval gate.
- Regenerate native ARM recipe during CI and packaging from pinned stock metadata; exact-tree host tests and signed package verification required before owner-approved release. No vehicle validation claimed.


## K5 1.0.0-rc.10 (vehicle-testing release)

### Climate
- Preserve explicit seat-level requests across companion power writes; arbitrate heating/ventilation transitions without inactive-mode zero writes cancelling the selected mode.
- Validate paired ventilation power/level feedback and reconcile requested values with authoritative BCM readings; protect cached values even before a phone subscription.
- Preserve full high-ventilation command in the optional four-step combined seat layout.
- Use a single immutable DATC callback snapshot for climate readings and passenger-temperature correlation; reset it on service reconnect.

### Radio and audio
- Separate OEM FM scan frequencies from observed fallback stations; retain the last nonempty OEM scan without treating observed stations as a complete scan.
- Correlate delayed SiriusXM metadata with tuner observations; propagate explicit channel exclusions across the RadioNA bridge, distinguish all-excluded catalogs from transient empty responses, and recheck source and eligibility before native tuning.
- Correlate read-only Bose availability/setting samples with source, session and elapsed sampling time. No amplifier writes or entitlement changes.
- Bind queued volume and mute requests to their authorizing VehicleState and projection generation; recheck authorization before native dispatch and restore OEM panel fallback on failed observation delivery.

### Projection, themes and vehicle application
- Associate AirPlay screen borrowing and recovery callbacks with the owning playback generation; isolate recovery from unrelated thread failures. Park enforcement unchanged.
- Refresh theme preferences under a shared lock; preserve independent display selections and avoid overwriting malformed preference files.
- Replay authoritative Vehicle page values on session replacement. Capture session-owned writers before sending updates outside the shared session lock; reject incoming packets from replaced connections and use the owned delivery path for climate and audio observations. Delivery retry is best-effort, not a guarantee of phone receipt.

### Release preparation
- Target HMG-API RC10 and Nemesis 1.17.1 on DL322 USA 260507; retain existing API 19 compatibility requirements.
- Persistent session logs, incremental firmware collection and automated regression coverage support focused RC10 evidence gathering.
- WCP 2.2.1-rc.6 (4885b9f), STBT 1.0.0-rc.3 (e877025), HMG-API RC10 (daa0837). Companion source audits and shared identity compatibility reviewed; signed bundle verification pending. Companion journal/exporter and transport policy regressions included in bundle workflow. Publication requires explicit manual opt-in.
- No new vehicle verification is claimed. See [RC10 test plan](docs/rc10-testing.md).

## K5 1.0.0-rc.9 (vehicle testing)

- Persistent categorized session logging, bounded retention, USB export, native capture, cross-process SiriusXM diagnostic capture, session summaries and regression tests.
- Independent heating and ventilation immediate-feedback settings; separate requested and actual seat state, typed paired feedback, transition tests and authoritative BCM reconciliation.
- Radio source activation traces, bounded coalescing and stale tune rejection, service recovery, HD primary preservation and multicast diagnostics. SXM catalog retry, nonempty snapshot preservation, guarded live metadata capture and session/channel invalidation. HD multicast control, SXM artwork/seek and live metadata verification remain pending.
- AirPlay shared readiness/rate reporting, seekability checks, player teardown and first-frame diagnostics, session generation checks on queued commands and responses. First hardware frame reported in RC8; disconnect cause unresolved. No parked-video restriction changes.
- Theme provider recheck in consumer processes, display-local handshake state and merged preferences. Independent UI Sync Negotiation and Phone-Managed Cluster Layout experiments default off. Existing DBX identity and center wallpaper experiment preserved. Theme selector and center negotiation remain unverified.
- Bose read-only sample validity, strict audio getter replies, stale volume authorization invalidation, OEM panel fallback, vehicle information and Comfort session checks. No new Bose setter.
- Wireless remains primary validation; wired optional only if equivalent identity configuration is available. See [RC9 test plan](docs/rc9-testing.md).
- WCP 2.2.1-rc.5 (`9c904db`): bounded persistent session journal and USB export, opt-in native capture, teardown timing and transport counters, attachment generation guards, receive readiness race fixes; shared identity API and native capability recipes unchanged.
- STBT 1.0.0-rc.2 (`d5720ef`): bounded session journal and USB export, stale distance invalidation, guidance registration gating, route-stop cleanup, focused opt-in third-party packet diagnostics and component attribution; Waze guidance not verified.
- PPS 1.0.0, Comfort 1.1.8-cpul.rc.1, collector RC4 and compatibility check remain pinned to the previous bundle revisions. Seven packages signed with the existing permanent author key and verified before publication.
- Known vehicle blockers: EWF currently uninstalled; external-file conflicts require separate HMG-API recovery. USB dongle projection crashes with CPUL sidebars enabled (reported); avoid this configuration pending investigation. Wired operation is not verified. No claim of vehicle validation.

## K5 1.0.0-rc.8 (vehicle testing)

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
- Seven packages, common permanent signing key, signature verification and checksums; normal non-draft release marked Latest after successful signed packaging.

Full [RC8 test guide](docs/rc8-testing.md). Historical upstream Ioniq releases follow.


Versions after 1.0.0 are test builds running on one car (Hyundai Ioniq 6, US, firmware 260507). 1.3.3 is published as a test build (an update for a car on 1.0.0); the main release is still 1.0.0.

## 1.3.3 (2026-11-10, in testing)

- **Experiment "Only CarPlay's volume slider in CarPlay"** (off by default): while CarPlay is on the screen, the volume knob and the steering wheel's volume buttons change the volume without the head unit's volume pop-up, so only CarPlay's slider shows.
  - The knob's speed is kept: a fast spin moves as many steps as it does outside CarPlay.
  - CarPlay's slider is told at once when a key changes the volume.
  - The mute button stays the car's own; its pop-up is dismissed straight afterwards. Known issue: the pop-up still flashes for a moment.
  - Anything unexpected leaves the key to the car, so the volume keys keep working as stock.

## 1.2.x (2026-11-10, in testing)

- **Car volume in CarPlay:** CarPlay's Volume page shows the head unit's real levels for media, navigation, phone and Siri, follows the knob and the steering wheel, and sets the car's volume. The phone can't change a level before it has been shown the car's own one, so nothing jumps.
- **Vehicle notifications:** the Vehicle app's notification list is filled by the car; an entry you dismiss stays dismissed. One test entry is posted at each start.
- **Readings match the cluster:** battery, range and odometer are rounded down as the instrument cluster does.
- CarPlay's Radio app: its play and source buttons open the head unit's own radio (the app itself still has no stations).

## 1.1.x (2026-11-10, in testing)

- **The car's page in CarPlay's Vehicle app:** battery level, range and odometer; a Car home screen button; a Battery preconditioning switch (through Launchpad); the Light Vibes switch and button; buttons for HMG-API, NEMESIS Manager, Radio, EV and Vehicle settings.
- Found on the way: the iPhone builds the Vehicle app from the description it is given when CarPlay connects, so the page is part of that description.

## 1.0.0 (2026-11-08)

- Climate in CarPlay, both ways, with a panel for each seat.
- The CarPlay Ultra theme with its wallpapers and Themes page.

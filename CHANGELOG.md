# Changelog

## K5 1.0.0-rc.9 (unreleased development candidate)

- Persistent categorized session logging, bounded retention, USB export, native capture, cross-process SiriusXM diagnostic capture, session summaries and regression tests.
- Independent heating and ventilation immediate-feedback settings; separate requested and actual seat state, typed paired feedback, transition tests and authoritative BCM reconciliation.
- Radio source activation traces, bounded coalescing and stale tune rejection, service recovery, HD primary preservation and multicast diagnostics. SXM catalog retry, nonempty snapshot preservation, guarded live metadata capture and session/channel invalidation. HD multicast control, SXM artwork/seek and live metadata verification remain pending.
- AirPlay shared readiness/rate reporting, seekability checks, player teardown and first-frame diagnostics, session generation checks on queued commands and responses. First hardware frame reported in RC8; disconnect cause unresolved. No parked-video restriction changes.
- Theme provider recheck in consumer processes, display-local handshake state and merged preferences. Independent UI Sync Negotiation and Phone-Managed Cluster Layout experiments default off. Existing DBX identity and center wallpaper experiment preserved. Theme selector and center negotiation remain unverified.
- Bose read-only sample validity, strict audio getter replies, stale volume authorization invalidation, OEM panel fallback, vehicle information and Comfort session checks. No new Bose setter.
- Wireless remains primary validation; wired optional only if equivalent identity configuration is available. See [RC9 test plan](docs/rc9-testing.md).
- Companion release revisions and signed packaging pending coordinated release authorization; no claim of vehicle validation.

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

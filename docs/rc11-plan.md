# RC11 scope and cleanup

Stage 1 is behavior-preserving repository cleanup. Stage 2 improves capture of actual runtime interactions, including pre-session native setup, stream routing and seat commands. Stage 3 implements evidence-backed corrections. Stage 4 adds an opt-in historical heated-seat behavior path after comparing RC5 and RC6 with RC10; preserve physical feedback and avoid competing actuation paths. Stage 5 investigates theme negotiation against upstream and EV6; stage 6 validates and packages.

RC10 vehicle observations: source switching, steering wheel media controls, native shortcuts, heated-seat physical feedback and Apple/Google Maps cluster guidance worked; seat writes, ventilation representation, passenger temperature, SYNC, radio catalog/metadata, HD multicast, SXM numbering/favorites, vehicle distance display, Waze, theme selection, wallpaper and parked video failed. NEMESIS 1.17.1 upgrade succeeded. CPUL export appeared functional but runtime interaction coverage was sparse. WCP/STBT exporter fixes belong to their repositories.

## Virtual cluster evidence

Compared current K5 source with lths/carplay-ultra-lite-ev6 main (CpClimate.java and native/hooks.c) and SafariForest/carplay-ultra-lite-gen5w main README. Both K5 and EV6 retain ClusterSink on TCP 7132, full instrument panel, alternate display type 111 and native alt_stream reply. K5 uses themed IDs only when provider preflight and sidebars are enabled; EV6 uses Ultra directly. K5 gates viewAreas on the phone offer; EV6 does not. K5 SETUP without VSP generally leaves reply stock except optional independent UI Sync. Type 130 is a multiplex of client UUIDs, not a single theme stream.

Base project documents working theme selection on Ioniq 6 with modified E-GMP EV Routing identity provider and online re-pairing. K5 uses shared WCP identity instead. Do not install competing routing patches. Compare actual accessory configuration, /info, SETUP offers/replies, hook entry/return, listener readiness and received bytes before changing negotiation. Virtual cluster should participate even if physical cluster cannot render it; do not send graphics to physical cluster or interfere with native TBT. No theme fix proven yet.

RC5/RC6 snapshots retained in rebased history (9f121c86, fa0f940a); original tags remain available. Do not remove experimental paths merely because current vehicle tests failed until dependencies and evidence are reviewed.

## Agreed RC11 display behavior

Keep Ultra Sidebars and identity dependency gates. Prepare display configuration and listeners before relevant negotiation. Keep virtual cluster and native TBT independent; receive and discard virtual video. Center and cluster layouts, wallpaper and palettes are phone-managed; remove forced-layout experiments only after auditing persisted settings. Restore upstream-style viewAreas negotiation with sidebars after validating the native gate. Add independent experimental widescreen view areas selector (cpc-wide: 0 disabled/default, 1 whole display, 2 right-hand area). Coordinate scaling with PPS. Do not implement WCP or STBT changes here. WCP owns group 20/21, stock/explicit Kia K5 gasoline identity, startup StartSession and optional FTv2 experiments; integrate the new provider only when available.


## Detailed RC11 tracking (expanded from the same agreed scope)

1. Repository cleanup and history consolidation (initial pass complete; final release consolidation pending).
2. Session logging, startup/teardown, privacy, USB export and diagnostics (implemented; vehicle validation pending).
3. Legacy heated-seat compatibility (implemented; physical results pending).
4. Seat ventilation schema and feedback; passenger temperature and SYNC (evidence-limited; preserve existing mappings unless supported).
5. FM station catalog, seek and HD multicast (in progress; no speculative Apple protocol keys).
6. SiriusXM channel identity, displayed number, catalog, actual favorites, live metadata and artwork (in progress; preserve eligibility and locks).
7. Vehicle information (odometer/range/units), audio/volume and media routing, native shortcuts (review and regressions; no speculative writes).
8. Parked AirPlay lifecycle and rendering (retain Park interlock; only evidence-backed corrections).
9. Theme catalog/eligibility, phone-managed center and virtual cluster layouts/palettes/wallpaper, sidebar behavior.
10. Native display negotiation (viewAreas with sidebars), virtual cluster and UI Sync readiness before advertisement.
11. Independent widescreen view areas selector (disabled/whole/right; preserve PPS ownership).
12. Companion integration: consume WCP readiness, align CPUL advertisement/startup with WCP identity and StartSession, keep STBT sole navigation publisher and PPS sole scaling owner; update verified pins and validate combined installation. No implementation of companion-owned features here.
13. Final regression, consolidation, signed candidate, changelog and owner-authorized release; targeted parked vehicle acceptance.

These are tracking subdivisions, not additional promises of fixes or a change to the agreed scope. Evidence-limited functionality can remain deferred with explicit acceptance tests. Waze investigation belongs to WCP/STBT; CPUL must preserve working TBT. Host regression success is not vehicle validation.

## RC11 phone-managed layout policy

Production UI Sync now always passes null cluster layout and false forced center wallpaper. Retired settings ultra_phone_managed_cluster and ultra_force_wallpaper are removed from the manifest and session header; previously persisted values are ignored rather than migrated into forced requests. The existing protocol fixtures for legacy requests remain as historical regression coverage. UI Sync master and sidebar gates are unchanged. This is not a verified theme selector or wallpaper fix.

## Approved WCP API v3 source integration
Verified provider source `3b951c745ce78fde4ca6085b28d682c7d629bb98`; [pinned contract](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa/blob/75f1ecadc704e39be9095799f62bea64aa79bf4d/docs/cpul-api-v3-integration.md). FileTransfer is no longer a gate. Exact API v3 via provider class loader, stable theme-only owner and three-argument registration; WCP owns gasoline identity and StartSession. Compare identity/negotiation generations with identity reread in iAP2 process; no display activation gate on builder evidence. Preserve native apps, sidebars, virtual cluster and standard fallback. Combined source bundle verification and user-approved release remain outstanding.

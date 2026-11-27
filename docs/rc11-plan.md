# RC11 scope and cleanup

Stage 1 is behavior-preserving repository cleanup. Stage 2 improves capture of actual runtime interactions, including pre-session native setup, stream routing and seat commands. Stage 3 implements evidence-backed corrections. Stage 4 adds an opt-in historical heated-seat behavior path after comparing RC5 and RC6 with RC10; preserve physical feedback and avoid competing actuation paths. Stage 5 investigates theme negotiation against upstream and EV6; stage 6 validates and packages.

RC10 vehicle observations: source switching, steering wheel media controls, native shortcuts, heated-seat physical feedback and Apple/Google Maps cluster guidance worked; seat writes, ventilation representation, passenger temperature, SYNC, radio catalog/metadata, HD multicast, SXM numbering/favorites, vehicle distance display, Waze, theme selection, wallpaper and parked video failed. NEMESIS 1.17.1 upgrade succeeded. CPUL export appeared functional but runtime interaction coverage was sparse. WCP/STBT exporter fixes belong to their repositories.

## Virtual cluster evidence

Compared current K5 source with lths/carplay-ultra-lite-ev6 main (CpClimate.java and native/hooks.c) and SafariForest/carplay-ultra-lite-gen5w main README. Both K5 and EV6 retain ClusterSink on TCP 7132, full instrument panel, alternate display type 111 and native alt_stream reply. K5 uses themed IDs only when provider preflight and sidebars are enabled; EV6 uses Ultra directly. K5 gates viewAreas on the phone offer; EV6 does not. K5 SETUP without VSP generally leaves reply stock except optional independent UI Sync. Type 130 is a multiplex of client UUIDs, not a single theme stream.

Base project documents working theme selection on Ioniq 6 with modified E-GMP EV Routing identity provider and online re-pairing. K5 uses shared WCP identity instead. Do not install competing routing patches. Compare actual accessory configuration, /info, SETUP offers/replies, hook entry/return, listener readiness and received bytes before changing negotiation. Virtual cluster should participate even if physical cluster cannot render it; do not send graphics to physical cluster or interfere with native TBT. No theme fix proven yet.

RC5/RC6 snapshots retained in rebased history (9f121c86, fa0f940a); original tags remain available. Do not remove experimental paths merely because current vehicle tests failed until dependencies and evidence are reviewed.

## Agreed RC11 display behavior

Keep Ultra Sidebars and identity dependency gates. Prepare display configuration and listeners before relevant negotiation. Keep virtual cluster and native TBT independent; receive and discard virtual video. Center and cluster layouts, wallpaper and palettes are phone-managed; remove forced-layout experiments only after auditing persisted settings. Restore upstream-style viewAreas negotiation with sidebars after validating the native gate. Add independent experimental widescreen view areas selector (cpc-wide: 0 disabled/default, 1 whole display, 2 right-hand area). Coordinate scaling with PPS. Do not implement WCP or STBT changes here. WCP owns group 20/21, stock/explicit Kia K5 gasoline identity, startup StartSession and optional FTv2 experiments; integrate the new provider only when available.

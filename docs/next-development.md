# Next testing release — development notes

## Theme appearance (center display only)
- Goal: populate native Theme Selector with all compatible assets, Midnight initial default, user choice retained.
- First experiment: request the existing, source-backed Modular/blue layout during the virtual cluster handshake, only for Cluster_Display. This is a bootstrap hypothesis, not proof of center-screen palette support. Midnight identifier and center-display layout metadata are not verified; do not invent them or claim the selector is fixed.
- No physical cluster changes. Preserve current wireless provider, display IDs and DBX cabin model.

## Climate
- Current combined seat range -3..3 is advertised but single-stage heating and missing ventilation are reported; feedback reads -1. Need OEM dump and actual service metadata to determine correct getters and layout.
- Wheel control overlaps HVAC and does not actuate hardware. Initial defensive fix records a pending press only after the binder transaction returns successfully. This is not hardware confirmation.
- Investigate separate seat fan services and explicit layout metadata against original schema before editing production service types.

## Deferred / dependencies
- Session teardown crashes deferred at user's request; do not change working wireless negotiation.
- Native volume/media popup suppression and radio integration require firmware dump (SystemUI, Mobis_SystemPopup, HKMC_MediaGate, framework2, media_cmd and OEM USM classes if available).
- Parked video remains unverified; keep safety interlock.
- Do not publish until changes reviewed and tested. Keep current release version during development.

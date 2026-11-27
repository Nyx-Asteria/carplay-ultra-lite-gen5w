# Matched WCP source compatibility

Pinned WCP source: [3b951c745ce78fde4ca6085b28d682c7d629bb98](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa/tree/3b951c745ce78fde4ca6085b28d682c7d629bb98). Reviewed actual exported interface, manifest, settings and Wireless. API v3 shape matches CPUL reflection, full legacy theme tuple and three-argument registration. Both manifests accept DL322 USA 260507 and HMG-API >=1.1.0-rc.10; WCP exports API, CPUL requires WCP and does not own libiap2session. Both observe DiPO session events; both patch libCarPlayCore, so byte-range compatibility is a separate check, not inferred from manifests.

For a clean local checkout at the exact pinned WCP commit, run:

```sh
bash source/tests/run-companion-contract.sh /path/to/wcp
```

Requires JDK 17 (Java 8 target), Python 3 and Git. Compiles actual provider API and production CPUL reflection shape checker without adding any API to CPUL's bundle; verifies tuple and gasoline representation, manifest targets and defaults. No network access or CI credentials needed. It does not test HMG-API registry class loading, native runtime relocation, built package installation, runtime config or phone acceptance.

Combined K5 settings: enable WCP bridge and explicit k5_vehicle_identity; restart after identity change. Keep WCP tbt off with STBT. Do not install competing E-GMP identity patch. CPUL owns display and theme contribution, WCP owns identity and StartSession, STBT navigation and PPS scaling. WCP manifest still labels rc.6 and CPUL rc.10 at these source snapshots; neither label proves matching binary compatibility. Build matched source bundles, version-align during authorized release. FileTransfer research remains independent and disabled.

## CarPlayCore static recipe audit

Compared the actual CPUL memory recipe and both WCP recipes from the pinned source. CPUL has 8 stock verification windows and 8 stock writes (4 branches, 4 byte patches). WCP 260507 Wi-Fi recipe has 2 verification windows and 4 writes; alternate 260903 frame recipe has 1 verification window and 2 writes. No cross-owner stock write overlaps the other's verification window or write range. Both 260507 recipes identify stock MD5 d3a47296437df56fb1ffc7ac3dfca4cb. The alternate 260903 recipe is not the K5 target, but checked conservatively anyway. The new runner checks these ranges automatically and rejects unknown directives. Separately mapped segment/rel32 entries are not stock file offsets and are excluded; runtime relocation, in-memory mapping, loader ordering, and actual application still require verification. This is static source compatibility, not proof of a successful combined patch.

## STBT and PPS source ownership audit

STBT pinned [e877025](https://github.com/Nyx-Asteria/Standalone_TBT/tree/e8770256ff6679546c483af43aed3f7a6b19b998); PPS pinned [bd57be3](https://github.com/Nyx-Asteria/Phone-Projection-Scaling/tree/bd57be364d95d9542b5aaf81269fa2b8dc1d35e2). Actual STBT manifest overlaps CPUL only at two session lifecycle observers, not replacement hooks or native libraries. STBT lane-notify guard and WCP identification patch use disjoint stock verification/write regions. The three AA TBT recipes have identical executable directives after excluding comments and recipe IDs (not byte-identical files). Explicitly disable integrated WCP TBT, including saved settings, with STBT.

PPS v1.0.0 intentionally defers its CarPlay recipe whenever CPUL is enabled or public ownership state cannot be established; CPUL reads PPS enabled state and carplay_scale from public state.json, accepts 50–100 in increments of 5, otherwise uses 100. Android Auto remains PPS-owned. This is deliberate mutual exclusion, not non-overlapping native branches. When CPUL cannot initialize, disable CPUL and restart for standalone PPS. PPS standalone and Android Auto remain unverified in vehicle.

Extended optional host runner: `bash source/tests/run-companion-contract.sh /path/to/wcp /path/to/stbt /path/to/pps`. Checks clean pinned source, iAP2 recipe ranges, shared observers, normalized AA recipe directives and PPS ownership/settings source. Not a combined build or runtime test.

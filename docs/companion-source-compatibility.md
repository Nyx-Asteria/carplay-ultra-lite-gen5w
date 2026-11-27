# Matched WCP source compatibility

Pinned WCP source: [3b951c745ce78fde4ca6085b28d682c7d629bb98](https://github.com/Nyx-Asteria/hmgapi-wireless-cp-aa/tree/3b951c745ce78fde4ca6085b28d682c7d629bb98). Reviewed actual exported interface, manifest, settings and Wireless. API v3 shape matches CPUL reflection, full legacy theme tuple and three-argument registration. Both manifests accept DL322 USA 260507 and HMG-API >=1.1.0-rc.10; WCP exports API, CPUL requires WCP and does not own libiap2session. Both observe DiPO session events; both patch libCarPlayCore, so byte-range compatibility is a separate check, not inferred from manifests.

For a clean local checkout at the exact pinned WCP commit, run:

```sh
bash source/tests/run-companion-contract.sh /path/to/wcp
```

Requires JDK 17 (Java 8 target), Python 3 and Git. Compiles actual provider API and production CPUL reflection shape checker without adding any API to CPUL's bundle; verifies tuple and gasoline representation, manifest targets and defaults. No network access or CI credentials needed. It does not test HMG-API registry class loading, native recipe overlap, built package installation, runtime config or phone acceptance.

Combined K5 settings: enable WCP bridge and explicit k5_vehicle_identity; restart after identity change. Keep WCP tbt off with STBT. Do not install competing E-GMP identity patch. CPUL owns display and theme contribution, WCP owns identity and StartSession, STBT navigation and PPS scaling. WCP manifest still labels rc.6 and CPUL rc.10 at these source snapshots; neither label proves matching binary compatibility. Build matched source bundles, version-align during authorized release. FileTransfer research remains independent and disabled.

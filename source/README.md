# CPUL K5 source guide

This is the active K5 port, not the upstream Ioniq 6 or EV6 package. RC11 development starts from the RC10 vehicle-tested baseline. See [RC11 plan](../docs/rc11-plan.md) and [integration](../docs/integration.md). For original upstream technical notes, see [historical source guide](https://github.com/Nyx-Asteria/carplay-ultra-lite-gen5w/blob/fbeb2beac5fc32249595dbdbd2a306b8d39fb18d/source/README.md).

## Source ownership

| Path | Responsibility |
| --- | --- |
| mod/src/ioniq6/cpclimate | HMG-API hooks, vehicle services, projection channels and session logging |
| mod/settings.json | User-facing settings and defaults |
| mod/mod.json | Package identity, supported firmware, hooks, dependency and network declarations |
| mod/tools/video | Park-gated video engine, pinned third-party dependencies and isolated tests |
| native | Thumb-2 native CarPlayCore hooks |
| mod/recipes | Generated firmware-specific HMG-API memory recipe |
| data | LIVI-derived vehicle-state data and generation source |
| tools | Native recipe and data generation utilities |
| tests | Host regression suite and additional diagnostic fixtures |
| check | Separate read-only compatibility checker |

The inherited Java package name is intentionally retained; do not rename it without updating the manifest, build rules and all reflective consumers. WCP owns iAP2 identification; CPUL owns display and vehicle-state integration. Do not apply the retired provider overlay.

## Development regression

Use the pinned HMG-API SDK from the development workflow. Build the core using the SDK before running the shared host suite.

```sh
export HMGAPI_SDK=/absolute/path/to/sdk
export ANDROID_HOME=/absolute/path/to/android-sdk
sh "$HMGAPI_SDK/bin/build-mod.sh" source/mod out/core out/work
bash source/tests/run-regressions.sh
sh source/mod/tools/video/fetch-libs.sh
HMGAPI_MOD_CLASSES="$PWD/out/work/classes" HMGAPI_R8_CLASSPATH_ARGS="--classpath $HMGAPI_SDK/lib/hmgapi-runtime.jar --classpath $HMGAPI_SDK/lib/hmgapi-runtime-stubs.jar" sh source/mod/build.sh "$PWD/out/core"
unzip -t out/core/hooks.jar
```

The video engine is merged into one dex for API 19. Keep dependency checksums, TLS validation and Park safety checks intact. Host tests are not vehicle validation.

## Native changes

Only regenerate the recipe from the verified matching stock library, using `python3 source/tools/build.py <stock-library> <HMG-API-checkout> <output-dir> full`. Review all hook sites, relocation targets and generated recipe changes before packaging. Do not ship proprietary firmware. The checked-in recipe is a separate build input; native source edits alone do not update runtime behavior.

## Additional test fixtures

The shared regression runner is the automated baseline. Additional tests in this directory are retained until reviewed: CarApiTest requires generated OEM stand-ins, RecipePlaceTest requires a stock library, VehicleStateTest needs the matching LIVI replay, and VsChannelTest needs the Python phone simulation. IapBridgeTest, IapChannelTest, DescriptionCheck and the native host fixture require independent compatibility review before promotion to CI.

## Release

Follow the mandatory [pre-RC consolidation policy](../docs/release-consolidation.md). Use the signed release workflow with pinned companion repositories. No publication without explicit approval. Preserve permanent author identity and verify all packages. See the root README and current release testing guide.

## Attribution

Based on LIVI by Lasse Heitgres (https://github.com/f-io/LIVI). Original CPUL by SafariForest; EV6 reference by lths. See repository LICENSE, NOTICE, source/mod/NOTICE.md and data/LIVI-COMMIT. The upstream source guide and retired development infrastructure remain available in history.

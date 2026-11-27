#!/usr/bin/env bash
# Shared host regression suite. Run from repository root after SDK and core build.
set -euo pipefail
: "${HMGAPI_SDK:?SDK missing}"
SDK="$HMGAPI_SDK"
AJ=$(find "$ANDROID_HOME/platforms" -name android.jar | sort -V | tail -1)
mkdir -p out/tests
CP="$PWD/out/work/classes:$AJ:$SDK/lib/hmgapi-runtime.jar:$SDK/lib/hmgapi-runtime-stubs.jar"
# Each entry is compiled and run against the production core classes.
run_core_test() {
  local test=$1
  shift
  javac --release 8 -cp "$CP" -d out/tests "source/tests/$test.java"
  java -cp "out/tests:$CP" "ioniq6.cpclimate.$test" "$@"
}

# Session logging, parked safety, climate and radio.
for T in SessionLogPolicyTest SessionLogWriterTest SessionUsbExportTest ProjectionLogCaptureTest PreSessionEventsTest ParkedStateTest PassengerTempTraceTest SeatCommandTraceTest RadioActivationTraceTest FmSeekTraceTest RadioSourceRequestTest SxmCatalogRequestTest SxmLiveMetadataTest SeatSchemaTest ClimateMapTest VolumePanelPolicyTest RadioPolicyTest; do
  run_core_test "$T"
done

python3 - <<'PY'
import plistlib
with open('out/python.bplist','wb') as f:
    plistlib.dump(dict(small=7,mid=300,wide=70000,huge=5000000000,neg=-2,s='Cluster_Display',u='Zürich ✓',t=True,f=False,d=bytes([1,2,3]),l=[1,'two'],n={'x':2},r=1.5,long='x'*40),f,fmt=plistlib.FMT_BINARY)
PY
run_core_test UiSyncTest out/tests out/python.bplist out/java.bplist
javac --release 8 -d out/tests source/mod/tools/video/src/ioniq6/cpclimate/video/PlaybackReport.java source/mod/tools/video/tests/PlaybackReportTest.java source/mod/tools/video/src/ioniq6/cpclimate/video/VideoSessionPolicy.java source/mod/tools/video/tests/VideoSessionPolicyTest.java
java -cp out/tests ioniq6.cpclimate.video.PlaybackReportTest
java -cp out/tests ioniq6.cpclimate.video.VideoSessionPolicyTest

run_core_test RuntimeEnvironmentTest

run_core_test ThemeOwnershipTest
run_core_test NativeProfileTest

run_core_test VehiclePageStateTest

run_core_test VehicleDeliveryOwnershipTest

# Exercise simultaneous lane startup and session replacement against production code.
mkdir -p out/tests/session-pair
run_core_test SessionPairTest out/tests/session-pair

python3 source/tests/session_settings_test.py

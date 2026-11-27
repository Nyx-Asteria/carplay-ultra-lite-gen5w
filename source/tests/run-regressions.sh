#!/usr/bin/env bash
# Shared host regression suite. Run from repository root after SDK and core build.
set -euo pipefail
: "${HMGAPI_SDK:?SDK missing}"
SDK="$HMGAPI_SDK"
AJ=$(find "$ANDROID_HOME/platforms" -name android.jar | sort -V | tail -1)
mkdir -p out/tests
CP="$PWD/out/work/classes:$AJ:$SDK/lib/hmgapi-runtime.jar:$SDK/lib/hmgapi-runtime-stubs.jar"
javac --release 8 -cp "$CP" -d out/tests source/tests/SessionLogPolicyTest.java source/tests/SessionLogWriterTest.java source/tests/SessionUsbExportTest.java source/tests/ProjectionLogCaptureTest.java source/tests/PreSessionEventsTest.java
for T in SessionLogPolicyTest SessionLogWriterTest SessionUsbExportTest ProjectionLogCaptureTest PreSessionEventsTest; do
  java -cp "out/tests:$CP" "ioniq6.cpclimate.$T"
done
javac --release 8 -cp "$CP" -d out/tests source/tests/ParkedStateTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.ParkedStateTest
javac --release 8 -cp "$CP" -d out/tests source/tests/PassengerTempTraceTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.PassengerTempTraceTest
javac --release 8 -cp "$CP" -d out/tests source/tests/SeatCommandTraceTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.SeatCommandTraceTest
javac --release 8 -cp "$CP" -d out/tests source/tests/SeatSchemaTest.java
javac --release 8 -cp "$CP" -d out/tests source/tests/RadioActivationTraceTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.RadioActivationTraceTest
javac --release 8 -cp "$CP" -d out/tests source/tests/RadioSourceRequestTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.RadioSourceRequestTest
javac --release 8 -cp "$CP" -d out/tests source/tests/SxmCatalogRequestTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.SxmCatalogRequestTest
javac --release 8 -cp "$CP" -d out/tests source/tests/SxmLiveMetadataTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.SxmLiveMetadataTest
java -cp "out/tests:$CP" ioniq6.cpclimate.SeatSchemaTest
javac --release 8 -cp "$CP" -d out/tests source/tests/ClimateMapTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.ClimateMapTest
javac --release 8 -cp "$CP" -d out/tests source/tests/VolumePanelPolicyTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.VolumePanelPolicyTest
javac --release 8 -cp "$CP" -d out/tests source/tests/RadioPolicyTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.RadioPolicyTest
python3 - <<'PY'
import plistlib
with open('out/python.bplist','wb') as f:
    plistlib.dump(dict(small=7,mid=300,wide=70000,huge=5000000000,neg=-2,s='Cluster_Display',u='Zürich ✓',t=True,f=False,d=bytes([1,2,3]),l=[1,'two'],n={'x':2},r=1.5,long='x'*40),f,fmt=plistlib.FMT_BINARY)
PY
javac --release 8 -cp "$CP" -d out/tests source/tests/UiSyncTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.UiSyncTest out/tests out/python.bplist out/java.bplist
javac --release 8 -d out/tests source/mod/tools/video/src/ioniq6/cpclimate/video/PlaybackReport.java source/mod/tools/video/tests/PlaybackReportTest.java source/mod/tools/video/src/ioniq6/cpclimate/video/VideoSessionPolicy.java source/mod/tools/video/tests/VideoSessionPolicyTest.java
java -cp out/tests ioniq6.cpclimate.video.PlaybackReportTest
java -cp out/tests ioniq6.cpclimate.video.VideoSessionPolicyTest

javac --release 8 -cp "$CP" -d out/tests source/tests/RuntimeEnvironmentTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.RuntimeEnvironmentTest

javac --release 8 -cp "$CP" -d out/tests source/tests/ThemeOwnershipTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.ThemeOwnershipTest

javac --release 8 -cp "$CP" -d out/tests source/tests/VehiclePageStateTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.VehiclePageStateTest

javac --release 8 -cp "$CP" -d out/tests source/tests/VehicleDeliveryOwnershipTest.java
java -cp "out/tests:$CP" ioniq6.cpclimate.VehicleDeliveryOwnershipTest

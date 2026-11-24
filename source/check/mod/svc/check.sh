#!/system/bin/sh
# CarPlay Ultra-Lite compatibility check (read-only). Runs once when Android is up: writes the report to
# $HMGAPI_DATA/report, then copies it into CarPlayUltraLite-Check on every USB stick that is in (or goes
# in within 30 minutes). The checker only reads; see cp-climate-check.md for what it looks at.
OUT=$HMGAPI_DATA/report
rm -rf "$OUT"
mkdir -p "$OUT"
sleep 20    # let the car's climate, seat and body services finish starting
CLASSPATH=$HMGAPI_MOD_DIR/cp-climate-check.jar app_process /system/bin ioniq6.cpclimate.check.Main "$OUT"
# Explicit, fixed allowlist of OEM binaries needed for later interface analysis.
# No user data, settings, logs or pairing credentials are collected here.
mkdir -p "$OUT/radio"
for f in /system/app/HKMC_RadioNA.apk /system/app/HKMC_RadioNA.odex /system/framework/automotive.jar /system/framework/automotive.odex /system/framework/automotive-service.jar /system/framework/automotive-service.odex; do
    if [ -f "$f" ]; then
        cp "$f" "$OUT/radio/" && echo "$f copied" >> "$OUT/radio/inventory.txt"
    else
        echo "$f missing" >> "$OUT/radio/inventory.txt"
    fi
done
echo "check finished: $(ls "$OUT")"
n=0
while [ $n -lt 180 ]; do
    copied=0
    for d in $(hmgapid usb); do
        cp "$OUT/cp-climate-check.md" "$OUT/cp-climate-check.json" "$d/" 2>/dev/null && mkdir -p "$d/radio" && cp "$OUT/radio/"* "$d/radio/" && copied=1
    done
    if [ $copied = 1 ]; then
        echo "report copied to the USB stick"
        exit 0
    fi
    n=$((n + 1))
    sleep 10
done
echo "no USB stick seen within 30 minutes; the report stays in $OUT"

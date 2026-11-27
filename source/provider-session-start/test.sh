#!/bin/sh
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
M="$HERE/overlay/wireless-carplay-aa"
S="$M/src/hmgapi/mods/wireless"
T=$(mktemp -d);trap 'rm -rf "$T"' EXIT
javac --release 7 -Xlint:-options -d "$T" "$S/api/CarPlayIdentification.java" "$S/IdentityRegistry.java" "$S/StartSessionCodec.java" "$M/native/identity/StartSessionCodecTest.java"
java -cp "$T" hmgapi.mods.wireless.StartSessionCodecTest

#!/bin/sh
# Apply only to the reviewed public Wireless base; build and sign separately.
set -eu
HERE=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
DEST=$(CDPATH= cd -- "${1:?provide Wireless checkout}" && pwd)
PIN=23c564fb09d9951d2c81e1fe599bf1cd058918fb
test "$(git -C "$DEST" rev-parse HEAD)" = "$PIN" || { echo "Unexpected Wireless base" >&2; exit 1; }
test -z "$(git -C "$DEST" status --porcelain)" || { echo "Wireless checkout not clean" >&2; exit 1; }
cp -R "$HERE/overlay/." "$DEST/"

#!/usr/bin/env bash
# Optional cross-repository source test. No network, no provider API bundled.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
wcp="$(cd "${1:?usage: bash source/tests/run-companion-contract.sh /path/to/pinned-wcp}" && pwd)"
pin=3c3d2489b5bd4b8f4b8d37a5a451341701f78bce
test "$(git -C "$wcp" rev-parse HEAD)" = "$pin"
test -z "$(git -C "$wcp" status --porcelain -- wireless-carplay-aa)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
api="$wcp/wireless-carplay-aa/src/hmgapi/mods/wireless/api/CarPlayIdentification.java"
javac --release 8 -d "$work" "$api" \
  "$root/source/mod/src/ioniq6/cpclimate/ThemeNegotiation.java" \
  "$root/source/tests/CompanionContractTest.java"
java -cp "$work" ioniq6.cpclimate.CompanionContractTest
python3 - "$root/source/mod/mod.json" "$wcp/wireless-carplay-aa/mod.json" "$wcp/wireless-carplay-aa/settings.json" <<'PY'
import json,sys
c,w,s=(json.load(open(p)) for p in sys.argv[1:])
assert c['id']=='carplay-ultra-lite-k5' and c['version']=='1.0.0-rc.11'
assert w['version']=='2.2.1-rc.7'
assert w['id']=='wireless-carplay-aa' and w['id'] in c['requires']
assert 'egmp-carplay-ev-routing' in c['conflicts']
assert c['hmgapi']==w['hmgapi']=='>=1.1.0-rc.10'
assert 'hmgapi.mods.wireless.api' in w['api']['packages']
def target(m):
    return any('DL322' in ([x['model']] if isinstance(x['model'],str) else x['model'])
               and x['region']=='USA' and x['firmware']=='260507' for x in m['system'])
assert target(c) and target(w)
assert 'libiap2session.so' not in c['hooks']['native']
assert 'libiap2session.so' in w['hooks']['native']
settings={x['key']:x for x in s['settings']}
assert settings['bridge']['default'] is True
assert settings['tbt']['default'] is False
assert settings['k5_vehicle_identity']['default'] is False
assert settings['k5_vehicle_identity']['apply']=='restart'
print('Pinned companion manifest contract passed')
PY

python3 "$root/source/tests/companion_recipe_ranges.py" \
  "$root/source/mod/recipes/carplay-climate-m1.recipe" \
  "$wcp/wireless-carplay-aa/recipes/carplay-core-wifi.recipe" \
  "$wcp/wireless-carplay-aa/recipes/carplay-core-frames-ef49c09b.recipe"

# Optional STBT and PPS checkouts: both must be supplied together.
if [ "$#" -gt 1 ]; then
  stbt="$(cd "${2:?STBT checkout required}" && pwd)"
  pps="$(cd "${3:?PPS checkout required}" && pwd)"
  test "$(git -C "$stbt" rev-parse HEAD)" = fac2ec367028a97334d548c53d5930d768ec2694
  test "$(git -C "$pps" rev-parse HEAD)" = bd57be364d95d9542b5aaf81269fa2b8dc1d35e2
  test -z "$(git -C "$stbt" status --porcelain)"
  test -z "$(git -C "$pps" status --porcelain)"
  python3 -c 'import json,sys; assert json.load(open(sys.argv[1]))["version"]=="1.0.0-rc.4"' "$stbt/mod.json"
  python3 "$root/source/tests/companion_recipe_ranges.py" \
    "$wcp/wireless-carplay-aa/recipes/carplay-identification-fc7.recipe" \
    "$stbt/recipes/lane-notify-guard.recipe"
  python3 "$root/source/tests/companion_ownership.py" "$root" "$wcp" "$stbt" "$pps"
fi

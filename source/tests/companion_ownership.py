#!/usr/bin/env python3
"""Pinned companion manifest, AA recipe and PPS settings compatibility."""
import json
import pathlib
import sys

root,wcp,stbt,pps=map(pathlib.Path,sys.argv[1:])
def read(p):
    return json.loads(p.read_text())
c=read(root/'source/mod/mod.json')
w=read(wcp/'wireless-carplay-aa/mod.json')
t=read(stbt/'mod.json')
p=read(pps/'mod.json')
assert t['id']=='standalone-tbt'
assert p['id']=='phone-projection-scaling'
assert t['hmgapi']==c['hmgapi']==w['hmgapi']
assert not set(c['hooks']['native']) & set(t['hooks']['native'])
assert set(c['hooks']['points']) & set(t['hooks']['points']) == {
    'dipo.carplay.session_started','dipo.carplay.session_finalized'}
for point in set(c['hooks']['points']) & set(t['hooks']['points']):
    assert c['hooks']['points'][point]==t['hooks']['points'][point]=='observe'
def directives(path):
    return [line.split() for line in path.read_text().splitlines()
            if line.strip() and not line.lstrip().startswith(('#','id '))]
aa=w['hooks']['native']['libautoreceiver_infobank.so']
assert aa==t['hooks']['native']['libautoreceiver_infobank.so']
for recipe in aa:
    assert directives(wcp/'wireless-carplay-aa'/recipe)==directives(stbt/recipe),recipe
settings={x['key']:x for x in read(pps/'settings.json')['settings']}
for name in ('carplay_scale','android_auto_scale'):
    item=settings[name]
    assert item['default']=='100' and item['apply']=='restart'
    assert [x['value'] for x in item['options']]==[str(i) for i in range(50,101,5)]
assert 'libCarPlayCore.so' in p['hooks']['native']
assert p['hooks']['points']=={'gpm.video_sink.nearest':'change'}
# PPS must defer its overlapping /info hook while CPUL is enabled.
# This is a source guard, not a runtime test of the public-state loader.
source=(pps/'src/hmgapi/mods/scaling/ProjectionScaling.java').read_text()
assert 'if (cpulOwnsInfo(mod)) { clear(mod); return; }' in source
assert 'mods.optJSONObject("carplay-ultra-lite-k5")' in source
consumer=(root/'source/mod/src/ioniq6/cpclimate/CpClimate.java').read_text()
assert 'mods.optJSONObject("phone-projection-scaling")' in consumer
assert 'optString("carplay_scale", "100")' in consumer
print('Pinned STBT/PPS source ownership checks passed')

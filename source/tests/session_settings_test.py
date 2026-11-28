#!/usr/bin/env python3
"""Keep session-header settings and manifest defaults aligned."""
import json
import re
from pathlib import Path
root = Path(__file__).resolve().parents[1]
manifest = json.loads((root / "mod/settings.json").read_text())["settings"]
source = (root / "mod/src/ioniq6/cpclimate/SessionSettings.java").read_text()
keys = re.findall(r'"([^"]+)"', source.split("KEYS = {", 1)[1].split("};", 1)[0])
defaults = re.findall(r'\b(?:true|false)\b', source.split("DEFAULTS = {", 1)[1].split("};", 1)[0])
assert len(keys) == len(defaults) == len(set(keys)), "invalid settings table"
expected = {x["key"]: x["default"] for x in manifest if x["type"] == "toggle"}
assert dict(zip(keys, (x == "true" for x in defaults))) == expected, "session settings drift"
wide_key = re.search(r'WIDE_KEY="([^"]+)"', source).group(1)
wide_default = re.search(r'WIDE_DEFAULT="([^"]+)"', source).group(1)
choices = {x["key"]: x for x in manifest if x["type"] == "choice"}
assert set(choices) == {wide_key}, "session choice settings drift"
assert choices[wide_key]["default"] == wide_default == "0"
assert [x["value"] for x in choices[wide_key]["options"]] == ["0", "1", "2"]
print("Session settings match manifest")

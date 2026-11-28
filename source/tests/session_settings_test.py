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
print("Session settings match manifest")

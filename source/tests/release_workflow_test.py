#!/usr/bin/env python3
"""Guard release gates, unique jobs and shell syntax."""
from pathlib import Path
import re
import json
import subprocess
import textwrap

root = Path(__file__).resolve().parents[2]
workflow = (root / ".github/workflows/release-bundle.yml").read_text()
header, jobs = workflow.split("\njobs:\n", 1)
assert re.findall(r"^  ([\w-]+):$", jobs, re.M) == ["package", "publish"]
assert "  push:\n    branches: [rc11-final]" in header and "  workflow_dispatch:" in header
assert "default: false" in header
release_gate = "github.event_name == 'push' && github.ref == 'refs/heads/rc11-final' && github.event.head_commit.message == 'Release 1.0.0-rc.11'"
assert jobs.count(release_gate) == 2
assert "if: github.event_name == 'workflow_dispatch' || (" + release_gate + ")" in jobs
assert "if: (github.event_name == 'workflow_dispatch' && inputs.publish == true) || (" + release_gate + ")" in jobs
assert "    needs: package" in jobs
assert '--prerelease' in jobs and '--draft' not in jobs
assert 'test "$VERSION" = "1.0.0-rc.11"' in jobs
manifest = json.loads((root / "source/mod/mod.json").read_text())
assert manifest["id"] == "carplay-ultra-lite-k5"
assert manifest["name"] == "CarPlay Ultra K5"
assert 'AUTHOR_UUID: d515981b-afc8-8230-ae0f-62d32de366bc' in jobs
assert 'CPUL_ZIP="out/packages/carplay-ultra-lite-k5-$VERSION.zip"' in jobs
assert 'bundle="CarPlay-Ultra-K5-$VERSION-bundle.zip"' in jobs
assert "CarPlay-Ultra-K5-*-bundle.zip" in jobs
assert 'VERSION=${bundle#CarPlay-Ultra-K5-}' in jobs
assert 'VERSION=${VERSION%-bundle.zip}' in jobs
assert 'CarPlay Ultra K5 $VERSION' in jobs
assert 'CarPlay-Ultra-Lite-K5' not in jobs and '-testing-bundle.zip' not in jobs
lines = workflow.splitlines()
blocks = 0
for i, line in enumerate(lines):
    if not line.strip().startswith("run: |"):
        continue
    indent = len(line) - len(line.lstrip())
    end = i + 1
    while end < len(lines) and (not lines[end].strip() or len(lines[end]) - len(lines[end].lstrip()) > indent):
        end += 1
    body = textwrap.dedent("\n".join(lines[i+1:end])) + "\n"
    subprocess.run(["bash", "-n"], input=body, text=True, check=True)
    blocks += 1
assert blocks >= 4
print("Release workflow structure and", blocks, "shell blocks passed")

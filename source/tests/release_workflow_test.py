#!/usr/bin/env python3
"""Guard the manual signing workflow against duplicate jobs and broken shell blocks."""
from pathlib import Path
import re
import subprocess
import textwrap

root = Path(__file__).resolve().parents[2]
workflow = (root / ".github/workflows/release-bundle.yml").read_text()
header, jobs = workflow.split("\njobs:\n", 1)
assert re.findall(r"^  ([\w-]+):$", jobs, re.M) == ["package", "publish"], "duplicate or unexpected release jobs"
assert "  push:" not in header and "  workflow_dispatch:" in header, "signing must be manual only"
assert "default: false" in header, "publish must default off"
assert "if: github.event_name == 'workflow_dispatch'" in jobs
assert "if: github.event_name == 'workflow_dispatch' && inputs.publish == true" in jobs
assert "    needs: package" in jobs
assert "--prerelease" in jobs and "--draft" not in jobs
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
assert blocks >= 4, "expected release run blocks missing"
print("Release workflow structure and", blocks, "shell blocks passed")

#!/usr/bin/env bash
# Execute the actual native hooks against the existing CFLite mock.
set -euo pipefail
root="$(cd "$(dirname "$0")/../.." && pwd)"
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
mkdir -p "$work/files"
# The mock uses signed 32-bit relative function offsets; non-PIE keeps
# code and data near one another on the Linux x86_64 host.
cc -std=gnu11 -O0 -g -fno-pie -no-pie -DCPC_HOST_TEST \
  "-DDIR=\"$work/files/\"" "-DVSPI=\"$root/source/data/vspi.bplist\"" \
  -I "$root/source/native" "$root/source/tests/host_test.c" \
  "$root/source/native/hooks.c" -o "$work/host_test"
"$work/host_test"

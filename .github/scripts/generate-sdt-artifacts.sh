#!/usr/bin/env bash

set -e

# Use the same installation as the FPGA build runner.
source /opt/xilinx/2025.2/Vivado/settings64.sh
set -u
test -f "$CUSTOM_SDT_REPO/device_tree/data/device_tree.tcl"

work_dir="$(mktemp -d "${RUNNER_TEMP}/sdt-${VARIANT}.XXXXXX")"
artifact_dir="${RUNNER_TEMP}/sdt-artifacts"
mkdir -p "$artifact_dir"

curl --fail --location --retry 3 --proto '=https' --proto-redir '=https' \
  --output "$work_dir/$XSA_NAME" "${ARTIFACT_URL}${XSA_NAME}"

for revision in reva revb; do
  name="versal-scobc-v1-${VARIANT}-${revision}"
  sdtgen "$GITHUB_WORKSPACE/scripts/gen-sdt.tcl" \
    "$work_dir/$XSA_NAME" "$work_dir/$name" "${BOARD}-${revision}"
  test -s "$work_dir/$name/system-top.dts"
  tar -czf "$artifact_dir/$name.tar.gz" -C "$work_dir" "$name"
done

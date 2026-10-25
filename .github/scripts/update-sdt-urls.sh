#!/usr/bin/env bash

set -eu

[[ "$RELEASE_TAG" =~ ^v[0-9]{4}\.[0-9]{2}$ ]]

for variant in ve2302i ve2302e ve2002e; do
  prefix="${R2_RELEASE_PREFIX%/}"
  if [[ "$variant" == ve2002e ]]; then
    prefix="${R2_RELEASE_VE2002_PREFIX%/}"
  fi
  [[ "$prefix" =~ ^[A-Za-z0-9._/-]+$ ]]
  for revision in reva revb; do
    name="versal-scobc-v1-${variant}-${revision}"
    file="meta-scobc/conf/machineyaml/${name}-sdt-full.yaml"
    url="https://downloads.spacecubics.com/${prefix}/${RELEASE_TAG}/sdt/${name}.tar.gz"
    pattern='^([[:space:]]*-[[:space:]]+--hw-description[[:space:]]+)[^[:space:]]+$'
    [[ "$(grep -Ec "$pattern" "$file")" == 1 ]]
    sed -i -E "s#${pattern}#\1${url}#" "$file"
  done
done

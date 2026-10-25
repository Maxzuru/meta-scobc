#!/usr/bin/env bash

set -eu

: "${R2_BUCKET:?Missing R2_BUCKET}"
[[ "$RELEASE_TAG" =~ ^v[0-9]{4}\.[0-9]{2}$ ]]

artifact_dir="${RUNNER_TEMP}/sdt-artifacts"
commands=()

for variant in ve2302i ve2302e ve2002e; do
  prefix="${R2_RELEASE_PREFIX%/}"
  if [[ "$variant" == ve2002e ]]; then
    prefix="${R2_RELEASE_VE2002_PREFIX%/}"
  fi
  for revision in reva revb; do
    name="versal-scobc-v1-${variant}-${revision}.tar.gz"
    artifact="$artifact_dir/$name"
    test -s "$artifact"
    if (( $(stat --format='%s' "$artifact") > 315000000 )); then
      echo "::error::$name exceeds Wrangler's 315 MB upload limit"
      exit 1
    fi
    object_path="${R2_BUCKET}/${prefix}/${RELEASE_TAG}/sdt/${name}"
    printf -v command 'r2 object put %q --file %q --content-type application/gzip --remote' \
      "$object_path" "$artifact"
    commands+=("$command")
  done
done

{
  echo 'commands<<EOF'
  printf '%s\n' "${commands[@]}"
  echo 'EOF'
} >> "$GITHUB_OUTPUT"

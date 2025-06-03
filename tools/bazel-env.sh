#!/usr/bin/env bash
set -euo pipefail

check_and_export() {
  local name="$1"
  local value="${!name:-}"
  if [ -z "$value" ]; then
    echo "$name not set" >&2
  else
    export "$name"="$value"
    echo "$name=${!name}" >&2
  fi
}

check_and_export LOGOS_AWS_REGION
check_and_export LOGOS_AWS_REGISTRY
check_and_export STORAGE_PG_BACKEND_HOST
check_and_export LOGOS_DEV_COGNITO_USERNAME
check_and_export LOGOS_DEV_COGNITO_PASSWORD

exec "$@"
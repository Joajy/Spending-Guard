#!/usr/bin/env bash

set -Eeuo pipefail

DATABASE_SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${DATABASE_SCRIPT_DIRECTORY}/../.." && pwd)"

COMPOSE_FILE="${COMPOSE_FILE:-${REPOSITORY_ROOT}/deploy/compose.production.yml}"
ENV_FILE="${ENV_FILE:-${REPOSITORY_ROOT}/deploy/.env}"
DATABASE_NAME="${DATABASE_NAME:-spending_guard}"
DATABASE_USER="${DATABASE_USER:-spending_guard}"

require_command() {
  if ! command -v "$1" >/dev/null 2>&1; then
    echo "required command not found: $1" >&2
    exit 1
  fi
}

require_file() {
  if [[ ! -f "$1" ]]; then
    echo "required file not found: $1" >&2
    exit 1
  fi
}

compose() {
  local command=(docker compose)
  if [[ -n "${COMPOSE_PROJECT_NAME:-}" ]]; then
    command+=(--project-name "${COMPOSE_PROJECT_NAME}")
  fi
  command+=(--env-file "${ENV_FILE}" -f "${COMPOSE_FILE}")
  "${command[@]}" "$@"
}

require_command docker
require_file "${COMPOSE_FILE}"
require_file "${ENV_FILE}"

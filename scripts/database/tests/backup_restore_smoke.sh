#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPOSITORY_ROOT="$(cd "${SCRIPT_DIRECTORY}/../../.." && pwd)"
TEMP_DIRECTORY="$(mktemp -d)"
PROJECT_NAME="spending-guard-backup-smoke-${GITHUB_RUN_ID:-local}-$$"
ENV_FILE="${REPOSITORY_ROOT}/deploy/.env.example"
COMPOSE_FILE="${REPOSITORY_ROOT}/deploy/compose.production.yml"
BACKUP_DIRECTORY="${TEMP_DIRECTORY}/backups"

compose() {
  docker compose \
    --project-name "${PROJECT_NAME}" \
    --env-file "${ENV_FILE}" \
    -f "${COMPOSE_FILE}" \
    "$@"
}

cleanup() {
  compose down --volumes --remove-orphans >/dev/null 2>&1 || true
  rm -f -- "${BACKUP_DIRECTORY}"/*
  rmdir "${BACKUP_DIRECTORY}" "${TEMP_DIRECTORY}" 2>/dev/null || true
}
trap cleanup EXIT

compose up -d --wait postgres
compose exec -T postgres psql \
  --username spending_guard \
  --dbname spending_guard \
  --set ON_ERROR_STOP=1 \
  --command "CREATE TABLE backup_probe (value text NOT NULL); INSERT INTO backup_probe VALUES ('original');"

backup_path="$(
  COMPOSE_PROJECT_NAME="${PROJECT_NAME}" \
  COMPOSE_FILE="${COMPOSE_FILE}" \
  ENV_FILE="${ENV_FILE}" \
  BACKUP_DIRECTORY="${BACKUP_DIRECTORY}" \
  "${REPOSITORY_ROOT}/scripts/database/backup.sh"
)"

compose exec -T postgres psql \
  --username spending_guard \
  --dbname spending_guard \
  --set ON_ERROR_STOP=1 \
  --command "UPDATE backup_probe SET value = 'changed';"

COMPOSE_PROJECT_NAME="${PROJECT_NAME}" \
COMPOSE_FILE="${COMPOSE_FILE}" \
ENV_FILE="${ENV_FILE}" \
"${REPOSITORY_ROOT}/scripts/database/restore.sh" \
  "${backup_path}" \
  --confirm-database spending_guard

restored_value="$(
  compose exec -T postgres psql \
    --username spending_guard \
    --dbname spending_guard \
    --tuples-only \
    --no-align \
    --command "SELECT value FROM backup_probe;"
)"

if [[ "${restored_value}" != "original" ]]; then
  echo "restored value mismatch: ${restored_value}" >&2
  exit 1
fi

echo "backup and restore smoke test passed"

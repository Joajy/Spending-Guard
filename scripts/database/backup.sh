#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIRECTORY}/common.sh"

require_command sha256sum

umask 077
BACKUP_DIRECTORY="${BACKUP_DIRECTORY:-${REPOSITORY_ROOT}/backups/postgres}"
mkdir -p "${BACKUP_DIRECTORY}"

timestamp="$(date -u +%Y%m%dT%H%M%SZ)"
backup_name="${DATABASE_NAME}-${timestamp}.dump"
backup_path="${BACKUP_DIRECTORY}/${backup_name}"
partial_path="${backup_path}.partial"
checksum_path="${backup_path}.sha256"
checksum_partial_path="${checksum_path}.partial"

if [[ -e "${backup_path}" || -e "${checksum_path}" ]]; then
  echo "backup already exists: ${backup_path}" >&2
  exit 1
fi

cleanup_partial_files() {
  rm -f -- "${partial_path}" "${checksum_partial_path}"
}
trap cleanup_partial_files EXIT

compose exec -T postgres pg_dump \
  --username "${DATABASE_USER}" \
  --dbname "${DATABASE_NAME}" \
  --format custom \
  --no-owner \
  --no-privileges > "${partial_path}"

if [[ ! -s "${partial_path}" ]]; then
  echo "database dump is empty" >&2
  exit 1
fi

compose exec -T postgres pg_restore --list < "${partial_path}" >/dev/null

checksum_output="$(sha256sum "${partial_path}")"
checksum="${checksum_output%% *}"
printf '%s  %s\n' "${checksum}" "${backup_name}" > "${checksum_partial_path}"

mv "${partial_path}" "${backup_path}"
mv "${checksum_partial_path}" "${checksum_path}"
trap - EXIT

printf '%s\n' "${backup_path}"

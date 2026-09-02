#!/usr/bin/env bash

set -Eeuo pipefail

SCRIPT_DIRECTORY="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
source "${SCRIPT_DIRECTORY}/common.sh"

require_command sha256sum

if [[ $# -ne 3 || "$2" != "--confirm-database" || "$3" != "${DATABASE_NAME}" ]]; then
  echo "usage: $0 <backup.dump> --confirm-database ${DATABASE_NAME}" >&2
  exit 2
fi

backup_path="$1"
require_file "${backup_path}"
require_file "${backup_path}.sha256"

backup_directory="$(cd "$(dirname "${backup_path}")" && pwd)"
backup_name="$(basename "${backup_path}")"
backup_path="${backup_directory}/${backup_name}"

if ! (cd "${backup_directory}" && sha256sum --check --status "${backup_name}.sha256"); then
  echo "backup checksum verification failed: ${backup_path}" >&2
  exit 1
fi

if ! compose exec -T postgres pg_restore --list < "${backup_path}" >/dev/null; then
  echo "backup archive validation failed: ${backup_path}" >&2
  exit 1
fi

running_services="$(compose ps --services --filter status=running)"
application_services=()
for service in backend frontend gateway; do
  if grep -qx "${service}" <<< "${running_services}"; then
    application_services+=("${service}")
  fi
done

if (( ${#application_services[@]} > 0 )); then
  compose stop "${application_services[@]}"
fi

restore_failed() {
  echo "restore failed; application services remain stopped to avoid serving partial data" >&2
}
trap restore_failed ERR

compose exec -T postgres dropdb \
  --username "${DATABASE_USER}" \
  --if-exists \
  --force \
  "${DATABASE_NAME}"
compose exec -T postgres createdb \
  --username "${DATABASE_USER}" \
  "${DATABASE_NAME}"
compose exec -T postgres pg_restore \
  --username "${DATABASE_USER}" \
  --dbname "${DATABASE_NAME}" \
  --exit-on-error \
  --no-owner \
  --no-privileges < "${backup_path}"

trap - ERR
if (( ${#application_services[@]} > 0 )); then
  compose up -d --wait "${application_services[@]}"
fi

printf 'restored %s from %s\n' "${DATABASE_NAME}" "${backup_path}"

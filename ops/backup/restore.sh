#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${BACKUP_ENCRYPTION_PASSWORD:?Set backup encryption passphrase}"
: "${CLINIC_RESTORE_DATABASE:?Set dedicated empty restore database name}"
[[ "$CLINIC_RESTORE_DATABASE" =~ ^clinic_restore_[a-zA-Z0-9_]+$ ]] || { echo 'Refusing restore: database must start clinic_restore_' >&2; exit 2; }
clinic_file=${1:?Usage: restore.sh /path/clinic-TIMESTAMP.sql.gz.enc}
[[ -f "$clinic_file" && -f "$clinic_file.sha256" ]] || { echo 'Backup or checksum missing' >&2; exit 2; }
shasum -a 256 -c "$clinic_file.sha256"
clinic_container=${CLINIC_RESTORE_CONTAINER:?Set separate MySQL restore container name}
clinic_temp=$(mktemp)
trap 'rm -f "$clinic_temp"' EXIT
# Fully decrypt and decompress BEFORE piping any SQL into MySQL.
openssl enc -d -aes-256-cbc -pbkdf2 -iter 200000 -pass env:BACKUP_ENCRYPTION_PASSWORD -in "$clinic_file" | gzip -d > "$clinic_temp"
clinic_query="SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='$CLINIC_RESTORE_DATABASE'"
clinic_count=$(docker exec "$clinic_container" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root -N -e "$1"' sh "$clinic_query")
[[ "$clinic_count" == 0 ]] || { echo 'Refusing restore into nonempty database' >&2; exit 2; }
docker exec -e RESTORE_DB="$CLINIC_RESTORE_DATABASE" "$clinic_container" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -u root -e "CREATE DATABASE IF NOT EXISTS $RESTORE_DB"'
docker exec -i -e RESTORE_DB="$CLINIC_RESTORE_DATABASE" "$clinic_container" sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" exec mysql -u root "$RESTORE_DB"' < "$clinic_temp"
printf 'Restored into isolated database %s. Verify counts and app compatibility before any cutover.\n' "$CLINIC_RESTORE_DATABASE"

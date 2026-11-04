#!/usr/bin/env bash
set -euo pipefail
umask 077
: "${BACKUP_ENCRYPTION_PASSWORD:?Set backup encryption passphrase}"
clinic_compose=${CLINIC_COMPOSE_FILE:-compose.production.yml}
clinic_env=${CLINIC_ENV_FILE:-.env.production}
clinic_dir=${CLINIC_BACKUP_DIR:-./backups}
clinic_retention=${CLINIC_BACKUP_RETENTION_DAYS:-14}
clinic_database=${CLINIC_BACKUP_DATABASE:-}
[[ -z "$clinic_database" || "$clinic_database" =~ ^[a-zA-Z0-9_]+$ ]] || { echo 'Invalid database name' >&2; exit 2; }
[[ "$clinic_retention" =~ ^[0-9]+$ ]] && (( clinic_retention >= 1 )) || { echo 'Invalid retention days' >&2; exit 2; }
mkdir -p "$clinic_dir"
clinic_stamp=$(date -u +%Y%m%dT%H%M%SZ)
clinic_file="$clinic_dir/clinic-$clinic_stamp.sql.gz.enc"
trap 'rm -f "$clinic_file.partial"' EXIT
docker compose --env-file "$clinic_env" -f "$clinic_compose" exec -T -e BACKUP_DB="$clinic_database" mysql sh -c 'MYSQL_PWD="$MYSQL_PASSWORD" exec mysqldump -u "$MYSQL_USER" --single-transaction --routines --triggers --no-tablespaces "${BACKUP_DB:-$MYSQL_DATABASE}"' |
 gzip | openssl enc -aes-256-cbc -salt -pbkdf2 -iter 200000 -pass env:BACKUP_ENCRYPTION_PASSWORD -out "$clinic_file.partial"
mv "$clinic_file.partial" "$clinic_file"
(cd "$clinic_dir" && shasum -a 256 "$(basename "$clinic_file")" > "$(basename "$clinic_file").sha256")
find "$clinic_dir" -name 'clinic-*.sql.gz.enc*' -type f -mtime "+$clinic_retention" -delete
clinic_metric_dir=${CLINIC_BACKUP_METRICS_DIR:-$clinic_dir/metrics}
mkdir -p "$clinic_metric_dir"
chmod 0755 "$clinic_metric_dir"
printf 'clinic_backup_last_success_unixtime %s\n' "$(date +%s)" > "$clinic_metric_dir/clinic_backup.prom.partial"
mv "$clinic_metric_dir/clinic_backup.prom.partial" "$clinic_metric_dir/clinic_backup.prom"
chmod 0644 "$clinic_metric_dir/clinic_backup.prom"
printf 'Encrypted backup created: %s\n' "$clinic_file"

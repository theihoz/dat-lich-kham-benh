#!/usr/bin/env bash
set -euo pipefail
clinic_image=${1:?Usage: deploy.sh ghcr.io/owner/repo@sha256:DIGEST}
[[ "$clinic_image" =~ ^ghcr\.io/[a-zA-Z0-9_./-]+@sha256:[a-f0-9]{64}$ ]] || { echo 'Use an immutable GHCR digest' >&2; exit 2; }
clinic_env=${CLINIC_ENV_FILE:-.env.production}
clinic_file=${CLINIC_COMPOSE_FILE:-compose.production.yml}
clinic_service_id=$(docker compose --env-file "$clinic_env" -f "$clinic_file" ps -q backend)
clinic_previous=''
if [[ -n "$clinic_service_id" ]]; then clinic_previous=$(docker inspect --format '{{.Config.Image}}' "$clinic_service_id"); fi
export CLINIC_IMAGE="$clinic_image"
docker compose --env-file "$clinic_env" -f "$clinic_file" pull backend
rollback() {
  if [[ -n "$clinic_previous" ]]; then
    echo 'Readiness failed; restoring previous application image (database schema is not rolled back)' >&2
    CLINIC_IMAGE="$clinic_previous" docker compose --env-file "$clinic_env" -f "$clinic_file" up -d --no-deps backend
  fi
}
if ! docker compose --env-file "$clinic_env" -f "$clinic_file" up -d --wait --wait-timeout 180 backend; then rollback; exit 1; fi
# Public TLS is deliberately verified after container readiness.
: "${CLINIC_PUBLIC_URL:?Set CLINIC_PUBLIC_URL=https://your-domain}"
if ! curl --fail --silent --show-error --retry 5 --retry-delay 2 "$CLINIC_PUBLIC_URL/actuator/health/readiness" | python3 -c 'import sys,json; assert json.load(sys.stdin)["status"]=="UP"'; then rollback; exit 1; fi
if [[ -n "$clinic_previous" ]]; then (umask 077; printf '%s\n' "$clinic_previous" > .clinic-previous-image); fi
echo 'Deployment passed readiness; run authenticated smoke before approving rollout.'

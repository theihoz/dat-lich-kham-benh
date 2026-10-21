#!/usr/bin/env sh
set -eu
clinic_root=$(CDPATH= cd -- "$(dirname -- "$0")" && pwd)
cd "$clinic_root/backend"
exec sh "$clinic_root/scripts/maven.sh" spring-boot:run -Dspring-boot.run.profiles=demo "$@"

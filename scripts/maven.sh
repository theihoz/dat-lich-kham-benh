#!/usr/bin/env sh
# Dùng Maven đã cài; hỗ trợ bản Maven đi kèm Oracle Java extension trên macOS.
set -eu
if [ -n "${MVN:-}" ]; then
  exec "$MVN" "$@"
fi
if command -v mvn >/dev/null 2>&1; then
  exec mvn "$@"
fi
for clinic_mvn in "$HOME"/.vscode/extensions/oracle.oracle-java-*/nbcode/java/maven/bin/mvn.sh; do
  if [ -x "$clinic_mvn" ]; then
    exec "$clinic_mvn" "$@"
  fi
done
echo 'Cần Maven 3.9+ và JDK 21. Hoặc đặt MVN=/đường/dẫn/tới/mvn.' >&2
exit 1

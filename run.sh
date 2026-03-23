#!/usr/bin/env bash
set -euo pipefail

cd "$(dirname "$0")"

./mvnw package

JAVA_OPTS_VALUE="${JAVA_OPTS:-}"
if [[ -n "${JAVA_OPTS_VALUE}" ]]; then
  # shellcheck disable=SC2206
  JAVA_OPTS_ARR=(${JAVA_OPTS_VALUE})
  exec java "${JAVA_OPTS_ARR[@]}" -jar target/quarkus-app/quarkus-run.jar
fi

exec java -jar target/quarkus-app/quarkus-run.jar


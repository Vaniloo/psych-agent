#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"

if [[ -f "$ROOT_DIR/local.env" ]]; then
  set -a
  source "$ROOT_DIR/local.env"
  set +a
fi

if [[ "${SKIP_INFRA_CHECK:-false}" != "true" ]]; then
  "$ROOT_DIR/scripts/start-local-infra.sh"
fi

cd "$ROOT_DIR/backend"
exec ./mvnw spring-boot:run

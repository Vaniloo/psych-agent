#!/usr/bin/env bash
set -euo pipefail

API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"

curl --fail --silent --show-error "$API_BASE_URL/test/ping"
printf '\n'
curl --fail --silent --show-error "$API_BASE_URL/actuator/health"
printf '\n'
curl --fail --silent --show-error "$API_BASE_URL/help/resources"
printf '\n'

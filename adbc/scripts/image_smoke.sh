#!/usr/bin/env bash
# Smoke-tests a built grok_connect_adbc image: every driver it ships loads in the runtime
# base, the service starts and reports healthy, and a real query round-trips through the
# ClickHouse driver.
#
# Usage: image_smoke.sh <image>
# Needs a ClickHouse reachable from the host network at $CLICKHOUSE_HOST:8123 (user
# `default`, password $CLICKHOUSE_PASSWORD), plus curl, jq and python3 with websockets and
# pyarrow.

set -euo pipefail

IMAGE="${1:?Usage: image_smoke.sh <image>}"
CH_HOST="${CLICKHOUSE_HOST:-127.0.0.1}"
CH_PASSWORD="${CLICKHOUSE_PASSWORD:-clickhouse}"
PORT="${GROK_CONNECT_PORT:-1234}"
BASE_URL="http://127.0.0.1:${PORT}"
NAME="grok-connect-adbc-smoke-$$"
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Drivers load (--probe-drivers) ==="
docker run --rm "${IMAGE}" --probe-drivers

echo "=== Service starts ==="
trap 'docker logs "${NAME}" 2>&1 | tail -n 100; docker rm -f "${NAME}" >/dev/null 2>&1 || true' EXIT
docker run -d --name "${NAME}" --network host -e GROK_CONNECT_PORT="${PORT}" "${IMAGE}"

health=""
for _ in $(seq 1 60); do
  health="$(docker inspect -f '{{.State.Health.Status}}' "${NAME}")"
  [[ "${health}" == "healthy" ]] && break
  [[ "$(docker inspect -f '{{.State.Running}}' "${NAME}")" == "true" ]] || { echo "container exited"; exit 1; }
  sleep 1
done
[[ "${health}" == "healthy" ]] || { echo "HEALTHCHECK never passed: ${health}"; exit 1; }
[[ "$(curl -sf "${BASE_URL}/health")" == "OK" ]]

info="$(curl -sf "${BASE_URL}/info")"
echo "${info}"
for provider in ClickHouseADBC BigQueryADBC; do
  jq -e --arg p "${provider}" '.providers | index($p)' <<<"${info}" >/dev/null \
    || { echo "/info does not advertise ${provider}"; exit 1; }
done

echo "=== ClickHouse connection test ==="
conn="$(jq -n --arg host "${CH_HOST}" --arg pw "${CH_PASSWORD}" '{
  id: "smoke", dataSource: "ClickHouseADBC",
  parameters: {server: $host, port: 8123},
  credentials: {parameters: {login: "default", password: $pw}}}')"
result="$(curl -sf -X POST -H 'Content-Type: application/json' -d "${conn}" "${BASE_URL}/test")"
[[ "${result}" == "ok" ]] || { echo "/test: ${result}"; exit 1; }

echo "=== ClickHouse query over /query_socket ==="
python3 "${SCRIPT_DIR}/image_smoke_query.py" "ws://127.0.0.1:${PORT}/query_socket" "${CH_HOST}" "${CH_PASSWORD}"

echo "=== Smoke test passed ==="

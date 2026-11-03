#!/usr/bin/env bash
# Smoke test of a built grok_connect image: boots it, checks the runtime contract
# (user, locale, JVM settings) and runs real queries through the HTTP API.
#   main:     SQLite (northwind file shipped in the image; native sqlite-jdbc on musl)
#             and Postgres (service container on localhost:5432).
#   extended: /conn advertises exactly the quarantined providers.
# Also lists the native libraries bundled in the jars and what the musl loader
# cannot resolve in them (informational).
# Usage: jdbc_smoke_test.sh <image> <main|extended>
set -euo pipefail

image=$1
flavor=$2
name=grok-connect-smoke
url=http://localhost:1234
work=$(mktemp -d)

fail() { echo "SMOKE FAIL: $*" >&2; exit 1; }

cleanup() {
  echo "::group::grok_connect container log"
  docker logs "$name" 2>&1 | tail -n 300 || true
  echo "::endgroup::"
  docker rm -f "$name" > /dev/null 2>&1 || true
}
trap cleanup EXIT

docker run -d --name "$name" --network host "$image" > /dev/null

for _ in $(seq 1 90); do
  if curl -sf "$url/health" > /dev/null 2>&1; then break; fi
  if [ "$(docker inspect -f '{{.State.Running}}' "$name")" != "true" ]; then fail "container exited"; fi
  sleep 2
done
[ "$(curl -sf "$url/health")" = "OK" ] || fail "/health did not answer OK"
echo "health: OK"

ids=$(docker exec "$name" id)
echo "id: $ids"
[[ "$ids" == "uid=1001(grok) gid=2001(grok)"* ]] || fail "unexpected user: $ids"
docker exec "$name" sh -c 'echo "LANG=$LANG LC_ALL=$LC_ALL"; java -version 2>&1'
[ "$(docker exec "$name" sh -c 'echo $LANG/$LC_ALL')" = "C.UTF-8/C.UTF-8" ] || fail "locale is not C.UTF-8"
docker exec "$name" sh -c 'grep -qx "networkaddress.cache.ttl=5" "$JAVA_HOME/lib/security/java.security"' \
  || fail "networkaddress.cache.ttl=5 missing from java.security"
docker exec "$name" cat /home/grok/git-info.txt
docker exec "$name" test -f /home/grok/northwind/Northwind.sl3 || fail "northwind files missing"

curl -sf "$url/conn" > "$work/conn.json"
providers=$(python3 -c 'import json,sys; print(",".join(sorted(d["type"] for d in json.load(open(sys.argv[1])))))' "$work/conn.json")
echo "providers: $providers"

# POST /query and check the JSON header of the binary reply: no error, expected row count.
query() {
  local label=$1 body=$2 rows=$3 out="$work/$1.bin"
  curl -sf -X POST "$url/query" -H 'Content-Type: application/json' --data-binary "$body" -o "$out" \
    || fail "$label: /query request failed"
  python3 - "$out" "$rows" "$label" <<'PY'
import json, struct, sys
data = open(sys.argv[1], 'rb').read()
# d42 string header: uint16 type code, float64 (LE) byte length, UTF-8 JSON
length = int(struct.unpack_from('<d', data, 2)[0])
header = json.loads(data[10:10 + length].decode('utf-8'))
label = sys.argv[3]
if header.get('errorMessage'):
    sys.exit('%s: query failed: %s' % (label, header['errorMessage']))
if header.get('rows') != int(sys.argv[2]):
    sys.exit('%s: expected %s rows, got %s' % (label, sys.argv[2], header.get('rows')))
print('%s: %d rows x %d columns, %d bytes' % (label, header['rows'], header['columns'], header['blobLength']))
PY
}

if [ "$flavor" = "main" ]; then
  for p in SQLite Postgres MariaDB; do
    [[ ",$providers," == *",$p,"* ]] || fail "provider $p is not advertised"
  done

  query sqlite '{"id":"smoke-sqlite","options":{},"func":{"name":"products","params":[],
    "query":"select ProductID, ProductName, UnitPrice, Discontinued from Products",
    "connection":{"dataSource":"SQLite","parameters":{"db":"/home/grok/northwind/Northwind.sl3"}}}}' 77

  query postgres '{"id":"smoke-postgres","options":{},"func":{"name":"pg","params":[],
    "query":"select g as id, '\''Grüße ® '\'' || g as label, now() as ts from generate_series(1, 1000) g",
    "connection":{"dataSource":"Postgres","parameters":{"server":"localhost","port":5432,"db":"smoke"},
    "credentials":{"parameters":{"login":"smoke","password":"smoke"}}}}' 1000
  grep -aq 'Grüße ®' "$work/postgres.bin" || fail "postgres: UTF-8 string did not survive the round trip"
  echo "postgres: UTF-8 round trip OK"
else
  [ "$providers" = "Impala,Neptune" ] || fail "extended image must advertise exactly Impala,Neptune"
fi

if docker logs "$name" 2>&1 | grep -E 'UnsatisfiedLinkError|Error relocating|SIGSEGV'; then
  fail "native library errors in the container log"
fi

echo "::group::bundled native libraries (musl loader check, informational)"
docker exec "$name" sh -c '
  cd /tmp && rm -rf natives && mkdir natives && cd natives
  for jar in "$GROK_CONNECT_DIR"/grok_connect.jar "$GROK_CONNECT_DIR"/lib/*.jar; do
    unzip -l "$jar" 2>/dev/null | awk "{print \$4}" | grep -E "\.so(\.[0-9]+)*$" | \
      grep -viE "aarch|arm|ppc|s390|riscv|i386|x86/|x86_32|linux32|linux-x86/|loongarch|mips|sparc|freebsd|sunos|solaris|aix|android|osx|mac|darwin|windows" | \
    while read -r so; do
      d=$(basename "$jar")
      mkdir -p "$d" && unzip -o -q "$jar" "$so" -d "$d" 2>/dev/null || continue
      echo "== $d!$so"
      /lib/ld-musl-x86_64.so.1 --list "$d/$so" 2>&1 | grep -E "not found|Error" || echo "   resolves on musl"
    done
  done
  cd /tmp && rm -rf natives'
echo "::endgroup::"

echo "SMOKE OK ($flavor)"

#!/bin/sh
set -eu

url=${1:?Usage: smoke-test.sh BASE_URL ENVIRONMENT COMMIT}
environment=${2:?Expected environment is required}
commit=${3:?Expected commit is required}
url=${url%/}
: "${SMOKE_USERNAME:?SMOKE_USERNAME must be set}"
: "${SMOKE_PASSWORD:?SMOKE_PASSWORD must be set}"
command -v python3 >/dev/null 2>&1 || { echo 'Smoke test requires python3 to validate JSON.' >&2; exit 1; }

fetch() {
  curl --fail --silent --show-error --connect-timeout 2 --max-time 5 "$1"
}

is_expected_build() {
  printf '%s' "$1" | python3 -c '
import json, sys
try:
    body = json.load(sys.stdin)
    valid = body.get("environment") == sys.argv[1] and body.get("build", {}).get("commit") == sys.argv[2]
except (ValueError, AttributeError):
    valid = False
sys.exit(0 if valid else 1)
' "$environment" "$commit"
}

is_ready() {
  printf '%s' "$1" | python3 -c '
import json, sys
try:
    valid = json.load(sys.stdin).get("status") == "UP"
except (ValueError, AttributeError):
    valid = False
sys.exit(0 if valid else 1)
'
}

attempt=1
until info=$(fetch "$url/actuator/info" 2>/dev/null) && is_expected_build "$info" &&
  health=$(fetch "$url/actuator/health/readiness" 2>/dev/null) && is_ready "$health"; do
  if [ "$attempt" -ge 30 ]; then
    echo "Expected $environment build $commit was not ready at $url" >&2
    exit 1
  fi
  echo "Waiting for $environment build $commit at $url ($attempt/30)"
  attempt=$((attempt + 1))
  sleep 5
done

token=$(curl -sf -o /dev/null -D - \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$SMOKE_USERNAME\",\"password\":\"$SMOKE_PASSWORD\"}" \
  "$url/api/public/login" | tr -d '\r' | sed -n 's/^[Aa]uthorization: *//p')
if [ -z "$token" ]; then
  echo 'Login failed' >&2
  exit 1
fi

if ! curl -sf -o /dev/null -H "Authorization: Bearer $token" "$url/api/authors?name=smoke"; then
  echo 'Authenticated request failed' >&2
  exit 1
fi

status=$(curl -s -o /dev/null -w '%{http_code}' "$url/api/authors?name=smoke")
if [ "$status" != 401 ]; then
  echo "Request without a token returned $status instead of 401" >&2
  exit 1
fi

echo "Smoke test passed: $url ($environment, $commit)"

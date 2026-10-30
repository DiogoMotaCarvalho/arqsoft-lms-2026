#!/bin/sh
set -eu

BASE_URL="$1"
: "${SMOKE_USERNAME:?SMOKE_USERNAME must be set}"
: "${SMOKE_PASSWORD:?SMOKE_PASSWORD must be set}"

attempt=1
until curl -sf -o /dev/null "$BASE_URL/api-docs"; do
  if [ "$attempt" -ge 30 ]; then
    echo "Application did not start at $BASE_URL"
    exit 1
  fi
  attempt=$((attempt + 1))
  sleep 5
done

TOKEN=$(curl -sf -o /dev/null -D - \
  -H 'Content-Type: application/json' \
  -d "{\"username\":\"$SMOKE_USERNAME\",\"password\":\"$SMOKE_PASSWORD\"}" \
  "$BASE_URL/api/public/login" | tr -d '\r' | sed -n 's/^[Aa]uthorization: *//p')

if [ -z "$TOKEN" ]; then
  echo "Login failed"
  exit 1
fi

if ! curl -sf -o /dev/null -H "Authorization: Bearer $TOKEN" "$BASE_URL/api/authors?name=smoke"; then
  echo "Authenticated request failed"
  exit 1
fi

STATUS=$(curl -s -o /dev/null -w '%{http_code}' "$BASE_URL/api/authors?name=smoke")
if [ "$STATUS" != "401" ]; then
  echo "Request without a token returned $STATUS instead of 401"
  exit 1
fi

echo "Smoke tests passed"

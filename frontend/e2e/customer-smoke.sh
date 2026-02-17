#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="../docker-compose.yml"

run_in_service() {
  local service="$1"
  local command="$2"
  docker compose -f "$COMPOSE_FILE" exec -T "$service" sh -lc "$command"
}

assert_status() {
  local name="$1"
  local expected="$2"
  local actual="$3"
  if [ "$actual" != "$expected" ]; then
    echo "[FAIL] $name expected=$expected actual=$actual"
    exit 1
  fi
  echo "[PASS] $name status=$actual"
}

products_status="$(run_in_service customer-bff "curl -sS -o /dev/null -w '%{http_code}' http://localhost:3001/api/products")"
assert_status "customer products" "200" "$products_status"

cart_unauth_status="$(run_in_service customer-bff "curl -sS -o /dev/null -w '%{http_code}' http://localhost:3001/api/cart")"
assert_status "customer cart unauth" "401" "$cart_unauth_status"

login_response="$(run_in_service customer-bff "curl -sS -X POST http://localhost:3001/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"member01@example.com\",\"password\":\"password\"}'")"
token="$(printf '%s' "$login_response" | sed -n 's/.*\"token\":\"\([^\"]*\)\".*/\1/p' | head -n 1)"
if [ -z "$token" ]; then
  echo "[FAIL] customer login token not found"
  exit 1
fi
echo "[PASS] customer login token acquired"

members_me_status="$(run_in_service customer-bff "curl -sS -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $token' http://localhost:3001/api/members/me")"
assert_status "customer members me" "200" "$members_me_status"

echo "customer-e2e-smoke: ok"

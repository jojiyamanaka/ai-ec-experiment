#!/usr/bin/env bash
set -euo pipefail

COMPOSE_FILE="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/docker-compose.yml"

if [ "${SKIP_E2E_SEED:-0}" != "1" ]; then
  bash "$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)/scripts/e2e_seed.sh"
fi

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

bo_login_response="$(run_in_service backoffice-bff "curl -sS -X POST http://localhost:3002/api/bo-auth/login -H 'Content-Type: application/json' -d '{\"email\":\"admin@example.com\",\"password\":\"password\"}'")"
bo_token="$(printf '%s' "$bo_login_response" | sed -n 's/.*\"token\":\"\([^\"]*\)\".*/\1/p' | head -n 1)"
if [ -z "$bo_token" ]; then
  echo "[FAIL] bo login token not found"
  exit 1
fi
echo "[PASS] bo login token acquired"

inventory_status="$(run_in_service backoffice-bff "curl -sS -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $bo_token' http://localhost:3002/api/inventory")"
assert_status "admin inventory" "200" "$inventory_status"

orders_status="$(run_in_service backoffice-bff "curl -sS -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $bo_token' http://localhost:3002/api/admin/orders")"
assert_status "admin orders" "200" "$orders_status"

members_status="$(run_in_service backoffice-bff "curl -sS -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $bo_token' http://localhost:3002/api/admin/members")"
assert_status "admin members" "200" "$members_status"

bousers_status="$(run_in_service backoffice-bff "curl -sS -o /dev/null -w '%{http_code}' -H 'Authorization: Bearer $bo_token' http://localhost:3002/api/admin/bo-users")"
assert_status "admin bo-users" "200" "$bousers_status"

echo "admin-e2e-smoke: ok"

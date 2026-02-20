#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
COMPOSE_FILE="$PROJECT_ROOT/docker-compose.yml"

SEED_SQL="$PROJECT_ROOT/backend/src/main/resources/db/e2e/seed_reference_data.sql"
ASSERT_SQL="$PROJECT_ROOT/backend/src/main/resources/db/e2e/assert_reference_data.sql"
ADJUST_SEQ_SQL="$PROJECT_ROOT/scripts/adjust_sequences.sql"

run_in_service() {
  local service="$1"
  local command="$2"
  docker compose -f "$COMPOSE_FILE" exec -T "$service" sh -lc "$command"
}

run_psql_file() {
  local sql_file="$1"
  docker compose -f "$COMPOSE_FILE" exec -T postgres \
    psql -U ec_user -d ec_db -v ON_ERROR_STOP=1 < "$sql_file"
}

query_single_value() {
  local sql="$1"
  docker compose -f "$COMPOSE_FILE" exec -T postgres \
    psql -U ec_user -d ec_db -Atc "$sql" | tr -d '\r'
}

extract_json_number() {
  local payload="$1"
  local key="$2"
  printf '%s' "$payload" | sed -n "s/.*\"${key}\":\\([0-9][0-9]*\\).*/\\1/p" | head -n 1
}

extract_json_token() {
  local payload="$1"
  printf '%s' "$payload" | sed -n 's/.*\"token\":\"\([^\"]*\)\".*/\1/p' | head -n 1
}

login_customer() {
  local response
  response="$(run_in_service customer-bff "curl -sS -X POST http://localhost:3001/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"member01@example.com\",\"password\":\"password\"}'")"
  local token
  token="$(extract_json_token "$response")"
  if [ -z "$token" ]; then
    echo "[seed][FAIL] customer login token not found"
    echo "$response"
    exit 1
  fi
  printf '%s' "$token"
}

login_admin() {
  local response
  response="$(run_in_service backoffice-bff "curl -sS -X POST http://localhost:3002/api/bo-auth/login -H 'Content-Type: application/json' -d '{\"email\":\"admin@example.com\",\"password\":\"password\"}'")"
  local token
  token="$(extract_json_token "$response")"
  if [ -z "$token" ]; then
    echo "[seed][FAIL] admin login token not found"
    echo "$response"
    exit 1
  fi
  printf '%s' "$token"
}

cleanup_seed_orders() {
  local user_id
  user_id="$(query_single_value "SELECT id FROM users WHERE email = 'member01@example.com' AND is_deleted = FALSE LIMIT 1;")"
  if [ -z "$user_id" ]; then
    echo "[seed][FAIL] member01@example.com not found"
    exit 1
  fi

  local session_id="user-$user_id"
  docker compose -f "$COMPOSE_FILE" exec -T postgres psql -U ec_user -d ec_db -v ON_ERROR_STOP=1 <<SQL
DELETE FROM stock_reservations
 WHERE session_id = '$session_id'
    OR order_id IN (SELECT id FROM orders WHERE user_id = $user_id);

DELETE FROM orders WHERE user_id = $user_id;

DELETE FROM cart_items
 WHERE cart_id IN (SELECT id FROM carts WHERE session_id = '$session_id');

UPDATE location_stocks ls
   SET committed_qty = 0
  FROM products p
 WHERE p.id = ls.product_id
   AND p.product_code IN ('P-E2E-0001', 'P-E2E-0005');
SQL
}

create_order() {
  local product_code="$1"
  local quantity="$2"
  local customer_token="$3"
  local product_id
  product_id="$(query_single_value "SELECT id FROM products WHERE product_code = '$product_code' AND is_deleted = FALSE LIMIT 1;")"
  if [ -z "$product_id" ]; then
    echo "[seed][FAIL] product id not found: $product_code"
    exit 1
  fi

  run_in_service customer-bff "curl -sS -X POST http://localhost:3001/api/cart/items -H 'Authorization: Bearer $customer_token' -H 'Content-Type: application/json' -d '{\"productId\":$product_id,\"quantity\":$quantity}' >/dev/null"
  local order_response
  order_response="$(run_in_service customer-bff "curl -sS -X POST http://localhost:3001/api/orders -H 'Authorization: Bearer $customer_token' -H 'Content-Type: application/json' -d '{}'" )"
  if ! printf '%s' "$order_response" | grep -q '"success":true'; then
    echo "[seed][FAIL] order creation API failed for $product_code"
    echo "$order_response"
    exit 1
  fi

  local order_id
  order_id="$(extract_json_number "$order_response" "orderId")"
  if [ -z "$order_id" ]; then
    order_id="$(extract_json_number "$order_response" "id")"
  fi
  if [ -z "$order_id" ]; then
    echo "[seed][FAIL] order id not found in response for $product_code"
    echo "$order_response"
    exit 1
  fi
  printf '%s' "$order_id"
}

admin_post() {
  local endpoint="$1"
  local admin_token="$2"
  local response
  response="$(run_in_service backoffice-bff "curl -sS -X POST http://localhost:3002${endpoint} -H 'Authorization: Bearer $admin_token' -H 'Content-Type: application/json' -d '{}'" )"
  if ! printf '%s' "$response" | grep -q '"success":true'; then
    echo "[seed][FAIL] admin action failed: $endpoint"
    echo "$response"
    exit 1
  fi
}

customer_post() {
  local endpoint="$1"
  local customer_token="$2"
  local response
  response="$(run_in_service customer-bff "curl -sS -X POST http://localhost:3001${endpoint} -H 'Authorization: Bearer $customer_token' -H 'Content-Type: application/json' -d '{}'" )"
  if ! printf '%s' "$response" | grep -q '"success":true'; then
    echo "[seed][FAIL] customer action failed: $endpoint"
    echo "$response"
    exit 1
  fi
}

set_preparing_status() {
  local order_id="$1"
  local updated_id
  updated_id="$(query_single_value "UPDATE orders SET status = 'PREPARING_SHIPMENT' WHERE id = $order_id RETURNING id;")"
  if [ -z "$updated_id" ]; then
    echo "[seed][FAIL] failed to set PREPARING_SHIPMENT: order_id=$order_id"
    exit 1
  fi
}

echo "[seed] apply reference DML"
run_psql_file "$SEED_SQL"

existing_order_count="$(query_single_value "SELECT COUNT(DISTINCT o.id) FROM orders o JOIN users u ON u.id = o.user_id JOIN order_items oi ON oi.order_id = o.id JOIN products p ON p.id = oi.product_id WHERE u.email = 'member01@example.com' AND p.product_code LIKE 'P-E2E-%' AND o.is_deleted = FALSE;")"
existing_status_count="$(query_single_value "SELECT COUNT(*) FROM (SELECT o.status FROM orders o JOIN users u ON u.id = o.user_id JOIN order_items oi ON oi.order_id = o.id JOIN products p ON p.id = oi.product_id WHERE u.email = 'member01@example.com' AND p.product_code LIKE 'P-E2E-%' AND o.is_deleted = FALSE GROUP BY o.status) statuses;")"
if [ "${existing_order_count:-0}" -ge 6 ] && [ "${existing_status_count:-0}" -eq 6 ]; then
  echo "[seed] existing seeded orders detected; skip order recreation"
  echo "[seed] assert counts and FK integrity"
  run_psql_file "$ASSERT_SQL"
  echo "[seed] completed"
  exit 0
fi

echo "[seed] cleanup previous seeded orders"
cleanup_seed_orders

echo "[seed] adjust sequences"
run_psql_file "$ADJUST_SEQ_SQL"

customer_token="$(login_customer)"
admin_token="$(login_admin)"

echo "[seed] create orders for all statuses"
pending_order_id="$(create_order "P-E2E-0004" 1 "$customer_token")"
confirmed_order_id="$(create_order "P-E2E-0001" 1 "$customer_token")"
preparing_order_id="$(create_order "P-E2E-0001" 1 "$customer_token")"
shipped_order_id="$(create_order "P-E2E-0001" 1 "$customer_token")"
delivered_order_id="$(create_order "P-E2E-0001" 1 "$customer_token")"
cancelled_order_id="$(create_order "P-E2E-0001" 1 "$customer_token")"

admin_post "/api/admin/orders/${confirmed_order_id}/confirm" "$admin_token"

admin_post "/api/admin/orders/${preparing_order_id}/confirm" "$admin_token"
set_preparing_status "$preparing_order_id"

admin_post "/api/admin/orders/${shipped_order_id}/confirm" "$admin_token"
set_preparing_status "$shipped_order_id"
admin_post "/api/admin/orders/${shipped_order_id}/ship" "$admin_token"

admin_post "/api/admin/orders/${delivered_order_id}/confirm" "$admin_token"
set_preparing_status "$delivered_order_id"
admin_post "/api/admin/orders/${delivered_order_id}/ship" "$admin_token"
admin_post "/api/admin/orders/${delivered_order_id}/deliver" "$admin_token"

customer_post "/api/orders/${cancelled_order_id}/cancel" "$customer_token"

echo "[seed] created order ids: pending=$pending_order_id confirmed=$confirmed_order_id preparing=$preparing_order_id shipped=$shipped_order_id delivered=$delivered_order_id cancelled=$cancelled_order_id"

echo "[seed] assert counts and FK integrity"
run_psql_file "$ASSERT_SQL"

echo "[seed] completed"

#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
BACKEND_DIR="$PROJECT_ROOT/backend"
PROJECT_NAME="${COMPOSE_PROJECT_NAME:-$(basename "$PROJECT_ROOT")}"
COMPOSE_NETWORK="${COMPOSE_NETWORK:-${PROJECT_NAME}_internal}"
MAVEN_IMAGE="${MAVEN_IMAGE:-maven:3.9.9-eclipse-temurin-21}"
CONTAINER_NAME="flyway-repair-$(date +%s)-$$"
CONTAINER_ID=""

cleanup() {
  if [[ -n "$CONTAINER_ID" ]]; then
    docker rm -f "$CONTAINER_ID" >/dev/null 2>&1 || true
  fi
}
trap cleanup EXIT

# 起動ネットワークは bridge とし、Maven 依存の外部取得を可能にする。
CONTAINER_ID="$(docker run -d --rm \
  --name "$CONTAINER_NAME" \
  -v "$BACKEND_DIR:/build" \
  -w /build \
  "$MAVEN_IMAGE" \
  tail -f /dev/null)"

# DB 到達用に compose 内部ネットワークを追加接続する。
docker network connect "$COMPOSE_NETWORK" "$CONTAINER_ID"

docker exec "$CONTAINER_ID" \
  ./mvnw \
    -Dflyway.url="${FLYWAY_URL:-jdbc:postgresql://postgres:5432/ec_db}" \
    -Dflyway.user="${FLYWAY_USER:-ec_user}" \
    -Dflyway.password="${FLYWAY_PASSWORD:-ec_password}" \
    -Dflyway.locations=filesystem:src/main/resources/db/flyway \
    flyway:repair

docker stop "$CONTAINER_ID" >/dev/null
CONTAINER_ID=""

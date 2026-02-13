#!/usr/bin/env bash
set -euo pipefail

docker compose run --rm backend ./mvnw \
  -Dflyway.url="${FLYWAY_URL:-jdbc:postgresql://postgres:5432/ec_app}" \
  -Dflyway.user="${FLYWAY_USER:-ec_app_user}" \
  -Dflyway.password="${FLYWAY_PASSWORD:-changeme}" \
  -Dflyway.locations=filesystem:src/main/resources/db/flyway \
  flyway:repair

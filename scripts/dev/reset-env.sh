#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker/compose/compose.yml"
ENV_FILE="$ROOT_DIR/docker/compose/.env"

if [[ ! -f "$ENV_FILE" ]]; then
  cp "$ROOT_DIR/docker/compose/.env.example" "$ENV_FILE"
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" down -v --remove-orphans
rm -f "$ROOT_DIR"/backend/data/*.db
docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up --build -d

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

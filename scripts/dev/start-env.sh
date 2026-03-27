#!/usr/bin/env bash
set -euo pipefail

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
COMPOSE_FILE="$ROOT_DIR/docker/compose/compose.yml"
ENV_FILE="$ROOT_DIR/docker/compose/.env"
EXAMPLE_ENV_FILE="$ROOT_DIR/docker/compose/.env.example"

if [[ ! -f "$ENV_FILE" ]]; then
  cp "$EXAMPLE_ENV_FILE" "$ENV_FILE"
fi

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" up --build -d

docker compose --env-file "$ENV_FILE" -f "$COMPOSE_FILE" ps

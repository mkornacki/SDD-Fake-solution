# Quickstart: Developer Environment Bootstrap with Seeded Database

## Purpose
Start the local backend environment with Docker Compose, verify application and database readiness, confirm seeded sample data is present, and know how to stop or reset the stack to a clean baseline.

## Prerequisites
- Docker Engine installed and running
- Docker Compose available via `docker compose`
- Java 11 available only if running the backend outside the container for debugging
- Access to the repository workspace and permission to create local Docker volumes/files

## Start the Environment
1. Navigate to the repository root.
2. Copy environment defaults if needed: `cp docker/compose/.env.example docker/compose/.env`.
3. Start the stack: `./scripts/dev/start-env.sh`.
4. Watch service status: `docker compose --env-file docker/compose/.env -f docker/compose/compose.yml ps`.
5. Wait until the backend service reports healthy.

## Verify the Environment
1. Check backend readiness: `curl http://localhost:8080/actuator/health/readiness`.
2. Confirm the health response reports `UP` and database readiness is included.
3. Verify schema initialization by checking migration logs or the migration metadata table.
4. Verify seeded data using an authenticated application endpoint or a direct SQLite query, for example:
   `curl -H "Authorization: Bearer <jwt>" http://localhost:8080/api/v1/dev/sample-data/status`
5. Confirm the expected baseline rows exist and match the documented sample dataset.

## Stop the Environment
1. Stop services without deleting persisted data:
   `./scripts/dev/stop-env.sh`

## Reset the Environment
1. Stop and remove services plus persisted development data:
   `./scripts/dev/reset-env.sh`
2. Restart the stack:
   `./scripts/dev/start-env.sh`
3. Re-run the verification steps to confirm schema and sample data were recreated.

## Troubleshooting
### Backend never becomes healthy
- Inspect container logs: `docker compose --env-file docker/compose/.env -f docker/compose/compose.yml logs backend`
- Check whether migration or seed initialization failed before readiness was reported.

### Database file or volume permissions fail
- Confirm the mounted path exists and is writable by the container user.
- Reset the stack and remove the persisted volume if the SQLite file is corrupted.

### Port 8080 is already in use
- Identify the conflicting process or change the mapped port in `docker/compose/.env`.
- Restart the compose stack after correcting the conflict using `./scripts/dev/stop-env.sh` and `./scripts/dev/start-env.sh`.

### Seed data is duplicated or missing
- Review seed initializer logs to confirm idempotent upsert behavior.
- Run the documented reset flow to restore the baseline dataset.

## Test Strategy Mapping
- Unit tests: migration orchestration helpers, seed idempotency rules, configuration validation.
- Integration tests: application startup with SQLite, migration execution, seeded-data verification, Actuator readiness.
- Contract tests: local bootstrap and verification interface contract, including health and diagnostic responses where exposed.
- Smoke tests: compose startup, healthy status, and baseline dataset checks from a clean environment.
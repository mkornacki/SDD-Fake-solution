# Developer Environment Runbook

## Prerequisites

- Docker Engine and Docker Compose v2
- Access to this repository

## Startup

```bash
cp docker/compose/.env.example docker/compose/.env
./scripts/dev/start-env.sh
```

## Verification

```bash
curl -s http://localhost:8081/actuator/health/readiness
curl -s -H "Authorization: Bearer <jwt>" http://localhost:8080/api/v1/dev/sample-data/status
```

## Shutdown

```bash
./scripts/dev/stop-env.sh
```

## Reset

```bash
./scripts/dev/reset-env.sh
```

## Troubleshooting

- Port conflicts: change `BACKEND_PORT`/`MANAGEMENT_PORT` in `docker/compose/.env`
- Configuration failures: restore `.env` from `.env.example`
- Health failures: check backend logs and readiness endpoint output

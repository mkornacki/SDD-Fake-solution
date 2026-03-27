# Local Docker Compose Environment

## Start

```bash
cp docker/compose/.env.example docker/compose/.env
./scripts/dev/start-env.sh
```

## Verify

```bash
curl -s http://localhost:8081/actuator/health/readiness
```

For the authenticated sample-data endpoint, use a valid bearer token configured for the local issuer:

```bash
curl -s -H "Authorization: Bearer <jwt>" http://localhost:8080/api/v1/dev/sample-data/status
```

## Stop

```bash
./scripts/dev/stop-env.sh
```

## Reset

```bash
./scripts/dev/reset-env.sh
```

## Troubleshooting

### Port conflicts

If `8080` or `8081` is in use, update `BACKEND_PORT` / `MANAGEMENT_PORT` in `docker/compose/.env`.

### Configuration errors

If startup fails with missing env vars, ensure `docker/compose/.env` exists and contains valid values copied from `.env.example`.

### Health failures

Inspect logs and readiness details:

```bash
docker compose --env-file docker/compose/.env -f docker/compose/compose.yml logs backend
curl -s http://localhost:8081/actuator/health/readiness
```

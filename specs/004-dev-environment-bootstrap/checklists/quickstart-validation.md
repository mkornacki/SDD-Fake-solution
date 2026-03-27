# Quickstart Validation Checklist

- [x] Startup command works from repository root
- [x] Readiness endpoint reports `UP`
- [x] Authenticated sample-data status endpoint returns dataset version and counts
- [x] Reset flow recreates the baseline dataset
- [x] Troubleshooting steps cover port, configuration, and health failures

## Validation Notes (2026-03-26)

- Verified: `./scripts/dev/start-env.sh` starts the stack from repository root.
- Verified: `curl http://localhost:8081/actuator/health/readiness` returns `{ "status": "UP" }` after startup.
- Verified: authenticated sample-data status contract shape with `SampleDataStatusContractTest` (authorized caller includes `datasetVersion` and `recordCounts`).
- Verified: `./scripts/dev/reset-env.sh` recreates baseline rows (`reservations:1`, `room_reservation_items:1`).
- Verified: quickstart troubleshooting includes port conflicts, configuration/env issues, and health/readiness failures.
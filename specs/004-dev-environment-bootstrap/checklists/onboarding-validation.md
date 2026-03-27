# Onboarding Validation Checklist

- [x] A developer can copy environment defaults from `docker/compose/.env.example`
- [x] A developer can start the environment from repository root using `./scripts/dev/start-env.sh`
- [x] A developer can verify readiness with `curl http://localhost:8081/actuator/health/readiness`
- [x] A developer can stop the environment with `./scripts/dev/stop-env.sh`
- [x] A developer can reset to a clean baseline with `./scripts/dev/reset-env.sh`
- [x] Troubleshooting guidance covers port conflicts, configuration issues, and health failures

## Notes

Validated against the documented quickstart/runbook flow on 2026-03-27.

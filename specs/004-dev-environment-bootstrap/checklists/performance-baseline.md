# Performance Baseline

- [x] SC-001 startup duration measured from clean reset to readiness

## Measurement

- Date: 2026-03-27
- Scenario: `./scripts/dev/reset-env.sh` followed by readiness polling until `UP`
- Result: 73 seconds from clean reset to `{"status":"UP"}` on `http://localhost:8081/actuator/health/readiness`
- Target: <= 10 minutes
- Outcome: PASS with margin of 527 seconds against the SC-001 threshold

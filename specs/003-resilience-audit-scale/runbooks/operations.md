# Operations Runbook: Resilience, DLQ Replay, and Retry Storms

## Purpose
This runbook defines operational procedures for scaling pressure, DLQ replay governance, and retry-storm mitigation for the resilient reservation backend.

## Preconditions
- Service is deployed with health probes and metrics enabled.
- Operators have least-privilege access and `admin:dlq` authority only when needed.
- Correlation IDs are captured in request and worker logs.

## 1. Scaling Response Procedure

### Trigger Conditions
- `http.server.requests` p95 latency above SLO for 5+ minutes.
- Queue depth and consumer lag increasing without recovery.
- Bulkhead saturation or 503 backpressure rate increasing.

### Steps
1. Confirm application health via liveness and readiness endpoints.
2. Inspect metrics for request rate, queue depth, consumer lag, retry count, and DLQ growth.
3. Check for dependency degradation (partner endpoint latency/failure rate).
4. Scale worker replicas first when lag/retry is dominant.
5. Scale API replicas when sustained request pressure causes backpressure.
6. Re-check p95 latency, lag, and retry counters after each scaling change.

### Exit Criteria
- p95 latency returns to target window.
- Queue depth and lag trend downward for at least 15 minutes.
- 503 backpressure ratio stabilizes below alert threshold.

## 2. DLQ Replay Procedure

### Trigger Conditions
- DLQ item is marked for operator replay.
- Root-cause condition is mitigated (dependency recovered or payload corrected).

### Authorization
- Replay endpoint requires `admin:dlq` scope.
- Replay reason is mandatory and must describe the remediation context.

### Steps
1. Identify DLQ item ID and verify original failure class and attempt history.
2. Validate the issue is resolved (partner status, configuration, or data correction).
3. Invoke `POST /api/v1/operations/dlq/{dlqId}/replay` with a non-empty reason.
4. Capture correlation ID for the replay action.
5. Verify replay acceptance (`202`) and DLQ status transition to replayed state.
6. Confirm governance audit event contains operator identity, trace ID, action, and reason reference.
7. Monitor downstream processing and final reservation status.

### Rollback / Escalation
- If replay fails repeatedly, pause manual replay and escalate to incident owner.
- Keep item in triage and attach investigation evidence to incident timeline.

## 3. Retry Storm Response Procedure

### Trigger Conditions
- Retry counters spike rapidly across workers.
- Circuit breaker repeatedly opens and closes in short intervals.
- DLQ growth accelerates with transient-class failures.

### Steps
1. Verify partner or network fault domain from logs and metrics.
2. Increase backoff base and max interval to reduce synchronized retries.
3. Reduce worker concurrency temporarily if downstream is overloaded.
4. Confirm circuit breaker/open-state behavior is active.
5. Route terminal failures to DLQ and avoid uncontrolled auto-replay.
6. Communicate incident status and temporary controls to stakeholders.

### Recovery Validation
- Retry count trends downward.
- Circuit breaker stabilizes in closed state.
- DLQ growth flattens and targeted replay succeeds.

## Audit and Security Notes
- Never replay without explicit business reason.
- Preserve idempotency semantics across retries and replay actions.
- Do not expose unmasked PII in logs, traces, or operator-visible payloads.
- Keep all operator actions traceable via immutable governance audit events.

-- H2-compatible version of V007 for integration tests

CREATE TABLE IF NOT EXISTS reservation_audit_events (
    event_id VARCHAR(64) NOT NULL PRIMARY KEY,
    entity_id VARCHAR(255) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    actor_id VARCHAR(255) NOT NULL,
    actor_type VARCHAR(20) NOT NULL CHECK (actor_type IN ('USER', 'SYSTEM', 'SERVICE')),
    action VARCHAR(255) NOT NULL,
    outcome VARCHAR(20) NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE', 'PARTIAL')),
    before_snapshot_ref VARCHAR(255),
    after_snapshot_ref VARCHAR(255),
    trace_id VARCHAR(255) NOT NULL,
    occurred_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_entity
    ON reservation_audit_events (entity_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_trace_id
    ON reservation_audit_events (trace_id);

CREATE TABLE IF NOT EXISTS retention_policy_rules (
    rule_id VARCHAR(64) NOT NULL PRIMARY KEY,
    artifact_class VARCHAR(32) NOT NULL UNIQUE CHECK (
        artifact_class IN ('RAW_PAYLOAD', 'REQUEST_SNAPSHOT', 'RESPONSE_SNAPSHOT', 'AUDIT_EVENT')
    ),
    disposition VARCHAR(20) NOT NULL CHECK (disposition IN ('DELETE', 'ARCHIVE', 'RETAIN')),
    retention_days INTEGER NOT NULL,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);
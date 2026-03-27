-- H2-compatible version of V001 for integration tests
CREATE TABLE IF NOT EXISTS audit_events (
    audit_event_id  VARCHAR(36)  NOT NULL,
    actor_id        VARCHAR(255) NOT NULL,
    actor_type      VARCHAR(20)  NOT NULL,
    action          VARCHAR(255) NOT NULL,
    resource_type   VARCHAR(100),
    resource_id     VARCHAR(255),
    outcome         VARCHAR(20)  NOT NULL,
    trace_id        VARCHAR(255) NOT NULL,
    occurred_at     TIMESTAMP    NOT NULL,
    ip_address_hash VARCHAR(255),
    CONSTRAINT pk_audit_events PRIMARY KEY (audit_event_id)
);

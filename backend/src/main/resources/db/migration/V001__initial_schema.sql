-- V001: Initial Schema — audit_events table
-- Append-only audit log for security-relevant activity records.
-- GDPR-compliant: IP addresses stored as one-way hashes only.

CREATE TABLE IF NOT EXISTS audit_events (
    audit_event_id TEXT NOT NULL,
    actor_id       TEXT NOT NULL,
    actor_type     TEXT NOT NULL CHECK (actor_type IN ('USER', 'SERVICE', 'SYSTEM')),
    action         TEXT NOT NULL,
    resource_type  TEXT,
    resource_id    TEXT,
    outcome        TEXT NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE')),
    trace_id       TEXT NOT NULL,
    occurred_at    TEXT NOT NULL,
    ip_address_hash TEXT,
    CONSTRAINT pk_audit_events PRIMARY KEY (audit_event_id)
);

CREATE INDEX IF NOT EXISTS idx_audit_events_actor_id   ON audit_events (actor_id);
CREATE INDEX IF NOT EXISTS idx_audit_events_occurred_at ON audit_events (occurred_at);
CREATE INDEX IF NOT EXISTS idx_audit_events_trace_id    ON audit_events (trace_id);

-- V007: Audit Retention Schema
-- Immutable audit events, dead-letter items, and retention policy rules

CREATE TABLE IF NOT EXISTS reservation_audit_events (
    event_id TEXT NOT NULL PRIMARY KEY,
    entity_id TEXT NOT NULL,
    entity_type TEXT NOT NULL,
    actor_id TEXT NOT NULL,
    actor_type TEXT NOT NULL CHECK (actor_type IN ('USER', 'SYSTEM', 'SERVICE')),
    action TEXT NOT NULL,
    outcome TEXT NOT NULL CHECK (outcome IN ('SUCCESS', 'FAILURE', 'PARTIAL')),
    before_snapshot_ref TEXT,
    after_snapshot_ref TEXT,
    trace_id TEXT NOT NULL,
    occurred_at TEXT NOT NULL,
    CONSTRAINT unique_audit_event_id UNIQUE (event_id)
);

CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_entity ON reservation_audit_events (entity_id, entity_type);
CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_occurred_at ON reservation_audit_events (occurred_at);
CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_actor_id ON reservation_audit_events (actor_id);
CREATE INDEX IF NOT EXISTS idx_reservation_audit_events_trace_id ON reservation_audit_events (trace_id);

CREATE TABLE IF NOT EXISTS dead_letter_items (
    dlq_id TEXT NOT NULL PRIMARY KEY,
    work_item_id TEXT NOT NULL,
    context_id TEXT NOT NULL,
    failure_reason TEXT NOT NULL,
    attempt_history_ref TEXT NOT NULL,
    masked_payload_ref TEXT NOT NULL,
    replay_status TEXT NOT NULL DEFAULT 'PENDING' CHECK (replay_status IN ('PENDING', 'IN_REVIEW', 'REPLAYED', 'CLOSED')),
    replay_count INTEGER NOT NULL DEFAULT 0,
    replayed_at TEXT,
    created_at TEXT NOT NULL,
    CONSTRAINT fk_dlq_work_item FOREIGN KEY (work_item_id) REFERENCES asynchronous_work_items (work_item_id),
    CONSTRAINT fk_dlq_context FOREIGN KEY (context_id) REFERENCES reservation_request_contexts (context_id)
);

CREATE INDEX IF NOT EXISTS idx_dlq_context_id ON dead_letter_items (context_id);
CREATE INDEX IF NOT EXISTS idx_dlq_replay_status ON dead_letter_items (replay_status);
CREATE INDEX IF NOT EXISTS idx_dlq_created_at ON dead_letter_items (created_at);

CREATE TABLE IF NOT EXISTS retention_policy_rules (
    rule_id TEXT NOT NULL PRIMARY KEY,
    artifact_class TEXT NOT NULL UNIQUE CHECK (artifact_class IN ('RAW_PAYLOAD', 'REQUEST_SNAPSHOT', 'RESPONSE_SNAPSHOT', 'AUDIT_EVENT')),
    disposition TEXT NOT NULL CHECK (disposition IN ('DELETE', 'ARCHIVE', 'RETAIN')),
    retention_days INTEGER NOT NULL,
    description TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_retention_rules_artifact_class ON retention_policy_rules (artifact_class);

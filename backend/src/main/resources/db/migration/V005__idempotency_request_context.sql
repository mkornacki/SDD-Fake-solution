-- V005: Idempotency Request Context Schema
-- Deduplication records and request context tracking for resilient reservation operations

CREATE TABLE IF NOT EXISTS idempotency_records (
    id TEXT NOT NULL PRIMARY KEY,
    idempotency_key TEXT NOT NULL UNIQUE,
    fingerprint TEXT NOT NULL,
    created_at TEXT NOT NULL,
    expires_at TEXT NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_idempotency_key ON idempotency_records (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_records (expires_at);

CREATE TABLE IF NOT EXISTS reservation_request_contexts (
    context_id TEXT NOT NULL PRIMARY KEY,
    idempotency_key TEXT NOT NULL,
    correlation_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('RECEIVED', 'VALIDATED', 'QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'DLQ')),
    request_payload_ref TEXT,
    response_payload_ref TEXT,
    error_context TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CONSTRAINT fk_request_context_idempotency FOREIGN KEY (idempotency_key) REFERENCES idempotency_records (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_request_context_idempotency_key ON reservation_request_contexts (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_request_context_correlation_id ON reservation_request_contexts (correlation_id);
CREATE INDEX IF NOT EXISTS idx_request_context_status ON reservation_request_contexts (status);
CREATE INDEX IF NOT EXISTS idx_request_context_created_at ON reservation_request_contexts (created_at);

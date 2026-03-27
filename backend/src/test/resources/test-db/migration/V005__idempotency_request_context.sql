-- H2-compatible version of V005 for integration tests

CREATE TABLE IF NOT EXISTS reservation_request_contexts (
    context_id VARCHAR(64) NOT NULL PRIMARY KEY,
    idempotency_key VARCHAR(255) NOT NULL,
    correlation_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (
        status IN ('RECEIVED', 'VALIDATED', 'QUEUED', 'PROCESSING', 'COMPLETED', 'FAILED', 'DLQ')
    ),
    request_payload_ref VARCHAR(255),
    response_payload_ref VARCHAR(255),
    error_context VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_request_context_idempotency
        FOREIGN KEY (idempotency_key) REFERENCES idempotency_records (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_request_context_idempotency_key
    ON reservation_request_contexts (idempotency_key);
CREATE INDEX IF NOT EXISTS idx_request_context_correlation_id
    ON reservation_request_contexts (correlation_id);
CREATE INDEX IF NOT EXISTS idx_request_context_status
    ON reservation_request_contexts (status);
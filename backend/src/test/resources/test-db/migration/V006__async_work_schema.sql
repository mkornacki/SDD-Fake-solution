-- H2-compatible version of V006 for integration tests

CREATE TABLE IF NOT EXISTS asynchronous_work_items (
    work_item_id VARCHAR(64) NOT NULL PRIMARY KEY,
    context_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL CHECK (
        status IN ('READY', 'RUNNING', 'RETRY_WAIT', 'SUCCEEDED', 'FAILED', 'TERMINAL_FAILED')
    ),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_attempt_at TIMESTAMP,
    failure_class VARCHAR(32) NOT NULL DEFAULT 'NONE' CHECK (
        failure_class IN ('TRANSIENT', 'PERMANENT', 'NONE')
    ),
    last_failure_reason VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_work_item_context
        FOREIGN KEY (context_id) REFERENCES reservation_request_contexts (context_id)
);

CREATE TABLE IF NOT EXISTS partner_interaction_logs (
    log_id VARCHAR(64) NOT NULL PRIMARY KEY,
    work_item_id VARCHAR(64) NOT NULL,
    request_snapshot_ref VARCHAR(255) NOT NULL,
    response_snapshot_ref VARCHAR(255),
    masked_fields_applied VARCHAR(1000),
    response_status_code INTEGER,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_interaction_log_work_item
        FOREIGN KEY (work_item_id) REFERENCES asynchronous_work_items (work_item_id)
);
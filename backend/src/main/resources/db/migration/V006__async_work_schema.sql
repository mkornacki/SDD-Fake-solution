-- V006: Asynchronous Work Schema
-- Work items for partner processing and their interaction logs

CREATE TABLE IF NOT EXISTS asynchronous_work_items (
    work_item_id TEXT NOT NULL PRIMARY KEY,
    context_id TEXT NOT NULL,
    status TEXT NOT NULL CHECK (status IN ('READY', 'RUNNING', 'RETRY_WAIT', 'SUCCEEDED', 'FAILED', 'TERMINAL_FAILED')),
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_attempt_at TEXT,
    failure_class TEXT NOT NULL DEFAULT 'NONE' CHECK (failure_class IN ('TRANSIENT', 'PERMANENT', 'NONE')),
    last_failure_reason TEXT,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CONSTRAINT fk_work_item_context FOREIGN KEY (context_id) REFERENCES reservation_request_contexts (context_id)
);

CREATE INDEX IF NOT EXISTS idx_work_item_context_id ON asynchronous_work_items (context_id);
CREATE INDEX IF NOT EXISTS idx_work_item_status ON asynchronous_work_items (status);
CREATE INDEX IF NOT EXISTS idx_work_item_next_attempt_at ON asynchronous_work_items (next_attempt_at);

CREATE TABLE IF NOT EXISTS partner_interaction_logs (
    log_id TEXT NOT NULL PRIMARY KEY,
    work_item_id TEXT NOT NULL,
    request_snapshot_ref TEXT NOT NULL,
    response_snapshot_ref TEXT,
    masked_fields_applied TEXT,
    response_status_code INTEGER,
    error_message TEXT,
    created_at TEXT NOT NULL,
    CONSTRAINT fk_interaction_log_work_item FOREIGN KEY (work_item_id) REFERENCES asynchronous_work_items (work_item_id)
);

CREATE INDEX IF NOT EXISTS idx_interaction_log_work_item_id ON partner_interaction_logs (work_item_id);
CREATE INDEX IF NOT EXISTS idx_interaction_log_created_at ON partner_interaction_logs (created_at);

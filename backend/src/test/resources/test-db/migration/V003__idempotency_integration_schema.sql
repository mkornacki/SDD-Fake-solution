-- V003: Idempotency and Integration Schema
-- Deduplication records, async integration tasks, and dead-letter queue items.

CREATE TABLE IF NOT EXISTS idempotency_records (
    idempotency_key  TEXT NOT NULL,
    operation_type   TEXT NOT NULL CHECK (operation_type IN ('CREATE', 'CANCEL_RESERVATION', 'CANCEL_ROOM')),
    reservation_id   TEXT,
    result_status    TEXT NOT NULL CHECK (result_status IN ('IN_PROGRESS', 'COMPLETED', 'FAILED')),
    response_digest  TEXT,
    first_seen_at    TEXT NOT NULL,
    completed_at     TEXT,
    expires_at       TEXT NOT NULL,
    CONSTRAINT pk_idempotency_records PRIMARY KEY (idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_idempotency_expires_at ON idempotency_records (expires_at);

CREATE TABLE IF NOT EXISTS integration_tasks (
    task_id              TEXT NOT NULL,
    reservation_id       TEXT NOT NULL,
    room_item_id         TEXT,
    task_type            TEXT NOT NULL CHECK (task_type IN ('PARTNER_CREATE', 'PARTNER_CANCEL', 'REFUND_INITIATE')),
    state                TEXT NOT NULL CHECK (state IN ('READY', 'RUNNING', 'RETRY_WAIT', 'SUCCEEDED', 'TERMINAL_FAILED')),
    attempt_count        INTEGER NOT NULL DEFAULT 0,
    max_attempts         INTEGER NOT NULL DEFAULT 5,
    next_attempt_at      TEXT,
    last_failure_reason  TEXT,
    failure_class        TEXT NOT NULL DEFAULT 'NONE' CHECK (failure_class IN ('TRANSIENT', 'PERMANENT', 'NONE')),
    created_at           TEXT NOT NULL,
    updated_at           TEXT NOT NULL,
    CONSTRAINT pk_integration_tasks PRIMARY KEY (task_id),
    CONSTRAINT fk_integration_tasks_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id)
);

CREATE INDEX IF NOT EXISTS idx_integration_tasks_reservation_id ON integration_tasks (reservation_id);
CREATE INDEX IF NOT EXISTS idx_integration_tasks_state          ON integration_tasks (state);
CREATE INDEX IF NOT EXISTS idx_integration_tasks_next_attempt   ON integration_tasks (next_attempt_at);

CREATE TABLE IF NOT EXISTS dlq_items (
    dlq_id               TEXT NOT NULL,
    task_id              TEXT NOT NULL,
    reservation_id       TEXT NOT NULL,
    failure_reason       TEXT NOT NULL,
    attempt_history_ref  TEXT NOT NULL,
    masked_payload_ref   TEXT NOT NULL,
    replay_status        TEXT NOT NULL DEFAULT 'PENDING'
        CHECK (replay_status IN ('PENDING', 'IN_REVIEW', 'REPLAYED', 'CLOSED')),
    replay_count         INTEGER NOT NULL DEFAULT 0,
    created_at           TEXT NOT NULL,
    CONSTRAINT pk_dlq_items PRIMARY KEY (dlq_id),
    CONSTRAINT fk_dlq_task FOREIGN KEY (task_id) REFERENCES integration_tasks (task_id)
);

CREATE INDEX IF NOT EXISTS idx_dlq_items_reservation_id ON dlq_items (reservation_id);
CREATE INDEX IF NOT EXISTS idx_dlq_items_replay_status  ON dlq_items (replay_status);

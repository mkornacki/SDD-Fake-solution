-- V100: Development bootstrap baseline metadata
-- Tracks local dataset bootstrapping lifecycle.

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id TEXT NOT NULL,
    partner_id TEXT NOT NULL,
    external_reference TEXT,
    status TEXT NOT NULL,
    currency_code TEXT NOT NULL,
    total_price NUMERIC NOT NULL DEFAULT 0,
    total_refund_amount NUMERIC NOT NULL DEFAULT 0,
    total_cancellation_fee NUMERIC NOT NULL DEFAULT 0,
    room_count INTEGER NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    guest_id TEXT NOT NULL,
    CONSTRAINT pk_reservations PRIMARY KEY (reservation_id)
);

CREATE TABLE IF NOT EXISTS room_reservation_items (
    room_item_id TEXT NOT NULL,
    reservation_id TEXT NOT NULL,
    room_code TEXT NOT NULL,
    check_in_date TEXT NOT NULL,
    check_out_date TEXT NOT NULL,
    status TEXT NOT NULL,
    base_price NUMERIC NOT NULL,
    cancellation_fee NUMERIC,
    refund_amount NUMERIC,
    cancellation_reason TEXT,
    cancelled_at TEXT,
    processing_status TEXT NOT NULL DEFAULT 'NOT_STARTED',
    CONSTRAINT pk_room_reservation_items PRIMARY KEY (room_item_id),
    CONSTRAINT fk_room_items_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id)
);

CREATE TABLE IF NOT EXISTS integration_tasks (
    task_id TEXT NOT NULL,
    reservation_id TEXT NOT NULL,
    room_item_id TEXT,
    task_type TEXT NOT NULL,
    state TEXT NOT NULL,
    attempt_count INTEGER NOT NULL DEFAULT 0,
    max_attempts INTEGER NOT NULL DEFAULT 5,
    next_attempt_at TEXT,
    last_failure_reason TEXT,
    failure_class TEXT NOT NULL DEFAULT 'NONE',
    created_at TEXT NOT NULL,
    updated_at TEXT NOT NULL,
    CONSTRAINT pk_integration_tasks PRIMARY KEY (task_id)
);

CREATE TABLE IF NOT EXISTS sample_dataset_metadata (
    dataset_name TEXT NOT NULL,
    dataset_version TEXT NOT NULL,
    seed_status TEXT NOT NULL CHECK (seed_status IN ('NOT_STARTED', 'RUNNING', 'COMPLETED', 'FAILED')),
    seeded_at TEXT,
    updated_at TEXT NOT NULL,
    CONSTRAINT pk_sample_dataset_metadata PRIMARY KEY (dataset_name)
);

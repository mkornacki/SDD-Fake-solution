-- V101: Ensure metadata table baseline row exists

CREATE TABLE IF NOT EXISTS sample_dataset_metadata (
    dataset_name TEXT NOT NULL,
    dataset_version TEXT NOT NULL,
    seed_status TEXT NOT NULL CHECK (seed_status IN ('NOT_STARTED', 'RUNNING', 'COMPLETED', 'FAILED')),
    seeded_at TEXT,
    updated_at TEXT NOT NULL,
    CONSTRAINT pk_sample_dataset_metadata PRIMARY KEY (dataset_name)
);

INSERT OR IGNORE INTO sample_dataset_metadata (
    dataset_name,
    dataset_version,
    seed_status,
    seeded_at,
    updated_at
) VALUES (
    'foundation-sample-dataset',
    '1.0.0',
    'NOT_STARTED',
    NULL,
    CURRENT_TIMESTAMP
);

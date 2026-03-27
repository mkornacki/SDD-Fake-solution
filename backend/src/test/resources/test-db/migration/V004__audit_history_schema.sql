-- V004: Audit History and Financial Breakdown Schema
-- Immutable reservation history events and financial reconciliation tables.

CREATE TABLE IF NOT EXISTS reservation_history_events (
    event_id          TEXT NOT NULL,
    reservation_id    TEXT NOT NULL,
    room_item_id      TEXT,
    event_type        TEXT NOT NULL CHECK (event_type IN (
        'CREATED', 'ROOM_CANCELLED', 'RESERVATION_CANCELLED',
        'STATUS_CHANGED', 'FINANCIAL_RECALCULATED', 'ASYNC_FAILED', 'REPLAYED'
    )),
    actor_id          TEXT NOT NULL,
    actor_type        TEXT NOT NULL CHECK (actor_type IN ('USER', 'PARTNER', 'SERVICE', 'SYSTEM')),
    reason            TEXT,
    before_state_ref  TEXT,
    after_state_ref   TEXT,
    trace_id          TEXT NOT NULL,
    occurred_at       TEXT NOT NULL,
    CONSTRAINT pk_reservation_history_events PRIMARY KEY (event_id),
    CONSTRAINT fk_history_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id)
);

CREATE INDEX IF NOT EXISTS idx_history_events_reservation_id ON reservation_history_events (reservation_id);
CREATE INDEX IF NOT EXISTS idx_history_events_occurred_at    ON reservation_history_events (occurred_at);
CREATE INDEX IF NOT EXISTS idx_history_events_event_type     ON reservation_history_events (event_type);

CREATE TABLE IF NOT EXISTS financial_breakdowns (
    breakdown_id      TEXT NOT NULL,
    reservation_id    TEXT NOT NULL,
    subtotal          NUMERIC NOT NULL DEFAULT 0,
    total_tax         NUMERIC NOT NULL DEFAULT 0,
    total_fees        NUMERIC NOT NULL DEFAULT 0,
    total_penalties   NUMERIC NOT NULL DEFAULT 0,
    total_refunds     NUMERIC NOT NULL DEFAULT 0,
    net_total         NUMERIC NOT NULL DEFAULT 0,
    calculated_at     TEXT NOT NULL,
    CONSTRAINT pk_financial_breakdowns PRIMARY KEY (breakdown_id),
    CONSTRAINT fk_financial_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id)
);

CREATE INDEX IF NOT EXISTS idx_financial_breakdowns_reservation_id ON financial_breakdowns (reservation_id);

CREATE TABLE IF NOT EXISTS financial_line_items (
    line_item_id   TEXT NOT NULL,
    breakdown_id   TEXT NOT NULL,
    line_type      TEXT NOT NULL CHECK (line_type IN ('BASE_PRICE', 'TAX', 'FEE', 'CANCELLATION_PENALTY', 'REFUND')),
    description    TEXT NOT NULL,
    amount         NUMERIC NOT NULL,
    room_item_id   TEXT,
    CONSTRAINT pk_financial_line_items PRIMARY KEY (line_item_id),
    CONSTRAINT fk_line_item_breakdown FOREIGN KEY (breakdown_id) REFERENCES financial_breakdowns (breakdown_id)
);

CREATE INDEX IF NOT EXISTS idx_line_items_breakdown_id ON financial_line_items (breakdown_id);

-- V002: Reservation Schema — reservations and room_reservation_items tables
-- Supports multi-room reservation lifecycle with optimistic concurrency control.

CREATE TABLE IF NOT EXISTS reservations (
    reservation_id       TEXT NOT NULL,
    partner_id           TEXT NOT NULL,
    external_reference   TEXT,
    status               TEXT NOT NULL CHECK (status IN ('PENDING', 'ACTIVE', 'PARTIALLY_CANCELLED', 'CANCELLED', 'FAILED')),
    currency_code        TEXT NOT NULL,
    total_price          NUMERIC NOT NULL DEFAULT 0,
    total_refund_amount  NUMERIC NOT NULL DEFAULT 0,
    total_cancellation_fee NUMERIC NOT NULL DEFAULT 0,
    room_count           INTEGER NOT NULL DEFAULT 0,
    version              INTEGER NOT NULL DEFAULT 0,
    created_at           TEXT NOT NULL,
    updated_at           TEXT NOT NULL,
    guest_id             TEXT NOT NULL,
    CONSTRAINT pk_reservations PRIMARY KEY (reservation_id)
);

CREATE INDEX IF NOT EXISTS idx_reservations_partner_id ON reservations (partner_id);
CREATE INDEX IF NOT EXISTS idx_reservations_status     ON reservations (status);
CREATE INDEX IF NOT EXISTS idx_reservations_created_at ON reservations (created_at);

CREATE TABLE IF NOT EXISTS room_reservation_items (
    room_item_id         TEXT NOT NULL,
    reservation_id       TEXT NOT NULL,
    room_code            TEXT NOT NULL,
    check_in_date        TEXT NOT NULL,
    check_out_date       TEXT NOT NULL,
    status               TEXT NOT NULL CHECK (status IN ('ACTIVE', 'CANCELLATION_PENDING', 'CANCELLED')),
    base_price           NUMERIC NOT NULL,
    cancellation_fee     NUMERIC,
    refund_amount        NUMERIC,
    cancellation_reason  TEXT,
    cancelled_at         TEXT,
    processing_status    TEXT NOT NULL DEFAULT 'NOT_STARTED'
        CHECK (processing_status IN ('NOT_STARTED', 'IN_PROGRESS', 'COMPLETED', 'FAILED')),
    CONSTRAINT pk_room_reservation_items PRIMARY KEY (room_item_id),
    CONSTRAINT fk_room_items_reservation FOREIGN KEY (reservation_id) REFERENCES reservations (reservation_id)
);

CREATE INDEX IF NOT EXISTS idx_room_items_reservation_id ON room_reservation_items (reservation_id);
CREATE INDEX IF NOT EXISTS idx_room_items_status         ON room_reservation_items (status);

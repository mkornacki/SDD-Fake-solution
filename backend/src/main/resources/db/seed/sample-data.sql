-- Deterministic development sample data (idempotent)

INSERT OR IGNORE INTO reservations (
    reservation_id,
    partner_id,
    external_reference,
    status,
    currency_code,
    total_price,
    total_refund_amount,
    total_cancellation_fee,
    room_count,
    version,
    created_at,
    updated_at,
    guest_id
) VALUES (
    'res-dev-001',
    'partner-dev',
    'ext-dev-001',
    'ACTIVE',
    'USD',
    220.00,
    0,
    0,
    1,
    0,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    'guest-dev-001'
);

INSERT OR IGNORE INTO room_reservation_items (
    room_item_id,
    reservation_id,
    room_code,
    check_in_date,
    check_out_date,
    status,
    base_price,
    cancellation_fee,
    refund_amount,
    cancellation_reason,
    cancelled_at,
    processing_status
) VALUES (
    'room-dev-001',
    'res-dev-001',
    'DLX-101',
    '2026-04-01',
    '2026-04-03',
    'ACTIVE',
    220.00,
    NULL,
    NULL,
    NULL,
    NULL,
    'NOT_STARTED'
);

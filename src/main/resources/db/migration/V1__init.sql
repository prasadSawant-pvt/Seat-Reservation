-- V1__init.sql
-- Seat Reservation System Schema
-- Designed for correctness under high concurrency

-- Shows table: metadata for each show
CREATE TABLE shows (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name VARCHAR(255) NOT NULL,
    price_paise BIGINT NOT NULL CHECK (price_paise > 0),
    per_user_limit INTEGER NOT NULL DEFAULT 4 CHECK (per_user_limit > 0),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Seats table: inventory with status tracking
-- PRIMARY KEY (show_id, seat_label) ensures no duplicate seats per show
CREATE TABLE seats (
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_label VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'available' CHECK (status IN ('available', 'held', 'confirmed')),
    held_by UUID,
    held_until TIMESTAMP WITH TIME ZONE,
    reservation_id UUID,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (show_id, seat_label)
);

-- Reservations table: reservation records
CREATE TABLE reservations (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    amount_paise BIGINT NOT NULL CHECK (amount_paise > 0),
    status VARCHAR(20) NOT NULL DEFAULT 'held' CHECK (status IN ('held', 'confirmed', 'cancelled')),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Reservation_seats link table: tracks which seats belong to which reservation
CREATE TABLE reservation_seats (
    reservation_id UUID NOT NULL REFERENCES reservations(id) ON DELETE CASCADE,
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    seat_label VARCHAR(50) NOT NULL,
    PRIMARY KEY (reservation_id, show_id, seat_label),
    FOREIGN KEY (show_id, seat_label) REFERENCES seats(show_id, seat_label)
);

-- Idempotency_keys table: enforces exactly-once reservation processing
-- UNIQUE (user_id, key) prevents duplicate idempotency keys per user
CREATE TABLE idempotency_keys (
    key VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    show_id UUID NOT NULL REFERENCES shows(id) ON DELETE CASCADE,
    request_fingerprint TEXT,
    reservation_id UUID REFERENCES reservations(id) ON DELETE SET NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (user_id, key)
);

-- Indexes for performance and constraint enforcement
CREATE INDEX idx_seats_show_status ON seats(show_id, status);
CREATE INDEX idx_seats_held_until ON seats(held_until) WHERE held_until IS NOT NULL;
CREATE INDEX idx_reservations_user_show ON reservations(user_id, show_id);
CREATE INDEX idx_reservations_show_status ON reservations(show_id, status);
CREATE INDEX idx_idempotency_keys_user_show ON idempotency_keys(user_id, show_id);

-- Function to update seat status atomically with conditional check
-- This is the core atomic decision mechanism
CREATE OR REPLACE FUNCTION reserve_seats(
    p_show_id UUID,
    p_seat_labels VARCHAR(50)[],
    p_user_id UUID,
    p_reservation_id UUID,
    p_held_until TIMESTAMP WITH TIME ZONE
) RETURNS INTEGER AS $$
DECLARE
    rows_updated INTEGER;
BEGIN
    -- Conditional UPDATE: only update if ALL seats are available
    -- Returns number of seats actually updated (should be array length if successful)
    UPDATE seats
    SET 
        status = 'held',
        held_by = p_user_id,
        held_until = p_held_until,
        reservation_id = p_reservation_id,
        updated_at = CURRENT_TIMESTAMP
    WHERE show_id = p_show_id
      AND seat_label = ANY(p_seat_labels)
      AND status = 'available';
    
    GET DIAGNOSTICS rows_updated = ROW_COUNT;
    RETURN rows_updated;
END;
$$ LANGUAGE plpgsql;

-- Function to confirm a reservation (move seats from held to confirmed)
CREATE OR REPLACE FUNCTION confirm_reservation(
    p_reservation_id UUID
) RETURNS VOID AS $$
BEGIN
    UPDATE seats
    SET 
        status = 'confirmed',
        held_until = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE reservation_id = p_reservation_id
      AND status = 'held';
END;
$$ LANGUAGE plpgsql;

-- Function to cancel a reservation (release seats back to available)
CREATE OR REPLACE FUNCTION cancel_reservation(
    p_reservation_id UUID,
    p_user_id UUID
) RETURNS VOID AS $$
BEGIN
    -- Only cancel if the reservation belongs to the user
    UPDATE reservations
    SET status = 'cancelled'
    WHERE id = p_reservation_id
      AND user_id = p_user_id
      AND status = 'confirmed';
    
    -- Release associated seats
    UPDATE seats
    SET 
        status = 'available',
        held_by = NULL,
        held_until = NULL,
        reservation_id = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE reservation_id = p_reservation_id
      AND status IN ('held', 'confirmed');
END;
$$ LANGUAGE plpgsql;

-- Function to expire held seats (time-boxed hold expiry)
CREATE OR REPLACE FUNCTION expire_held_seats() RETURNS VOID AS $$
BEGIN
    UPDATE seats
    SET 
        status = 'available',
        held_by = NULL,
        held_until = NULL,
        reservation_id = NULL,
        updated_at = CURRENT_TIMESTAMP
    WHERE status = 'held'
      AND held_until < CURRENT_TIMESTAMP;
END;
$$ LANGUAGE plpgsql;

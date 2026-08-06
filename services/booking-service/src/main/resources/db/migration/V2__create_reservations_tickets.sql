CREATE TABLE reservations (
    id              UUID PRIMARY KEY,
    user_id         VARCHAR(100)   NOT NULL,
    event_id        BIGINT         NOT NULL REFERENCES event_projections (event_id),
    event_name      VARCHAR(150)   NOT NULL,
    starts_at       TIMESTAMP      NOT NULL,
    price           NUMERIC(10, 2) NOT NULL,
    status          VARCHAR(20)    NOT NULL,
    idempotency_key VARCHAR(100)   NOT NULL,
    created_at      TIMESTAMP      NOT NULL,
    CONSTRAINT uq_reservations_user_idempotency UNIQUE (user_id, idempotency_key)
);

CREATE UNIQUE INDEX uq_reservations_one_active_per_user_event
    ON reservations (user_id, event_id)
    WHERE status = 'ACTIVE';

CREATE TABLE tickets (
    id             UUID PRIMARY KEY,
    reservation_id UUID         NOT NULL REFERENCES reservations (id) ON DELETE CASCADE,
    code           VARCHAR(32)  NOT NULL UNIQUE,
    qr             VARCHAR(255) NOT NULL,
    created_at     TIMESTAMP    NOT NULL
);

CREATE INDEX ix_tickets_reservation ON tickets (reservation_id);

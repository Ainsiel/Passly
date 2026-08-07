CREATE TABLE reservation_outbox (
    id               BIGSERIAL PRIMARY KEY,
    reservation_id   UUID         NOT NULL,
    payload          JSONB        NOT NULL,
    created_at       TIMESTAMP    NOT NULL DEFAULT now(),
    published_at     TIMESTAMP
);

CREATE UNIQUE INDEX uq_reservation_outbox_reservation ON reservation_outbox (reservation_id);
CREATE INDEX idx_reservation_outbox_pending ON reservation_outbox (id) WHERE published_at IS NULL;

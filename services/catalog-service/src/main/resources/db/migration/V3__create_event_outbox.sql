CREATE TABLE event_outbox (
    id           BIGSERIAL PRIMARY KEY,
    event_id     BIGINT       NOT NULL,
    event_type   VARCHAR(30)  NOT NULL,
    payload      JSONB        NOT NULL,
    created_at   TIMESTAMP    NOT NULL DEFAULT now(),
    published_at TIMESTAMP
);

CREATE INDEX idx_event_outbox_pending ON event_outbox (id) WHERE published_at IS NULL;

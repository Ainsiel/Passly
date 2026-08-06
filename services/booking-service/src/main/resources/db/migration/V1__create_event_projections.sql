CREATE TABLE event_projections (
    event_id         BIGINT PRIMARY KEY,
    name             VARCHAR(150)   NOT NULL,
    starts_at        TIMESTAMP      NOT NULL,
    price            NUMERIC(10, 2) NOT NULL,
    capacity         INT            NOT NULL CHECK (capacity >= 0),
    reserved_tickets INT            NOT NULL DEFAULT 0
        CHECK (reserved_tickets >= 0 AND reserved_tickets <= capacity),
    version          BIGINT         NOT NULL DEFAULT 0,
    updated_at       TIMESTAMP      NOT NULL DEFAULT now()
);

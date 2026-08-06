CREATE TABLE events (
    id               BIGSERIAL PRIMARY KEY,
    name             VARCHAR(150)  NOT NULL,
    description      TEXT,
    category         VARCHAR(50)   NOT NULL,
    venue            VARCHAR(150)  NOT NULL,
    starts_at        TIMESTAMP     NOT NULL,
    price            NUMERIC(10, 2) NOT NULL,
    capacity         INT           NOT NULL CHECK (capacity >= 0),
    reserved_tickets INT           NOT NULL DEFAULT 0
        CHECK (reserved_tickets >= 0 AND reserved_tickets <= capacity)
);

CREATE INDEX idx_events_category ON events (category);
CREATE INDEX idx_events_starts_at ON events (starts_at);
CREATE INDEX idx_events_venue ON events (venue);

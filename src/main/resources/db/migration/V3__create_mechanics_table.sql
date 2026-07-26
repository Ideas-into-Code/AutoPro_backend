-- V3__create_mechanics_table.sql
-- Creation of the mechanics table

CREATE TABLE mechanics (
    id                  BIGSERIAL       PRIMARY KEY,
    user_id             BIGINT          NOT NULL UNIQUE,
    specialization      VARCHAR(150),
    experience_years    INT,
    bio                 TEXT,
    is_available        BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_mechanics_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

-- Index on user_id (one-to-one join)
CREATE INDEX idx_mechanics_user_id      ON mechanics (user_id);
-- Index on availability for scheduling queries
CREATE INDEX idx_mechanics_is_available ON mechanics (is_available);

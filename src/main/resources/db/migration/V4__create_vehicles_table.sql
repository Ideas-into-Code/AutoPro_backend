-- V4__create_vehicles_table.sql
-- Creation of the vehicles table

CREATE TABLE vehicles (
    id              BIGSERIAL       PRIMARY KEY,
    owner_id        BIGINT          NOT NULL,
    brand           VARCHAR(100)    NOT NULL,
    model           VARCHAR(100)    NOT NULL,
    year            INT             NOT NULL,
    license_plate   VARCHAR(20)     NOT NULL UNIQUE,
    vin             VARCHAR(17)     UNIQUE,
    color           VARCHAR(50),
    mileage         INT,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_vehicles_owner_id
        FOREIGN KEY (owner_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_vehicles_year
        CHECK (year >= 1886 AND year <= EXTRACT(YEAR FROM NOW()) + 1),

    CONSTRAINT chk_vehicles_mileage
        CHECK (mileage IS NULL OR mileage >= 0)
);

-- Index on owner for retrieving a user's fleet
CREATE INDEX idx_vehicles_owner_id      ON vehicles (owner_id);
-- Index on license plate for identification lookups
CREATE INDEX idx_vehicles_license_plate ON vehicles (license_plate);
-- Index on VIN for identification lookups
CREATE INDEX idx_vehicles_vin           ON vehicles (vin);

-- V8__create_service_requests_table.sql
-- Creation of the service_requests table

CREATE TABLE service_requests (
    id              BIGSERIAL       PRIMARY KEY,
    client_id       BIGINT          NOT NULL,
    mechanic_id     BIGINT,
    vehicle_id      BIGINT,
    description     TEXT            NOT NULL,
    status          VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    address         VARCHAR(255),
    latitude        DOUBLE PRECISION,
    longitude       DOUBLE PRECISION,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_service_requests_client_id
        FOREIGN KEY (client_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_service_requests_mechanic_id
        FOREIGN KEY (mechanic_id) REFERENCES mechanics (id)
        ON DELETE SET NULL ON UPDATE CASCADE,

    CONSTRAINT fk_service_requests_vehicle_id
        FOREIGN KEY (vehicle_id) REFERENCES vehicles (id)
        ON DELETE SET NULL ON UPDATE CASCADE
);

CREATE INDEX idx_service_requests_client_id   ON service_requests (client_id);
CREATE INDEX idx_service_requests_mechanic_id ON service_requests (mechanic_id);
CREATE INDEX idx_service_requests_status      ON service_requests (status);
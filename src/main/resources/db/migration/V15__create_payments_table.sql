-- V15__create_payments_table.sql
-- Paiement des demandes de service. Espèces uniquement pour l'instant
-- (method = 'CASH'), modèle conçu pour accueillir d'autres moyens plus tard.

ALTER TABLE service_requests
    ADD COLUMN price NUMERIC(10, 2);

CREATE TABLE payments (
    id                  BIGSERIAL       PRIMARY KEY,
    service_request_id  BIGINT          NOT NULL UNIQUE,
    amount              NUMERIC(10, 2)  NOT NULL,
    currency            VARCHAR(3)      NOT NULL DEFAULT 'XOF',
    method              VARCHAR(20)     NOT NULL DEFAULT 'CASH',
    status              VARCHAR(20)     NOT NULL DEFAULT 'PENDING',
    collected_at        TIMESTAMP,
    notes               VARCHAR(500),
    created_at          TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_payments_service_request_id
        FOREIGN KEY (service_request_id) REFERENCES service_requests (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_payments_status ON payments (status);

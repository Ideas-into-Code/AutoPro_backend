-- service_request_id est une reference "molle" (pas de FK) : la table service_requests
-- vient de la branche feat/mechanic_services, pas encore fusionnee dans develop a ce stade.
-- Ajouter la contrainte FK dans une migration ulterieure une fois cette branche fusionnee.
CREATE TABLE payments (
    id                   BIGSERIAL PRIMARY KEY,
    service_request_id   BIGINT,
    payer_id             BIGINT NOT NULL,
    amount               NUMERIC(12, 2) NOT NULL,
    currency             VARCHAR(3) NOT NULL DEFAULT 'XOF',
    provider             VARCHAR(20) NOT NULL,
    provider_reference   VARCHAR(100) NOT NULL,
    status               VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    checkout_url         VARCHAR(500),
    failure_reason       VARCHAR(500),
    created_at           TIMESTAMP NOT NULL DEFAULT now(),
    updated_at           TIMESTAMP NOT NULL DEFAULT now(),

    CONSTRAINT fk_payments_payer_id FOREIGN KEY (payer_id) REFERENCES users (id),
    CONSTRAINT uq_payments_provider_reference UNIQUE (provider_reference)
);

CREATE INDEX idx_payments_service_request_id ON payments (service_request_id);
CREATE INDEX idx_payments_payer_id ON payments (payer_id);
CREATE INDEX idx_payments_status ON payments (status);

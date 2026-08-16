-- V10__create_interventions_table.sql
-- Table des interventions mécanicien (base du calcul des gains)

CREATE TABLE interventions (
    id              BIGSERIAL       PRIMARY KEY,
    mechanic_id     BIGINT          NOT NULL,
    description     VARCHAR(500),
    amount          NUMERIC(10, 2)  NOT NULL DEFAULT 0.00,
    status          VARCHAR(30)     NOT NULL DEFAULT 'COMPLETED',
    intervention_date TIMESTAMP     NOT NULL DEFAULT NOW(),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_interventions_mechanic_id
        FOREIGN KEY (mechanic_id) REFERENCES mechanics (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_interventions_mechanic_id   ON interventions (mechanic_id);
CREATE INDEX idx_interventions_date          ON interventions (intervention_date);

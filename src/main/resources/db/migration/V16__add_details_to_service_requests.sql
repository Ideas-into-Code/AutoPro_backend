-- V16__add_details_to_service_requests.sql
-- Détails de la demande fournis par le client : type de panne, téléphone de
-- rappel, indicateur d'urgence (immobilisation).

ALTER TABLE service_requests
    ADD COLUMN problem_type  VARCHAR(20) NOT NULL DEFAULT 'OTHER',
    ADD COLUMN contact_phone VARCHAR(20),
    ADD COLUMN is_emergency  BOOLEAN     NOT NULL DEFAULT FALSE;

CREATE INDEX idx_service_requests_problem_type ON service_requests (problem_type);

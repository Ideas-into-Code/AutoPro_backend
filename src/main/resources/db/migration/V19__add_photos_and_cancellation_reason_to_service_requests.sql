-- Photos jointes par le client à sa demande (URLs Cloudinary) et motif
-- d'annulation choisi dans une liste.

CREATE TABLE service_request_photos (
    service_request_id BIGINT       NOT NULL REFERENCES service_requests (id) ON DELETE CASCADE,
    url                VARCHAR(500) NOT NULL
);
CREATE INDEX idx_srp_service_request_id ON service_request_photos (service_request_id);

ALTER TABLE service_requests
    ADD COLUMN cancellation_reason VARCHAR(40);

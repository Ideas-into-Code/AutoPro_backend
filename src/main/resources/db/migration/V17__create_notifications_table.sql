-- V17__create_notifications_table.sql
-- Notifications utilisateur (demande acceptée, paiement encaissé, message…).

CREATE TABLE notifications (
    id              BIGSERIAL       PRIMARY KEY,
    recipient_id    BIGINT          NOT NULL,
    type            VARCHAR(30)     NOT NULL DEFAULT 'GENERAL',
    title           VARCHAR(150)    NOT NULL,
    body            VARCHAR(500),
    link            VARCHAR(255),
    is_read         BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_notifications_recipient_id
        FOREIGN KEY (recipient_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_notifications_recipient_id   ON notifications (recipient_id);
CREATE INDEX idx_notifications_recipient_read ON notifications (recipient_id, is_read);

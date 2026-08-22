-- V11__create_reviews_table.sql
-- Creation of the reviews table

CREATE TABLE reviews (
    id              BIGSERIAL       PRIMARY KEY,
    mechanic_id     BIGINT          NOT NULL,
    reviewer_id     BIGINT          NOT NULL,
    rating          SMALLINT        NOT NULL,
    comment         VARCHAR(1000),
    created_at      TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_reviews_mechanic_id
        FOREIGN KEY (mechanic_id) REFERENCES mechanics (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_reviews_reviewer_id
        FOREIGN KEY (reviewer_id) REFERENCES users (id)
        ON DELETE CASCADE,
    CONSTRAINT uq_reviews_mechanic_reviewer
        UNIQUE (mechanic_id, reviewer_id),
    CONSTRAINT chk_reviews_rating
        CHECK (rating BETWEEN 1 AND 5)
);

CREATE INDEX idx_reviews_mechanic_id ON reviews (mechanic_id);

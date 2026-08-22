-- V12__add_reputation_to_mechanics.sql
-- Denormalized reputation fields, kept in sync when a review is created

ALTER TABLE mechanics
    ADD COLUMN average_rating NUMERIC(3,2) NOT NULL DEFAULT 0,
    ADD COLUMN review_count   INT          NOT NULL DEFAULT 0;

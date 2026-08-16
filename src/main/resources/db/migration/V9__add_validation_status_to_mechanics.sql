-- V9__add_validation_status_to_mechanics.sql
-- Add validation_status column to mechanics table for admin approval workflow

ALTER TABLE mechanics
    ADD COLUMN validation_status VARCHAR(20) NOT NULL DEFAULT 'PENDING';

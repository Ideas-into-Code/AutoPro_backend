-- V1__create_roles_table.sql
-- Creation of the roles table

CREATE TABLE roles (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(50)     NOT NULL UNIQUE,
    description VARCHAR(255)
);

-- Index on the role name for fast lookups
CREATE INDEX idx_roles_name ON roles (name);

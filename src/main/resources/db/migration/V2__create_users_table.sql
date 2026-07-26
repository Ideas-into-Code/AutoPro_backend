-- V2__create_users_table.sql
-- Creation of the users table

CREATE TABLE users (
    id          BIGSERIAL       PRIMARY KEY,
    first_name  VARCHAR(100)    NOT NULL,
    last_name   VARCHAR(100)    NOT NULL,
    email       VARCHAR(150)    NOT NULL UNIQUE,
    password    VARCHAR(255)    NOT NULL,
    phone       VARCHAR(20),
    is_active   BOOLEAN         NOT NULL DEFAULT TRUE,
    role_id     BIGINT          NOT NULL,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_users_role_id
        FOREIGN KEY (role_id) REFERENCES roles (id)
        ON DELETE RESTRICT ON UPDATE CASCADE
);

-- Index on email for authentication lookups
CREATE INDEX idx_users_email    ON users (email);
-- Index on role_id for role-based queries
CREATE INDEX idx_users_role_id  ON users (role_id);

-- V5__seed_default_roles.sql
-- Insert default application roles

INSERT INTO roles (name, description) VALUES
    ('ROLE_ADMIN',    'System administrator with full access'),
    ('ROLE_CLIENT',   'Regular client who owns vehicles'),
    ('ROLE_MECHANIC', 'Mechanic who performs vehicle maintenance');

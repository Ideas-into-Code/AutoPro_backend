CREATE TABLE password_reset_tokens (
    id          BIGSERIAL PRIMARY KEY,
    token       VARCHAR(255)             NOT NULL UNIQUE,
    user_id     BIGINT                   NOT NULL,
    expires_at  TIMESTAMP                NOT NULL,
    used        BOOLEAN                  NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP                NOT NULL DEFAULT NOW(),
    CONSTRAINT fk_prt_user_id FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_prt_token ON password_reset_tokens (token);

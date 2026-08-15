-- V7__create_chat_rooms_table.sql
-- Creation of the chat_rooms and chat_room_participants tables

CREATE TABLE chat_rooms (
    id          BIGSERIAL       PRIMARY KEY,
    name        VARCHAR(200),
    is_group    BOOLEAN         NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP       NOT NULL DEFAULT NOW(),
    updated_at  TIMESTAMP       NOT NULL DEFAULT NOW()
);

CREATE TABLE chat_room_participants (
    chat_room_id    BIGINT  NOT NULL,
    user_id         BIGINT  NOT NULL,

    PRIMARY KEY (chat_room_id, user_id),

    CONSTRAINT fk_crp_chat_room_id
        FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_crp_user_id
        FOREIGN KEY (user_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE
);

CREATE INDEX idx_crp_chat_room_id ON chat_room_participants (chat_room_id);
CREATE INDEX idx_crp_user_id      ON chat_room_participants (user_id);

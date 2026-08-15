-- V8__create_chat_messages_table.sql
-- Creation of the chat_messages table

CREATE TABLE chat_messages (
    id              BIGSERIAL       PRIMARY KEY,
    chat_room_id    BIGINT          NOT NULL,
    sender_id       BIGINT          NOT NULL,
    content         TEXT            NOT NULL,
    message_type    VARCHAR(20)     NOT NULL DEFAULT 'CHAT',
    sent_at         TIMESTAMP       NOT NULL DEFAULT NOW(),

    CONSTRAINT fk_chat_messages_chat_room_id
        FOREIGN KEY (chat_room_id) REFERENCES chat_rooms (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT fk_chat_messages_sender_id
        FOREIGN KEY (sender_id) REFERENCES users (id)
        ON DELETE CASCADE ON UPDATE CASCADE,

    CONSTRAINT chk_chat_messages_type
        CHECK (message_type IN ('CHAT', 'JOIN', 'LEAVE'))
);

CREATE INDEX idx_chat_messages_chat_room_id ON chat_messages (chat_room_id);
CREATE INDEX idx_chat_messages_sender_id    ON chat_messages (sender_id);
CREATE INDEX idx_chat_messages_sent_at      ON chat_messages (sent_at DESC);

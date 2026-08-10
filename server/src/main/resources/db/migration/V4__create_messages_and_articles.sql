CREATE TABLE conversations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    user_one_id BIGINT NOT NULL,
    user_two_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_conversations_user_one FOREIGN KEY (user_one_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_conversations_user_two FOREIGN KEY (user_two_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_conversation_pair UNIQUE (user_one_id, user_two_id),
    CONSTRAINT chk_conversation_pair CHECK (user_one_id < user_two_id),
    INDEX idx_conversations_updated_at (updated_at)
);

CREATE TABLE direct_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    conversation_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    body VARCHAR(2000) NOT NULL,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_messages_conversation FOREIGN KEY (conversation_id) REFERENCES conversations (id) ON DELETE CASCADE,
    CONSTRAINT fk_messages_sender FOREIGN KEY (sender_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_messages_conversation_created (conversation_id, created_at),
    INDEX idx_messages_sender (sender_id),
    INDEX idx_messages_read_at (read_at)
);

CREATE TABLE articles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    body LONGTEXT NOT NULL,
    cover_image_url VARCHAR(2048) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_articles_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_articles_created_at (created_at),
    INDEX idx_articles_author (author_id)
);

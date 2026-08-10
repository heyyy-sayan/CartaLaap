CREATE TABLE community_polls (
    id BIGINT NOT NULL AUTO_INCREMENT,
    message_id BIGINT NOT NULL,
    question VARCHAR(300) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_community_poll_message UNIQUE (message_id),
    CONSTRAINT fk_community_poll_message FOREIGN KEY (message_id) REFERENCES community_messages(id) ON DELETE CASCADE
);

CREATE TABLE community_poll_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    poll_id BIGINT NOT NULL,
    option_text VARCHAR(120) NOT NULL,
    option_position INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_community_poll_option_poll FOREIGN KEY (poll_id) REFERENCES community_polls(id) ON DELETE CASCADE,
    INDEX idx_community_poll_options_poll (poll_id, option_position)
);

CREATE TABLE community_poll_votes (
    poll_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    option_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (poll_id, user_id),
    CONSTRAINT fk_community_poll_vote_poll FOREIGN KEY (poll_id) REFERENCES community_polls(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_poll_vote_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_poll_vote_option FOREIGN KEY (option_id) REFERENCES community_poll_options(id) ON DELETE CASCADE,
    INDEX idx_community_poll_votes_option (option_id)
);

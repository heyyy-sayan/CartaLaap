ALTER TABLE users
    ADD COLUMN location VARCHAR(100) NULL AFTER avatar_url,
    ADD COLUMN vehicle_interests VARCHAR(300) NULL AFTER location;

CREATE TABLE user_follows (
    follower_id BIGINT NOT NULL,
    following_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (follower_id, following_id),
    CONSTRAINT fk_user_follows_follower FOREIGN KEY (follower_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_user_follows_following FOREIGN KEY (following_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_user_follows_distinct CHECK (follower_id <> following_id),
    INDEX idx_user_follows_following (following_id),
    INDEX idx_user_follows_created_at (created_at)
);

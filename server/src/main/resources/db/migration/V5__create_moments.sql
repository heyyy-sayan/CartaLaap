CREATE TABLE moments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    author_id BIGINT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    caption VARCHAR(300) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    expires_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_moments_author FOREIGN KEY (author_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_moments_expires_at (expires_at),
    INDEX idx_moments_author_created (author_id, created_at)
);

CREATE TABLE moment_views (
    moment_id BIGINT NOT NULL,
    viewer_id BIGINT NOT NULL,
    viewed_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (moment_id, viewer_id),
    CONSTRAINT fk_moment_views_moment FOREIGN KEY (moment_id) REFERENCES moments (id) ON DELETE CASCADE,
    CONSTRAINT fk_moment_views_viewer FOREIGN KEY (viewer_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_moment_views_viewer (viewer_id),
    INDEX idx_moment_views_viewed_at (viewed_at)
);

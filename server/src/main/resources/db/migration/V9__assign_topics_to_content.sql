ALTER TABLE posts ADD COLUMN topic_slug VARCHAR(50) NULL AFTER image_url;
ALTER TABLE articles ADD COLUMN topic_slug VARCHAR(50) NULL AFTER cover_image_url;

CREATE INDEX idx_posts_topic_created ON posts(topic_slug, created_at);
CREATE INDEX idx_articles_topic_created ON articles(topic_slug, created_at);

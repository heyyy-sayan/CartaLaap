CREATE TABLE community_topics (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(80) NOT NULL,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(240) NULL,
    created_by_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    CONSTRAINT uk_community_topics_slug UNIQUE (slug),
    CONSTRAINT uk_community_topics_name UNIQUE (name),
    CONSTRAINT fk_community_topics_creator FOREIGN KEY (created_by_id) REFERENCES users(id) ON DELETE SET NULL
);

INSERT INTO community_topics (slug, name, description) VALUES
    ('turbo-builds', 'Turbo builds', 'Boost, tuning, and forced-induction projects.'),
    ('weekend-rides', 'Weekend rides', 'Road trips, group drives, and memorable routes.'),
    ('ev-conversions', 'EV conversions', 'Electric powertrains, batteries, and conversions.'),
    ('detailing', 'Detailing', 'Paint care, restoration, interiors, and protection.'),
    ('motorcycles', 'Motorcycles', 'Bikes, riding gear, maintenance, and touring.'),
    ('maintenance', 'Maintenance', 'Repairs, diagnostics, service, and ownership advice.'),
    ('classic-cars', 'Classic cars', 'Vintage machines, restoration, and preservation.'),
    ('track-days', 'Track days', 'Circuit driving, motorsport setup, and technique.');

ALTER TABLE articles MODIFY COLUMN topic_slug VARCHAR(80) NULL;
ALTER TABLE articles ADD CONSTRAINT fk_articles_topic_slug
    FOREIGN KEY (topic_slug) REFERENCES community_topics(slug) ON DELETE SET NULL ON UPDATE CASCADE;

DROP INDEX idx_posts_topic_created ON posts;
ALTER TABLE posts DROP COLUMN topic_slug;

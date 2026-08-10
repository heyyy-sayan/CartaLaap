CREATE TABLE marketplace_listings (
    id BIGINT NOT NULL AUTO_INCREMENT,
    seller_id BIGINT NOT NULL,
    category VARCHAR(20) NOT NULL,
    title VARCHAR(160) NOT NULL,
    description VARCHAR(5000) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    item_condition VARCHAR(20) NOT NULL,
    location VARCHAR(120) NOT NULL,
    brand VARCHAR(80) NULL,
    model VARCHAR(80) NULL,
    manufacture_year INT NULL,
    mileage BIGINT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_seller FOREIGN KEY (seller_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT chk_marketplace_price CHECK (price >= 0),
    INDEX idx_marketplace_category_status (category, status),
    INDEX idx_marketplace_price (price),
    INDEX idx_marketplace_created (created_at),
    INDEX idx_marketplace_seller (seller_id)
);

CREATE TABLE marketplace_listing_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_images_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    INDEX idx_marketplace_images_order (listing_id, sort_order)
);

CREATE TABLE marketplace_favorites (
    user_id BIGINT NOT NULL,
    listing_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (user_id, listing_id),
    CONSTRAINT fk_marketplace_favorites_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_favorites_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    INDEX idx_marketplace_favorites_listing (listing_id)
);

CREATE TABLE marketplace_reports (
    id BIGINT NOT NULL AUTO_INCREMENT,
    listing_id BIGINT NOT NULL,
    reporter_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_marketplace_reports_listing FOREIGN KEY (listing_id) REFERENCES marketplace_listings (id) ON DELETE CASCADE,
    CONSTRAINT fk_marketplace_reports_reporter FOREIGN KEY (reporter_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT uk_marketplace_report UNIQUE (listing_id, reporter_id)
);

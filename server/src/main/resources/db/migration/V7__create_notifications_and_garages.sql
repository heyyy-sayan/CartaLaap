CREATE TABLE notifications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    recipient_id BIGINT NOT NULL,
    actor_id BIGINT NULL,
    notification_type VARCHAR(30) NOT NULL,
    message VARCHAR(300) NOT NULL,
    target_id BIGINT NULL,
    read_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_notifications_recipient FOREIGN KEY (recipient_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_notifications_actor FOREIGN KEY (actor_id) REFERENCES users (id) ON DELETE SET NULL,
    INDEX idx_notifications_recipient_created (recipient_id, created_at),
    INDEX idx_notifications_recipient_read (recipient_id, read_at)
);

CREATE TABLE garage_vehicles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    owner_id BIGINT NOT NULL,
    vehicle_type VARCHAR(20) NOT NULL,
    garage_status VARCHAR(20) NOT NULL,
    brand VARCHAR(80) NOT NULL,
    model VARCHAR(80) NOT NULL,
    manufacture_year INT NULL,
    variant_name VARCHAR(100) NULL,
    fuel_type VARCHAR(30) NULL,
    modifications VARCHAR(3000) NULL,
    ownership_story VARCHAR(5000) NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_garage_vehicle_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE,
    INDEX idx_garage_vehicle_owner (owner_id),
    INDEX idx_garage_vehicle_status (garage_status)
);

CREATE TABLE garage_vehicle_images (
    id BIGINT NOT NULL AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    image_url VARCHAR(2048) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_garage_images_vehicle FOREIGN KEY (vehicle_id) REFERENCES garage_vehicles (id) ON DELETE CASCADE,
    INDEX idx_garage_images_order (vehicle_id, sort_order)
);

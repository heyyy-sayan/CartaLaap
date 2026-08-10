ALTER TABLE comments MODIFY COLUMN body VARCHAR(2000) NULL;
ALTER TABLE comments ADD COLUMN image_url VARCHAR(2048) NULL AFTER body;

CREATE TABLE communities (
    id BIGINT NOT NULL AUTO_INCREMENT,
    slug VARCHAR(50) NOT NULL,
    description VARCHAR(300) NULL,
    creator_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_communities_slug UNIQUE (slug),
    CONSTRAINT fk_communities_creator FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_communities_created (created_at)
);

CREATE TABLE community_members (
    community_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    member_role VARCHAR(20) NOT NULL,
    joined_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (community_id, user_id),
    CONSTRAINT fk_community_members_community FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_members_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_community_members_user (user_id)
);

CREATE TABLE community_invites (
    id BIGINT NOT NULL AUTO_INCREMENT,
    community_id BIGINT NOT NULL,
    inviter_id BIGINT NOT NULL,
    invitee_id BIGINT NOT NULL,
    invite_status VARCHAR(20) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    updated_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT uk_community_invitee UNIQUE (community_id, invitee_id),
    CONSTRAINT fk_community_invites_community FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_invites_inviter FOREIGN KEY (inviter_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_invites_invitee FOREIGN KEY (invitee_id) REFERENCES users(id) ON DELETE CASCADE,
    INDEX idx_community_invites_invitee_status (invitee_id, invite_status)
);

CREATE TABLE community_messages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    community_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    body VARCHAR(2000) NULL,
    image_url VARCHAR(2048) NULL,
    reply_to_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT fk_community_messages_community FOREIGN KEY (community_id) REFERENCES communities(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_messages_sender FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_community_messages_reply FOREIGN KEY (reply_to_id) REFERENCES community_messages(id) ON DELETE SET NULL,
    INDEX idx_community_messages_room_created (community_id, created_at),
    INDEX idx_community_messages_reply (reply_to_id)
);

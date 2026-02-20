ALTER TABLE users
    ADD COLUMN last_login_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN terms_agreed_at TIMESTAMP WITH TIME ZONE,
    ADD COLUMN full_name VARCHAR(100),
    ADD COLUMN phone_number VARCHAR(30),
    ADD COLUMN birth_date DATE,
    ADD COLUMN newsletter_opt_in BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN member_rank VARCHAR(50) NOT NULL DEFAULT 'STANDARD',
    ADD COLUMN loyalty_points INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN deactivation_reason VARCHAR(500);

ALTER TABLE users
    ADD CONSTRAINT ck_users_member_rank
    CHECK (member_rank IN ('STANDARD', 'SILVER', 'GOLD', 'PLATINUM'));

ALTER TABLE users
    ADD CONSTRAINT ck_users_loyalty_points_non_negative
    CHECK (loyalty_points >= 0);

CREATE INDEX idx_users_member_rank ON users(member_rank) WHERE is_deleted = FALSE;
CREATE INDEX idx_users_loyalty_points ON users(loyalty_points) WHERE is_deleted = FALSE;

CREATE TABLE user_addresses (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    label VARCHAR(100),
    recipient_name VARCHAR(100) NOT NULL,
    recipient_phone_number VARCHAR(30),
    postal_code VARCHAR(20) NOT NULL,
    prefecture VARCHAR(100) NOT NULL,
    city VARCHAR(100) NOT NULL,
    address_line1 VARCHAR(255) NOT NULL,
    address_line2 VARCHAR(255),
    is_default BOOLEAN NOT NULL DEFAULT FALSE,
    address_order INTEGER NOT NULL DEFAULT 0,

    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by_type VARCHAR(50),
    created_by_id BIGINT,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by_type VARCHAR(50),
    updated_by_id BIGINT,
    is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    deleted_at TIMESTAMP WITH TIME ZONE,
    deleted_by_type VARCHAR(50),
    deleted_by_id BIGINT,

    CONSTRAINT fk_user_addresses_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT ck_user_addresses_address_order_non_negative
        CHECK (address_order >= 0)
);

CREATE UNIQUE INDEX uk_user_addresses_user_default
    ON user_addresses(user_id)
    WHERE is_default = TRUE AND is_deleted = FALSE;

CREATE INDEX idx_user_addresses_user_id ON user_addresses(user_id);
CREATE INDEX idx_user_addresses_user_order ON user_addresses(user_id, address_order) WHERE is_deleted = FALSE;
CREATE INDEX idx_user_addresses_is_deleted ON user_addresses(is_deleted);

CREATE TRIGGER update_user_addresses_updated_at
    BEFORE UPDATE ON user_addresses
    FOR EACH ROW
    EXECUTE FUNCTION update_updated_at_column();

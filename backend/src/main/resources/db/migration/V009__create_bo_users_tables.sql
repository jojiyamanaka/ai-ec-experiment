-- bo_users テーブル作成
CREATE TABLE bo_users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    permission_level VARCHAR(50) NOT NULL DEFAULT 'OPERATOR',
    last_login_at DATETIME,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_bo_users_email ON bo_users(email);
CREATE INDEX idx_bo_users_is_active ON bo_users(is_active);

-- bo_auth_tokens テーブル作成
CREATE TABLE bo_auth_tokens (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    bo_user_id INTEGER NOT NULL,
    token_hash VARCHAR(255) NOT NULL UNIQUE,
    expires_at DATETIME NOT NULL,
    is_revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (bo_user_id) REFERENCES bo_users(id) ON DELETE CASCADE
);

CREATE INDEX idx_bo_auth_tokens_token_hash ON bo_auth_tokens(token_hash);
CREATE INDEX idx_bo_auth_tokens_bo_user_id ON bo_auth_tokens(bo_user_id);
CREATE INDEX idx_bo_auth_tokens_expires_at ON bo_auth_tokens(expires_at);

-- 既存 User (Role=ADMIN) を bo_users に移行
INSERT INTO bo_users (email, password_hash, display_name, permission_level, is_active, created_at, updated_at)
SELECT email, password_hash, display_name, 'ADMIN', is_active, created_at, updated_at
FROM users
WHERE role = 'ADMIN';

-- 管理者レコードを users から削除
DELETE FROM users WHERE role = 'ADMIN';

-- users テーブルから role カラムを削除（SQLite はテーブル再作成が必要）
CREATE TABLE users_new (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    email VARCHAR(255) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO users_new (id, email, password_hash, display_name, is_active, created_at, updated_at)
SELECT id, email, password_hash, display_name, is_active, created_at, updated_at
FROM users;

DROP TABLE users;
ALTER TABLE users_new RENAME TO users;

CREATE INDEX idx_users_email ON users(email);
CREATE INDEX idx_users_is_active ON users(is_active);

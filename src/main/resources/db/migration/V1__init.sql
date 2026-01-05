CREATE TABLE staff_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(64) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE identity_vault (
    id BIGSERIAL PRIMARY KEY,
    citizen_hash VARCHAR(128) NOT NULL UNIQUE
);

CREATE TABLE complaint (
    id BIGSERIAL PRIMARY KEY,
    tracking_id VARCHAR(40) NOT NULL UNIQUE,
    identity_vault_id BIGINT NOT NULL REFERENCES identity_vault(id),
    status VARCHAR(32) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    encrypted_text TEXT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_address VARCHAR(512) NOT NULL,
    company_contact VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE evidence (
    id BIGSERIAL PRIMARY KEY,
    complaint_id BIGINT NOT NULL REFERENCES complaint(id),
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    encrypted_content BYTEA NOT NULL
);

CREATE TABLE derivation_rule (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    severity_match VARCHAR(32),
    destination VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE audit_event (
    id BIGSERIAL PRIMARY KEY,
    event_time TIMESTAMPTZ NOT NULL,
    actor_role VARCHAR(32) NOT NULL,
    actor_username VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    tracking_id VARCHAR(40),
    details VARCHAR(512)
);

CREATE TABLE terms_acceptance (
    id BIGSERIAL PRIMARY KEY,
    session_token VARCHAR(64) NOT NULL UNIQUE,
    accepted_at TIMESTAMPTZ NOT NULL
);

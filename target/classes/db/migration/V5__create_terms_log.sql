-- Tabla para almacenar aceptación de términos y condiciones
CREATE TABLE IF NOT EXISTS terms_acceptance_log (
    id BIGSERIAL PRIMARY KEY,
    session_id VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45),
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    terms_version VARCHAR(20) NOT NULL DEFAULT 'v1.0_2026'
);

CREATE INDEX idx_terms_session ON terms_acceptance_log(session_id);


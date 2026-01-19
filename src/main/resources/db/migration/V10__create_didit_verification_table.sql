-- V10__create_didit_verification_table.sql
-- Crea la tabla para almacenar datos de verificación con Didit

CREATE TABLE IF NOT EXISTS secure_identities.didit_verification (
    id BIGSERIAL PRIMARY KEY,
    didit_session_id VARCHAR(255) NOT NULL UNIQUE,
    document_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    verification_status VARCHAR(50) NOT NULL,
    citizen_hash VARCHAR(128),
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    webhook_ip VARCHAR(45),
    webhook_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);

-- Índices para búsquedas rápidas
CREATE INDEX idx_didit_verification_document_number ON secure_identities.didit_verification(document_number);
CREATE INDEX idx_didit_verification_citizen_hash ON secure_identities.didit_verification(citizen_hash);
CREATE INDEX idx_didit_verification_verified_at ON secure_identities.didit_verification(verified_at);
CREATE INDEX idx_didit_verification_session_id ON secure_identities.didit_verification(didit_session_id);

-- Comentarios de documentación
COMMENT ON TABLE secure_identities.didit_verification IS 'Almacena datos de verificación biométrica obtenidos de Didit: nombre completo y cédula del documento escaneado';
COMMENT ON COLUMN secure_identities.didit_verification.didit_session_id IS 'ID único de sesión proporcionado por Didit';
COMMENT ON COLUMN secure_identities.didit_verification.document_number IS 'Número de cédula del documento escaneado';
COMMENT ON COLUMN secure_identities.didit_verification.full_name IS 'Nombre completo extraído del documento (first_name + last_name)';
COMMENT ON COLUMN secure_identities.didit_verification.verification_status IS 'Estado de la verificación (VERIFIED, FAILED, PENDING, etc.)';
COMMENT ON COLUMN secure_identities.didit_verification.citizen_hash IS 'Hash SHA-256 del ciudadano para vincular con denuncias';
COMMENT ON COLUMN secure_identities.didit_verification.webhook_payload IS 'Payload completo del webhook para auditoría';

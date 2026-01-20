-- ============================================================================
-- V21__simplify_didit_verification.sql
-- Simplifica registro_civil.didit_verification con solo los campos necesarios
-- document_number debe coincidir con cedula de registro_civil.personas
-- ============================================================================

-- ============================================================================
-- PASO 1: Copiar datos existentes a tabla temporal
-- ============================================================================
CREATE TABLE IF NOT EXISTS registro_civil.didit_verification_backup AS
SELECT * FROM registro_civil.didit_verification;

-- ============================================================================
-- PASO 2: Eliminar tabla actual
-- ============================================================================
DROP TABLE IF EXISTS registro_civil.didit_verification CASCADE;

-- ============================================================================
-- PASO 3: Recrear tabla con estructura simplificada
-- Solo campos: id, id_registro, didit_session_id, document_number, 
--              verification_status, verified_at, created_at, updated_at
-- ============================================================================
CREATE TABLE IF NOT EXISTS registro_civil.didit_verification (
    id BIGSERIAL PRIMARY KEY,
    id_registro BIGINT NOT NULL REFERENCES registro_civil.personas(id_registro) ON DELETE CASCADE,
    didit_session_id VARCHAR(255) NOT NULL UNIQUE,
    document_number VARCHAR(20) NOT NULL,  -- Debe coincidir con personas.cedula
    verification_status VARCHAR(50) NOT NULL,  -- VERIFIED, FAILED, PENDING
    verified_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    
    -- Constraint: asegurar valores válidos de estado
    CONSTRAINT didit_verification_status_check CHECK (verification_status IN ('VERIFIED', 'FAILED', 'PENDING'))
);

-- ============================================================================
-- PASO 4: Crear índices para optimización
-- ============================================================================
CREATE INDEX IF NOT EXISTS idx_didit_id_registro ON registro_civil.didit_verification(id_registro);
CREATE INDEX IF NOT EXISTS idx_didit_session_id ON registro_civil.didit_verification(didit_session_id);
CREATE INDEX IF NOT EXISTS idx_didit_document_number ON registro_civil.didit_verification(document_number);
CREATE INDEX IF NOT EXISTS idx_didit_verified_at ON registro_civil.didit_verification(verified_at);
CREATE INDEX IF NOT EXISTS idx_didit_verification_status ON registro_civil.didit_verification(verification_status);

-- ============================================================================
-- PASO 5: Comentarios descriptivos
-- ============================================================================
COMMENT ON TABLE registro_civil.didit_verification IS 'Registros de verificación biométrica Didit. Enlaza usuarios verificados con personas. Validación: document_number debe coincidir con cedula de la persona referenciada.';
COMMENT ON COLUMN registro_civil.didit_verification.id IS 'Identificador único del registro de verificación';
COMMENT ON COLUMN registro_civil.didit_verification.id_registro IS 'Foreign key a registro_civil.personas - persona verificada';
COMMENT ON COLUMN registro_civil.didit_verification.didit_session_id IS 'ID de sesión de Didit para auditoría y trazabilidad';
COMMENT ON COLUMN registro_civil.didit_verification.document_number IS 'Cédula de la persona (cifrada a nivel aplicación). Debe coincidir con personas.cedula asociada';
COMMENT ON COLUMN registro_civil.didit_verification.verification_status IS 'Estado de la verificación: VERIFIED (aprobada), FAILED (rechazada), PENDING (pendiente)';
COMMENT ON COLUMN registro_civil.didit_verification.verified_at IS 'Timestamp cuando se completó la verificación';

-- ============================================================================
-- PASO 6: Habilitar RLS
-- ============================================================================
ALTER TABLE registro_civil.didit_verification ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- V21 COMPLETE: didit_verification tabla simplificada y optimizada
-- ============================================================================

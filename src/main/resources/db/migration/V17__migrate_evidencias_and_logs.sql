-- ============================================================================
-- V17__migrate_evidencias_and_logs.sql
-- Migra evidence y audit_event a nuevos esquemas
-- VERSIÓN SIMPLIFICADA: SQL directo sin DO blocks
-- ============================================================================

-- ============================================================================
-- PASO 1: Crear tabla evidencias.evidencia (reemplaza evidence_vault.evidence)
-- ============================================================================
CREATE TABLE IF NOT EXISTS evidencias.evidencia (
    id BIGSERIAL PRIMARY KEY,
    id_denuncia BIGINT NOT NULL REFERENCES denuncias.denuncia(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    content_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    encrypted_content BYTEA NOT NULL,
    checksum VARCHAR(128),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_evidencia_id_denuncia ON evidencias.evidencia(id_denuncia);
CREATE INDEX IF NOT EXISTS idx_evidencia_created_at ON evidencias.evidencia(created_at);
CREATE UNIQUE INDEX IF NOT EXISTS idx_evidencia_checksum ON evidencias.evidencia(checksum) WHERE checksum IS NOT NULL;

-- ============================================================================
-- PASO 2: Migrar evidencia desde evidence_vault.evidence
-- Solo insertar evidencias cuya denuncia existe en denuncias.denuncia
-- ============================================================================
INSERT INTO evidencias.evidencia (
    id_denuncia, file_name, content_type, size_bytes, encrypted_content, created_at, updated_at
)
SELECT
    ee.complaint_id,
    ee.file_name,
    ee.content_type,
    ee.size_bytes,
    ee.encrypted_content,
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP
FROM evidence_vault.evidence ee
WHERE ee.complaint_id IS NOT NULL
  AND EXISTS (SELECT 1 FROM denuncias.denuncia WHERE id = ee.complaint_id)
  AND NOT EXISTS (SELECT 1 FROM evidencias.evidencia WHERE file_name = ee.file_name AND id_denuncia = ee.complaint_id)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- PASO 3: Crear tabla logs.evento_auditoria (reemplaza audit_logs.audit_event)
-- ============================================================================
CREATE TABLE IF NOT EXISTS logs.evento_auditoria (
    id BIGSERIAL PRIMARY KEY,
    id_registro BIGINT REFERENCES registro_civil.personas(id_registro) ON DELETE SET NULL,
    event_time TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actor_role VARCHAR(32),
    actor_username VARCHAR(64),
    event_type VARCHAR(64) NOT NULL,
    tracking_id VARCHAR(40),
    details TEXT,
    ip_address VARCHAR(45),
    user_agent TEXT,
    status VARCHAR(32) DEFAULT 'SUCCESS'
);

CREATE INDEX IF NOT EXISTS idx_evento_auditoria_event_time ON logs.evento_auditoria(event_time DESC);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_event_type ON logs.evento_auditoria(event_type);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_id_registro ON logs.evento_auditoria(id_registro);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_tracking_id ON logs.evento_auditoria(tracking_id);
CREATE INDEX IF NOT EXISTS idx_evento_auditoria_actor_username ON logs.evento_auditoria(actor_username);

-- ============================================================================
-- PASO 4: Migrar audit_event desde audit_logs.audit_event
-- ============================================================================
INSERT INTO logs.evento_auditoria (
    event_time, actor_role, actor_username, event_type, tracking_id, details, status
)
SELECT
    ae.event_time,
    ae.actor_role,
    ae.actor_username,
    ae.event_type,
    ae.tracking_id,
    ae.details,
    'SUCCESS'
FROM audit_logs.audit_event ae
WHERE NOT EXISTS (
    SELECT 1 FROM logs.evento_auditoria
    WHERE event_time = ae.event_time AND actor_username = ae.actor_username AND event_type = ae.event_type
)
ON CONFLICT DO NOTHING;

-- ============================================================================
-- PASO 5: Habilitar RLS en schemas sensibles
-- ============================================================================
ALTER TABLE evidencias.evidencia ENABLE ROW LEVEL SECURITY;
ALTER TABLE logs.evento_auditoria ENABLE ROW LEVEL SECURITY;


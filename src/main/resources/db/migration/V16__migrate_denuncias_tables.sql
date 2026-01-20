-- ============================================================================
-- V16__migrate_denuncias_tables.sql
-- Migra complaint, terms_acceptance, y crea relaciones con personas
-- VERSIÓN SIMPLIFICADA: SQL directo sin DO blocks
-- ============================================================================

-- ============================================================================
-- PASO 1: Crear tabla denuncias.denuncia (reemplaza public.complaint)
-- ============================================================================
CREATE TABLE IF NOT EXISTS denuncias.denuncia (
    id BIGSERIAL PRIMARY KEY,
    id_registro BIGINT REFERENCES registro_civil.personas(id_registro) ON DELETE CASCADE,
    tracking_id VARCHAR(40) NOT NULL UNIQUE,
    identity_vault_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    severity VARCHAR(16) NOT NULL,
    complaint_type VARCHAR(64),
    priority VARCHAR(16) DEFAULT 'MEDIUM',
    analyst_notes TEXT,
    derived_to VARCHAR(255),
    derived_at TIMESTAMPTZ,
    requires_more_info BOOLEAN DEFAULT FALSE,
    encrypted_text TEXT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    company_address VARCHAR(512) NOT NULL,
    company_contact VARCHAR(255) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT denuncia_tracking_id_not_empty CHECK (tracking_id != '')
);

CREATE INDEX IF NOT EXISTS idx_denuncia_tracking_id ON denuncias.denuncia(tracking_id);
CREATE INDEX IF NOT EXISTS idx_denuncia_id_registro ON denuncias.denuncia(id_registro);
CREATE INDEX IF NOT EXISTS idx_denuncia_status ON denuncias.denuncia(status);
CREATE INDEX IF NOT EXISTS idx_denuncia_severity ON denuncias.denuncia(severity);
CREATE INDEX IF NOT EXISTS idx_denuncia_created_at ON denuncias.denuncia(created_at);
CREATE INDEX IF NOT EXISTS idx_denuncia_derived_to ON denuncias.denuncia(derived_to);

-- ============================================================================
-- PASO 2: Migrar denuncias desde public.complaint
-- ============================================================================
INSERT INTO denuncias.denuncia (
    tracking_id, identity_vault_id, status, severity, complaint_type, priority,
    analyst_notes, derived_to, derived_at, requires_more_info, encrypted_text,
    company_name, company_address, company_contact, created_at, updated_at
)
SELECT
    pc.tracking_id, pc.identity_vault_id, pc.status, pc.severity, pc.complaint_type,
    pc.priority, pc.analyst_notes, pc.derived_to, pc.derived_at, pc.requires_more_info,
    pc.encrypted_text, pc.company_name, pc.company_address, pc.company_contact,
    pc.created_at, pc.updated_at
FROM public.complaint pc
WHERE NOT EXISTS (SELECT 1 FROM denuncias.denuncia WHERE tracking_id = pc.tracking_id)
ON CONFLICT (tracking_id) DO NOTHING;

-- ============================================================================
-- PASO 3: Crear tabla denuncias.aceptacion_terminos
-- ============================================================================
CREATE TABLE IF NOT EXISTS denuncias.aceptacion_terminos (
    id BIGSERIAL PRIMARY KEY,
    id_registro BIGINT REFERENCES registro_civil.personas(id_registro) ON DELETE CASCADE,
    session_token VARCHAR(64) NOT NULL UNIQUE,
    accepted_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ip_address VARCHAR(45),
    user_agent TEXT
);

CREATE INDEX IF NOT EXISTS idx_aceptacion_session_token ON denuncias.aceptacion_terminos(session_token);
CREATE INDEX IF NOT EXISTS idx_aceptacion_id_registro ON denuncias.aceptacion_terminos(id_registro);
CREATE INDEX IF NOT EXISTS idx_aceptacion_accepted_at ON denuncias.aceptacion_terminos(accepted_at);

-- ============================================================================
-- PASO 4: Migrar terms_acceptance desde public.terms_acceptance
-- ============================================================================
INSERT INTO denuncias.aceptacion_terminos (session_token, accepted_at)
SELECT pta.session_token, pta.accepted_at
FROM public.terms_acceptance pta
WHERE NOT EXISTS (SELECT 1 FROM denuncias.aceptacion_terminos WHERE session_token = pta.session_token)
ON CONFLICT (session_token) DO NOTHING;

-- ============================================================================
-- PASO 5: Habilitar RLS en schema denuncias
-- ============================================================================
ALTER TABLE denuncias.denuncia ENABLE ROW LEVEL SECURITY;
ALTER TABLE denuncias.aceptacion_terminos ENABLE ROW LEVEL SECURITY;


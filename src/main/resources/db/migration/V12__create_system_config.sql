-- =============================================================================
-- V12__create_system_config.sql
-- Crear tabla de configuraciones del sistema
-- Elimina valores hardcodeados y permite gestión dinámica
-- =============================================================================

-- Crear tabla de configuraciones del sistema
CREATE TABLE IF NOT EXISTS system_config (
    id BIGSERIAL PRIMARY KEY,
    config_group VARCHAR(64) NOT NULL,
    config_key VARCHAR(64) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    display_label VARCHAR(255) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE(config_group, config_key)
);

-- Índices para búsqueda eficiente
CREATE INDEX IF NOT EXISTS idx_system_config_group ON system_config(config_group);
CREATE INDEX IF NOT EXISTS idx_system_config_active ON system_config(active);

-- Comentarios de documentación
COMMENT ON TABLE system_config IS 'Configuraciones del sistema almacenadas en BD';
COMMENT ON COLUMN system_config.config_group IS 'Grupo de configuración: COMPLAINT_TYPE, PRIORITY, EVENT_TYPE, STATUS';
COMMENT ON COLUMN system_config.config_key IS 'Clave única dentro del grupo';
COMMENT ON COLUMN system_config.config_value IS 'Valor técnico (código)';
COMMENT ON COLUMN system_config.display_label IS 'Etiqueta para mostrar al usuario (en español)';
COMMENT ON COLUMN system_config.sort_order IS 'Orden de aparición en listas';

-- =============================================================================
-- TIPOS DE DENUNCIA
-- =============================================================================
INSERT INTO system_config (config_group, config_key, config_value, display_label, sort_order, active)
VALUES
    ('COMPLAINT_TYPE', 'LABOR_RIGHTS', 'LABOR_RIGHTS', 'Derechos Laborales', 1, true),
    ('COMPLAINT_TYPE', 'HARASSMENT', 'HARASSMENT', 'Acoso Laboral', 2, true),
    ('COMPLAINT_TYPE', 'DISCRIMINATION', 'DISCRIMINATION', 'Discriminación', 3, true),
    ('COMPLAINT_TYPE', 'SAFETY', 'SAFETY', 'Seguridad Laboral', 4, true),
    ('COMPLAINT_TYPE', 'FRAUD', 'FRAUD', 'Fraude', 5, true),
    ('COMPLAINT_TYPE', 'OTHER', 'OTHER', 'Otro', 99, true)
ON CONFLICT (config_group, config_key) DO NOTHING;

-- =============================================================================
-- PRIORIDADES
-- =============================================================================
INSERT INTO system_config (config_group, config_key, config_value, display_label, sort_order, active)
VALUES
    ('PRIORITY', 'LOW', 'LOW', 'Baja', 1, true),
    ('PRIORITY', 'MEDIUM', 'MEDIUM', 'Media', 2, true),
    ('PRIORITY', 'HIGH', 'HIGH', 'Alta', 3, true),
    ('PRIORITY', 'CRITICAL', 'CRITICAL', 'Crítica', 4, true)
ON CONFLICT (config_group, config_key) DO NOTHING;

-- =============================================================================
-- ESTADOS DE DENUNCIA
-- =============================================================================
INSERT INTO system_config (config_group, config_key, config_value, display_label, sort_order, active)
VALUES
    ('STATUS', 'PENDING', 'PENDING', 'Pendiente de revisión', 1, true),
    ('STATUS', 'IN_REVIEW', 'IN_REVIEW', 'En revisión', 2, true),
    ('STATUS', 'NEEDS_INFO', 'NEEDS_INFO', 'Requiere información adicional', 3, true),
    ('STATUS', 'APPROVED', 'APPROVED', 'Aprobado', 4, true),
    ('STATUS', 'REJECTED', 'REJECTED', 'No procede', 5, true),
    ('STATUS', 'DERIVED', 'DERIVED', 'Derivado a autoridad competente', 6, true),
    ('STATUS', 'RESOLVED', 'RESOLVED', 'Resuelto', 7, true),
    ('STATUS', 'ARCHIVED', 'ARCHIVED', 'Archivado', 8, true)
ON CONFLICT (config_group, config_key) DO NOTHING;

-- =============================================================================
-- TIPOS DE EVENTO (para auditoría)
-- =============================================================================
INSERT INTO system_config (config_group, config_key, config_value, display_label, sort_order, active)
VALUES
    ('EVENT_TYPE', 'LOGIN_SUCCESS', 'LOGIN_SUCCESS', 'Inicio de sesión exitoso', 1, true),
    ('EVENT_TYPE', 'LOGIN_FAILED', 'LOGIN_FAILED', 'Intento de acceso fallido', 2, true),
    ('EVENT_TYPE', 'LOGIN_STEP1_SUCCESS', 'LOGIN_STEP1_SUCCESS', 'Verificación de identidad exitosa', 3, true),
    ('EVENT_TYPE', 'LOGOUT', 'LOGOUT', 'Cierre de sesión', 4, true),
    ('EVENT_TYPE', 'COMPLAINT_CREATED', 'COMPLAINT_CREATED', 'Denuncia creada', 10, true),
    ('EVENT_TYPE', 'STATUS_CHANGED', 'STATUS_CHANGED', 'Estado cambiado', 11, true),
    ('EVENT_TYPE', 'COMPLAINT_CLASSIFIED', 'COMPLAINT_CLASSIFIED', 'Denuncia clasificada', 12, true),
    ('EVENT_TYPE', 'MORE_INFO_REQUESTED', 'MORE_INFO_REQUESTED', 'Información solicitada', 13, true),
    ('EVENT_TYPE', 'COMPLAINT_REJECTED', 'COMPLAINT_REJECTED', 'Denuncia rechazada', 14, true),
    ('EVENT_TYPE', 'CASE_DERIVED', 'CASE_DERIVED', 'Caso derivado', 15, true),
    ('EVENT_TYPE', 'ADDITIONAL_INFO_SUBMITTED', 'ADDITIONAL_INFO_SUBMITTED', 'Información adicional enviada', 16, true),
    ('EVENT_TYPE', 'EVIDENCE_VIEWED', 'EVIDENCE_VIEWED', 'Evidencia visualizada', 20, true),
    ('EVENT_TYPE', 'RULE_CREATED', 'RULE_CREATED', 'Regla creada', 30, true),
    ('EVENT_TYPE', 'RULE_UPDATED', 'RULE_UPDATED', 'Regla actualizada', 31, true),
    ('EVENT_TYPE', 'RULE_DELETED', 'RULE_DELETED', 'Regla eliminada', 32, true),
    ('EVENT_TYPE', 'IDENTITY_REVEALED', 'IDENTITY_REVEALED', 'Identidad revelada', 50, true)
ON CONFLICT (config_group, config_key) DO NOTHING;

-- =============================================================================
-- SEVERIDADES
-- =============================================================================
INSERT INTO system_config (config_group, config_key, config_value, display_label, sort_order, active)
VALUES
    ('SEVERITY', 'LOW', 'LOW', 'Baja', 1, true),
    ('SEVERITY', 'MEDIUM', 'MEDIUM', 'Media', 2, true),
    ('SEVERITY', 'HIGH', 'HIGH', 'Alta', 3, true),
    ('SEVERITY', 'CRITICAL', 'CRITICAL', 'Crítica', 4, true)
ON CONFLICT (config_group, config_key) DO NOTHING;

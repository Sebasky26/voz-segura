-- =============================================================================
-- V9__enhance_complaints_and_rules.sql
-- Mejorar estructura de denuncias y reglas de derivación
-- =============================================================================

-- Agregar campos adicionales a complaint para clasificación por analista
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS complaint_type VARCHAR(64);
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS priority VARCHAR(16) DEFAULT 'MEDIUM';
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS analyst_notes TEXT;
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS derived_to VARCHAR(255);
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS derived_at TIMESTAMPTZ;
ALTER TABLE complaint ADD COLUMN IF NOT EXISTS requires_more_info BOOLEAN DEFAULT FALSE;

-- Comentarios de documentación
COMMENT ON COLUMN complaint.complaint_type IS 'Tipo de denuncia: LABOR_RIGHTS, HARASSMENT, DISCRIMINATION, SAFETY, FRAUD, OTHER';
COMMENT ON COLUMN complaint.priority IS 'Prioridad: LOW, MEDIUM, HIGH, CRITICAL';
COMMENT ON COLUMN complaint.analyst_notes IS 'Notas internas del analista (cifradas si es necesario)';
COMMENT ON COLUMN complaint.derived_to IS 'Entidad a la que fue derivada la denuncia';
COMMENT ON COLUMN complaint.derived_at IS 'Fecha y hora de derivación';
COMMENT ON COLUMN complaint.requires_more_info IS 'Indica si el denunciante debe proporcionar más información';

-- Expandir derivation_rule con más criterios
ALTER TABLE derivation_rule ADD COLUMN IF NOT EXISTS complaint_type_match VARCHAR(64);
ALTER TABLE derivation_rule ADD COLUMN IF NOT EXISTS priority_match VARCHAR(16);
ALTER TABLE derivation_rule ADD COLUMN IF NOT EXISTS description VARCHAR(512);
ALTER TABLE derivation_rule ADD COLUMN IF NOT EXISTS created_at TIMESTAMPTZ DEFAULT NOW();
ALTER TABLE derivation_rule ADD COLUMN IF NOT EXISTS updated_at TIMESTAMPTZ DEFAULT NOW();

-- Índices para búsqueda eficiente
CREATE INDEX IF NOT EXISTS idx_complaint_type ON complaint(complaint_type);
CREATE INDEX IF NOT EXISTS idx_complaint_priority ON complaint(priority);
CREATE INDEX IF NOT EXISTS idx_complaint_requires_info ON complaint(requires_more_info);
CREATE INDEX IF NOT EXISTS idx_derivation_rule_active ON derivation_rule(active);
CREATE INDEX IF NOT EXISTS idx_derivation_rule_type ON derivation_rule(complaint_type_match);

-- Insertar reglas de derivación por defecto (entidades ecuatorianas)
INSERT INTO derivation_rule (name, complaint_type_match, severity_match, priority_match, destination, description, active)
VALUES
    ('Ministerio del Trabajo - Derechos Laborales', 'LABOR_RIGHTS', NULL, NULL, 'Ministerio del Trabajo del Ecuador', 'Denuncias relacionadas con violación de derechos laborales', true),
    ('Ministerio del Trabajo - Acoso Laboral', 'HARASSMENT', NULL, 'HIGH', 'Ministerio del Trabajo del Ecuador - Unidad de Acoso', 'Casos de acoso laboral de alta prioridad', true),
    ('Defensoría del Pueblo - Discriminación', 'DISCRIMINATION', NULL, NULL, 'Defensoría del Pueblo del Ecuador', 'Casos de discriminación en el ámbito laboral', true),
    ('IESS - Seguridad Laboral', 'SAFETY', NULL, NULL, 'Instituto Ecuatoriano de Seguridad Social - Riesgos del Trabajo', 'Denuncias sobre condiciones de seguridad laboral', true),
    ('Fiscalía - Fraude', 'FRAUD', 'CRITICAL', 'CRITICAL', 'Fiscalía General del Estado', 'Casos de fraude laboral con evidencia crítica', true),
    ('Superintendencia de Compañías', 'FRAUD', NULL, 'MEDIUM', 'Superintendencia de Compañías, Valores y Seguros', 'Irregularidades empresariales', true)
ON CONFLICT DO NOTHING;

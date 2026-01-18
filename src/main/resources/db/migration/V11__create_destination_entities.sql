-- =============================================================================
-- V11__create_destination_entities.sql
-- Tabla para almacenar entidades de destino de derivación
-- Las entidades se cargan desde BD, no hardcodeadas en frontend
-- =============================================================================

CREATE TABLE IF NOT EXISTS destination_entity (
    id SERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Insertar entidades ecuatorianas predefinidas
INSERT INTO destination_entity (code, name, description, active) VALUES
    ('MDT', 'Ministerio del Trabajo', 'Entidad rectora de políticas laborales', true),
    ('MDT_INSP', 'Ministerio del Trabajo - Inspectoría', 'Unidad de inspección laboral', true),
    ('DEF_PUEBLO', 'Defensoría del Pueblo', 'Protección de derechos ciudadanos', true),
    ('FISCALIA', 'Fiscalía General del Estado', 'Investigación de delitos', true),
    ('IESS_RT', 'IESS - Riesgos del Trabajo', 'Seguridad y salud ocupacional', true),
    ('SUPERCIAS', 'Superintendencia de Compañías', 'Regulación empresarial', true),
    ('CJ', 'Consejo de la Judicatura', 'Administración de justicia', true),
    ('CGE', 'Contraloría General del Estado', 'Control de recursos públicos', true),
    ('PGE', 'Procuraduría General del Estado', 'Defensa del Estado', true),
    ('MIES', 'Ministerio de Inclusión Económica y Social', 'Protección social', true),
    ('CNIG', 'Consejo Nacional para la Igualdad de Género', 'Equidad de género', true),
    ('CONADIS', 'Consejo Nacional para la Igualdad de Discapacidades', 'Derechos de personas con discapacidad', true)
ON CONFLICT (code) DO NOTHING;

-- Índice para búsqueda
CREATE INDEX IF NOT EXISTS idx_dest_entity_active ON destination_entity(active);

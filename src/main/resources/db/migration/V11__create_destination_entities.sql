-- =============================================================================
-- V11__create_destination_entities.sql
-- Tabla para almacenar entidades de destino de derivación
-- =============================================================================

-- Crear tabla solo si no existe
CREATE TABLE IF NOT EXISTS destination_entity (
    id SERIAL PRIMARY KEY,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(512),
    active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

-- Agregar constraint único solo si no existe
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'destination_entity_code_key') THEN
        ALTER TABLE destination_entity ADD CONSTRAINT destination_entity_code_key UNIQUE (code);
    END IF;
END$$;

-- Insertar entidades solo si no existen
INSERT INTO destination_entity (code, name, description, active)
SELECT 'MDT', 'Ministerio del Trabajo', 'Entidad rectora de políticas laborales', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'MDT');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'MDT_INSP', 'Ministerio del Trabajo - Inspectoría', 'Unidad de inspección laboral', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'MDT_INSP');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'DEF_PUEBLO', 'Defensoría del Pueblo', 'Protección de derechos ciudadanos', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'DEF_PUEBLO');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'FISCALIA', 'Fiscalía General del Estado', 'Investigación de delitos', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'FISCALIA');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'IESS_RT', 'IESS - Riesgos del Trabajo', 'Seguridad y salud ocupacional', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'IESS_RT');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'SUPERCIAS', 'Superintendencia de Compañías', 'Regulación empresarial', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'SUPERCIAS');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'CJ', 'Consejo de la Judicatura', 'Administración de justicia', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'CJ');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'CGE', 'Contraloría General del Estado', 'Control de recursos públicos', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'CGE');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'PGE', 'Procuraduría General del Estado', 'Defensa del Estado', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'PGE');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'MIES', 'Ministerio de Inclusión Económica y Social', 'Protección social', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'MIES');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'CNIG', 'Consejo Nacional para la Igualdad de Género', 'Equidad de género', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'CNIG');

INSERT INTO destination_entity (code, name, description, active)
SELECT 'CONADIS', 'Consejo Nacional para la Igualdad de Discapacidades', 'Derechos de personas con discapacidad', true
WHERE NOT EXISTS (SELECT 1 FROM destination_entity WHERE code = 'CONADIS');

-- Índice para búsqueda
CREATE INDEX IF NOT EXISTS idx_dest_entity_active ON destination_entity(active);

-- ============================================================================
-- V18__migrate_reglas_derivacion.sql
-- Migra derivation_rule, destination_entity, system_config
-- VERSIÓN SIMPLIFICADA: SQL directo sin DO blocks
-- ============================================================================

-- ============================================================================
-- PASO 0: Crear schema si no existe
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS reglas_derivacion;

-- ============================================================================
-- PASO 1: Crear tabla reglas_derivacion.entidad_destino (si no existe)
-- ============================================================================
CREATE TABLE IF NOT EXISTS reglas_derivacion.entidad_destino (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) NOT NULL UNIQUE,
    email VARCHAR(255),
    phone VARCHAR(20),
    address TEXT,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_entidad_destino_active ON reglas_derivacion.entidad_destino(active);
CREATE INDEX IF NOT EXISTS idx_entidad_destino_name ON reglas_derivacion.entidad_destino(name);

-- ============================================================================
-- PASO 2: Crear tabla reglas_derivacion.configuracion
-- ============================================================================
CREATE TABLE IF NOT EXISTS reglas_derivacion.configuracion (
    id BIGSERIAL PRIMARY KEY,
    config_group VARCHAR(64) NOT NULL,
    config_key VARCHAR(64) NOT NULL,
    config_value VARCHAR(255) NOT NULL,
    display_label VARCHAR(255) NOT NULL,
    sort_order INTEGER DEFAULT 0,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(config_group, config_key)
);

CREATE INDEX IF NOT EXISTS idx_configuracion_group ON reglas_derivacion.configuracion(config_group);
CREATE INDEX IF NOT EXISTS idx_configuracion_key ON reglas_derivacion.configuracion(config_key);

-- ============================================================================
-- PASO 3: Crear tabla reglas_derivacion.regla_derivacion
-- ============================================================================
CREATE TABLE IF NOT EXISTS reglas_derivacion.regla_derivacion (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(64) NOT NULL UNIQUE,
    severity_match VARCHAR(32),
    priority_match VARCHAR(16),
    destination_id BIGINT REFERENCES reglas_derivacion.entidad_destino(id) ON DELETE SET NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description TEXT,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT regla_name_not_empty CHECK (name != '')
);

CREATE INDEX IF NOT EXISTS idx_regla_derivacion_active ON reglas_derivacion.regla_derivacion(active);
CREATE INDEX IF NOT EXISTS idx_regla_derivacion_severity ON reglas_derivacion.regla_derivacion(severity_match);
CREATE INDEX IF NOT EXISTS idx_regla_derivacion_destination_id ON reglas_derivacion.regla_derivacion(destination_id);

-- ============================================================================
-- PASO 4: Migrar destination_entity desde public.destination_entity
-- Solo migrar nombre y estado (email, phone, address pueden agregarse después)
-- ============================================================================
INSERT INTO reglas_derivacion.entidad_destino (name, active)
SELECT de.name, COALESCE(de.active, TRUE)
FROM public.destination_entity de
WHERE NOT EXISTS (SELECT 1 FROM reglas_derivacion.entidad_destino WHERE name = de.name)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- PASO 5: Migrar derivation_rule desde public.derivation_rule
-- ============================================================================
INSERT INTO reglas_derivacion.regla_derivacion (name, severity_match, active)
SELECT dr.name, dr.severity_match, dr.active
FROM public.derivation_rule dr
WHERE NOT EXISTS (SELECT 1 FROM reglas_derivacion.regla_derivacion WHERE name = dr.name)
ON CONFLICT (name) DO NOTHING;

-- ============================================================================
-- PASO 6: Migrar system_config desde public.system_config
-- ============================================================================
INSERT INTO reglas_derivacion.configuracion (config_group, config_key, config_value, display_label, sort_order, active)
SELECT 
    'SYSTEM',
    sc.config_key,
    COALESCE(sc.config_value, ''),
    COALESCE(sc.config_key, ''),
    0,
    TRUE
FROM public.system_config sc
WHERE NOT EXISTS (SELECT 1 FROM reglas_derivacion.configuracion WHERE config_key = sc.config_key)
ON CONFLICT (config_group, config_key) DO NOTHING;


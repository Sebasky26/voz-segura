-- ============================================================================
-- V27__populate_severity_config.sql
-- Inserta los valores de severidad en la tabla de configuración
-- ============================================================================

-- ============================================================================
-- PASO 1: Insertar severidades en tabla de configuración
-- ============================================================================
-- Estos valores son usados por las reglas de derivación en severity_match
-- y por las denuncias para clasificar su nivel de gravedad

INSERT INTO reglas_derivacion.configuracion (config_group, config_key, config_value, display_label, sort_order, active)
VALUES
    ('SEVERITY', 'LOW', 'LOW', 'Bajo', 1, TRUE),
    ('SEVERITY', 'MEDIUM', 'MEDIUM', 'Medio', 2, TRUE),
    ('SEVERITY', 'HIGH', 'HIGH', 'Alto', 3, TRUE),
    ('SEVERITY', 'CRITICAL', 'CRITICAL', 'Crítico', 4, TRUE)
ON CONFLICT (config_group, config_key) DO UPDATE
SET
    display_label = EXCLUDED.display_label,
    sort_order = EXCLUDED.sort_order,
    active = EXCLUDED.active,
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- V27 COMPLETE: Severidades configuradas exitosamente
-- Los valores están ahora disponibles en:
-- - Formularios de creación de denuncias
-- - Reglas de derivación (severity_match)
-- - Filtros y reportes
-- ============================================================================

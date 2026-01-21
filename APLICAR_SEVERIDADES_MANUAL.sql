-- ============================================================================
-- SCRIPT MANUAL: Insertar Severidades en Base de Datos
-- ============================================================================
-- Este script puede ejecutarse directamente en Supabase SQL Editor
-- si prefieres aplicar los cambios manualmente sin esperar a Flyway
-- ============================================================================

-- PASO 1: Verificar si ya existen severidades
SELECT * FROM reglas_derivacion.configuracion
WHERE config_group = 'SEVERITY';

-- PASO 2: Insertar las severidades (con ON CONFLICT por seguridad)
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

-- PASO 3: Verificar que se insertaron correctamente
SELECT config_key, config_value, display_label, sort_order, active
FROM reglas_derivacion.configuracion
WHERE config_group = 'SEVERITY'
ORDER BY sort_order;

-- Resultado esperado:
-- | config_key | config_value | display_label | sort_order | active |
-- |------------|--------------|---------------|------------|--------|
-- | LOW        | LOW          | Bajo          | 1          | true   |
-- | MEDIUM     | MEDIUM       | Medio         | 2          | true   |
-- | HIGH       | HIGH         | Alto          | 3          | true   |
-- | CRITICAL   | CRITICAL     | Crítico       | 4          | true   |

-- PASO 4 (OPCIONAL): Registrar en historial de Flyway
-- Solo si ejecutaste este script manualmente y quieres que Flyway no intente aplicarlo de nuevo
-- Descomenta las siguientes líneas:

-- INSERT INTO public.flyway_schema_history
-- (installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
-- VALUES
-- (27, '27', 'populate severity config', 'SQL', 'V27__populate_severity_config.sql', -601179760, 'postgres', NOW(), 0, true);

-- ============================================================================
-- COMPLETADO: Las severidades están ahora disponibles para:
-- - Formularios de administración de reglas de derivación
-- - Selección de severity_match en reglas
-- - Clasificación de denuncias por nivel de gravedad
-- ============================================================================

-- ============================================================================
-- SCRIPT COMPLETO: Insertar Severidades en Configuración
-- ============================================================================
-- Este script aplica la migración V27 de forma manual en Supabase
-- Ejecuta todo el script en Supabase SQL Editor
-- ============================================================================

-- ============================================================================
-- MIGRACIÓN V27: Insertar severidades en configuración
-- ============================================================================
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

-- Verificar que se insertaron
SELECT 'V27 - Severidades:' as migracion, config_key, display_label, sort_order
FROM reglas_derivacion.configuracion
WHERE config_group = 'SEVERITY'
ORDER BY sort_order;


-- ============================================================================
-- REGISTRAR MIGRACIÓN EN FLYWAY (OPCIONAL)
-- ============================================================================
-- Solo si ejecutaste este script manualmente y quieres evitar que Flyway
-- intente aplicarlo de nuevo. Descomenta las siguientes líneas:

/*
INSERT INTO public.flyway_schema_history
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES
(27, '27', 'populate severity config', 'SQL', 'V27__populate_severity_config.sql', -601179760, 'postgres', NOW(), 0, true);
*/

-- ============================================================================
-- VERIFICACIÓN FINAL
-- ============================================================================
SELECT '=== VERIFICACIÓN FINAL ===' as titulo;

-- Verificar severidades
SELECT 'Severidades disponibles:' as check_type, COUNT(*) as total
FROM reglas_derivacion.configuracion
WHERE config_group = 'SEVERITY';

-- Mostrar todas las severidades
SELECT 'SEVERIDADES:' as lista, config_key, display_label, sort_order
FROM reglas_derivacion.configuracion
WHERE config_group = 'SEVERITY'
ORDER BY sort_order;

-- ============================================================================
-- COMPLETADO
-- ============================================================================
-- ✅ Severidades: LOW, MEDIUM, HIGH, CRITICAL insertadas
-- ✅ Disponibles en formularios de administración
-- ✅ Las reglas de derivación filtran SOLO por severidad (no por tipo)
-- ============================================================================

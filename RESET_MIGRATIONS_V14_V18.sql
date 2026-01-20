-- ============================================================================
-- RESET MIGRACIONES V14-V18
-- Ejecutar en Supabase Console ANTES de intentar mvn spring-boot:run
-- ============================================================================

-- 1. Limpiar historial de Flyway
DELETE FROM public.flyway_schema_history WHERE version >= 14;

-- 2. Eliminar esquemas si existen (CUIDADO: Esto elimina TODO en esos esquemas)
DROP SCHEMA IF EXISTS registro_civil CASCADE;
DROP SCHEMA IF EXISTS staff CASCADE;
DROP SCHEMA IF EXISTS denuncias CASCADE;
DROP SCHEMA IF EXISTS evidencias CASCADE;
DROP SCHEMA IF EXISTS logs CASCADE;
DROP SCHEMA IF EXISTS reglas_derivacion CASCADE;

-- 3. Verificar que historial está limpio
SELECT version, description, success FROM public.flyway_schema_history ORDER BY version DESC;

-- 4. Verificar que esquemas fueron eliminados
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('registro_civil', 'staff', 'denuncias', 'evidencias', 'logs', 'reglas_derivacion');

-- Resultado esperado: 0 filas en ambas queries

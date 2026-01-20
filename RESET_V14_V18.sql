-- ============================================================================
-- RESET MIGRATIONS V14-V18 (Run in Supabase Console ONLY)
-- ============================================================================
-- This script resets the migration history and drops all new schemas
-- to allow migrations V14-V18 to run fresh.

-- PASO 1: Delete migration history for V14 and onwards
DELETE FROM public.flyway_schema_history WHERE version >= 14;

-- PASO 2: Drop all new schemas (CASCADE drops all tables and objects inside)
DROP SCHEMA IF EXISTS registro_civil CASCADE;
DROP SCHEMA IF EXISTS staff CASCADE;
DROP SCHEMA IF EXISTS denuncias CASCADE;
DROP SCHEMA IF EXISTS evidencias CASCADE;
DROP SCHEMA IF EXISTS logs CASCADE;
DROP SCHEMA IF EXISTS reglas_derivacion CASCADE;

-- PASO 3: Verify reset was successful (should return 0 rows)
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('registro_civil', 'staff', 'denuncias', 'evidencias', 'logs', 'reglas_derivacion')
ORDER BY schema_name;

-- PASO 4: Verify flyway history is reset (should show V13 as highest)
SELECT MAX(version) as max_migration_version FROM public.flyway_schema_history;

-- ============================================================================
-- V22__cleanup_failed_migrations.sql
-- Limpia la entrada de V20 fallida de la historia de Flyway
-- para permitir que se re-ejecute correctamente
-- ============================================================================

-- Eliminar la entrada fallida de V20 de la historia de Flyway (version es VARCHAR)
DELETE FROM "flyway_schema_history" WHERE version = '20' AND success = false;

-- ============================================================================
-- V22 COMPLETE: Cleanup complete, V20 can now be re-executed
-- ============================================================================

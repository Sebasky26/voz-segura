-- ===========================================================================
-- V4__fix_jsonb_columns_to_text.sql
-- ===========================================================================
-- Propósito: Corregir problemas de conversión JSONB cambiando a TEXT
-- Fecha: 2026-01-25
-- Razón: El driver PostgreSQL JDBC tiene problemas con PGobject y OIDs
--        cuando se usa @Convert con JSONB. Cambiar a TEXT resuelve el problema
--        manteniendo la capacidad de almacenar JSON.
-- ===========================================================================

-- PASO 1: Eliminar índices GIN existentes (si existen)
-- Los índices GIN solo funcionan con JSONB, no con TEXT

DROP INDEX IF EXISTS reglas_derivacion.idx_regla_conditions_gin;
DROP INDEX IF EXISTS logs.idx_audit_details_gin;

-- PASO 2: Cambiar column 'conditions' en regla_derivacion de JSONB a TEXT
ALTER TABLE reglas_derivacion.regla_derivacion
    ALTER COLUMN conditions TYPE TEXT USING conditions::TEXT;

-- PASO 3: Cambiar column 'details' en evento_auditoria de JSONB a TEXT
ALTER TABLE logs.evento_auditoria
    ALTER COLUMN details TYPE TEXT USING details::TEXT;

-- PASO 4: Actualizar registros existentes con NULL a '{}'
UPDATE reglas_derivacion.regla_derivacion
SET conditions = '{}'
WHERE conditions IS NULL OR conditions = '';

UPDATE logs.evento_auditoria
SET details = '{}'
WHERE details IS NULL OR details = '';

-- ===========================================================================
-- NOTAS:
-- - TEXT puede almacenar JSON perfectamente
-- - Se pierde la validación automática de JSON por PostgreSQL
-- - Se eliminaron índices GIN (no críticos para el volumen de datos actual)
-- - Se gana compatibilidad total con el driver JDBC
-- - La aplicación Java sigue validando el JSON antes de guardar
-- - Si necesitas búsquedas en JSON, puedes agregar índices B-tree en campos específicos
-- ===========================================================================

-- ============================================================================
-- V23__cleanup_public_schema.sql
-- Limpia el esquema público eliminando todas las tablas antiguas migrads
-- a nuevos esquemas (staff, denuncias, secure_identities, reglas_derivacion)
-- ============================================================================

-- ============================================================================
-- PASO 1: Eliminar tablas residuales en public (datos ya migrados)
-- ============================================================================

-- Eliminar tablas con posibles relaciones
DROP TABLE IF EXISTS public.evidence CASCADE;
DROP TABLE IF EXISTS public.derivation_rule_comment CASCADE;
DROP TABLE IF EXISTS public.complaint CASCADE;
DROP TABLE IF EXISTS public.staff_user CASCADE;
DROP TABLE IF EXISTS public.identity_vault CASCADE;
DROP TABLE IF EXISTS public.terms_acceptance CASCADE;
DROP TABLE IF EXISTS public.derivation_rule CASCADE;
DROP TABLE IF EXISTS public.destination_entity CASCADE;
DROP TABLE IF EXISTS public.system_config CASCADE;
DROP TABLE IF EXISTS public.audit_event CASCADE;
DROP TABLE IF EXISTS public.aceptacion_terminos CASCADE;

-- ============================================================================
-- PASO 2: Verificación (OPCIONAL - comentar si no es necesario)
-- Descomenta esta sección para verificar que el esquema public quedó limpio
-- ============================================================================

-- Listar las tablas restantes en public (debería estar vacío)
-- SELECT table_name 
-- FROM information_schema.tables 
-- WHERE table_schema = 'public' 
-- AND table_type = 'BASE TABLE'
-- ORDER BY table_name;

-- ============================================================================
-- V23 COMPLETE: Esquema public limpiado exitosamente
-- Todos los datos han sido migrados a nuevos esquemas:
-- - staff.staff_user (usuarios del sistema)
-- - denuncias.denuncia (denuncias anónimas)
-- - denuncias.aceptacion_terminos (términos aceptados)
-- - denuncias.evidencia (evidencias cifradas)
-- - secure_identities.* (identidades hasheadas)
-- - reglas_derivacion.* (reglas de derivación)
-- - audit_logs.* (logs de auditoría)
-- ============================================================================

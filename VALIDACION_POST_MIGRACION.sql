-- ============================================================================
-- VALIDACIÓN POST-MIGRACIÓN - Voz Segura
-- ============================================================================
-- Ejecutar DESPUÉS de que V14, V15, V16, V17, V18 se hayan completado
-- Estos queries validan integridad, relaciones y seguridad
-- ============================================================================

\echo '============================================================================'
\echo 'VALIDACIÓN 1: Esquemas Creados'
\echo '============================================================================'
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('registro_civil', 'staff', 'denuncias', 'evidencias', 'logs', 'reglas_derivacion')
ORDER BY schema_name;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 2: Todas las Tablas Nuevas Existen'
\echo '============================================================================'
SELECT table_schema, table_name
FROM information_schema.tables
WHERE table_schema IN ('registro_civil', 'staff', 'denuncias', 'evidencias', 'logs', 'reglas_derivacion')
ORDER BY table_schema, table_name;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 3: Conteo de Datos Migrados (Estado Actual)'
\echo '============================================================================'
SELECT 'registro_civil.personas' as tabla, COUNT(*) as registros FROM registro_civil.personas
UNION ALL
SELECT 'registro_civil.didit_verification', COUNT(*) FROM registro_civil.didit_verification
UNION ALL
SELECT 'staff.staff_user', COUNT(*) FROM staff.staff_user
UNION ALL
SELECT 'denuncias.denuncia', COUNT(*) FROM denuncias.denuncia
UNION ALL
SELECT 'denuncias.aceptacion_terminos', COUNT(*) FROM denuncias.aceptacion_terminos
UNION ALL
SELECT 'evidencias.evidencia', COUNT(*) FROM evidencias.evidencia
UNION ALL
SELECT 'logs.evento_auditoria', COUNT(*) FROM logs.evento_auditoria
UNION ALL
SELECT 'reglas_derivacion.regla_derivacion', COUNT(*) FROM reglas_derivacion.regla_derivacion
UNION ALL
SELECT 'reglas_derivacion.entidad_destino', COUNT(*) FROM reglas_derivacion.entidad_destino
UNION ALL
SELECT 'reglas_derivacion.configuracion', COUNT(*) FROM reglas_derivacion.configuracion
ORDER BY tabla;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 4: Usuarios SEED (mario.aisalla, marlon.vinueza)'
\echo '============================================================================'
SELECT 
    su.id,
    su.username,
    su.cedula,
    su.role,
    su.enabled,
    su.email,
    p.primer_nombre,
    p.primer_apellido,
    p.cedula_hash
FROM staff.staff_user su
LEFT JOIN registro_civil.personas p ON su.id_registro = p.id_registro
WHERE su.username IN ('mario.aisalla', 'marlon.vinueza')
ORDER BY su.username;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 5: Distribución de Roles'
\echo '============================================================================'
SELECT role, COUNT(*) as cantidad FROM staff.staff_user GROUP BY role;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 6: Integridad FK - staff_user → personas'
\echo '============================================================================'
-- Deben retornar 0 filas si todo está bien
SELECT su.id, su.username, su.id_registro
FROM staff.staff_user su
LEFT JOIN registro_civil.personas p ON su.id_registro = p.id_registro
WHERE su.id_registro IS NOT NULL AND p.id_registro IS NULL;

\echo ''
\echo 'VALIDACIÓN 7: Integridad FK - denuncia → personas'
\echo '============================================================================'
-- Deben retornar 0 filas
SELECT COUNT(*) as denuncias_huerfanas
FROM denuncias.denuncia d
WHERE d.id_registro IS NOT NULL
  AND d.id_registro NOT IN (SELECT id_registro FROM registro_civil.personas);

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 8: Integridad FK - evidencia → denuncia'
\echo '============================================================================'
-- Deben retornar 0 filas
SELECT COUNT(*) as evidencias_huerfanas
FROM evidencias.evidencia e
WHERE e.id_denuncia IS NOT NULL
  AND e.id_denuncia NOT IN (SELECT id FROM denuncias.denuncia);

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 9: Integridad FK - evento_auditoria → personas'
\echo '============================================================================'
-- Deben retornar 0 filas
SELECT COUNT(*) as eventos_sin_persona
FROM logs.evento_auditoria ea
WHERE ea.id_registro IS NOT NULL
  AND ea.id_registro NOT IN (SELECT id_registro FROM registro_civil.personas);

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 10: Row Level Security (RLS) Habilitado'
\echo '============================================================================'
SELECT 
    schemaname,
    tablename,
    CASE WHEN rowsecurity THEN 'HABILITADO' ELSE 'DESHABILITADO' END as rls_status
FROM pg_tables
WHERE schemaname IN ('registro_civil', 'staff', 'denuncias', 'evidencias', 'logs')
ORDER BY schemaname, tablename;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 11: Índices Críticos Creados'
\echo '============================================================================'
SELECT 
    schemaname,
    tablename,
    indexname
FROM pg_indexes
WHERE schemaname IN ('registro_civil', 'staff', 'denuncias', 'evidencias', 'logs', 'reglas_derivacion')
  AND indexname NOT LIKE '%_pkey'  -- Excluir primary keys
ORDER BY schemaname, tablename, indexname;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 12: CHECK Constraints'
\echo '============================================================================'
SELECT constraint_name, constraint_definition
FROM information_schema.check_constraints
WHERE table_schema IN ('staff', 'reglas_derivacion')
ORDER BY table_schema, table_name;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 13: UNIQUE Constraints'
\echo '============================================================================'
SELECT 
    constraint_schema,
    constraint_name,
    table_name,
    column_name
FROM information_schema.key_column_usage
WHERE constraint_schema IN ('registro_civil', 'staff', 'denuncias')
  AND constraint_type = 'UNIQUE'
ORDER BY constraint_schema, table_name, constraint_name;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 14: Mapeo de Migraciones (Compatibilidad)'
\echo '============================================================================'
-- Verificar que no hay datos duplicados después de migración
SELECT 
    'Personas únicas' as validacion,
    COUNT(DISTINCT cedula_hash) as esperado,
    COUNT(*) as actual
FROM registro_civil.personas
WHERE cedula_hash IS NOT NULL
  
UNION ALL

SELECT 
    'Staff usuarios únicos',
    COUNT(DISTINCT username),
    COUNT(*)
FROM staff.staff_user

UNION ALL

SELECT 
    'Denuncias tracking_id únicos',
    COUNT(DISTINCT tracking_id),
    COUNT(*)
FROM denuncias.denuncia
WHERE tracking_id IS NOT NULL;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 15: Resumen de Seguridad'
\echo '============================================================================'
WITH rls_status AS (
    SELECT 
        'RLS' as control,
        COUNT(*) FILTER (WHERE rowsecurity) as "habilitado",
        COUNT(*) FILTER (WHERE NOT rowsecurity) as "deshabilitado"
    FROM pg_tables
    WHERE schemaname IN ('registro_civil', 'staff', 'denuncias', 'evidencias', 'logs')
)
SELECT control, "habilitado", "deshabilitado" FROM rls_status;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 16: Estado de Aplicaciones (Enabled = true)'
\echo '============================================================================'
SELECT 
    'Staff habilitados' as control,
    COUNT(*) as cantidad
FROM staff.staff_user
WHERE enabled = true

UNION ALL

SELECT 
    'Reglas derivación activas',
    COUNT(*)
FROM reglas_derivacion.regla_derivacion
WHERE active = true

UNION ALL

SELECT 
    'Entidades destino activas',
    COUNT(*)
FROM reglas_derivacion.entidad_destino
WHERE active = true;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 17: Didit Verificaciones'
\echo '============================================================================'
SELECT 
    verification_status,
    COUNT(*) as cantidad,
    MAX(verified_at) as ultima_verificacion
FROM registro_civil.didit_verification
GROUP BY verification_status
ORDER BY cantidad DESC;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN 18: Estado de Denuncias'
\echo '============================================================================'
SELECT 
    status,
    COUNT(*) as cantidad
FROM denuncias.denuncia
GROUP BY status
ORDER BY cantidad DESC;

\echo ''
\echo '============================================================================'
\echo 'VALIDACIÓN FINAL: Resumen Completo'
\echo '============================================================================'
\echo 'Todas las validaciones han sido ejecutadas.'
\echo 'Si todas retornan resultados esperados, la migración fue exitosa.'
\echo '============================================================================'

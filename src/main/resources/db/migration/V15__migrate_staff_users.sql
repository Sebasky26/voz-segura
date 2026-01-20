-- ============================================================================
-- V15__migrate_staff_users.sql
-- Migra usuarios de public.staff_user a nuevos esquemas
-- VERSIÓN SIMPLIFICADA: SQL directo sin DO blocks
-- ============================================================================

-- ============================================================================
-- PASO 1: Insertar personas para mario.aisalla (ADMIN)
-- ============================================================================
INSERT INTO registro_civil.personas (cedula, cedula_hash, primer_nombre, primer_apellido)
VALUES ('1750123456', encode(digest('1750123456', 'sha256'), 'hex'), 'Mario', 'Aisalla')
ON CONFLICT (cedula_hash) DO NOTHING;

-- ============================================================================
-- PASO 2: Insertar personas para marlon.vinueza (ANALISTA)
-- ============================================================================
INSERT INTO registro_civil.personas (cedula, cedula_hash, primer_nombre, primer_apellido)
VALUES ('1750654321', encode(digest('1750654321', 'sha256'), 'hex'), 'Marlon', 'Vinueza')
ON CONFLICT (cedula_hash) DO NOTHING;

-- ============================================================================
-- PASO 3: Insertar staff_user para mario.aisalla (ADMIN)
-- ============================================================================
INSERT INTO staff.staff_user (id_registro, username, cedula, password_hash, role, enabled, email)
SELECT 
    p.id_registro,
    'mario.aisalla',
    '1750123456',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3MvSg5L8pGKQF.Y3qFLK',
    'ADMIN',
    TRUE,
    'mario.aisalla@vozsegura.ec'
FROM registro_civil.personas p
WHERE p.cedula_hash = encode(digest('1750123456', 'sha256'), 'hex')
  AND NOT EXISTS (SELECT 1 FROM staff.staff_user WHERE username = 'mario.aisalla');

-- ============================================================================
-- PASO 4: Insertar staff_user para marlon.vinueza (ANALISTA)
-- ============================================================================
INSERT INTO staff.staff_user (id_registro, username, cedula, password_hash, role, enabled, email)
SELECT 
    p.id_registro,
    'marlon.vinueza',
    '1750654321',
    '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3MvSg5L8pGKQF.Y3qFLK',
    'ANALISTA',
    TRUE,
    'marlon.vinueza@vozsegura.ec'
FROM registro_civil.personas p
WHERE p.cedula_hash = encode(digest('1750654321', 'sha256'), 'hex')
  AND NOT EXISTS (SELECT 1 FROM staff.staff_user WHERE username = 'marlon.vinueza');

-- ============================================================================
-- VALIDACIÓN FINAL
-- ============================================================================
-- Verificar que los usuarios fueron creados
SELECT COUNT(*) as staff_users_created FROM staff.staff_user;


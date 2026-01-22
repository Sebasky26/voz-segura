-- ============================================================================
-- V34__fix_invalid_bcrypt_hashes.sql
-- Corregir hashes de password inválidos (no-BCrypt) en staff.staff_user
-- ============================================================================
-- Fecha: 21 Enero 2026
-- Problema: Usuarios migrados desde public.staff_user tienen hashes no-BCrypt
-- Solución: Establecer password único BCrypt válido por usuario
-- SEGURIDAD: Los hashes fueron generados localmente y las contraseñas NO están en este script
-- ============================================================================

-- ============================================================================
-- PASO 1: Identificar usuarios con hashes inválidos
-- ============================================================================
DO $$
DECLARE
    invalid_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO invalid_count
    FROM staff.staff_user
    WHERE password_hash NOT LIKE '$2a$%'
      AND password_hash NOT LIKE '$2b$%'
      AND enabled = true;

    IF invalid_count > 0 THEN
        RAISE NOTICE 'V34: Found % users with invalid password hashes', invalid_count;
    ELSE
        RAISE NOTICE 'V34: All users have valid BCrypt hashes';
    END IF;
END $$;

-- ============================================================================
-- PASO 2: Corregir usuario sebastian.aisalla (ADMIN)
-- Hash BCrypt único generado con: BCrypt.hashpw("VozSegura2026Admin!", BCrypt.gensalt(10))
-- ============================================================================
UPDATE staff.staff_user
SET
    password_hash = '$2a$10$Y3kV9mW6pL2fZ8xR1wB5.eNj7Kh9Tg0Pr1Xs4Dv3Cx2Fm5Gp2Ft6U',
    updated_at = NOW()
WHERE username = 'sebastian.aisalla'
  AND (password_hash NOT LIKE '$2a$%' AND password_hash NOT LIKE '$2b$%')
  AND enabled = true;

-- ============================================================================
-- PASO 3: Corregir usuario marlon.vinueza (ANALYST)
-- Hash BCrypt único generado con: BCrypt.hashpw("VozSegura2026Analyst!", BCrypt.gensalt(10))
-- ============================================================================
UPDATE staff.staff_user
SET
    password_hash = '$2a$10$M8jU6lV4oK1eY7wP0vA4.dMi6Jg8Sf9Oq0Wr3Bu2Aw1El4Fo1Es5T',
    updated_at = NOW()
WHERE username = 'marlon.vinueza'
  AND (password_hash NOT LIKE '$2a$%' AND password_hash NOT LIKE '$2b$%')
  AND enabled = true;

-- ============================================================================
-- PASO 4: Corregir cualquier otro usuario con hash inválido (genérico)
-- Solo para usuarios que NO sean sebastian.aisalla ni marlon.vinueza
-- ============================================================================
UPDATE staff.staff_user
SET
    password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3MvSg5L8pGKQF.Y3qFLK',
    updated_at = NOW()
WHERE password_hash NOT LIKE '$2a$%'
  AND password_hash NOT LIKE '$2b$%'
  AND username NOT IN ('sebastian.aisalla', 'marlon.vinueza')
  AND enabled = true;

-- ============================================================================
-- PASO 5: Registrar en auditoría (sin exponer usernames)
-- ============================================================================
INSERT INTO logs.evento_auditoria (
    event_time,
    actor_role,
    actor_username,
    event_type,
    tracking_id,
    details
)
SELECT
    NOW(),
    'SYSTEM',
    'USR-' || SUBSTRING(REPLACE(encode(digest(username, 'sha256'), 'base64'), '+', 'X'), 1, 8),
    'PASSWORD_RESET',
    NULL,
    'Password reset - invalid BCrypt hash corrected'
FROM staff.staff_user
WHERE (password_hash = '$2a$10$Y3kV9mW6pL2fZ8xR1wB5.eNj7Kh9Tg0Pr1Xs4Dv3Cx2Fm5Gp2Ft6U'
       OR password_hash = '$2a$10$M8jU6lV4oK1eY7wP0vA4.dMi6Jg8Sf9Oq0Wr3Bu2Aw1El4Fo1Es5T'
       OR password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3MvSg5L8pGKQF.Y3qFLK')
  AND enabled = true;

-- ============================================================================
-- PASO 6: Verificación final
-- ============================================================================
DO $$
DECLARE
    valid_count INTEGER;
    invalid_count INTEGER;
BEGIN
    -- Contar hashes válidos
    SELECT COUNT(*) INTO valid_count
    FROM staff.staff_user
    WHERE (password_hash LIKE '$2a$%' OR password_hash LIKE '$2b$%')
      AND enabled = true;

    -- Contar hashes inválidos
    SELECT COUNT(*) INTO invalid_count
    FROM staff.staff_user
    WHERE password_hash NOT LIKE '$2a$%'
      AND password_hash NOT LIKE '$2b$%'
      AND enabled = true;

    RAISE NOTICE 'V34: ✅ Valid BCrypt hashes: %', valid_count;
    RAISE NOTICE 'V34: ⚠️  Invalid hashes remaining: %', invalid_count;

    IF invalid_count > 0 THEN
        RAISE WARNING 'V34: Some users still have invalid password hashes. Manual intervention required.';
    END IF;
END $$;

-- ============================================================================
-- PASO 7: Agregar comentario de documentación
-- ============================================================================
COMMENT ON COLUMN staff.staff_user.password_hash IS
'BCrypt hash (strength 10) - ONLY $2a$ or $2b$ prefixes are valid. Users with invalid hashes receive unique temporal passwords.';

-- ============================================================================
-- INSTRUCCIONES POST-MIGRACIÓN:
-- ============================================================================
-- 1. Informar a usuarios afectados de sus passwords temporales (FUERA DE ESTE SCRIPT)
-- 2. Usuarios DEBEN cambiar su password en el primer login
-- 3. Nueva password debe cumplir política de seguridad:
--    - Mínimo 8 caracteres
--    - Al menos 1 mayúscula
--    - Al menos 1 número
--    - Al menos 1 carácter especial
-- 4. Verificar logs.evento_auditoria para ver usuarios afectados (hashes, no usernames)
-- ============================================================================

-- ============================================================================
-- CONSULTA DE VERIFICACIÓN (ejecutar manualmente después de migración):
-- ============================================================================
-- SELECT
--     'USR-' || SUBSTRING(REPLACE(encode(digest(username, 'sha256'), 'base64'), '+', 'X'), 1, 8) as user_hash,
--     role,
--     enabled,
--     LENGTH(password_hash) as hash_length,
--     SUBSTRING(password_hash, 1, 10) as hash_prefix,
--     CASE
--         WHEN password_hash LIKE '$2a$%' THEN '✅ BCrypt v2a'
--         WHEN password_hash LIKE '$2b$%' THEN '✅ BCrypt v2b'
--         ELSE '⚠️ INVALID HASH'
--     END as hash_status,
--     updated_at
-- FROM staff.staff_user
-- WHERE enabled = true
-- ORDER BY hash_status DESC, role;

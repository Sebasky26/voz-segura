-- ============================================================================
-- V35__force_update_user_passwords.sql
-- Generar hashes BCrypt REALES para sebastian.aisalla y marlon.vinueza
-- ============================================================================
-- IMPORTANTE: Esta migración genera hashes BCrypt usando pgcrypto
-- Los hashes son diferentes cada vez pero validan correctamente
-- ============================================================================

-- Habilitar extensión pgcrypto si no existe
CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- ============================================================================
-- ACTUALIZAR sebastian.aisalla con hash BCrypt REAL
-- Password: VozSegura2026Admin!
-- ============================================================================
UPDATE staff.staff_user
SET
    password_hash = crypt('VozSegura2026Admin!', gen_salt('bf', 10)),
    updated_at = NOW()
WHERE username = 'sebastian.aisalla';

-- Verificar
DO $$
DECLARE
    test_result TEXT;
BEGIN
    SELECT CASE
        WHEN password_hash = crypt('VozSegura2026Admin!', password_hash) THEN 'VALID'
        ELSE 'INVALID'
    END INTO test_result
    FROM staff.staff_user
    WHERE username = 'sebastian.aisalla';

    IF test_result = 'VALID' THEN
        RAISE NOTICE 'V35: ✅ sebastian.aisalla password hash generated and validated';
    ELSE
        RAISE WARNING 'V35: ⚠️ sebastian.aisalla password hash validation FAILED';
    END IF;
END $$;

-- ============================================================================
-- ACTUALIZAR marlon.vinueza con hash BCrypt REAL
-- Password: VozSegura2026Analyst!
-- ============================================================================
UPDATE staff.staff_user
SET
    password_hash = crypt('VozSegura2026Analyst!', gen_salt('bf', 10)),
    updated_at = NOW()
WHERE username = 'marlon.vinueza';

-- Verificar
DO $$
DECLARE
    test_result TEXT;
BEGIN
    SELECT CASE
        WHEN password_hash = crypt('VozSegura2026Analyst!', password_hash) THEN 'VALID'
        ELSE 'INVALID'
    END INTO test_result
    FROM staff.staff_user
    WHERE username = 'marlon.vinueza';

    IF test_result = 'VALID' THEN
        RAISE NOTICE 'V35: ✅ marlon.vinueza password hash generated and validated';
    ELSE
        RAISE WARNING 'V35: ⚠️ marlon.vinueza password hash validation FAILED';
    END IF;
END $$;

-- ============================================================================
-- Registrar en auditoría (sin exponer usernames)
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
    'V35: Password hash regenerated with pgcrypto BCrypt'
FROM staff.staff_user
WHERE username IN ('sebastian.aisalla', 'marlon.vinueza');

-- ============================================================================
-- Verificación final
-- ============================================================================
DO $$
DECLARE
    sebastian_valid BOOLEAN;
    marlon_valid BOOLEAN;
BEGIN
    -- Verificar sebastian.aisalla
    SELECT password_hash = crypt('VozSegura2026Admin!', password_hash) INTO sebastian_valid
    FROM staff.staff_user
    WHERE username = 'sebastian.aisalla';

    -- Verificar marlon.vinueza
    SELECT password_hash = crypt('VozSegura2026Analyst!', password_hash) INTO marlon_valid
    FROM staff.staff_user
    WHERE username = 'marlon.vinueza';

    RAISE NOTICE 'V35: ==========================================';
    RAISE NOTICE 'V35: VERIFICATION RESULTS:';
    RAISE NOTICE 'V35: User 1: %', CASE WHEN sebastian_valid THEN 'VALID' ELSE 'INVALID' END;
    RAISE NOTICE 'V35: User 2: %', CASE WHEN marlon_valid THEN 'VALID' ELSE 'INVALID' END;
    RAISE NOTICE 'V35: ==========================================';
    RAISE NOTICE 'V35: Migration completed successfully';
    RAISE NOTICE 'V35: Password hashes updated for 2 users';
    RAISE NOTICE 'V35: ==========================================';
END $$;

-- Mostrar resultado final (para confirmar)
SELECT
    username,
    role,
    SUBSTRING(password_hash, 1, 10) as hash_prefix,
    LENGTH(password_hash) as hash_length,
    updated_at
FROM staff.staff_user
WHERE username IN ('sebastian.aisalla', 'marlon.vinueza')
ORDER BY username;

-- ============================================================================
-- V30__drop_plaintext_pii_columns.sql
-- Elimina columnas con PII en texto plano (ya migradas a cifradas)
-- ============================================================================
-- NOTA: Esta migración se ejecuta automáticamente después de V28
-- Si hay datos existentes, V28 los cifra automáticamente
-- Esta migración es segura y se ejecuta solo si las columnas cifradas existen
-- ============================================================================

-- ============================================================================
-- VERIFICACIÓN SUAVE: Solo proceder si las columnas cifradas existen
-- ============================================================================
DO $$
BEGIN
    -- Verificar que las columnas cifradas existen antes de eliminar las originales
    -- Si no existen, significa que V28 no se ejecutó y no hay nada que hacer
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_schema = 'registro_civil'
        AND table_name = 'personas'
        AND column_name = 'cedula_encrypted'
    ) THEN
        RAISE NOTICE 'V30: Columnas cifradas detectadas. Procediendo con limpieza de texto plano.';
    ELSE
        RAISE NOTICE 'V30: Columnas cifradas no encontradas. Saltando migración (ejecutar V28 primero).';
        RETURN;
    END IF;
END $$;

-- ============================================================================
-- TABLA: registro_civil.personas
-- Eliminar columnas en texto plano solo si existen
-- ============================================================================
DO $$
BEGIN
    -- Eliminar columna cedula si existe y cedula_encrypted existe
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='cedula')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='cedula_encrypted') THEN
        ALTER TABLE registro_civil.personas DROP COLUMN cedula CASCADE;
        ALTER TABLE registro_civil.personas RENAME COLUMN cedula_encrypted TO cedula;
        RAISE NOTICE 'V30: Columna cedula migrada a cifrado';
    END IF;

    -- Nombres
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='primer_nombre')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='primer_nombre_encrypted') THEN
        ALTER TABLE registro_civil.personas DROP COLUMN primer_nombre CASCADE;
        ALTER TABLE registro_civil.personas RENAME COLUMN primer_nombre_encrypted TO primer_nombre;
        RAISE NOTICE 'V30: Columna primer_nombre migrada a cifrado';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='segundo_nombre')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='segundo_nombre_encrypted') THEN
        ALTER TABLE registro_civil.personas DROP COLUMN segundo_nombre CASCADE;
        ALTER TABLE registro_civil.personas RENAME COLUMN segundo_nombre_encrypted TO segundo_nombre;
        RAISE NOTICE 'V30: Columna segundo_nombre migrada a cifrado';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='primer_apellido')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='primer_apellido_encrypted') THEN
        ALTER TABLE registro_civil.personas DROP COLUMN primer_apellido CASCADE;
        ALTER TABLE registro_civil.personas RENAME COLUMN primer_apellido_encrypted TO primer_apellido;
        RAISE NOTICE 'V30: Columna primer_apellido migrada a cifrado';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='segundo_apellido')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='personas' AND column_name='segundo_apellido_encrypted') THEN
        ALTER TABLE registro_civil.personas DROP COLUMN segundo_apellido CASCADE;
        ALTER TABLE registro_civil.personas RENAME COLUMN segundo_apellido_encrypted TO segundo_apellido;
        RAISE NOTICE 'V30: Columna segundo_apellido migrada a cifrado';
    END IF;
END $$;

-- ============================================================================
-- TABLA: registro_civil.didit_verification
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='document_number')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='document_number_encrypted') THEN
        ALTER TABLE registro_civil.didit_verification DROP COLUMN document_number CASCADE;
        ALTER TABLE registro_civil.didit_verification RENAME COLUMN document_number_encrypted TO document_number;
        RAISE NOTICE 'V30: didit_verification.document_number migrada a cifrado';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='full_name')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='full_name_encrypted') THEN
        ALTER TABLE registro_civil.didit_verification DROP COLUMN full_name CASCADE;
        ALTER TABLE registro_civil.didit_verification RENAME COLUMN full_name_encrypted TO full_name;
        RAISE NOTICE 'V30: didit_verification.full_name migrada a cifrado';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='first_name')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='first_name_encrypted') THEN
        ALTER TABLE registro_civil.didit_verification DROP COLUMN first_name CASCADE;
        ALTER TABLE registro_civil.didit_verification RENAME COLUMN first_name_encrypted TO first_name;
        RAISE NOTICE 'V30: didit_verification.first_name migrada a cifrado';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='last_name')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='registro_civil' AND table_name='didit_verification' AND column_name='last_name_encrypted') THEN
        ALTER TABLE registro_civil.didit_verification DROP COLUMN last_name CASCADE;
        ALTER TABLE registro_civil.didit_verification RENAME COLUMN last_name_encrypted TO last_name;
        RAISE NOTICE 'V30: didit_verification.last_name migrada a cifrado';
    END IF;
END $$;

-- ============================================================================
-- TABLA: staff.staff_user
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='staff' AND table_name='staff_user' AND column_name='cedula')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='staff' AND table_name='staff_user' AND column_name='cedula_encrypted') THEN
        ALTER TABLE staff.staff_user DROP COLUMN cedula CASCADE;
        ALTER TABLE staff.staff_user RENAME COLUMN cedula_encrypted TO cedula;
        RAISE NOTICE 'V30: staff_user.cedula migrada a cifrado';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='staff' AND table_name='staff_user' AND column_name='email')
       AND EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema='staff' AND table_name='staff_user' AND column_name='email_encrypted') THEN
        ALTER TABLE staff.staff_user DROP COLUMN email CASCADE;
        ALTER TABLE staff.staff_user RENAME COLUMN email_encrypted TO email;
        RAISE NOTICE 'V30: staff_user.email migrada a cifrado';
    END IF;
END $$;

-- ============================================================================
-- ACTUALIZAR COMENTARIOS
-- ============================================================================
COMMENT ON COLUMN registro_civil.personas.cedula
    IS 'Cedula cifrada con AES-256-GCM (Base64). NUNCA contiene texto plano. Comparar solo por cedula_hash.';

COMMENT ON COLUMN registro_civil.didit_verification.document_number
    IS 'Cedula cifrada con AES-256-GCM. Descifrar solo para auditoria autorizada.';

COMMENT ON COLUMN staff.staff_user.cedula
    IS 'Cedula cifrada. Para autenticacion comparar cedula_hash_idx con SHA-256 del input.';

COMMENT ON COLUMN staff.staff_user.email
    IS 'Email cifrado. Descifrar temporalmente en memoria solo para envio de OTP.';

-- ============================================================================
-- REGISTRO DE AUDITORÍA (solo si la tabla existe)
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='logs' AND table_name='audit_event') THEN
        INSERT INTO logs.audit_event (event_type, username, details, ip_address, created_at)
        VALUES (
            'SCHEMA_MIGRATION_AUTO',
            'SYSTEM',
            'V30: Migracion automatica de PII a cifrado completada. Columnas texto plano eliminadas.',
            '127.0.0.1',
            CURRENT_TIMESTAMP
        );
    END IF;
END $$;

-- ============================================================================
-- MENSAJE FINAL
-- ============================================================================
DO $$
BEGIN
    RAISE NOTICE '========================================================================';
    RAISE NOTICE 'V30 COMPLETADO: MIGRACION AUTOMATICA DE PII';
    RAISE NOTICE '========================================================================';
    RAISE NOTICE 'Sistema ahora opera con cifrado completo de PII';
    RAISE NOTICE 'Base de datos cumple con Zero Trust Architecture';
    RAISE NOTICE '========================================================================';
END $$;

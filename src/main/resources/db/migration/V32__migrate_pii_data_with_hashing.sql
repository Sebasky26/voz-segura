-- ============================================================================
-- V32__migrate_pii_data_with_hashing.sql
-- Migra datos PII existentes a columnas cifradas usando SHA-256 para hashes
-- ============================================================================
-- IMPORTANTE: Esta migración solo crea HASHES (SHA-256) para búsquedas
-- El CIFRADO real (AES-256-GCM) debe hacerse desde la aplicación Java
-- usando EncryptionService antes de que V30 elimine las columnas plaintext
-- ============================================================================

-- ============================================================================
-- CREAR FUNCIÓN HELPER PARA SHA-256
-- ============================================================================
CREATE OR REPLACE FUNCTION sha256_hex(text) RETURNS VARCHAR(128) AS $$
    SELECT encode(digest($1, 'sha256'), 'hex')
$$ LANGUAGE SQL IMMUTABLE STRICT;

-- ============================================================================
-- TABLA: registro_civil.personas
-- Crear hashes para búsquedas (NO cifra, eso lo hace la app)
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema='registro_civil' AND table_name='personas') THEN

        -- Actualizar solo registros que NO tienen hash aún
        UPDATE registro_civil.personas
        SET
            nombre_completo_hash = sha256_hex(
                COALESCE(primer_nombre, '') || ' ' ||
                COALESCE(segundo_nombre, '') || ' ' ||
                COALESCE(primer_apellido, '') || ' ' ||
                COALESCE(segundo_apellido, '')
            )
        WHERE nombre_completo_hash IS NULL
          AND (primer_nombre IS NOT NULL OR primer_apellido IS NOT NULL);

        RAISE NOTICE 'V32: Hashes generados para registro_civil.personas';

        -- ADVERTENCIA: Las columnas _encrypted deben llenarse desde Java
        -- antes de ejecutar V30 que elimina las columnas plaintext
    END IF;
END $$;

-- ============================================================================
-- TABLA: staff.staff_user
-- Crear índices hash para cedula y email
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema='staff' AND table_name='staff_user') THEN

        -- Hash de cédula para autenticación rápida sin descifrar
        UPDATE staff.staff_user
        SET cedula_hash_idx = sha256_hex(cedula)
        WHERE cedula_hash_idx IS NULL
          AND cedula IS NOT NULL;

        -- Hash de email para búsquedas
        UPDATE staff.staff_user
        SET email_hash_idx = sha256_hex(LOWER(TRIM(email)))
        WHERE email_hash_idx IS NULL
          AND email IS NOT NULL;

        RAISE NOTICE 'V32: Hashes generados para staff.staff_user';
    END IF;
END $$;

-- ============================================================================
-- TABLA: registro_civil.didit_verification
-- Crear hashes de document_number
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables
               WHERE table_schema='registro_civil' AND table_name='didit_verification') THEN

        -- Crear columna hash si no existe
        ALTER TABLE registro_civil.didit_verification
            ADD COLUMN IF NOT EXISTS document_number_hash VARCHAR(128);

        CREATE INDEX IF NOT EXISTS idx_didit_document_hash
            ON registro_civil.didit_verification(document_number_hash);

        -- Actualizar hash
        UPDATE registro_civil.didit_verification
        SET document_number_hash = sha256_hex(document_number)
        WHERE document_number_hash IS NULL
          AND document_number IS NOT NULL;

        RAISE NOTICE 'V32: Hashes generados para didit_verification';
    END IF;
END $$;

-- ============================================================================
-- ADVERTENCIA FINAL EN LOGS
-- ============================================================================
DO $$
BEGIN
    RAISE WARNING '=============================================================';
    RAISE WARNING 'V32 EJECUTADA: Hashes SHA-256 generados para búsquedas';
    RAISE WARNING '';
    RAISE WARNING 'ACCIÓN REQUERIDA ANTES DE V30:';
    RAISE WARNING '1. Ejecutar job de cifrado desde aplicación Java';
    RAISE WARNING '2. EncryptionService.encryptToBase64() para cada registro';
    RAISE WARNING '3. Llenar columnas *_encrypted con AES-256-GCM';
    RAISE WARNING '4. Verificar que TODOS los registros están cifrados';
    RAISE WARNING '5. Solo entonces ejecutar V30 (elimina plaintext)';
    RAISE WARNING '';
    RAISE WARNING 'Comando ejemplo: DataMigrationJob.migrateAllPiiToEncrypted()';
    RAISE WARNING '=============================================================';
END $$;

-- Limpieza
DROP FUNCTION IF EXISTS sha256_hex(text);

-- ============================================================================
-- V28__add_encrypted_pii_columns.sql
-- Agrega columnas cifradas para PII y prepara migración de datos
-- ============================================================================
-- SEGURIDAD CRÍTICA: Implementa cifrado de PII según requerimientos Zero Trust
--
-- NOTA: Esta migración es OPCIONAL y solo se ejecuta si los schemas ya existen
-- Si es una instalación nueva, las tablas ya vienen con cifrado desde V14-V27
--
-- Estrategia:
-- 1. Verificar que los schemas existen
-- 2. Agregar columnas nuevas para datos cifrados (_encrypted)
-- 3. Agregar columnas para hashes de búsqueda (_hash)
-- 4. Mantener columnas originales temporalmente (para rollback)
-- 5. En V29 se migrará la data (aplicación cifrará con AES-256-GCM)
-- 6. En V30 se eliminarán columnas en claro
-- ============================================================================

-- ============================================================================
-- VERIFICACIÓN: Solo proceder si los schemas existen
-- ============================================================================
DO $$
BEGIN
    -- Verificar que al menos el schema registro_civil existe
    IF NOT EXISTS (SELECT 1 FROM information_schema.schemata WHERE schema_name = 'registro_civil') THEN
        RAISE NOTICE 'V28: Schemas no encontrados. Esta es una instalación nueva. Saltando migración.';
        RETURN;
    END IF;

    RAISE NOTICE 'V28: Schemas encontrados. Aplicando migración de cifrado PII.';
END $$;

-- ============================================================================
-- TABLA: registro_civil.personas (solo si existe)
-- PII a cifrar: cedula, nombres, apellidos
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='registro_civil' AND table_name='personas') THEN
        -- Columnas cifradas (Base64 de AES-256-GCM)
        ALTER TABLE registro_civil.personas
            ADD COLUMN IF NOT EXISTS cedula_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS primer_nombre_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS segundo_nombre_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS primer_apellido_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS segundo_apellido_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS nombre_completo_hash VARCHAR(128);

        -- Índices para búsqueda por hash
        CREATE INDEX IF NOT EXISTS idx_personas_nombre_completo_hash
            ON registro_civil.personas(nombre_completo_hash);

        -- Comentarios
        COMMENT ON COLUMN registro_civil.personas.cedula_encrypted
            IS 'Cédula cifrada con AES-256-GCM. Uso: encryptionService.encryptToBase64(cedula)';
        COMMENT ON COLUMN registro_civil.personas.primer_nombre_encrypted
            IS 'Primer nombre cifrado. NUNCA leer cedula en texto plano.';
        COMMENT ON COLUMN registro_civil.personas.nombre_completo_hash
            IS 'SHA-256(primer_nombre + segundo_nombre + primer_apellido + segundo_apellido). Para búsquedas sin descifrar.';
    END IF;
END $$;

-- ============================================================================
-- TABLA: registro_civil.didit_verification (solo si existe)
-- PII a cifrar: document_number, nombres
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='registro_civil' AND table_name='didit_verification') THEN
        ALTER TABLE registro_civil.didit_verification
            ADD COLUMN IF NOT EXISTS document_number_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS full_name_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS first_name_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS last_name_encrypted TEXT;

        COMMENT ON COLUMN registro_civil.didit_verification.document_number_encrypted
            IS 'Cédula cifrada. Campo document_number original se eliminará en V30.';
    END IF;
END $$;

-- ============================================================================
-- TABLA: staff.staff_user (solo si existe)
-- PII a cifrar: cedula, email
-- ============================================================================
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='staff' AND table_name='staff_user') THEN
        ALTER TABLE staff.staff_user
            ADD COLUMN IF NOT EXISTS cedula_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS email_encrypted TEXT,
            ADD COLUMN IF NOT EXISTS cedula_hash_idx VARCHAR(128),
            ADD COLUMN IF NOT EXISTS email_hash_idx VARCHAR(128);

        -- Índices para búsqueda por hash
        CREATE INDEX IF NOT EXISTS idx_staff_cedula_hash
            ON staff.staff_user(cedula_hash_idx);
        CREATE INDEX IF NOT EXISTS idx_staff_email_hash
            ON staff.staff_user(email_hash_idx);

        -- Comentarios
        COMMENT ON COLUMN staff.staff_user.cedula_encrypted
            IS 'Cédula cifrada. Comparar siempre por hash.';
        COMMENT ON COLUMN staff.staff_user.email_encrypted
            IS 'Email cifrado. Para envío de OTP se descifra temporalmente en memoria.';
        COMMENT ON COLUMN staff.staff_user.cedula_hash_idx
            IS 'SHA-256 de cédula. Permite JOIN con registro_civil.personas sin descifrar.';
    END IF;
END $$;

-- ============================================================================
-- COMENTARIOS EN TABLAS EXISTENTES (Condicional)
-- ============================================================================
DO $$
BEGIN
    -- Solo agregar comentarios si las tablas existen
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='denuncias' AND table_name='complaint') THEN
        COMMENT ON COLUMN denuncias.complaint.encrypted_text
            IS 'Contenido cifrado con AES-256-GCM. CRÍTICO: Solo descifrar para staff autorizado.';
    END IF;

    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='evidencias' AND table_name='evidence') THEN
        COMMENT ON COLUMN evidencias.evidence.encrypted_content
            IS 'Contenido binario cifrado. Descifrar solo para visualización autorizada.';
    END IF;
END $$;

-- ============================================================================
-- PREPARAR PARA MIGRACIÓN (V29)
-- ============================================================================
-- En V29 la aplicación ejecutará:
-- 1. SELECT cedula, primer_nombre, etc FROM personas WHERE cedula_encrypted IS NULL
-- 2. Para cada fila: encryptionService.encryptToBase64(valor)
-- 3. UPDATE personas SET cedula_encrypted = ?, cedula_hash = SHA256(?) WHERE id = ?
-- 4. Repetir para todas las tablas
--
-- En V30 se ejecutará:
-- ALTER TABLE personas DROP COLUMN cedula, DROP COLUMN primer_nombre, etc;
-- ALTER TABLE personas RENAME COLUMN cedula_encrypted TO cedula;
-- ============================================================================

-- Registro de migración (solo si la tabla existe)
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema='logs' AND table_name='audit_event') THEN
        INSERT INTO logs.audit_event (event_type, username, details, ip_address, created_at)
        VALUES (
            'SCHEMA_MIGRATION',
            'SYSTEM',
            'V28: Agregadas columnas cifradas para PII. Pendiente: migración de datos (V29) y eliminación de columnas en claro (V30).',
            '127.0.0.1',
            CURRENT_TIMESTAMP
        );
    END IF;
END $$;

-- ============================================================================
-- V14__create_new_schemas_and_personas.sql
-- Crea los 6 esquemas nuevos y la tabla central: registro_civil.personas
-- VERSIÓN SIMPLIFICADA: Sin DO/END blocks, solo SQL directo
-- ============================================================================

-- ============================================================================
-- 1. CREAR ESQUEMAS NUEVOS (idempotent)
-- ============================================================================
CREATE SCHEMA IF NOT EXISTS registro_civil;
CREATE SCHEMA IF NOT EXISTS staff;
CREATE SCHEMA IF NOT EXISTS denuncias;
CREATE SCHEMA IF NOT EXISTS evidencias;
CREATE SCHEMA IF NOT EXISTS logs;
CREATE SCHEMA IF NOT EXISTS reglas_derivacion;

-- ============================================================================
-- 2. CREAR TABLA CENTRAL: registro_civil.personas
-- ============================================================================
-- Esta es la verdad única de identidad en el sistema
CREATE TABLE IF NOT EXISTS registro_civil.personas (
    id_registro BIGSERIAL PRIMARY KEY,
    cedula VARCHAR(20) NOT NULL UNIQUE,
    primer_nombre VARCHAR(255),
    segundo_nombre VARCHAR(255),
    primer_apellido VARCHAR(255),
    segundo_apellido VARCHAR(255),
    sexo VARCHAR(10),
    cedula_hash VARCHAR(128) NOT NULL UNIQUE,
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT personas_cedula_not_empty CHECK (cedula IS NOT NULL AND cedula != '')
);

-- Crear índices (IF NOT EXISTS para idempotencia)
CREATE INDEX IF NOT EXISTS idx_personas_cedula_hash ON registro_civil.personas(cedula_hash);
CREATE INDEX IF NOT EXISTS idx_personas_created_at ON registro_civil.personas(created_at);

-- Comentarios
COMMENT ON TABLE registro_civil.personas IS 'Tabla central: todas las personas del sistema (denunciantes, staff, etc). Datos sensibles cifrados a nivel aplicación.';
COMMENT ON COLUMN registro_civil.personas.cedula IS 'Cédula cifrada a nivel aplicación (AES-256). Nunca leer en texto plano.';
COMMENT ON COLUMN registro_civil.personas.primer_nombre IS 'Nombre cifrado a nivel aplicación (AES-256). Nunca leer en texto plano.';
COMMENT ON COLUMN registro_civil.personas.cedula_hash IS 'Hash SHA-256 determinístico de cédula. Permite búsquedas sin descifrar.';

-- ============================================================================
-- 3. CREAR TABLA: registro_civil.didit_verification
-- Migrar desde secure_identities.didit_verification
-- ============================================================================
CREATE TABLE IF NOT EXISTS registro_civil.didit_verification (
    id BIGSERIAL PRIMARY KEY,
    id_registro BIGINT REFERENCES registro_civil.personas(id_registro) ON DELETE CASCADE,
    didit_session_id VARCHAR(255) NOT NULL UNIQUE,
    document_number VARCHAR(20) NOT NULL UNIQUE,  -- Cifrado a nivel app
    full_name VARCHAR(255),  -- Cifrado a nivel app
    first_name VARCHAR(255),  -- Cifrado a nivel app
    last_name VARCHAR(255),  -- Cifrado a nivel app
    verification_status VARCHAR(50) NOT NULL,  -- VERIFIED, FAILED, PENDING
    citizen_hash VARCHAR(128),  -- Hash determinístico
    verified_at TIMESTAMPTZ NOT NULL,
    webhook_ip VARCHAR(45),
    webhook_payload TEXT,  -- Payload de Didit para auditoría
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices (IF NOT EXISTS para idempotencia)
CREATE INDEX IF NOT EXISTS idx_didit_document_number ON registro_civil.didit_verification(document_number);
CREATE INDEX IF NOT EXISTS idx_didit_citizen_hash ON registro_civil.didit_verification(citizen_hash);
CREATE INDEX IF NOT EXISTS idx_didit_verified_at ON registro_civil.didit_verification(verified_at);
CREATE INDEX IF NOT EXISTS idx_didit_session_id ON registro_civil.didit_verification(didit_session_id);
CREATE INDEX IF NOT EXISTS idx_didit_id_registro ON registro_civil.didit_verification(id_registro);

COMMENT ON TABLE registro_civil.didit_verification IS 'Registros de verificación biométrica Didit. Enlaza usuarios verificados con personas en el sistema.';
COMMENT ON COLUMN registro_civil.didit_verification.document_number IS 'Cédula cifrada a nivel aplicación. Debe compararse solo con hashes.';

-- ============================================================================
-- 4. CREAR TABLA: staff.staff_user (Reemplaza public.staff_user)
-- ============================================================================
CREATE TABLE IF NOT EXISTS staff.staff_user (
    id BIGSERIAL PRIMARY KEY,
    id_registro BIGINT NOT NULL UNIQUE REFERENCES registro_civil.personas(id_registro) ON DELETE CASCADE,
    username VARCHAR(64) NOT NULL UNIQUE,
    cedula VARCHAR(20) NOT NULL UNIQUE,
    password_hash VARCHAR(120) NOT NULL,
    role VARCHAR(32) NOT NULL CHECK (role IN ('ADMIN', 'ANALISTA')),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    email VARCHAR(255),
    created_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- Crear índices (IF NOT EXISTS para idempotencia)
CREATE INDEX IF NOT EXISTS idx_staff_username ON staff.staff_user(username);
CREATE INDEX IF NOT EXISTS idx_staff_cedula ON staff.staff_user(cedula);
CREATE INDEX IF NOT EXISTS idx_staff_role ON staff.staff_user(role);
CREATE INDEX IF NOT EXISTS idx_staff_enabled ON staff.staff_user(enabled);
CREATE INDEX IF NOT EXISTS idx_staff_email ON staff.staff_user(email) WHERE email IS NOT NULL;
CREATE UNIQUE INDEX IF NOT EXISTS idx_staff_cedula_email ON staff.staff_user(cedula, email) WHERE enabled = true;

COMMENT ON TABLE staff.staff_user IS 'Personal interno del sistema con roles ADMIN o ANALISTA. Relacionado con registro_civil.personas.';
COMMENT ON COLUMN staff.staff_user.role IS 'Rol funcional: ADMIN (administrador total), ANALISTA (revisa denuncias)';
COMMENT ON COLUMN staff.staff_user.cedula IS 'Cédula cifrada a nivel aplicación. Campo único para validación de Didit.';

-- ============================================================================
-- 5. HABILITAR ROW LEVEL SECURITY (RLS) EN ESQUEMAS SENSIBLES
-- ============================================================================
ALTER TABLE registro_civil.personas ENABLE ROW LEVEL SECURITY;
ALTER TABLE registro_civil.didit_verification ENABLE ROW LEVEL SECURITY;
ALTER TABLE staff.staff_user ENABLE ROW LEVEL SECURITY;

-- ============================================================================
-- V14 COMPLETE: All schemas and core tables created successfully
-- ============================================================================

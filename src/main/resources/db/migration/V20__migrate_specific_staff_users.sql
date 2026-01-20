-- ============================================================================
-- V20__migrate_specific_staff_users.sql
-- Copia registros de public.staff_user a staff.staff_user
-- Solo para: sebastian.aisalla y marlon.vinueza
-- Vincula con registro_civil.personas por id_registro
-- ============================================================================

-- ============================================================================
-- PASO 1: Asegurar que staff.staff_user existe
-- (Por si V14 falló a pesar de marcarse como completa)
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

-- ============================================================================
-- PASO 2: Migrar sebastian.aisalla
-- Mapea roles: ANALYST -> ANALISTA, ADMIN -> ADMIN
-- ============================================================================
INSERT INTO staff.staff_user (
    id_registro, username, cedula, password_hash, role, enabled, email
)
SELECT
    p.id_registro,
    psu.username,
    psu.cedula,
    psu.password_hash,
    CASE 
        WHEN psu.role = 'ANALYST' THEN 'ANALISTA'
        WHEN psu.role = 'ADMIN' THEN 'ADMIN'
        ELSE 'ANALISTA'  -- Default to ANALISTA if role is unknown
    END AS role,
    psu.enabled,
    psu.email
FROM public.staff_user psu
INNER JOIN registro_civil.personas p ON p.cedula = psu.cedula
WHERE psu.username = 'sebastian.aisalla'
  AND NOT EXISTS (SELECT 1 FROM staff.staff_user WHERE username = 'sebastian.aisalla')
ON CONFLICT (username) DO UPDATE SET
    id_registro = EXCLUDED.id_registro,
    cedula = EXCLUDED.cedula,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    enabled = EXCLUDED.enabled,
    email = EXCLUDED.email,
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- PASO 3: Migrar marlon.vinueza
-- Mapea roles: ANALYST -> ANALISTA, ADMIN -> ADMIN
-- ============================================================================
INSERT INTO staff.staff_user (
    id_registro, username, cedula, password_hash, role, enabled, email
)
SELECT
    p.id_registro,
    psu.username,
    psu.cedula,
    psu.password_hash,
    CASE 
        WHEN psu.role = 'ANALYST' THEN 'ANALISTA'
        WHEN psu.role = 'ADMIN' THEN 'ADMIN'
        ELSE 'ANALISTA'  -- Default to ANALISTA if role is unknown
    END AS role,
    psu.enabled,
    psu.email
FROM public.staff_user psu
INNER JOIN registro_civil.personas p ON p.cedula = psu.cedula
WHERE psu.username = 'marlon.vinueza'
  AND NOT EXISTS (SELECT 1 FROM staff.staff_user WHERE username = 'marlon.vinueza')
ON CONFLICT (username) DO UPDATE SET
    id_registro = EXCLUDED.id_registro,
    cedula = EXCLUDED.cedula,
    password_hash = EXCLUDED.password_hash,
    role = EXCLUDED.role,
    enabled = EXCLUDED.enabled,
    email = EXCLUDED.email,
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- VALIDACIÓN FINAL
-- ============================================================================
SELECT username, role, enabled, email FROM staff.staff_user 
WHERE username IN ('sebastian.aisalla', 'marlon.vinueza')
ORDER BY username;

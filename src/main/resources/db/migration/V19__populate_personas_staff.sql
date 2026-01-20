-- ============================================================================
-- V19__populate_personas_staff.sql
-- Agrega/actualiza registros de personas en registro_civil.personas
-- ============================================================================

-- ============================================================================
-- PASO 1: Mario Sebastián Aisalla Pozo (cedula: 1726383514)
-- ============================================================================
INSERT INTO registro_civil.personas (
    cedula, cedula_hash, primer_nombre, segundo_nombre, 
    primer_apellido, segundo_apellido, sexo
)
VALUES (
    '1726383514',
    encode(digest('1726383514', 'sha256'), 'hex'),
    'Mario',
    'Sebastián',
    'Aisalla',
    'Pozo',
    'masculino'
)
ON CONFLICT (cedula_hash) DO UPDATE SET
    primer_nombre = 'Mario',
    segundo_nombre = 'Sebastián',
    primer_apellido = 'Aisalla',
    segundo_apellido = 'Pozo',
    sexo = 'masculino',
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- PASO 2: Marlon Andrés Vinueza Ramos (cedula: 1753848637)
-- ============================================================================
INSERT INTO registro_civil.personas (
    cedula, cedula_hash, primer_nombre, segundo_nombre,
    primer_apellido, segundo_apellido, sexo
)
VALUES (
    '1753848637',
    encode(digest('1753848637', 'sha256'), 'hex'),
    'Marlon',
    'Andrés',
    'Vinueza',
    'Ramos',
    'masculino'
)
ON CONFLICT (cedula_hash) DO UPDATE SET
    primer_nombre = 'Marlon',
    segundo_nombre = 'Andrés',
    primer_apellido = 'Vinueza',
    segundo_apellido = 'Ramos',
    sexo = 'masculino',
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- PASO 3: Francis Belén Velastegui Armas (cedula: 1754644415)
-- ============================================================================
INSERT INTO registro_civil.personas (
    cedula, cedula_hash, primer_nombre, segundo_nombre,
    primer_apellido, segundo_apellido, sexo
)
VALUES (
    '1754644415',
    encode(digest('1754644415', 'sha256'), 'hex'),
    'Francis',
    'Belén',
    'Velastegui',
    'Armas',
    'femenino'
)
ON CONFLICT (cedula_hash) DO UPDATE SET
    primer_nombre = 'Francis',
    segundo_nombre = 'Belén',
    primer_apellido = 'Velastegui',
    segundo_apellido = 'Armas',
    sexo = 'femenino',
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- PASO 4: Roberto Jhoel Narváez Sillo (cedula: 1750941013)
-- ============================================================================
INSERT INTO registro_civil.personas (
    cedula, cedula_hash, primer_nombre, segundo_nombre,
    primer_apellido, segundo_apellido, sexo
)
VALUES (
    '1750941013',
    encode(digest('1750941013', 'sha256'), 'hex'),
    'Roberto',
    'Jhoel',
    'Narváez',
    'Sillo',
    'masculino'
)
ON CONFLICT (cedula_hash) DO UPDATE SET
    primer_nombre = 'Roberto',
    segundo_nombre = 'Jhoel',
    primer_apellido = 'Narváez',
    segundo_apellido = 'Sillo',
    sexo = 'masculino',
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- PASO 5: Stalin Anthony Yungan Sinche (cedula: 1728848274)
-- ============================================================================
INSERT INTO registro_civil.personas (
    cedula, cedula_hash, primer_nombre, segundo_nombre,
    primer_apellido, segundo_apellido, sexo
)
VALUES (
    '1728848274',
    encode(digest('1728848274', 'sha256'), 'hex'),
    'Stalin',
    'Anthony',
    'Yungan',
    'Sinche',
    'masculino'
)
ON CONFLICT (cedula_hash) DO UPDATE SET
    primer_nombre = 'Stalin',
    segundo_nombre = 'Anthony',
    primer_apellido = 'Yungan',
    segundo_apellido = 'Sinche',
    sexo = 'masculino',
    updated_at = CURRENT_TIMESTAMP;

-- ============================================================================
-- VALIDACIÓN FINAL
-- ============================================================================
SELECT COUNT(*) as total_personas FROM registro_civil.personas;
SELECT cedula, primer_nombre, primer_apellido, sexo FROM registro_civil.personas ORDER BY primer_apellido;

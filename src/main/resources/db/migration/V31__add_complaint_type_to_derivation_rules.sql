-- ============================================================================
-- V31__add_complaint_type_to_derivation_rules.sql
-- Agrega columna complaint_type_match a regla_derivacion para derivación completa
-- ============================================================================

-- Agregar columna complaint_type_match (puede ser NULL = cualquier tipo)
ALTER TABLE reglas_derivacion.regla_derivacion
ADD COLUMN IF NOT EXISTS complaint_type_match VARCHAR(64);

-- Índice para búsquedas por tipo de denuncia
CREATE INDEX IF NOT EXISTS idx_regla_derivacion_complaint_type
ON reglas_derivacion.regla_derivacion(complaint_type_match);

-- Comentario explicativo
COMMENT ON COLUMN reglas_derivacion.regla_derivacion.complaint_type_match IS
'Tipo de denuncia a coincidir: LABOR_RIGHTS, HARASSMENT, DISCRIMINATION, SAFETY, FRAUD, OTHER.
NULL = cualquier tipo (wildcard). Se combina con severity_match para derivación precisa.';

-- Ejemplo de regla completa:
-- severity_match=HIGH + complaint_type_match=HARASSMENT → Ministerio de Trabajo
-- severity_match=CRITICAL + complaint_type_match=FRAUD → Fiscalía

-- ============================================================================
-- V31 COMPLETE: Columna complaint_type_match agregada exitosamente
-- ============================================================================

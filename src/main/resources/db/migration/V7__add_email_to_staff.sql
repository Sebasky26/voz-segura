-- =============================================================================
-- V7__add_email_to_staff.sql
-- Agregar columna de email para MFA con AWS SES
-- =============================================================================

-- Agregar columna de email a la tabla staff_user para MFA con AWS SES
ALTER TABLE staff_user ADD COLUMN IF NOT EXISTS email VARCHAR(255);

-- Agregar índice único para evitar duplicados
CREATE UNIQUE INDEX IF NOT EXISTS idx_staff_user_email ON staff_user(email) WHERE email IS NOT NULL;

-- Actualizar los emails de los usuarios existentes
UPDATE staff_user SET email = 'stalin.yungan@epn.edu.ec' WHERE username = 'stalin.yungan';
UPDATE staff_user SET email = 'mario.aisalla@epn.edu.ec' WHERE username = 'sebastian.aisalla';
UPDATE staff_user SET email = 'francis.velastegui@epn.edu.ec' WHERE username = 'francis.velastegui';
UPDATE staff_user SET email = 'marlon.vinueza@epn.edu.ec' WHERE username = 'marlon.vinueza';

-- Comentario de seguridad
COMMENT ON COLUMN staff_user.email IS 'Email verificado en AWS SES para MFA - Solo staff autorizado';

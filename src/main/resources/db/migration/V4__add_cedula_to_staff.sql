-- Agregar columna cedula a staff_user
ALTER TABLE staff_user ADD COLUMN cedula VARCHAR(20) UNIQUE;

-- Actualizar usuarios existentes con cédulas de ejemplo
UPDATE staff_user SET cedula = '1234567890' WHERE username = 'admin';
UPDATE staff_user SET cedula = '0987654321' WHERE username = 'analista';

-- Hacer la columna NOT NULL después de poblarla
ALTER TABLE staff_user ALTER COLUMN cedula SET NOT NULL;


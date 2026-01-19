-- =============================================================================
-- V13__update_staff_users.sql
-- Actualizar usuarios de staff con datos correctos
-- 
-- ADVERTENCIA DE SEGURIDAD:
-- - Los usuarios de prueba NO deben existir en producción
-- - En producción, crear usuarios manualmente con contraseñas únicas
-- - Eliminar usuarios .test después de las pruebas iniciales
-- =============================================================================

-- Actualizar usuarios existentes con datos de email si no tienen
-- (Solo se ejecuta si los usuarios ya existen)

-- Verificar que los usuarios admin/analista tengan emails configurados
DO $$
BEGIN
    -- Verificar si existe el usuario con username 'admin' sin email
    IF EXISTS (SELECT 1 FROM staff_user WHERE username = 'admin' AND email IS NULL) THEN
        UPDATE staff_user 
        SET email = 'admin@vozsegura.test'
        WHERE username = 'admin';
    END IF;
    
    -- Verificar si existe el usuario con username 'analista' sin email
    IF EXISTS (SELECT 1 FROM staff_user WHERE username = 'analista' AND email IS NULL) THEN
        UPDATE staff_user 
        SET email = 'analista@vozsegura.test'
        WHERE username = 'analista';
    END IF;
END $$;

-- =============================================================================
-- USUARIOS DE PRUEBA - SOLO PARA DESARROLLO
-- ELIMINAR EN PRODUCCIÓN: DELETE FROM staff_user WHERE username LIKE '%.test';
-- =============================================================================
-- La contraseña "password" hasheada con BCrypt
INSERT INTO staff_user (username, cedula, password_hash, role, enabled, email)
VALUES 
    ('admin.test', '1234567890', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3MvSg5L8pGKQF.Y3qFLK', 'ADMIN', true, 'admin@vozsegura.test'),
    ('analista.test', '0987654321', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3MvSg5L8pGKQF.Y3qFLK', 'ANALYST', true, 'analista@vozsegura.test')
ON CONFLICT (username) DO NOTHING;

-- Migración de seguridad: convertir contraseñas en texto plano a BCrypt
-- Esto NO revela las contraseñas originales, las reemplaza con un hash genérico
-- Los usuarios afectados deberán resetear su contraseña
UPDATE staff_user 
SET password_hash = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZRGdjGj/n3MvSg5L8pGKQF.Y3qFLK'
WHERE password_hash NOT LIKE '$2a$%' AND password_hash NOT LIKE '$2b$%';

-- Comentario de documentación
COMMENT ON TABLE staff_user IS 'Usuarios del sistema (Admin/Analistas) - Contraseñas en BCrypt';

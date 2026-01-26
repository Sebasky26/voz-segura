package com.vozsegura.client;

/**
 * Interface para cliente de AWS Secrets Manager.
 * 
 * Responsabilidades:
 * - Obtener secretos desde AWS Secrets Manager (configuración remota)
 * - Manejar rotación de credenciales (zero-downtime)
 * - Cache local de secretos para evitar latencia
 * - Validar integridad de secretos
 * 
 * Filosofía Zero Trust:
 * - NUNCA almacenar secretos en archivos (application.yml, .env)
 * - NUNCA enviar secretos en variables de entorno
 * - SIEMPRE obtener de Secrets Manager en runtime
 * - SIEMPRE encriptar en tránsito (KMS encryption by AWS)
 * 
 * Flujo de uso (en AwsDatabaseConfig):
 * 
 * 1. En startup (Spring Bean initialization):
 *    secretsClient.getSecretString("rds/database") → JSON con DB credentials
 * 
 * 2. Parsing del JSON:
 *    {
 *      "DB_USERNAME": "admin",
 *      "DB_PASSWORD": "xxxxxx",
 *      "DB_HOST": "db.xxx.rds.amazonaws.com",
 *      "DB_PORT": "5432"
 *    }
 * 
 * 3. Construcción de DataSource:
 *    Usar credenciales obtenidas para crear HikariCP pool
 * 
 * Secrets configurados en AWS:
 * 
 * - rds/database: Credenciales de base de datos
 * - rds/replica: Credenciales para replica (read-only)
 * - didit/api-key: Clave API para Didit v3 (biometría)
 * - registro-civil/api-key: Clave para Registro Civil
 * - otp/twilio: Credenciales de Twilio (SMS)
 * - email/sendgrid: Credenciales de SendGrid (Email)
 * - supabase/keys: Claves anónima y service-role
 * 
 * Rotación de secretos:
 * - AWS Secrets Manager permite rotación automática
 * - RDS database credentials: Rotación sin downtime (standby replica)
 * - API Keys: Rotación manual recomendada cada 90 días
 * - Certificados: Rotación automática 30 días antes de expiración
 * 
 * Fallos y recuperación:
 * - Si Secrets Manager no responde: Usar cache local (últimas 2 horas)
 * - Si cache expirado: Fallar con RuntimeException (requiere intervención)
 * - Si JSON malformado: RuntimeException (error de configuración)
 * - Si secret no existe: RuntimeException (404 del servicio)
 * 
 * Implementaciones:
 * - AWS Secrets Manager Client (prod): Integración real con AWS API
 * - Mock Client (dev): Retorna hardcoded secrets para desarrollo
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * 
 * @see AwsDatabaseConfig - Usa este cliente para obtener credenciales de BD
 * @see RestClientConfig - Usa para obtener API Keys de Didit, Registro Civil, OTP
 */
public interface SecretsManagerClient {

    /**
     * Obtiene un secreto desde AWS Secrets Manager.
     * 
     * Flujo:
     * 1. Construir request HTTPS GET a AWS Secrets Manager
     * 2. Incluir autenticación (IAM role del servidor)
     * 3. Especificar nombre del secreto (ej: "rds/database")
     * 4. AWS valida permisos (policy: secretsmanager:GetSecretValue)
     * 5. AWS desencripta con KMS key
     * 6. Retornar valor del secreto (String de JSON o texto)
     * 7. Cachear localmente por 2 horas
     * 
     * Cache:
     * - Tiempo de vida: 2 horas (TTL)
     * - Razón: Evitar latencia de queries a AWS (100ms+ por request)
     * - Rotación: Si secreto cambia en AWS, tarda máximo 2h en sincronizar
     * - Fallback: Si AWS no responde, usar cache (aunque esté expirado)
     * 
     * Seguridad:
     * - Conexión encriptada con TLS 1.2+
     * - Autenticación con IAM role (no API keys)
     * - KMS encryption-at-rest en AWS
     * - Auditoría: CloudTrail registra todos los accesos
     * - Logs: NO registrar el valor del secreto (solo que fue accedido)
     * 
     * @param secretName Nombre del secreto en AWS Secrets Manager
     *                   Formato: "ruta/del/secreto"
     *                   Ejemplos: "rds/database", "didit/api-key", "email/sendgrid"
     * 
     * @return Valor del secreto (puede ser JSON, API key, contraseña, etc.)
     *         Nunca nulo (si no existe, lanza RuntimeException)
     * 
     * @throws RuntimeException si:
     *         - Secreto no existe (404)
     *         - Permisos insuficientes (403)
     *         - AWS Secrets Manager no responde (timeout)
     *         - Desencriptación falla (KMS error)
     *         - Cache expirado Y AWS no disponible
     * 
     * @see AwsDatabaseConfig#dataSource() - Llamada para obtener DB credentials
     */
    String getSecretString(String secretName);
}

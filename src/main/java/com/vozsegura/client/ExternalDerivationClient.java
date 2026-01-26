package com.vozsegura.client;

/**
 * Interface para cliente de derivación de casos a entidades externas.
 * 
 * Responsabilidades:
 * - Derivar casos (denuncias) a entidades gubernamentales u organizaciones externas
 * - Encriptar datos del caso antes de enviar (Zero Trust - nunca enviar en claro)
 * - Validar entrega a entidad receptora
 * - Manejar reintentos y fallos de conexión
 * 
 * Flujo de uso (en DerivationService):
 * 
 * 1. Routing automático (DerivationService.derivarComplaint):
 *    - Analizar denuncia: severidad, prioridad, entidad responsable
 *    - Buscar DestinationEntity que cumpla criterios
 *    - Obtener URL/endpoint de entidad receptora
 * 
 * 2. Encriptación:
 *    - Cifrar datos del caso (encryptedPayload)
 *    - Solo incluir información esencial (no datos personales si es ciudadano anónimo)
 * 
 * 3. Envío:
 *    externalClient.derivateCase(trackingId, "https://entity.gov.ec/api/cases", encryptedPayload)
 * 
 * 4. Confirmación:
 *    - Si true: Actualizar estado de denuncia a ASSIGNED
 *    - Si false: Reintentar, enviar alerta a administrador
 * 
 * Seguridad Zero Trust:
 * - NUNCA enviar trackingId en claro (es único por denunciante)
 * - NUNCA enviar identidad del ciudadano (encryptedPayload la oculta)
 * - SIEMPRE encriptar con clave de la entidad receptora
 * - SIEMPRE validar certificado SSL/TLS de destino
 * - SIEMPRE usar HTTPS (no HTTP)
 * 
 * Entidades externas soportadas:
 * - CPCCS: Corte Penal y de Crímenes (delitos graves)
 * - CPC: Comisión Permanente de Control (corrupción)
 * - CIDH: Comisión Interamericana (derechos humanos)
 * - Defensoría: Defensoría Pública (asistencia legal)
 * - Fiscalía: Fiscal General del Estado (investigación)
 * 
 * Implementaciones:
 * - REST Client (prod): HTTP POST a endpoint de entidad
 * - Mock Client (dev): Simula éxito/fallo de derivación
 * 
 * @author Voz Segura Team
 * @since 2026-01
 * 
 * @see DerivationService - Orquesta la derivación de casos
 * @see DerivationRule - Define reglas de routing a entidades
 * @see DestinationEntity - Almacena configuración de entidades receptoras
 */
public interface ExternalDerivationClient {

    /**
     * Deriva un caso a una entidad externa.
     * 
     * Proceso:
     * 1. Construir petición HTTPS (POST)
     * 2. Agregar headers de autenticación (API Key de entidad)
     * 3. Incluir encryptedPayload (datos cifrados del caso)
     * 4. Enviar a URL de destino
     * 5. Esperar confirmación (200 OK + response body)
     * 6. Retornar éxito/fallo
     * 
     * Reintentos:
     * - Si timeout (> 30s): Reintentar 3 veces con backoff exponencial
     * - Si 5xx server error: Reintentar
     * - Si 4xx client error: Fallo definitivo (configuración incorrecta)
     * - Si network error: Encolar para reintento posterior
     * 
     * Logs:
     * - Registrar intento de derivación (antes de enviar)
     * - Registrar respuesta de entidad (después de recibir)
     * - Registrar error si falla
     * - NO registrar encryptedPayload (datos sensibles)
     * 
     * @param caseId ID de seguimiento del caso (trackingId)
     *               Usado solo como referencia en entidad receptora
     *               Nunca se expone al ciudadano
     *               Formato: UUID de 36 caracteres
     * 
     * @param destination URL de la entidad receptora (ej: "https://entity.gov.ec/api/cases")
     *                    DEBE ser HTTPS (no HTTP)
     *                    DEBE tener certificado SSL válido
     *                    Timeout: 30 segundos
     * 
     * @param encryptedPayload Datos del caso cifrados
     *                         Formato: Base64-encoded AES-256-GCM
     *                         Contiene: resumen, evidencias, clasificación
     *                         NO contiene: identidad del ciudadano
     * 
     * @return true si derivación fue exitosa (200 OK de entidad)
     *         false si fallo en envío/confirmación (timeout, error 5xx, etc.)
     * 
     * @throws IllegalArgumentException si URL no es válida o no es HTTPS
     * @throws RuntimeException si error no recoverable (ej: corrupción de payload)
     */
    boolean derivateCase(String caseId, String destination, String encryptedPayload);
}

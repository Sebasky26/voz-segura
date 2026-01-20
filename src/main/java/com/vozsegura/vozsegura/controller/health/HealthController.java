package com.vozsegura.vozsegura.controller.health;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para verificar configuracion y salud del sistema.
 * 
 * Proposito:
 * - Verificar que las configuraciones de Didit sean correctas
 * - Diagnosticar problemas de integracion
 * - Exponer informacion de configuracion (con sanitizacion para seguridad)
 * 
 * Rutas:
 * - GET /health/config -> Configuracion sanitizada de Didit
 * - GET /health/didit-debug -> Configuracion COMPLETA (SOLO DESARROLLO)
 * 
 * Seguridad:
 * - /config: Sanitiza las claves (primeras 10 caracteres + "...")
 * - /didit-debug: EXPONE SECRETOS COMPLETOS (SOLO para desarrollo local)
 *   NUNCA debe estar accesible en produccion
 * 
 * Nota:
 * - Ruta publica (no requiere autenticacion)
 * - Usado por admins para validar integracion con Didit
 * 
 * @see com.vozsegura.vozsegura.service.DiditService
 */
@Slf4j
@RestController
@RequestMapping("/health")
public class HealthController {

    /**
     * ID de aplicacion de Didit (inyectado desde application.yml).
     * Definido en: didit.app-id
     * Default: "NOT_SET" si no esta configurado
     */
    @Value("${didit.app-id:NOT_SET}")
    private String diditAppId;

    /**
     * Clave API de Didit (inyectado desde application.yml).
     * Definido en: didit.api-key
     * Default: "NOT_SET" si no esta configurado
     * SENSIBLE: Nunca exponer completo en logs o respuestas
     */
    @Value("${didit.api-key:NOT_SET}")
    private String diditApiKey;

    /**
     * ID de workflow en Didit (inyectado desde application.yml).
     * Definido en: didit.workflow-id
     * Default: "NOT_SET" si no esta configurado
     */
    @Value("${didit.workflow-id:NOT_SET}")
    private String diditWorkflowId;

    /**
     * URL base de API de Didit (inyectado desde application.yml).
     * Definido en: didit.api-url
     * Default: "NOT_SET" si no esta configurado
     * Ejemplo: https://api.didit.io/
     */
    @Value("${didit.api-url:NOT_SET}")
    private String diditApiUrl;

    /**
     * URL de webhook que Didit llamara (inyectado desde application.yml).
     * Definido en: didit.webhook-url
     * Default: "NOT_SET" si no esta configurado
     * Ejemplo: https://voz-segura.ec/webhooks/didit
     */
    @Value("${didit.webhook-url:NOT_SET}")
    private String diditWebhookUrl;

    /**
     * Obtiene la configuracion de Didit de forma SANITIZADA.
     * 
     * Proposito:
     * - Verificar que Didit esta configurado
     * - Diagnosticar problemas sin exponer secretos
     * - Ruta: GET /health/config
     * 
     * Seguridad:
     * - appId: Solo primeros 10 caracteres + "..." (No expone completo)
     * - apiKeySet: boolean (true si esta set, false si no) - NUNCA la clave
     * - workflowId: Solo primeros 10 caracteres + "..."
     * - apiUrl: URL publica (OK exponer)
     * - webhookUrl: URL publica (OK exponer)
     * 
     * @return Map con status y configuracion sanitizada
     * 
     * Ejemplo de respuesta:
     * {
     *   "didit": {
     *     "appId": "app_1234...",
     *     "apiKeySet": true,
     *     "workflowId": "wf_5678...",
     *     "apiUrl": "https://api.didit.io/",
     *     "webhookUrl": "https://voz-segura.ec/webhooks/didit"
     *   },
     *   "status": "OK"
     * }
     */
    @GetMapping("/config")
    public Map<String, Object> getConfig() {
        Map<String, Object> config = new HashMap<>();
        
        config.put("didit", Map.of(
            "appId", diditAppId.substring(0, Math.min(10, diditAppId.length())) + "...",
            "apiKeySet", !diditApiKey.equals("NOT_SET"),
            "workflowId", diditWorkflowId.substring(0, Math.min(10, diditWorkflowId.length())) + "...",
            "apiUrl", diditApiUrl,
            "webhookUrl", diditWebhookUrl
        ));
        
        config.put("status", "OK");
        return config;
    }

    /**
     * Obtiene la configuracion de Didit COMPLETA (incluyendo secretos).
     * 
     * ADVERTENCIA:
     * - Esta ruta EXPONE la clave API de Didit COMPLETA
     * - Solo debe estar accesible en desarrollo local
     * - En produccion, debe estar protegida o removida
     * - NO debe exponerse en internet publico
     * 
     * Proposito:
     * - Diagnosticar problemas de integracion
     * - Verificar que secretos estan correctamente configurados
     * - Solo para administradores de desarrollo
     * 
     * Ruta: GET /health/didit-debug
     * 
     * Seguridad:
     * - NUNCA exponer en endpoints publicos
     * - Considerar agregar @Secured(\"ROLE_ADMIN\")
     * - Considerar agregar IP whitelist (solo localhost)
     * 
     * @return Map con configuracion COMPLETA (incluyendo clave API)
     */
    @GetMapping("/didit-debug")
    public Map<String, Object> diditDebug() {
        Map<String, Object> debug = new HashMap<>();
        debug.put("didit_app_id", diditAppId);
        debug.put("didit_api_key_set", !diditApiKey.equals("NOT_SET"));
        debug.put("didit_workflow_id", diditWorkflowId);
        debug.put("didit_api_url", diditApiUrl);
        debug.put("didit_webhook_url", diditWebhookUrl);
        
        log.info("Didit Config Debug: {}", debug);
        
        return debug;
    }
}

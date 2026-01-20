package com.vozsegura.vozsegura.controller.webhook;

import com.vozsegura.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.vozsegura.service.DiditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.view.RedirectView;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador para recibir webhooks de Didit (verificacion biometrica).
 * 
 * Proposito:
 * - Recibir resultado de verificacion biometrica desde Didit
 * - Dos flujos: GET (callback usuario) y POST (webhook server-to-server)
 * - Actualizar estado de verificacion en BD
 * - Redirigir usuario a pagina apropiada
 * 
 * Rutas:
 * 1. GET /webhooks/didit?verificationSessionId=...&status=...
 *    - Didit redirige al usuario despues de verificacion
 *    - Usuario ve resultado (aprobado/rechazado)
 *    - Redirecciona a /auth/verify-callback
 * 
 * 2. POST /webhooks/didit (Content-Type: application/json)
 *    - Didit envia resultado a nivel servidor (server-to-server)
 *    - No es un callback de usuario, es un webhook del servidor
 *    - Firma HMAC-SHA256 para validacion
 * 
 * Flujo Didit:
 * 1. Usuario inicia verificacion (CitizenVerificationService)
 * 2. Didit crea sesion y redirige a su app (selfie/liveness)
 * 3. Usuario completa verificacion
 * 4. Didit envia GET a /webhooks/didit?verificationSessionId=...&status=...
 * 5. DiditWebhookController.handleDiditCallbackGet() es llamado
 * 6. Tambien Didit envia POST a /webhooks/didit (server-to-server)
 * 7. DiditWebhookController.handleDiditWebhookPost() es llamado
 * 8. Resultado se almacena en BD (DiditVerification)
 * 
 * Seguridad:
 * - Validar firma HMAC-SHA256 (Didit envia firma en headers)
 * - Validar que IP sea de Didit (si es posible)
 * - Almacenar resultado pero NUNCA fotos/videos
 * - Registrar todo en AuditEvent
 * 
 * @see com.vozsegura.vozsegura.service.DiditService
 */
@Slf4j
@Controller
@RequestMapping("/webhooks")
public class DiditWebhookController {

    /**
     * Servicio de Didit para procesar webhooks.
     * Maneja la logica de validacion y almacenamiento.
     */
    private final DiditService diditService;

    /**
     * Inyecta DiditService para procesar resultados.
     * @param diditService Servicio de Didit
     */
    public DiditWebhookController(DiditService diditService) {
        this.diditService = diditService;
    }

    /**
     * GET endpoint - Callback cuando usuario completa verificacion en Didit.
     * 
     * Proposito:
     * - Recibir redireccion de usuario despues de verificacion biometrica
     * - Almacenar sessionId en sesion HTTP
     * - Redirigir a pagina de confirmacion
     * 
     * URL: GET /webhooks/didit?verificationSessionId=abc123&status=Approved
     * 
     * Parametros:
     * - verificationSessionId: ID unico de sesion de Didit (ej: "sess_abc123xyz")
     * - status: Resultado (Approved, Rejected, Incomplete, Expired, etc.)
     * 
     * Flujo:
     * 1. Usuario completa verificacion en app de Didit
     * 2. Didit redirige a GET /webhooks/didit?verificationSessionId=...&status=...
     * 3. Navegador del usuario hace GET a esta ruta
     * 4. Guardamos verificationSessionId en sesion HTTP
     * 5. Redirigimos a /auth/verify-callback para confirmacion
     * 
     * @param verificationSessionId ID de sesion de Didit (opcional)
     * @param status Resultado de verificacion (opcional)
     * @param session Sesion HTTP para almacenar datos
     * @return RedirectView a /auth/verify-callback
     * 
     * Notas:
     * - Los parametros pueden ser nulos si Didit no los envia
     * - Se guarda en sesion para que /auth/verify-callback pueda consultar
     * - La validacion real se hace en DiditService (comparar con BD)
     */
    @GetMapping("/didit")
    public RedirectView handleDiditCallbackGet(
            @RequestParam(name = "verificationSessionId", required = false) String verificationSessionId,
            @RequestParam(name = "status", required = false) String status,
            HttpSession session) {
        log.info("Didit GET callback received. Session ID: {}, Status: {}", verificationSessionId, status);
        
        // Si viene el verificationSessionId en parametro, guardarlo en la sesion
        if (verificationSessionId != null && !verificationSessionId.isEmpty()) {
            session.setAttribute("diditSessionId", verificationSessionId);
            session.setAttribute("diditStatus", status); // Tambien guardar el status recibido
            log.info("📱 Stored diditSessionId: {} and status: {} in session", verificationSessionId, status);
        }
        
        // Redirigir a /auth/verify-callback pasando el sessionId como parametro
        // El callback consultara Didit o la BD para obtener los datos
        RedirectView redirectView = new RedirectView("/auth/verify-callback?session_id=" + 
                (verificationSessionId != null ? verificationSessionId : ""));
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    /**
     * POST endpoint - Webhook desde Didit con datos de verificacion (server-to-server).
     * 
     * Proposito:
     * - Recibir resultado de verificacion desde servidor de Didit
     * - Procesar payload JSON del webhook
     * - Almacenar resultado en BD (DiditVerification)
     * - Registrar en auditoria
     * 
     * Ruta: POST /webhooks/didit
     * Content-Type: application/json
     * 
     * Body (ejemplo):
     * {
     *   "verificationSessionId": "sess_abc123xyz",
     *   "status": "Approved",
     *   "decision": {
     *     "idVerification": {
     *       "documentNumber": "1234567890",
     *       "firstName": "Juan",
     *       "lastName": "Perez",
     *       "fullName": "Juan Perez"
     *     }
     *   }
     * }
     * 
     * Flujo:
     * 1. Didit envia POST con resultado de verificacion
     * 2. Leemos body del request (JSON)
     * 3. Procesamos con DiditService.processWebhookPayload()
     * 4. Se valida firma HMAC-SHA256
     * 5. Se almacena en BD si es valido
     * 6. Se retorna 200 OK
     * 
     * Seguridad:
     * - Validar firma HMAC en DiditService
     * - Validar que IP sea de Didit (si es posible)
     * - No exponer detalles en respuesta
     * 
     * @param request Request HTTP (para leer body y headers)
     * @return ResponseEntity con estado
     * 
     * Status de respuesta:
     * - 200 OK: Webhook procesado correctamente
     * - 400 BAD_REQUEST: Error leyendo body
     * - 500 INTERNAL_SERVER_ERROR: Error procesando
     */
    @PostMapping("/didit")
    public ResponseEntity<?> handleDiditWebhookPost(HttpServletRequest request) {
        try {
            log.info("📨 Didit webhook POST received at /webhooks/didit");
            
            // Leer el body del request
            StringBuilder bodyBuilder = new StringBuilder();
            try (BufferedReader reader = request.getReader()) {
                String line;
                while ((line = reader.readLine()) != null) {
                    bodyBuilder.append(line);
                }
            } catch (IOException e) {
                log.error("Error reading webhook body: {}", e.getMessage());
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error reading request body");
            }
            
            String webhookBody = bodyBuilder.toString();
            log.info("Webhook body received (length: {} chars): {}", webhookBody.length(), webhookBody);
            
            // Obtener la direccion IP del cliente
            String clientIpAddress = getClientIpAddress(request);
            log.info("📍 Webhook from IP: {}", clientIpAddress);
            
            // Procesar el webhook payload
            DiditVerification saved = diditService.processWebhookPayload(webhookBody, clientIpAddress);
            if (saved != null) {
                log.info("Webhook processed successfully. Saved verification with id_registro: {}, sessionId: {}", 
                        saved.getIdRegistro(), saved.getDiditSessionId());
            } else {
                log.warn("⚠️ Webhook processed but no verification saved (possibly status != 'Approved')");
            }
            
            // Retornar 200 OK para confirmar recepcion
            Map<String, String> response = new HashMap<>();
            response.put("status", "received");
            response.put("message", "Webhook processed successfully");
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            log.error("Error processing Didit webhook: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing webhook: " + e.getMessage());
        }
    }

    /**
     * Obtiene la direccion IP del cliente desde el request.
     * 
     * Proposito:
     * - Extraer IP real del cliente (considerando proxies)
     * - Registrar en auditoria para rastrabilidad
     * - Validar que webhook venga de Didit (si tenemos IP whitelist)
     * 
     * Algoritmo (intenta en orden):
     * 1. Header X-Forwarded-For (proxy, take first IP if comma-separated)
     * 2. Header X-Real-IP (nginx proxy)
     * 3. request.getRemoteAddr() (IP directa del cliente)
     * 
     * Por que multiples headers?
     * - X-Forwarded-For: Si hay cadena de proxies (AWS ALB, Cloudflare, etc.)
     * - X-Real-IP: Si hay un unico nginx proxy
     * - RemoteAddr: Si es conexion directa (desarrollo local)
     * 
     * @param request Request HTTP
     * @return String: IP del cliente (ej: "192.168.1.100")
     * 
     * Ejemplo:
     * - Si X-Forwarded-For: "203.0.113.1, 203.0.113.2"
     * - Retorna: "203.0.113.1" (primera IP, el cliente real)
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}

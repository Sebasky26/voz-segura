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
 * Controlador para recibir webhooks de Didit.
 * 
 * POST /webhooks/didit - Webhook desde Didit
 * GET /webhooks/didit - Redirección del usuario después de verificar
 */
@Slf4j
@Controller
@RequestMapping("/webhooks")
public class DiditWebhookController {

    private final DiditService diditService;

    public DiditWebhookController(DiditService diditService) {
        this.diditService = diditService;
    }

    /**
     * GET endpoint - Didit redirige al usuario aquí después de la verificación.
     * Llamamos a Didit para obtener el resultado en tiempo real.
     * 
     * GET /webhooks/didit?verificationSessionId=...&status=Approved
     */
    @GetMapping("/didit")
    public RedirectView handleDiditCallbackGet(
            @RequestParam(name = "verificationSessionId", required = false) String verificationSessionId,
            @RequestParam(name = "status", required = false) String status,
            HttpSession session) {
        log.info("Didit GET callback received. Session ID: {}, Status: {}", verificationSessionId, status);
        
        // Si viene el verificationSessionId en parámetro, guardarlo en la sesión
        if (verificationSessionId != null && !verificationSessionId.isEmpty()) {
            session.setAttribute("diditSessionId", verificationSessionId);
            session.setAttribute("diditStatus", status); // También guardar el status recibido
            log.info("📱 Stored diditSessionId: {} and status: {} in session", verificationSessionId, status);
        }
        
        // Redirigir a /auth/verify-callback pasando el sessionId como parámetro
        // El callback consultará Didit o la BD para obtener los datos
        RedirectView redirectView = new RedirectView("/auth/verify-callback?session_id=" + 
                (verificationSessionId != null ? verificationSessionId : ""));
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    /**
     * POST endpoint - Webhook desde Didit con los datos de verificación
     * Didit enviará los datos de verificación en el cuerpo del POST
     * 
     * POST /webhooks/didit
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
            
            // Obtener la dirección IP del cliente
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
            
            // Retornar 200 OK para confirmar recepción
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
     * Obtiene la dirección IP del cliente desde el request
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

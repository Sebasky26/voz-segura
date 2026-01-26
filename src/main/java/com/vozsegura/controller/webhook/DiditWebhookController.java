package com.vozsegura.controller.webhook;

import com.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.service.DiditService;
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
 * Recibe callbacks y webhooks de Didit.
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>El GET solo sirve para redirigir al usuario.</li>
 *   <li>El POST es el único que “confirma” el resultado y debe validarse (firma/secret) en DiditService.</li>
 *   <li>No se registran en logs cuerpos de webhook ni datos personales.</li>
 * </ul>
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
     * Callback de navegador. No confía en el status recibido por URL.
     * Solo guarda el sessionId para que el flujo continúe.
     */
    @GetMapping("/didit")
    public RedirectView handleDiditCallbackGet(
            @RequestParam(name = "verificationSessionId", required = false) String verificationSessionId,
            @RequestParam(name = "status", required = false) String status,
            HttpSession session) {

        if (verificationSessionId != null && !verificationSessionId.isBlank()) {
            session.setAttribute("diditSessionId", verificationSessionId);

            // No guardar status como fuente de verdad; solo para mostrar algo si quieres.
            session.setAttribute("diditStatusHint", status);
            log.info("Callback received, session stored");
        } else {
            log.warn("Callback received without session ID");
        }

        RedirectView redirectView = new RedirectView("/auth/verify-callback?session_id=" +
                (verificationSessionId != null ? verificationSessionId : ""));
        redirectView.setExposeModelAttributes(false);
        return redirectView;
    }

    /**
     * Webhook server-to-server. El cuerpo puede traer PII.
     * No se loguea el body. La validación de firma/secret debe estar en DiditService.
     */
    @PostMapping("/didit")
    public ResponseEntity<?> handleDiditWebhookPost(HttpServletRequest request) {

        try {
            String webhookBody = readBody(request);

            String clientIpAddress = getClientIpAddress(request);
            log.info("Didit webhook received. bodyLength={} ip={}", webhookBody.length(), clientIpAddress);

            DiditVerification saved = diditService.processWebhookPayload(webhookBody, clientIpAddress);

            Map<String, String> response = new HashMap<>();
            response.put("status", "received");
            response.put("message", "Webhook processed");
            response.put("saved", saved != null ? "true" : "false");

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            log.error("Error reading webhook body", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error reading request body");
        } catch (Exception e) {
            log.error("Error processing Didit webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Error processing webhook");
        }
    }

    private String readBody(HttpServletRequest request) throws IOException {
        StringBuilder bodyBuilder = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                bodyBuilder.append(line);
            }
        }
        return bodyBuilder.toString();
    }

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

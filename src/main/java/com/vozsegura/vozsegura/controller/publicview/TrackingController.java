package com.vozsegura.vozsegura.controller.publicview;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vozsegura.vozsegura.config.GatewayConfig;
import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.dto.ComplaintStatusDto;
import com.vozsegura.vozsegura.dto.forms.TrackingForm;
import com.vozsegura.vozsegura.repo.EvidenceRepository;
import com.vozsegura.vozsegura.security.RateLimiter;
import com.vozsegura.vozsegura.service.ComplaintService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Controlador para consulta anónima de seguimiento de denuncias.
 * 
 * Responsabilidades:
 * - Permitir denunciantes consultar estado usando tracking ID
 * - Mostrar información pública del caso (estado, severidad, tipo)
 * - Mantener anonimato del denunciante (never reveal citizen identity)
 * - Aplicar rate limiting para prevenir enumeración
 * - Validar formato de tracking ID
 * 
 * Flujo de Seguimiento:
 * 1. Usuario accede a /seguimiento
 * 2. showTrackingForm() → muestra formulario
 * 3. Usuario ingresa tracking ID (hash SHA-256 de cédula)
 * 4. processTracking() → busca denuncia + validaciones
 * 5. Retorna estado (PENDING, IN_PROGRESS, RESOLVED, etc.)
 * 
 * Seguridad y Privacidad:
 * - Require sesión autenticada (acreditar identidad)
 * - Rate limiting: máx 10 consultas por IP/hora
 * - Mensajes genéricos (no revelar si tracking ID existe)
 * - No expone nombre, email, detalles de denunciante
 * - Tracking ID es hash SHA-256 (imposible recuperar cédula)
 * - Solo muestra info pública (estado, fecha, tipo)
 * 
 * Información Visible para Denunciantes:
 * - Estado actual (PENDING, ASSIGNED, IN_PROGRESS, RESOLVED, REJECTED, DERIVED)
 * - Severidad (LOW, MEDIUM, HIGH, CRITICAL)
 * - Tipo de denuncia
 * - Fecha de creación y última actualización
 * - Cantidad de evidencias enviadas
 * - Entidad destino (si fue derivada)
 * - Notas públicas del analista (si hay)
 * - Si requiere más información
 * \n * NO Visible (privado):
 * - Identidad del denunciante
 * - Email del denunciante
 * - Contenido cifrado de denuncia\n * - Notas internas de staff
 * - Detalles de derivación interna
 * \n * @author Voz Segura Team
 * @since 2026-01
 */
@Controller
@RequestMapping("/seguimiento")
public class TrackingController {

    private final ComplaintService complaintService;
    private final EvidenceRepository evidenceRepository;
    private final RateLimiter rateLimiter;
    private final GatewayConfig gatewayConfig;

    public TrackingController(ComplaintService complaintService,
                               EvidenceRepository evidenceRepository,
                               RateLimiter rateLimiter,
                               GatewayConfig gatewayConfig) {
        this.complaintService = complaintService;
        this.evidenceRepository = evidenceRepository;
        this.rateLimiter = rateLimiter;
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Muestra el formulario para ingresar el código de seguimiento.
     * 
     * Requiere sesión autenticada (denunciante ya verificado con MFA).
     * Mostración inicial del formulario de búsqueda.
     * 
     * @param session sesión HTTP del usuario
     * @param model modelo Thymeleaf para pasar datos a vista
     * @return vista "public/seguimiento" con formulario vacío
     *         redirige si sesión no autenticada
     */
    @GetMapping
    public String showTrackingForm(HttpSession session, Model model) {
        // Verificar que el usuario esté autenticado
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return gatewayConfig.redirectToSessionExpired();
        }

        model.addAttribute("trackingForm", new TrackingForm());
        return "public/seguimiento";
    }

    /**
     * Procesa la consulta de seguimiento con rate limiting y validaciones.
     * 
     * Busca denuncia por tracking ID (hash SHA-256).
     * Aplica rate limiting por IP para prevenir enumeración.
     * Retorna información pública del caso (nunca datos sensibles).
     * 
     * Validaciones:
     * - Sesión autenticada
     * - Rate limit (máx intentos por IP)
     * - Formato de tracking ID válido
     * - Tracking ID existe en base de datos
     * 
     * Información Retornada (ComplaintStatusDto):
     * - tracking ID
     * - Estado actual (máquina de estados)
     * - Severidad (LOW/MEDIUM/HIGH/CRITICAL)
     * - Fecha creación/actualización
     * - Cantidad de evidencias
     * - Entidad destino (si derivada)
     * - Notas públicas
     * - Flag si requiere más información
     * - Tipo de denuncia
     * 
     * Privacidad:
     * - Nunca expone identidad denunciante
     * - Mensaje genérico si tracking ID no existe
     * - No hay diferencia en respuesta entre "no existe" y "acceso denegado"
     * 
     * @param form objeto con tracking ID ingresado
     * @param bindingResult validación de formulario
     * @param request HttpServletRequest para extraer IP
     * @param session sesión HTTP autenticada
     * @param model modelo Thymeleaf
     * @return vista "public/seguimiento-resultado" con estado
     *         redirige a formulario si hay errores
     */
    @PostMapping
    public String processTracking(@Valid @ModelAttribute TrackingForm form,
                                   BindingResult bindingResult,
                                   HttpServletRequest request,
                                   HttpSession session,
                                   Model model) {

        // Verificar que el usuario esté autenticado
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return gatewayConfig.redirectToSessionExpired();
        }

        // Rate limiting basado en IP para prevenir enumeración
        String clientIp = getClientIp(request);
        String rateLimitKey = "tracking-" + clientIp;

        if (!rateLimiter.tryConsume(rateLimitKey)) {
            model.addAttribute("error", "Demasiados intentos. Por favor espere un momento antes de intentar nuevamente.");
            model.addAttribute("trackingForm", form);
            return "public/seguimiento";
        }

        if (bindingResult.hasErrors()) {
            return "public/seguimiento";
        }

        // Buscar la denuncia
        String trackingId = form.getTrackingId().trim();
        Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);

        if (complaintOpt.isEmpty()) {
            // Mensaje genérico para no revelar si el código existe o no
            model.addAttribute("error", "No se encontró información para el código proporcionado. Verifica que esté correcto.");
            model.addAttribute("trackingForm", form);
            return "public/seguimiento";
        }

        Complaint complaint = complaintOpt.get();

        // Contar evidencias
        int evidenceCount = evidenceRepository.countByComplaint(complaint);

        // Crear DTO con información completa para el denunciante
        ComplaintStatusDto status = new ComplaintStatusDto(
            complaint.getTrackingId(),
            complaint.getStatus(),
            complaint.getSeverity(),
            complaint.getCreatedAt(),
            complaint.getUpdatedAt(),
            evidenceCount,
            complaint.getDerivedTo(),
            complaint.getAnalystNotes(),
            complaint.isRequiresMoreInfo(),
            complaint.getComplaintType()
        );

        model.addAttribute("complaintStatus", status);
        model.addAttribute("trackingForm", form);
        model.addAttribute("trackingId", trackingId);

        return "public/seguimiento-resultado";
    }

    /**
     * Obtiene la IP real del cliente considerando proxies y CDNs.
     * 
     * Prioridad de headers:
     * 1. X-Forwarded-For (CloudFlare, nginx, etc.)
     * 2. X-Real-IP (nginx reverse proxy)
     * 3. request.getRemoteAddr() (conexión directa)
     * 
     * Usado por rate limiting para identificar usuarios por IP.
     * 
     * @param request HttpServletRequest
     * @return IP del cliente (última en cadena si hay proxies)
     */
    private String getClientIp(HttpServletRequest request) {
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

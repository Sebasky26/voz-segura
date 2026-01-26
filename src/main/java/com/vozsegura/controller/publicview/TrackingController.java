package com.vozsegura.controller.publicview;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vozsegura.config.GatewayConfig;
import com.vozsegura.domain.entity.Complaint;
import com.vozsegura.dto.ComplaintStatusDto;
import com.vozsegura.dto.forms.TrackingForm;
import com.vozsegura.repo.EvidenceRepository;
import com.vozsegura.security.RateLimiter;
import com.vozsegura.service.AuditService;
import com.vozsegura.service.ComplaintService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Controlador para consulta anónima de seguimiento de denuncias.
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>Requiere sesión autenticada (verificación previa).</li>
 *   <li>Rate limiting por IP para evitar enumeración.</li>
 *   <li>Valida propiedad del caso por {@code identity_vault_id}.</li>
 *   <li>No expone contenido, datos cifrados ni notas internas.</li>
 * </ul>
 */
@Controller
@RequestMapping("/seguimiento")
public class TrackingController {

    private final ComplaintService complaintService;
    private final EvidenceRepository evidenceRepository;
    private final RateLimiter rateLimiter;
    private final GatewayConfig gatewayConfig;
    private final AuditService auditService;

    public TrackingController(
            ComplaintService complaintService,
            EvidenceRepository evidenceRepository,
            RateLimiter rateLimiter,
            GatewayConfig gatewayConfig,
            AuditService auditService
    ) {
        this.complaintService = complaintService;
        this.evidenceRepository = evidenceRepository;
        this.rateLimiter = rateLimiter;
        this.gatewayConfig = gatewayConfig;
        this.auditService = auditService;
    }

    @GetMapping
    public String showTrackingForm(HttpSession session, Model model) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        Long identityVaultId = (Long) session.getAttribute("identityVaultId");

        if (authenticated == null || !authenticated || identityVaultId == null) {
            return gatewayConfig.redirectToSessionExpired();
        }

        Boolean autoProcess = (Boolean) model.asMap().get("autoProcess");
        String trackingIdFlash = (String) model.asMap().get("trackingId");
        Boolean success = (Boolean) model.asMap().get("success");

        if (autoProcess != null && autoProcess && trackingIdFlash != null) {
            Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingIdFlash);
            if (complaintOpt.isPresent()) {
                Complaint complaint = complaintOpt.get();

                if (isOwner(complaint, identityVaultId)) {
                    int evidenceCount = evidenceRepository.countByComplaint(complaint);

                    ComplaintStatusDto status = new ComplaintStatusDto(
                            complaint.getTrackingId(),
                            complaint.getStatus(),
                            complaint.getSeverity(),
                            complaint.getCreatedAt(),
                            complaint.getUpdatedAt(),
                            evidenceCount,
                            complaint.getDerivedTo(),
                            null, // no exponer notas internas
                            complaint.isRequiresMoreInfo(),
                            complaint.getComplaintType()
                    );

                    model.addAttribute("complaintStatus", status);
                    model.addAttribute("success", success);
                    model.addAttribute("message", model.asMap().get("message"));
                    return "public/seguimiento-resultado";
                }
            }
        }

        model.addAttribute("trackingForm", new TrackingForm());
        return "public/seguimiento";
    }

    @PostMapping
    public String processTracking(
            @Valid @ModelAttribute TrackingForm form,
            BindingResult bindingResult,
            HttpServletRequest request,
            HttpSession session,
            Model model
    ) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        Long identityVaultId = (Long) session.getAttribute("identityVaultId");

        if (authenticated == null || !authenticated || identityVaultId == null) {
            return gatewayConfig.redirectToSessionExpired();
        }

        String clientIp = getClientIp(request);
        String rateLimitKey = "tracking-" + clientIp;

        if (!rateLimiter.tryConsume(rateLimitKey)) {
            model.addAttribute("error", "Demasiados intentos. Por favor espera un momento e intenta nuevamente.");
            model.addAttribute("trackingForm", form);
            return "public/seguimiento";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("trackingForm", form);
            return "public/seguimiento";
        }

        String trackingId = form.getTrackingId().trim();
        Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);

        if (complaintOpt.isEmpty()) {
            model.addAttribute("error", "No se encontró información para el código proporcionado. Verifica que esté correcto.");
            model.addAttribute("trackingForm", form);
            return "public/seguimiento";
        }

        Complaint complaint = complaintOpt.get();

        // Validación de propiedad sin PII
        if (!isOwner(complaint, identityVaultId)) {
            model.addAttribute("error", "No se encontró información para el código proporcionado. Verifica que esté correcto.");
            model.addAttribute("trackingForm", form);

            // Log sin PII: no cédula, no email, no nombres.
            complaintService.logUnauthorizedAccessAttempt(trackingId, identityVaultId, clientIp);

            return "public/seguimiento";
        }

        int evidenceCount = evidenceRepository.countByComplaint(complaint);

        ComplaintStatusDto status = new ComplaintStatusDto(
                complaint.getTrackingId(),
                complaint.getStatus(),
                complaint.getSeverity(),
                complaint.getCreatedAt(),
                complaint.getUpdatedAt(),
                evidenceCount,
                complaint.getDerivedTo(),
                null, // no notas
                complaint.isRequiresMoreInfo(),
                complaint.getComplaintType()
        );

        model.addAttribute("complaintStatus", status);
        model.addAttribute("trackingForm", form);
        model.addAttribute("trackingId", trackingId);

        // Auditar acceso exitoso (sin PII, solo tracking_id)
        try {
            auditService.logComplaintAccess(trackingId);
        } catch (Exception e) {
            // Silent failure - no interrumpir flujo
        }

        return "public/seguimiento-resultado";
    }

    private boolean isOwner(Complaint complaint, Long identityVaultId) {
        return complaint.getIdentityVaultId() != null && complaint.getIdentityVaultId().equals(identityVaultId);
    }

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

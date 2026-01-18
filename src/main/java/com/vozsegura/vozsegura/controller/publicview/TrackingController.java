package com.vozsegura.vozsegura.controller.publicview;

import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.dto.ComplaintStatusDto;
import com.vozsegura.vozsegura.dto.forms.TrackingForm;
import com.vozsegura.vozsegura.repo.EvidenceRepository;
import com.vozsegura.vozsegura.security.RateLimiter;
import com.vozsegura.vozsegura.service.ComplaintService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

/**
 * Controlador para consulta de seguimiento de denuncias.
 * Permite al denunciante consultar el estado de su caso usando el código de seguimiento.
 *
 * Seguridad:
 * - Rate limiting para prevenir enumeración de códigos
 * - No expone información sensible del denunciante
 * - Solo muestra estado general, no detalles de la denuncia
 */
@Controller
@RequestMapping("/seguimiento")
public class TrackingController {

    private final ComplaintService complaintService;
    private final EvidenceRepository evidenceRepository;
    private final RateLimiter rateLimiter;

    public TrackingController(ComplaintService complaintService,
                               EvidenceRepository evidenceRepository,
                               RateLimiter rateLimiter) {
        this.complaintService = complaintService;
        this.evidenceRepository = evidenceRepository;
        this.rateLimiter = rateLimiter;
    }

    /**
     * Muestra el formulario para ingresar el código de seguimiento.
     */
    @GetMapping
    public String showTrackingForm(Model model) {
        model.addAttribute("trackingForm", new TrackingForm());
        return "public/seguimiento";
    }

    /**
     * Procesa la consulta de seguimiento.
     * Aplica rate limiting para prevenir ataques de fuerza bruta.
     */
    @PostMapping
    public String processTracking(@Valid @ModelAttribute TrackingForm form,
                                   BindingResult bindingResult,
                                   HttpServletRequest request,
                                   Model model) {

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

        // Crear DTO con información no sensible
        ComplaintStatusDto status = new ComplaintStatusDto(
            complaint.getTrackingId(),
            complaint.getStatus(),
            complaint.getSeverity(),
            complaint.getCreatedAt(),
            complaint.getUpdatedAt(),
            evidenceCount
        );

        model.addAttribute("complaintStatus", status);
        model.addAttribute("trackingForm", form);

        return "public/seguimiento-resultado";
    }

    /**
     * Obtiene la IP real del cliente considerando proxies.
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

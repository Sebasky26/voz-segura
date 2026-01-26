package com.vozsegura.controller.publicview;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.domain.entity.Complaint;
import com.vozsegura.dto.forms.AdditionalInfoForm;
import com.vozsegura.dto.forms.ComplaintForm;
import com.vozsegura.dto.forms.DenunciaAccessForm;
import com.vozsegura.security.RateLimiter;
import com.vozsegura.service.AuditService;
import com.vozsegura.service.ComplaintService;
import com.vozsegura.service.DiditService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador del flujo público de denuncias anónimas.
 *
 * <p>Flujo:</p>
 * <ul>
 *   <li>Usuario inicia verificación con Didit.</li>
 *   <li>Webhook guarda verificación y vincula con identity_vault.</li>
 *   <li>Callback carga verificación y habilita sesión para denunciar.</li>
 *   <li>Denuncia se guarda con identity_vault_id (sin PII en claro).</li>
 * </ul>
 */
@Slf4j
@Controller
@SessionAttributes({"diditSessionId", "identityVaultId", "citizenHash"})
public class PublicComplaintController {

    private final RateLimiter rateLimiter;
    private final ComplaintService complaintService;
    private final DiditService diditService;
    private final AuditService auditService;

    public PublicComplaintController(
            RateLimiter rateLimiter,
            ComplaintService complaintService,
            DiditService diditService,
            AuditService auditService
    ) {
        this.rateLimiter = rateLimiter;
        this.complaintService = complaintService;
        this.diditService = diditService;
        this.auditService = auditService;
    }

    @ModelAttribute("denunciaAccessForm")
    public DenunciaAccessForm accessForm() {
        return new DenunciaAccessForm();
    }

    @GetMapping("/denuncia")
    public String showAccessForm() {
        return "redirect:/verification/inicio";
    }

    @GetMapping("/verification/inicio")
    public String verificationInicio() {
        return "public/verification-inicio";
    }

    @GetMapping("/verification/start")
    public String startVerification(HttpSession session, Model model) {
        try {
            // Si tu RateLimiter está activo, aplícalo aquí (por IP o por sesión).
            // if (!rateLimiter.allow(session)) { return "public/rate-limit"; }

            String vendorData = java.util.UUID.randomUUID().toString();
            Map<String, Object> diditSession = diditService.createVerificationSession(vendorData);

            String diditSessionId = (String) diditSession.get("session_id");
            String verificationUrl = (String) diditSession.get("url");

            session.setAttribute("diditSessionId", diditSessionId);
            session.setAttribute("vendorData", vendorData);

            log.info("Verification session created, redirecting to Didit");
            return "redirect:" + verificationUrl;

        } catch (Exception e) {
            log.error("Error starting Didit verification", e);
            model.addAttribute("error", "Error al iniciar verificación. Intenta nuevamente.");
            return "public/verification-inicio";
        }
    }

    @GetMapping("/verification/callback")
    public String verificationCallback(HttpSession session, Model model) {
        try {
            String diditSessionId = (String) session.getAttribute("diditSessionId");
            if (diditSessionId == null) {
                return "redirect:/verification/inicio?error=session_expired";
            }

            var verificationOpt = diditService.getVerificationBySessionId(diditSessionId);
            if (verificationOpt.isEmpty()) {
                model.addAttribute("error", "Verificación no completada. Por favor intenta nuevamente.");
                return "public/verification-inicio";
            }

            var verification = verificationOpt.get();

            // Recomendado: validar estado
            if (verification.getVerificationStatus() == null ||
                    !verification.getVerificationStatus().equalsIgnoreCase("VERIFIED")) {
                model.addAttribute("error", "La verificación no fue aprobada. Por favor intenta nuevamente.");
                return "public/verification-inicio";
            }

            Long identityVaultId = verification.getIdentityVaultId();
            String documentHash = verification.getDocumentNumberHash();

            if (identityVaultId == null) {
                model.addAttribute("error", "No se pudo vincular la identidad verificada. Intenta nuevamente.");
                return "public/verification-inicio";
            }

            // Guardar lo mínimo necesario en sesión
            session.setAttribute("identityVaultId", identityVaultId);
            session.setAttribute("citizenHash", documentHash); // opcional, útil para correlación sin PII
            session.setAttribute("authenticated", true);
            session.setAttribute("userRole", "DENUNCIANTE");

            log.info("Verification completed for vaultId={}", identityVaultId);
            return "public/verification-exitosa";

        } catch (Exception e) {
            log.error("Error in verification callback", e);
            model.addAttribute("error", "Error procesando verificación. Intenta nuevamente.");
            return "public/verification-inicio";
        }
    }

    @GetMapping("/denuncia/opciones")
    public String showOptions(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return "redirect:/verification/inicio?required";
        }
        return "public/denuncia-opciones";
    }

    @GetMapping("/denuncia/form")
    public String showComplaintForm(HttpSession session, Model model) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        Long identityVaultId = (Long) session.getAttribute("identityVaultId");

        if (authenticated == null || !authenticated || identityVaultId == null) {
            return "redirect:/verification/inicio?required";
        }

        model.addAttribute("complaintForm", new ComplaintForm());
        return "public/denuncia-form";
    }

    @PostMapping("/denuncia/submit")
    public String submitComplaint(
            @Valid @ModelAttribute("complaintForm") ComplaintForm form,
            BindingResult bindingResult,
            HttpSession session,
            SessionStatus sessionStatus,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        if (bindingResult.hasErrors()) {
            return "public/denuncia-form";
        }

        Long identityVaultId = (Long) session.getAttribute("identityVaultId");
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");

        if (authenticated == null || !authenticated || identityVaultId == null) {
            model.addAttribute("error", "Sesión expirada. Por favor verifica nuevamente.");
            return "public/denuncia-form";
        }

        try {
            // Crear denuncia
            String trackingId = complaintService.createComplaint(form, identityVaultId);

            // Auditar creación de denuncia (sin PII, solo tracking_id)
            auditService.logComplaintCreated(trackingId);

            // NO invalidar la sesión para permitir crear más denuncias o consultar seguimiento
            // El usuario puede cerrar sesión manualmente desde /denuncia/opciones
            sessionStatus.setComplete();

            redirectAttributes.addFlashAttribute("trackingId", trackingId);
            redirectAttributes.addFlashAttribute("success", true);
            return "redirect:/denuncia/confirmacion";

        } catch (Exception e) {
            log.error("Error creating complaint", e);
            model.addAttribute("error", "Error al procesar la denuncia. Por favor intenta de nuevo.");
            return "public/denuncia-form";
        }
    }

    @GetMapping("/denuncia/confirmacion")
    public String showConfirmation(Model model) {
        return "public/denuncia-confirmacion";
    }

    @GetMapping("/denuncia/editar/{trackingId}")
    public String showAdditionalInfoForm(
            @PathVariable String trackingId,
            HttpSession session,
            Model model
    ) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        Long identityVaultId = (Long) session.getAttribute("identityVaultId");

        if (authenticated == null || !authenticated || identityVaultId == null) {
            return "redirect:/verification/inicio?required";
        }

        Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);
        if (complaintOpt.isEmpty()) {
            model.addAttribute("error", "No se encontró la denuncia.");
            return "public/error-acceso";
        }

        Complaint complaint = complaintOpt.get();

        // Propiedad: la denuncia debe pertenecer a la misma bóveda de identidad
        if (complaint.getIdentityVaultId() == null || !complaint.getIdentityVaultId().equals(identityVaultId)) {
            model.addAttribute("error", "No tienes permiso para editar esta denuncia.");
            return "public/error-acceso";
        }

        if (!complaint.isRequiresMoreInfo()) {
            model.addAttribute("error", "Esta denuncia no requiere información adicional.");
            return "public/error-acceso";
        }

        model.addAttribute("trackingId", trackingId);
        model.addAttribute("additionalInfoForm", new AdditionalInfoForm());
        model.addAttribute("complaint", complaint);

        // Importante: NO exponer notas del analista al denunciante.
        return "public/denuncia-editar";
    }

    @PostMapping("/denuncia/editar/{trackingId}")
    public String submitAdditionalInfo(
            @PathVariable String trackingId,
            @Valid @ModelAttribute("additionalInfoForm") AdditionalInfoForm form,
            BindingResult bindingResult,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model
    ) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        Long identityVaultId = (Long) session.getAttribute("identityVaultId");

        if (authenticated == null || !authenticated || identityVaultId == null) {
            return "redirect:/verification/inicio?required";
        }

        Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);
        if (complaintOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "No se encontró la denuncia.");
            return "redirect:/seguimiento";
        }

        Complaint complaint = complaintOpt.get();
        if (complaint.getIdentityVaultId() == null || !complaint.getIdentityVaultId().equals(identityVaultId)) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para editar esta denuncia.");
            return "redirect:/seguimiento";
        }

        if (bindingResult.hasErrors() || form.getAdditionalInfo() == null || form.getAdditionalInfo().isBlank()) {
            if (form.getAdditionalInfo() == null || form.getAdditionalInfo().isBlank()) {
                bindingResult.rejectValue("additionalInfo", "error.empty", "Debes proporcionar información adicional");
            }
            model.addAttribute("trackingId", trackingId);
            model.addAttribute("complaint", complaint);
            return "public/denuncia-editar";
        }

        try {
            complaintService.addAdditionalInfo(trackingId, form.getAdditionalInfo(), form.getEvidences());

            // Auditar envío de información adicional (sin PII, solo tracking_id)
            try {
                auditService.logSecurityEvent(
                    "COMPLAINT_INFO_ADDED",
                    "SUCCESS",
                    null,
                    "denunciante",
                    null,
                    null,
                    Map.of(
                        "tracking_id", trackingId,
                        "has_new_evidences", form.getEvidences() != null && form.getEvidences().length > 0 ? "true" : "false"
                    )
                );
            } catch (Exception auditEx) {
                // Silent failure
            }

            redirectAttributes.addFlashAttribute("success", true);
            redirectAttributes.addFlashAttribute("message",
                    "Información adicional enviada exitosamente. El analista revisará tu denuncia nuevamente.");
            redirectAttributes.addFlashAttribute("trackingId", trackingId);

            return "redirect:/seguimiento";

        } catch (Exception e) {
            log.error("Error adding additional info", e);
            model.addAttribute("error", "Error al enviar la información. Por favor intenta de nuevo.");
            model.addAttribute("trackingId", trackingId);
            model.addAttribute("complaint", complaint);
            return "public/denuncia-editar";
        }
    }
}

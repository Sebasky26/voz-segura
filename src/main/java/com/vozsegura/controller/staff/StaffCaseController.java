package com.vozsegura.controller.staff;

import java.util.List;
import java.util.Optional;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.config.GatewayConfig;
import com.vozsegura.domain.entity.Complaint;
import com.vozsegura.domain.entity.Evidence;
import com.vozsegura.repo.EvidenceRepository;
import com.vozsegura.security.EncryptionService;
import com.vozsegura.service.AuditService;
import com.vozsegura.service.ComplaintService;
import com.vozsegura.service.DerivationService;
import com.vozsegura.service.SystemConfigService;

import jakarta.servlet.http.HttpSession;

/**
 * Controlador para gestion de casos por parte del staff.
 *
 * <p>Nota de seguridad:</p>
 * <ul>
 *   <li>Los datos sensibles se guardan cifrados en base de datos.</li>
 *   <li>El descifrado solo ocurre en memoria y solo para pantallas del staff.</li>
 *   <li>Todo acceso y accion se audita.</li>
 * </ul>
 */
@Controller
@RequestMapping("/staff")
public class StaffCaseController {

    private final ComplaintService complaintService;
    private final DerivationService derivationService;
    private final EvidenceRepository evidenceRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;
    private final SystemConfigService systemConfigService;
    private final GatewayConfig gatewayConfig;

    public StaffCaseController(
            ComplaintService complaintService,
            DerivationService derivationService,
            EvidenceRepository evidenceRepository,
            EncryptionService encryptionService,
            AuditService auditService,
            SystemConfigService systemConfigService,
            GatewayConfig gatewayConfig
    ) {
        this.complaintService = complaintService;
        this.derivationService = derivationService;
        this.evidenceRepository = evidenceRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
        this.systemConfigService = systemConfigService;
        this.gatewayConfig = gatewayConfig;
    }

    @GetMapping({"/casos", "/casos-list", ""})
    public String listCases(
            @RequestParam(name = "status", required = false) String status,
            HttpSession session,
            Model model
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        List<Complaint> complaints = (status != null && !status.isBlank())
                ? complaintService.findByStatus(status)
                : complaintService.findAllOrderByCreatedAtDesc();

        model.addAttribute("complaints", complaints);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("systemConfigService", systemConfigService);
        return "staff/casos-list";
    }

    @GetMapping("/casos/{trackingId}")
    public String viewCase(
            @PathVariable("trackingId") String trackingId,
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);
            if (complaintOpt.isEmpty()) {
                redirectAttributes.addFlashAttribute("error", "Denuncia no encontrada");
                return "redirect:/staff/casos";
            }

            Complaint complaint = complaintOpt.get();

            // Descifrar texto principal (solo en memoria)
            String decryptedText = complaintService.decryptComplaintText(complaint.getEncryptedText());

            // Descifrar datos de empresa (solo en memoria para mostrar al analista)
            String companyName = encryptionService.decryptFromBase64(complaint.getCompanyNameEncrypted());
            String companyContact = encryptionService.decryptFromBase64(complaint.getCompanyContactEncrypted());
            String companyAddress = encryptionService.decryptFromBase64(complaint.getCompanyAddressEncrypted());
            String companyEmail = complaint.getCompanyEmailEncrypted() != null
                ? encryptionService.decryptFromBase64(complaint.getCompanyEmailEncrypted()) : "";
            String companyPhone = complaint.getCompanyPhoneEncrypted() != null
                ? encryptionService.decryptFromBase64(complaint.getCompanyPhoneEncrypted()) : "";

            // Descifrar notas del analista (si existen)
            String analystNotes = "";
            if (complaint.getAnalystNotesEncrypted() != null && !complaint.getAnalystNotesEncrypted().isEmpty()) {
                try {
                    analystNotes = encryptionService.decryptFromBase64(complaint.getAnalystNotesEncrypted());
                } catch (Exception e) {
                    analystNotes = "[Error al descifrar notas]";
                }
            }

            // Evidencias
            List<Evidence> evidences = evidenceRepository.findByComplaintId(complaint.getId());

            // Desencriptar nombres de archivos de evidencias (solo en memoria)
            java.util.Map<Long, String> decryptedFileNames = new java.util.HashMap<>();
            for (Evidence ev : evidences) {
                try {
                    String decryptedFileName = encryptionService.decryptFromBase64(ev.getFileNameEncrypted());
                    decryptedFileNames.put(ev.getId(), decryptedFileName);
                } catch (Exception e) {
                    decryptedFileNames.put(ev.getId(), "[Error al descifrar nombre]");
                }
            }

            // Auditoria: acceso a caso
            String username = getUsername(session);
            String role = (String) session.getAttribute("userType");
            if (role == null) role = "ANALYST";

            auditService.logEvent(role, username, "CASE_VIEWED", trackingId, "Acceso a detalle de caso");

            model.addAttribute("complaint", complaint);
            model.addAttribute("decryptedText", decryptedText);
            model.addAttribute("companyName", companyName);
            model.addAttribute("companyContact", companyContact);
            model.addAttribute("companyAddress", companyAddress);
            model.addAttribute("companyEmail", companyEmail);
            model.addAttribute("companyPhone", companyPhone);
            model.addAttribute("analystNotes", analystNotes);
            model.addAttribute("evidences", evidences);
            model.addAttribute("decryptedFileNames", decryptedFileNames);
            model.addAttribute("systemConfigService", systemConfigService);
            model.addAttribute("complaintTypes", systemConfigService.getComplaintTypesAsArray());
            model.addAttribute("priorities", systemConfigService.getPrioritiesAsArray());

            return "staff/caso-detalle";

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al cargar el caso: " + e.getMessage());
            return "redirect:/staff/casos";
        }
    }

    @PostMapping("/casos/{trackingId}/clasificar")
    public String clasificarCaso(
            @PathVariable("trackingId") String trackingId,
            @RequestParam("complaintType") String complaintType,
            @RequestParam("priority") String priority,
            @RequestParam(value = "analystNotes", required = false) String analystNotes,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);
            complaintService.classifyComplaint(trackingId, complaintType, priority, analystNotes, username);
            redirectAttributes.addFlashAttribute("success", "Clasificación actualizada");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al guardar la clasificación.");
        }
        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/estado")
    public String updateEstado(
            @PathVariable("trackingId") String trackingId,
            @RequestParam("newStatus") String newStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);
            String userType = (String) session.getAttribute("userType");
            if (userType == null) userType = "ANALYST";

            complaintService.updateStatus(trackingId, newStatus, username, userType);
            redirectAttributes.addFlashAttribute("success", "Estado actualizado correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar estado.");
        }
        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/aprobar-derivar")
    public String aprobarYDerivar(
            @PathVariable("trackingId") String trackingId,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        String username = getUsername(session);

        try {
            String destination = derivationService.deriveComplaint(trackingId, username);
            redirectAttributes.addFlashAttribute("success", "Denuncia aprobada y enviada a: " + destination);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al derivar la denuncia.");
        }

        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/solicitar-info")
    public String solicitarMasInfo(
            @PathVariable("trackingId") String trackingId,
            @RequestParam(value = "motivo", required = false) String motivo,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);
            complaintService.requestMoreInfo(trackingId, motivo, username);
            redirectAttributes.addFlashAttribute("success", "Se ha solicitado más información al denunciante");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al solicitar información.");
        }
        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/rechazar")
    public String rechazarCaso(
            @PathVariable("trackingId") String trackingId,
            @RequestParam("motivo") String motivo,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);
            complaintService.rejectComplaint(trackingId, motivo, username);
            redirectAttributes.addFlashAttribute("success", "Denuncia rechazada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al rechazar la denuncia.");
        }
        return "redirect:/staff/casos/" + trackingId;
    }

    @GetMapping("/evidencias/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable("id") Long id, HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        Optional<Evidence> evidenceOpt = evidenceRepository.findById(id);
        if (evidenceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Evidence evidence = evidenceOpt.get();

        try {
            // 1) Descifrar contenido como bytes (no como String)
            byte[] decryptedContent = encryptionService.decryptBytes(evidence.getEncryptedContent());

            // 2) Descifrar nombre del archivo (si está cifrado en BD)
            String fileName = "evidencia";
            if (evidence.getFileNameEncrypted() != null && !evidence.getFileNameEncrypted().isBlank()) {
                fileName = encryptionService.decryptFromBase64(evidence.getFileNameEncrypted());
            }

            // 3) Auditoría
            String username = getUsername(session);
            String role = (String) session.getAttribute("userType");
            if (role == null) role = "ANALYST";

            auditService.logEvent(role, username, "EVIDENCE_VIEWED",
                    evidence.getComplaint().getTrackingId(),
                    "Evidencia descargada (id=" + id + ")");

            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (evidence.getContentType() != null) {
                try {
                    mediaType = MediaType.parseMediaType(evidence.getContentType());
                } catch (Exception ignored) { }
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .body(decryptedContent);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    private boolean isAuthenticated(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("authenticated");
        return auth != null && auth;
    }

    private String getUsername(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return username != null ? username : "ANALYST";
    }
}

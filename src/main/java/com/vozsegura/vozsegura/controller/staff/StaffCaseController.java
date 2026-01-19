package com.vozsegura.vozsegura.controller.staff;

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

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.domain.entity.Evidence;
import com.vozsegura.vozsegura.repo.EvidenceRepository;
import com.vozsegura.vozsegura.security.EncryptionService;
import com.vozsegura.vozsegura.service.AuditService;
import com.vozsegura.vozsegura.service.ComplaintService;
import com.vozsegura.vozsegura.service.DerivationService;

import jakarta.servlet.http.HttpSession;

/**
 * Controlador para la gestión de casos por parte del Staff (Analistas).
 *
 * Funciones del Analista:
 * - Ver listado de denuncias
 * - Revisar contenido y evidencias
 * - Clasificar tipo de denuncia y prioridad
 * - Cambiar estado (pendiente, en revisión, requiere más info, aprobado, rechazado)
 * - Agregar notas internas
 * - Aprobar para derivación automática
 */
@Controller
@RequestMapping("/staff")
public class StaffCaseController {

    private final ComplaintService complaintService;
    private final DerivationService derivationService;
    private final EvidenceRepository evidenceRepository;
    private final EncryptionService encryptionService;
    private final AuditService auditService;

    public StaffCaseController(ComplaintService complaintService,
                                DerivationService derivationService,
                                EvidenceRepository evidenceRepository,
                                EncryptionService encryptionService,
                                AuditService auditService) {
        this.complaintService = complaintService;
        this.derivationService = derivationService;
        this.evidenceRepository = evidenceRepository;
        this.encryptionService = encryptionService;
        this.auditService = auditService;
    }

    @GetMapping({"/casos", "/casos-list", ""})
    public String listCases(
            @RequestParam(name = "status", required = false) String status,
            HttpSession session,
            Model model) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        List<Complaint> complaints;
        if (status != null && !status.isBlank()) {
            complaints = complaintService.findByStatus(status);
        } else {
            complaints = complaintService.findAllOrderByCreatedAtDesc();
        }

        model.addAttribute("complaints", complaints);
        model.addAttribute("selectedStatus", status);
        return "staff/casos-list";
    }

    @GetMapping("/casos/{trackingId}")
    public String viewCase(@PathVariable("trackingId") String trackingId,
                           HttpSession session,
                           Model model,
                           RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);
        if (complaintOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Denuncia no encontrada");
            return "redirect:/staff/casos";
        }

        Complaint complaint = complaintOpt.get();
        String decryptedText = complaintService.decryptComplaintText(complaint.getEncryptedText());
        List<Evidence> evidences = evidenceRepository.findByComplaintId(complaint.getId());

        model.addAttribute("complaint", complaint);
        model.addAttribute("decryptedText", decryptedText);
        model.addAttribute("evidences", evidences);
        model.addAttribute("complaintTypes", getComplaintTypes());
        model.addAttribute("priorities", getPriorities());
        model.addAttribute("statuses", getStatuses());

        return "staff/caso-detalle";
    }

    @PostMapping("/casos/{trackingId}/clasificar")
    public String clasificarCaso(
            @PathVariable("trackingId") String trackingId,
            @RequestParam("complaintType") String complaintType,
            @RequestParam("priority") String priority,
            @RequestParam(value = "analystNotes", required = false) String analystNotes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);
        complaintService.classifyComplaint(trackingId, complaintType, priority, analystNotes, username);
        redirectAttributes.addFlashAttribute("success", "Clasificación actualizada");
        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/estado")
    public String updateEstado(
            @PathVariable("trackingId") String trackingId,
            @RequestParam("newStatus") String newStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);
        String userType = (String) session.getAttribute("userType");
        if (userType == null) userType = "ANALYST";

        complaintService.updateStatus(trackingId, newStatus, username, userType);

        String statusLabel = getStatusLabel(newStatus);
        redirectAttributes.addFlashAttribute("success", "Estado actualizado a: " + statusLabel);
        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/aprobar-derivar")
    public String aprobarYDerivar(
            @PathVariable("trackingId") String trackingId,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);

        try {
            String destination = derivationService.deriveComplaint(trackingId, username);
            redirectAttributes.addFlashAttribute("success",
                    "Denuncia aprobada y enviada a: " + destination);
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al derivar la denuncia");
        }

        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/solicitar-info")
    public String solicitarMasInfo(
            @PathVariable("trackingId") String trackingId,
            @RequestParam(value = "motivo", required = false) String motivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);
        complaintService.requestMoreInfo(trackingId, motivo, username);
        redirectAttributes.addFlashAttribute("success",
                "Se ha solicitado más información al denunciante");
        return "redirect:/staff/casos/" + trackingId;
    }

    @PostMapping("/casos/{trackingId}/rechazar")
    public String rechazarCaso(
            @PathVariable("trackingId") String trackingId,
            @RequestParam("motivo") String motivo,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);
        complaintService.rejectComplaint(trackingId, motivo, username);
        redirectAttributes.addFlashAttribute("success", "Denuncia rechazada");
        return "redirect:/staff/casos/" + trackingId;
    }

    /**
     * Descarga una evidencia específica (descifrada).
     */
    @GetMapping("/evidencias/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadEvidence(@PathVariable("id") Long id,
                                                   HttpSession session) {
        if (!isAuthenticated(session)) {
            return ResponseEntity.status(401).build();
        }

        Optional<Evidence> evidenceOpt = evidenceRepository.findById(id);
        if (evidenceOpt.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        Evidence evidence = evidenceOpt.get();

        try {
            // Descifrar el contenido
            byte[] encryptedBytes = evidence.getEncryptedContent();
            String encryptedB64 = new String(encryptedBytes, java.nio.charset.StandardCharsets.UTF_8);
            String decryptedB64 = encryptionService.decryptFromBase64(encryptedB64);
            byte[] decryptedContent = java.util.Base64.getDecoder().decode(decryptedB64);

            // Registrar auditoría
            String username = getUsername(session);
            auditService.logEvent("ANALYST", username, "EVIDENCE_VIEWED",
                                  evidence.getComplaint().getTrackingId(),
                                  "Evidencia visualizada: " + evidence.getFileName());

            // Determinar content type
            MediaType mediaType = MediaType.APPLICATION_OCTET_STREAM;
            if (evidence.getContentType() != null) {
                try {
                    mediaType = MediaType.parseMediaType(evidence.getContentType());
                } catch (Exception e) {
                    // Usar default
                }
            }

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION,
                            "inline; filename=\"" + evidence.getFileName() + "\"")
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

    private String[][] getComplaintTypes() {
        return new String[][] {
            {"LABOR_RIGHTS", "Derechos Laborales"},
            {"HARASSMENT", "Acoso Laboral"},
            {"DISCRIMINATION", "Discriminación"},
            {"SAFETY", "Seguridad Laboral"},
            {"FRAUD", "Fraude"},
            {"OTHER", "Otro"}
        };
    }

    private String[][] getPriorities() {
        return new String[][] {
            {"LOW", "Baja"},
            {"MEDIUM", "Media"},
            {"HIGH", "Alta"},
            {"CRITICAL", "Crítica"}
        };
    }

    private String[][] getStatuses() {
        return new String[][] {
            {"PENDING", "Pendiente"},
            {"IN_REVIEW", "En Revisión"},
            {"NEEDS_INFO", "Requiere Información"},
            {"APPROVED", "Aprobado"},
            {"REJECTED", "Rechazado"},
            {"DERIVED", "Derivado"}
        };
    }

    private String getStatusLabel(String status) {
        for (String[] s : getStatuses()) {
            if (s[0].equals(status)) return s[1];
        }
        return status;
    }
}

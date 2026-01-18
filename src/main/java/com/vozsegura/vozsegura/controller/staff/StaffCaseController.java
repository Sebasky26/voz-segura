package com.vozsegura.vozsegura.controller.staff;

import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.service.ComplaintService;

import jakarta.servlet.http.HttpSession;

/**
 * Controlador para la gestión de casos por parte del Staff (Analistas).
 * Permite listar, ver detalles, cambiar estado y derivar denuncias.
 */
@Controller
@RequestMapping("/staff")
public class StaffCaseController {

    private final ComplaintService complaintService;

    public StaffCaseController(ComplaintService complaintService) {
        this.complaintService = complaintService;
    }

    /**
     * Lista todas las denuncias ordenadas por fecha de creación.
     */
    @GetMapping({"/casos", "/casos-list"})
    public String listCases(
            @RequestParam(required = false) String status,
            Model model) {

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

    /**
     * Muestra el detalle de una denuncia específica.
     * El texto se descifra para mostrarlo al analista.
     */
    @GetMapping("/casos/{trackingId}")
    public String viewCase(@PathVariable String trackingId, Model model, RedirectAttributes redirectAttributes) {
        Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);

        if (complaintOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Denuncia no encontrada");
            return "redirect:/staff/casos";
        }

        Complaint complaint = complaintOpt.get();

        // Descifrar el texto de la denuncia para mostrarlo
        String decryptedText = complaintService.decryptComplaintText(complaint.getEncryptedText());

        model.addAttribute("complaint", complaint);
        model.addAttribute("decryptedText", decryptedText);

        return "staff/caso-detalle";
    }

    /**
     * Actualiza el estado de una denuncia.
     */
    @PostMapping("/casos/{trackingId}/estado")
    public String updateEstado(
            @PathVariable String trackingId,
            @RequestParam String newStatus,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        String userType = (String) session.getAttribute("userType");

        if (username == null) username = "STAFF";
        if (userType == null) userType = "ANALYST";

        complaintService.updateStatus(trackingId, newStatus, username, userType);

        redirectAttributes.addFlashAttribute("success", "Estado actualizado correctamente");
        return "redirect:/staff/casos/" + trackingId;
    }

    /**
     * Deriva una denuncia a otra unidad.
     */
    @PostMapping("/casos/{trackingId}/derivar")
    public String derivar(
            @PathVariable String trackingId,
            @RequestParam String destination,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String username = (String) session.getAttribute("username");
        String userType = (String) session.getAttribute("userType");

        if (username == null) username = "STAFF";
        if (userType == null) userType = "ANALYST";

        complaintService.derive(trackingId, destination, username, userType);

        redirectAttributes.addFlashAttribute("success", "Caso derivado correctamente a: " + destination);
        return "redirect:/staff/casos/" + trackingId;
    }
}

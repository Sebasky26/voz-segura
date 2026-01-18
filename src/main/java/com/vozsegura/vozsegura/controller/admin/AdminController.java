package com.vozsegura.vozsegura.controller.admin;

import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.vozsegura.domain.entity.AuditEvent;
import com.vozsegura.vozsegura.domain.entity.DerivationRule;
import com.vozsegura.vozsegura.domain.entity.DestinationEntity;
import com.vozsegura.vozsegura.repo.AuditEventRepository;
import com.vozsegura.vozsegura.repo.DestinationEntityRepository;
import com.vozsegura.vozsegura.service.DerivationService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private final DerivationService derivationService;
    private final AuditEventRepository auditEventRepository;
    private final DestinationEntityRepository destinationEntityRepository;

    public AdminController(DerivationService derivationService,
                           AuditEventRepository auditEventRepository,
                           DestinationEntityRepository destinationEntityRepository) {
        this.derivationService = derivationService;
        this.auditEventRepository = auditEventRepository;
        this.destinationEntityRepository = destinationEntityRepository;
    }

    @GetMapping({"", "/panel"})
    public String panel(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        long totalRules = derivationService.findAllRules().size();
        long activeRules = derivationService.findActiveRules().size();

        model.addAttribute("totalRules", totalRules);
        model.addAttribute("activeRules", activeRules);

        return "admin/panel";
    }

    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        List<DerivationRule> rules = derivationService.findAllRules();
        // Cargar entidades desde BD, no hardcodeadas
        List<DestinationEntity> entidades = destinationEntityRepository.findByActiveTrueOrderByNameAsc();

        model.addAttribute("rules", rules);
        model.addAttribute("complaintTypes", getComplaintTypes());
        model.addAttribute("priorities", getPriorities());
        model.addAttribute("entidades", entidades);

        return "admin/reglas";
    }

    @PostMapping("/reglas/crear")
    public String crearRegla(
            @RequestParam("name") String name,
            @RequestParam(value = "complaintTypeMatch", required = false) String complaintTypeMatch,
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
            @RequestParam(value = "priorityMatch", required = false) String priorityMatch,
            @RequestParam("destination") String destination,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);

        DerivationRule rule = new DerivationRule();
        rule.setName(name);
        rule.setComplaintTypeMatch(emptyToNull(complaintTypeMatch));
        rule.setSeverityMatch(emptyToNull(severityMatch));
        rule.setPriorityMatch(emptyToNull(priorityMatch));
        rule.setDestination(destination);
        rule.setDescription(description);
        rule.setActive(true);

        derivationService.createRule(rule, username);
        redirectAttributes.addFlashAttribute("success", "Regla creada correctamente");
        return "redirect:/admin/reglas";
    }

    @PostMapping("/reglas/{id}/editar")
    public String editarRegla(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "complaintTypeMatch", required = false) String complaintTypeMatch,
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
            @RequestParam(value = "priorityMatch", required = false) String priorityMatch,
            @RequestParam("destination") String destination,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);

        DerivationRule updated = new DerivationRule();
        updated.setName(name);
        updated.setComplaintTypeMatch(emptyToNull(complaintTypeMatch));
        updated.setSeverityMatch(emptyToNull(severityMatch));
        updated.setPriorityMatch(emptyToNull(priorityMatch));
        updated.setDestination(destination);
        updated.setDescription(description);
        updated.setActive(active);

        derivationService.updateRule(id, updated, username);
        redirectAttributes.addFlashAttribute("success", "Regla actualizada correctamente");
        return "redirect:/admin/reglas";
    }

    @PostMapping("/reglas/{id}/eliminar")
    public String eliminarRegla(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);
        derivationService.deleteRule(id, username);
        redirectAttributes.addFlashAttribute("success", "Regla desactivada correctamente");
        return "redirect:/admin/reglas";
    }

    @PostMapping("/reglas/{id}/activar")
    public String activarRegla(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        String username = getUsername(session);
        derivationService.activateRule(id, username);
        redirectAttributes.addFlashAttribute("success", "Regla activada correctamente");
        return "redirect:/admin/reglas";
    }

    @GetMapping("/logs")
    public String logs(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }

        PageRequest pageRequest = PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "eventTime"));
        Page<AuditEvent> events;

        if (eventType != null && !eventType.isBlank()) {
            events = auditEventRepository.findByEventType(eventType, pageRequest);
        } else {
            events = auditEventRepository.findAll(pageRequest);
        }

        model.addAttribute("events", events);
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("eventTypes", getEventTypes());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());

        return "admin/logs";
    }

    @GetMapping("/revelacion")
    public String revelacion(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return "redirect:/auth/login?session_expired";
        }
        return "admin/revelacion";
    }

    private boolean isAuthenticated(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("authenticated");
        String userType = (String) session.getAttribute("userType");
        return auth != null && auth && "ADMIN".equals(userType);
    }

    private String getUsername(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return username != null ? username : "ADMIN";
    }

    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }

    private String[][] getComplaintTypes() {
        return new String[][] {
            {"", "Cualquiera"},
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
            {"", "Cualquiera"},
            {"LOW", "Baja"},
            {"MEDIUM", "Media"},
            {"HIGH", "Alta"},
            {"CRITICAL", "Crítica"}
        };
    }

    private String[][] getEventTypes() {
        return new String[][] {
            {"", "Todos"},
            {"COMPLAINT_CREATED", "Denuncia creada"},
            {"STATUS_CHANGED", "Estado cambiado"},
            {"COMPLAINT_CLASSIFIED", "Denuncia clasificada"},
            {"MORE_INFO_REQUESTED", "Información solicitada"},
            {"COMPLAINT_REJECTED", "Denuncia rechazada"},
            {"COMPLAINT_DERIVED", "Denuncia derivada"},
            {"RULE_CREATED", "Regla creada"},
            {"RULE_UPDATED", "Regla actualizada"},
            {"RULE_DELETED", "Regla eliminada"},
            {"LOGIN_SUCCESS", "Inicio de sesión"},
            {"LOGIN_FAILED", "Intento fallido"}
        };
    }
}

package com.vozsegura.vozsegura.controller.admin;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

import com.vozsegura.vozsegura.config.GatewayConfig;
import com.vozsegura.vozsegura.domain.entity.AuditEvent;
import com.vozsegura.vozsegura.domain.entity.DerivationRule;
import com.vozsegura.vozsegura.domain.entity.DestinationEntity;
import com.vozsegura.vozsegura.repo.AuditEventRepository;
import com.vozsegura.vozsegura.repo.DestinationEntityRepository;
import com.vozsegura.vozsegura.service.DerivationService;
import com.vozsegura.vozsegura.service.SystemConfigService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final DerivationService derivationService;
    private final AuditEventRepository auditEventRepository;
    private final DestinationEntityRepository destinationEntityRepository;
    private final SystemConfigService systemConfigService;
    private final GatewayConfig gatewayConfig;

    public AdminController(DerivationService derivationService,
                           AuditEventRepository auditEventRepository,
                           DestinationEntityRepository destinationEntityRepository,
                           SystemConfigService systemConfigService,
                           GatewayConfig gatewayConfig) {
        this.derivationService = derivationService;
        this.auditEventRepository = auditEventRepository;
        this.destinationEntityRepository = destinationEntityRepository;
        this.systemConfigService = systemConfigService;
        this.gatewayConfig = gatewayConfig;
    }

    @GetMapping({"", "/panel"})
    public String panel(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            log.info("📊 Loading admin panel for session: {}", session.getId());
            
            long totalRules = derivationService.findAllRules().size();
            long activeRules = derivationService.findActiveRules().size();

            model.addAttribute("totalRules", totalRules);
            model.addAttribute("activeRules", activeRules);

            log.info("✅ Admin panel loaded successfully - Total rules: {}, Active rules: {}", totalRules, activeRules);
            return "admin/panel";
        } catch (Exception e) {
            log.error("❌ Error loading admin panel for session: {}", session.getId(), e);
            model.addAttribute("errorMessage", "Error al cargar el panel. Por favor intenta nuevamente.");
            return "error/generic-error";
        }
    }

    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        List<DerivationRule> rules = derivationService.findAllRules();
        List<DestinationEntity> entidades = destinationEntityRepository.findByActiveTrueOrderByNameAsc();

        model.addAttribute("rules", rules);
        model.addAttribute("complaintTypes", systemConfigService.getComplaintTypesForRules());
        model.addAttribute("priorities", systemConfigService.getPrioritiesForRules());
        model.addAttribute("entidades", entidades);

        return "admin/reglas";
    }

    @PostMapping("/reglas/crear")
    public String crearRegla(
            @RequestParam("name") String name,
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
            @RequestParam(value = "priorityMatch", required = false) String priorityMatch,
            @RequestParam("destinationId") Long destinationId,
            @RequestParam(value = "description", required = false) String description,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        String username = getUsername(session);

        DerivationRule rule = new DerivationRule();
        rule.setName(name);
        rule.setSeverityMatch(emptyToNull(severityMatch));
        rule.setPriorityMatch(emptyToNull(priorityMatch));
        rule.setDestinationId(destinationId);
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
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
            @RequestParam(value = "priorityMatch", required = false) String priorityMatch,
            @RequestParam("destinationId") Long destinationId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);

            DerivationRule updated = new DerivationRule();
            updated.setName(name);
            updated.setSeverityMatch(emptyToNull(severityMatch));
            updated.setPriorityMatch(emptyToNull(priorityMatch));
            updated.setDestinationId(destinationId);
            updated.setDescription(description);
            updated.setActive(active);

            derivationService.updateRule(id, updated, username);
            redirectAttributes.addFlashAttribute("success", "Regla actualizada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al actualizar la regla: " + e.getMessage());
        }
        return "redirect:/admin/reglas";
    }

    @PostMapping("/reglas/{id}/eliminar")
    public String eliminarRegla(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);
            derivationService.deleteRule(id, username);
            redirectAttributes.addFlashAttribute("success", "Regla desactivada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al desactivar la regla: " + e.getMessage());
        }
        return "redirect:/admin/reglas";
    }

    @PostMapping("/reglas/{id}/activar")
    public String activarRegla(
            @PathVariable("id") Long id,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);
            derivationService.activateRule(id, username);
            redirectAttributes.addFlashAttribute("success", "Regla activada correctamente");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al activar la regla: " + e.getMessage());
        }
        return "redirect:/admin/reglas";
    }

    @GetMapping("/logs")
    public String logs(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model) {

        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
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
        model.addAttribute("eventTypes", systemConfigService.getEventTypesAsArray());
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());

        return "admin/logs";
    }

    @GetMapping("/revelacion")
    public String revelacion(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
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
}

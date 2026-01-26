package com.vozsegura.controller.admin;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.config.GatewayConfig;
import com.vozsegura.domain.entity.AuditEvent;
import com.vozsegura.domain.entity.DerivationRule;
import com.vozsegura.domain.entity.DestinationEntity;
import com.vozsegura.domain.entity.StaffUser;
import com.vozsegura.repo.AuditEventRepository;
import com.vozsegura.repo.DestinationEntityRepository;
import com.vozsegura.repo.PersonaRepository;
import com.vozsegura.repo.StaffUserRepository;
import com.vozsegura.service.AuditService;
import com.vozsegura.service.CryptoService;
import com.vozsegura.service.DerivationService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

/**
 * Controlador del panel administrativo.
 *
 * <p>Responsabilidades:</p>
 * <ul>
 *   <li>Administrar reglas de derivacion (CRUD).</li>
 *   <li>Visualizar auditoria de eventos.</li>
 *   <li>Mostrar paginas administrativas (configuracion, revelacion).</li>
 * </ul>
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>Acceso restringido a usuarios con rol ADMIN.</li>
 *   <li>La validacion se realiza por sesion HTTP.</li>
 * </ul>
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    private final DerivationService derivationService;
    private final AuditEventRepository auditEventRepository;
    private final DestinationEntityRepository destinationEntityRepository;
    private final StaffUserRepository staffUserRepository;
    private final PersonaRepository personaRepository;
    private final AuditService auditService;
    private final PasswordEncoder passwordEncoder;
    private final CryptoService cryptoService;
    private final GatewayConfig gatewayConfig;

    public AdminController(
            DerivationService derivationService,
            AuditEventRepository auditEventRepository,
            DestinationEntityRepository destinationEntityRepository,
            StaffUserRepository staffUserRepository,
            PersonaRepository personaRepository,
            AuditService auditService,
            PasswordEncoder passwordEncoder,
            CryptoService cryptoService,
            GatewayConfig gatewayConfig
    ) {
        this.derivationService = derivationService;
        this.auditEventRepository = auditEventRepository;
        this.destinationEntityRepository = destinationEntityRepository;
        this.staffUserRepository = staffUserRepository;
        this.personaRepository = personaRepository;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
        this.cryptoService = cryptoService;
        this.gatewayConfig = gatewayConfig;
    }

    @GetMapping({"", "/panel"})
    public String panel(HttpSession session, Model model) {
        // Logging detallado para debugging
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        String userType = (String) session.getAttribute("userType");
        log.info("panel(): authenticated={}, userType={}, sessionId={}", authenticated, userType, session.getId());

        if (!isAuthenticated(session)) {
            log.warn("panel(): Access denied - authenticated={}, userType={}", authenticated, userType);
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            log.info("Loading admin panel for session: {}", session.getId());

            long totalRules = derivationService.findAllRules().size();
            long activeRules = derivationService.findActiveRules().size();

            model.addAttribute("totalRules", totalRules);
            model.addAttribute("activeRules", activeRules);

            log.info("Admin panel loaded successfully - Total rules: {}, Active rules: {}", totalRules, activeRules);
            return "admin/panel";
        } catch (Exception e) {
            log.error("Error loading admin panel", e);
            model.addAttribute("errorMessage", "Error al cargar el panel. Por favor intenta nuevamente.");
            return "error/generic-error";
        }
    }

    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            List<DerivationRule> rules = derivationService.findAllRules();
            List<DestinationEntity> destinations = destinationEntityRepository.findByActiveTrueOrderByNameAsc();

            model.addAttribute("rules", rules);
            model.addAttribute("destinations", destinations);

            // Cargar políticas activas para el selector
            model.addAttribute("policies", derivationService.findActivePolicies());

            // Tipos y severidades con etiquetas en español
            model.addAttribute("complaintTypes", com.vozsegura.domain.enums.ComplaintType.values());
            model.addAttribute("severities", com.vozsegura.domain.enums.Severity.values());

        } catch (Exception e) {
            log.error("Error loading derivation rules", e);
            model.addAttribute("error", "Error al cargar reglas: " + e.getMessage());
            model.addAttribute("rules", java.util.Collections.emptyList());
            model.addAttribute("destinations", java.util.Collections.emptyList());
            model.addAttribute("policies", java.util.Collections.emptyList());
            model.addAttribute("complaintTypes", java.util.Collections.emptyList());
            model.addAttribute("severities", java.util.Collections.emptyList());
        }

        return "admin/reglas";
    }

    @PostMapping("/reglas/crear")
    public String crearRegla(
            @RequestParam("name") String name,
            @RequestParam(value = "policyId", required = false) Long policyId,
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
            @RequestParam(value = "complaintTypeMatch", required = false) String complaintTypeMatch,
            @RequestParam("destinationId") Long destinationId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "priorityOrder", required = false) Integer priorityOrder,
            @RequestParam(value = "requiresManualReview", defaultValue = "false") boolean requiresManualReview,
            @RequestParam(value = "slaHours", required = false) Integer slaHours,
            @RequestParam(value = "normativeReference", required = false) String normativeReference,
            @RequestParam(value = "conditions", required = false) String conditionsJson,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        String username = getUsername(session);

        // Si no se proporciona policyId, obtener la política activa por defecto
        if (policyId == null) {
            try {
                var activePolicies = derivationService.findActivePolicies();
                if (activePolicies.isEmpty()) {
                    redirectAttributes.addFlashAttribute("error", "No hay políticas activas. Por favor cree una política primero.");
                    return "redirect:/admin/reglas";
                }
                policyId = activePolicies.get(0).getId();
            } catch (Exception e) {
                log.error("Error getting active policy", e);
                redirectAttributes.addFlashAttribute("error", "Error al obtener política activa");
                return "redirect:/admin/reglas";
            }
        }

        DerivationRule rule = new DerivationRule();
        rule.setPolicyId(policyId);
        rule.setName(name);
        rule.setSeverityMatch(emptyToNull(severityMatch));
        rule.setComplaintTypeMatch(emptyToNull(complaintTypeMatch));
        rule.setDestinationId(destinationId);
        rule.setDescription(emptyToNull(description));
        rule.setActive(true);

        rule.setPriorityOrder(priorityOrder != null ? priorityOrder : 100);
        rule.setRequiresManualReview(requiresManualReview);
        rule.setSlaHours(slaHours);
        rule.setNormativeReference(emptyToNull(normativeReference));
        rule.setConditions(normalizeJson(conditionsJson));

        derivationService.createRule(rule, username);
        redirectAttributes.addFlashAttribute("success", "Regla creada correctamente");
        return "redirect:/admin/reglas";
    }

    @PostMapping("/reglas/{id}/editar")
    public String editarRegla(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam("policyId") Long policyId,
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
            @RequestParam(value = "complaintTypeMatch", required = false) String complaintTypeMatch,
            @RequestParam("destinationId") Long destinationId,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "active", defaultValue = "false") boolean active,
            @RequestParam(value = "priorityOrder", required = false) Integer priorityOrder,
            @RequestParam(value = "requiresManualReview", defaultValue = "false") boolean requiresManualReview,
            @RequestParam(value = "slaHours", required = false) Integer slaHours,
            @RequestParam(value = "normativeReference", required = false) String normativeReference,
            @RequestParam(value = "conditions", required = false) String conditionsJson,
            HttpSession session,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            String username = getUsername(session);

            DerivationRule updated = new DerivationRule();
            updated.setPolicyId(policyId);
            updated.setName(name);
            updated.setSeverityMatch(emptyToNull(severityMatch));
            updated.setComplaintTypeMatch(emptyToNull(complaintTypeMatch));
            updated.setDestinationId(destinationId);
            updated.setDescription(emptyToNull(description));
            updated.setActive(active);

            updated.setPriorityOrder(priorityOrder != null ? priorityOrder : 100);
            updated.setRequiresManualReview(requiresManualReview);
            updated.setSlaHours(slaHours);
            updated.setNormativeReference(emptyToNull(normativeReference));
            updated.setConditions(normalizeJson(conditionsJson));

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
            RedirectAttributes redirectAttributes
    ) {
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


    @GetMapping("/logs")
    public String logs(
            @RequestParam(value = "eventType", required = false) String eventType,
            @RequestParam(value = "page", defaultValue = "0") int page,
            HttpSession session,
            Model model
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        PageRequest pageRequest = PageRequest.of(page, 50, Sort.by(Sort.Direction.DESC, "eventTime"));
        Page<AuditEvent> events = (eventType != null && !eventType.isBlank())
                ? auditEventRepository.findByEventType(eventType, pageRequest)
                : auditEventRepository.findAll(pageRequest);

        // Obtener tipos REALES de la BD y traducirlos al español
        List<String> eventTypesRaw = auditEventRepository.findDistinctEventTypes();
        Map<String, String> eventTypesMap = new java.util.LinkedHashMap<>();
        eventTypesMap.put("", "Todos los eventos");
        for (String type : eventTypesRaw) {
            eventTypesMap.put(type, translateEventType(type));
        }

        model.addAttribute("events", events);
        model.addAttribute("selectedEventType", eventType);
        model.addAttribute("eventTypes", eventTypesMap);
        model.addAttribute("currentPage", page);
        model.addAttribute("totalPages", events.getTotalPages());

        return "admin/logs";
    }

    /**
     * Traduce los tipos de eventos técnicos a español legible.
     */
    private String translateEventType(String eventType) {
        if (eventType == null) return "Desconocido";

        return switch (eventType) {
            case "LOGIN_SUCCESS" -> "Inicio de sesión exitoso";
            case "LOGIN_FAILED" -> "Intento de inicio de sesión fallido";
            case "LOGOUT" -> "Cierre de sesión";
            case "SECRET_KEY_VALIDATION" -> "Validación de clave secreta";
            case "SECRET_KEY_FAILED" -> "Clave secreta incorrecta";
            case "OTP_SENT" -> "OTP enviado";
            case "OTP_VERIFY_SUCCESS" -> "OTP verificado exitosamente";
            case "OTP_VERIFY_FAILED" -> "OTP incorrecto";
            case "STAFF_CREATED" -> "Analista creado";
            case "STAFF_CREATION_REJECTED" -> "Creación de analista rechazada";
            case "STAFF_ENABLED" -> "Analista activado";
            case "STAFF_DISABLED" -> "Analista desactivado";
            case "DISABLED_USER_BLOCKED" -> "Usuario desactivado bloqueado";
            case "RULE_ACTIVATED" -> "Regla activada";
            case "RULE_DEACTIVATED" -> "Regla desactivada";
            case "RULE_CREATED" -> "Regla creada";
            case "COMPLAINT_CREATED" -> "Denuncia creada";
            case "COMPLAINT_DERIVED" -> "Denuncia derivada";
            case "EVIDENCE_DOWNLOADED" -> "Evidencia descargada";
            case "UNAUTHORIZED_ACCESS" -> "Acceso no autorizado";
            case "NOT_FOUND" -> "Página no encontrada (404)";
            case "SYSTEM_ERROR" -> "Error del sistema (500)";
            default -> eventType.replace("_", " ").toLowerCase();
        };
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

    /**
     * Normaliza el campo conditions a JSON válido.
     * Si está vacío o no es JSON válido, retorna objeto vacío {}.
     */
    private String normalizeJson(String json) {
        if (json == null || json.isBlank()) {
            return "{}";
        }

        String trimmed = json.trim();

        // Si ya es JSON válido (comienza con { o [)
        if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
            try {
                // Validar que sea JSON parseable
                new com.fasterxml.jackson.databind.ObjectMapper().readTree(trimmed);
                return trimmed;
            } catch (Exception e) {
                log.warn("Invalid JSON in conditions field, using empty object. Error: {}", e.getMessage());
                return "{}";
            }
        }

        // Si es texto plano, retornar objeto vacío (no intentar guardarlo como JSON)
        log.warn("Conditions field contains plain text instead of JSON, using empty object");
        return "{}";
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip != null ? ip.split(",")[0].trim() : "unknown";
    }

    // =========================================================
    // GESTIÓN DE ANALISTAS
    // =========================================================

    @GetMapping("/analistas")
    public String analistas(HttpSession session, HttpServletRequest request, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        // RBAC: Solo ADMIN puede gestionar analistas
        String userType = (String) session.getAttribute("userType");
        if (!"ADMIN".equals(userType)) {
            log.warn("Unauthorized access attempt to /admin/analistas by role: {}", userType);

            // Auditar intento no autorizado
            auditService.logSecurityEvent(
                "UNAUTHORIZED_ACCESS",
                "FAILURE",
                (Long) session.getAttribute("staffUserId"),
                (String) session.getAttribute("username"),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "attempted_path", "/admin/analistas",
                    "user_role", userType != null ? userType : "unknown",
                    "reason", "insufficient_privileges"
                )
            );

            model.addAttribute("error", "Acceso denegado. Solo administradores pueden gestionar analistas.");
            return "error/generic-error";
        }

        try {
            // FILTRAR: Solo mostrar analistas, NO admins
            List<StaffUser> analistas = staffUserRepository.findAll()
                .stream()
                .filter(staff -> "ANALYST".equals(staff.getRole()))
                .sorted((a, b) -> a.getUsername().compareTo(b.getUsername()))
                .toList();

            model.addAttribute("analistas", analistas);
            return "admin/analistas";
        } catch (Exception e) {
            log.error("Error loading analysts", e);
            model.addAttribute("error", "Error al cargar analistas");
            model.addAttribute("analistas", java.util.Collections.emptyList());
            return "admin/analistas";
        }
    }

    @PostMapping("/analistas/crear")
    public String crearAnalista(
            @RequestParam("username") String username,
            @RequestParam("role") String role,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "phone", required = false) String phone,
            @RequestParam("cedula") String cedula,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        // RBAC: Solo ADMIN puede crear analistas
        String userType = (String) session.getAttribute("userType");
        if (!"ADMIN".equals(userType)) {
            log.warn("Unauthorized attempt to create analyst by role: {}", userType);

            // Auditar intento no autorizado
            auditService.logSecurityEvent(
                "UNAUTHORIZED_STAFF_CREATION",
                "FAILURE",
                (Long) session.getAttribute("staffUserId"),
                (String) session.getAttribute("username"),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "attempted_username", username,
                    "attempted_role", role,
                    "user_role", userType != null ? userType : "unknown",
                    "reason", "insufficient_privileges"
                )
            );

            redirectAttributes.addFlashAttribute("error", "Acceso denegado. Solo administradores pueden crear analistas.");
            return "redirect:/admin/panel";
        }

        try {
            // Validar entrada para prevenir inyecciones
            if (username == null || username.isBlank() || username.length() > 100) {
                redirectAttributes.addFlashAttribute("error", "Username inválido");
                return "redirect:/admin/analistas";
            }

            if (role == null || role.isBlank()) {
                redirectAttributes.addFlashAttribute("error", "Rol requerido");
                return "redirect:/admin/analistas";
            }

            if (cedula == null || cedula.isBlank() || !cedula.matches("^[0-9]{10}$")) {
                redirectAttributes.addFlashAttribute("error", "Cédula inválida (debe ser 10 dígitos)");
                return "redirect:/admin/analistas";
            }

            // Sanitizar inputs
            username = username.trim();
            role = role.trim().toUpperCase();
            cedula = cedula.trim();

            // VALIDACIÓN CRÍTICA: Verificar que la cédula exista en registro civil
            String cedulaHash = cryptoService.hashCedula(cedula);
            boolean existsInRegistroCivil = personaRepository.findByCedulaHash(cedulaHash).isPresent();

            if (!existsInRegistroCivil) {
                log.warn("Attempt to create staff with cedula not in registro_civil: hash={}", cedulaHash.substring(0, 8) + "...");

                auditService.logSecurityEvent(
                    "STAFF_CREATION_REJECTED",
                    "FAILURE",
                    (Long) session.getAttribute("staffUserId"),
                    getUsername(session),
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of(
                        "reason", "cedula_not_in_registro_civil",
                        "attempted_username", username
                    )
                );

                redirectAttributes.addFlashAttribute("error",
                    "La cédula no existe en el Registro Civil. Solo se pueden crear usuarios con identidad verificada.");
                return "redirect:/admin/analistas";
            }

            if (staffUserRepository.findByUsername(username).isPresent()) {
                redirectAttributes.addFlashAttribute("error", "El username ya existe");
                return "redirect:/admin/analistas";
            }

            if (!role.equals("ADMIN") && !role.equals("ANALYST")) {
                log.warn("Invalid role attempted: {}", role);
                redirectAttributes.addFlashAttribute("error", "Rol inválido");
                return "redirect:/admin/analistas";
            }

            String tempPassword = java.util.UUID.randomUUID().toString().substring(0, 8);
            String passwordHash = passwordEncoder.encode(tempPassword);

            String secretKey = role.equals("ADMIN") ?
                System.getenv("VOZ_STAFF_ADMIN_SECRET") :
                System.getenv("VOZ_STAFF_ANALYST_SECRET");

            if (secretKey == null || secretKey.isBlank()) {
                secretKey = "VozSegura2026" + role + "!";
            }
            String secretKeyHash = passwordEncoder.encode(secretKey);

            StaffUser staff = new StaffUser();
            staff.setUsername(username);
            staff.setPasswordHash(passwordHash);
            staff.setRole(role);
            staff.setEnabled(true);

            if (email != null && !email.isBlank()) {
                staff.setEmailEncrypted(cryptoService.encryptPII(email.trim()));
            }
            if (phone != null && !phone.isBlank()) {
                staff.setPhoneEncrypted(cryptoService.encryptPII(phone.trim()));
            }

            staff.setCedulaHashIdx(cryptoService.hashCedula(cedula));
            staff.setMfaSecretEncrypted(secretKeyHash);

            Long adminId = (Long) session.getAttribute("staffUserId");
            if (adminId != null) {
                staff.setCreatedBy(adminId);
            }

            staffUserRepository.save(staff);

            auditService.logSecurityEvent(
                "STAFF_CREATED",
                "SUCCESS",
                adminId,
                getUsername(session),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "new_staff_username", username,
                    "new_staff_role", role,
                    "created_by_admin_id", adminId != null ? adminId : "unknown"
                )
            );

            redirectAttributes.addFlashAttribute("success",
                "Analista creado. Password temporal: " + tempPassword + " (guárdelo de forma segura)");
            return "redirect:/admin/analistas";

        } catch (Exception e) {
            log.error("Error creating analyst", e);

            // Auditar error
            auditService.logSecurityEvent(
                "STAFF_CREATION_ERROR",
                "FAILURE",
                (Long) session.getAttribute("staffUserId"),
                getUsername(session),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "attempted_username", username,
                    "error_message", e.getMessage()
                )
            );

            redirectAttributes.addFlashAttribute("error", "Error al crear analista: " + e.getMessage());
            return "redirect:/admin/analistas";
        }
    }

    @PostMapping("/analistas/{id}/toggle")
    public String toggleAnalista(
            @PathVariable("id") Long id,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        // RBAC: Solo ADMIN puede activar/desactivar analistas
        String userType = (String) session.getAttribute("userType");
        if (!"ADMIN".equals(userType)) {
            log.warn("Unauthorized attempt to toggle analyst by role: {}", userType);

            // Auditar intento no autorizado
            auditService.logSecurityEvent(
                "UNAUTHORIZED_STAFF_TOGGLE",
                "FAILURE",
                (Long) session.getAttribute("staffUserId"),
                (String) session.getAttribute("username"),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "attempted_staff_id", id,
                    "user_role", userType != null ? userType : "unknown",
                    "reason", "insufficient_privileges"
                )
            );

            redirectAttributes.addFlashAttribute("error", "Acceso denegado. Solo administradores pueden modificar analistas.");
            return "redirect:/admin/panel";
        }

        try {
            StaffUser staff = staffUserRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Usuario no encontrado"));

            boolean newStatus = !staff.isEnabled();
            staff.setEnabled(newStatus);
            staffUserRepository.save(staff);

            Long adminId = (Long) session.getAttribute("staffUserId");
            auditService.logSecurityEvent(
                newStatus ? "STAFF_ENABLED" : "STAFF_DISABLED",
                "SUCCESS",
                adminId,
                getUsername(session),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "target_staff_id", id,
                    "target_staff_username", staff.getUsername(),
                    "target_staff_role", staff.getRole(),
                    "new_status", newStatus,
                    "admin_id", adminId != null ? adminId : "unknown"
                )
            );

            redirectAttributes.addFlashAttribute("success",
                "Usuario " + (newStatus ? "activado" : "desactivado") + " correctamente");
            return "redirect:/admin/analistas";

        } catch (Exception e) {
            log.error("Error toggling analyst", e);

            // Auditar error
            auditService.logSecurityEvent(
                "STAFF_TOGGLE_ERROR",
                "FAILURE",
                (Long) session.getAttribute("staffUserId"),
                getUsername(session),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "target_staff_id", id,
                    "error_message", e.getMessage()
                )
            );

            redirectAttributes.addFlashAttribute("error", "Error al cambiar estado: " + e.getMessage());
            return "redirect:/admin/analistas";
        }
    }

    // =========================================================
    // AUDITORÍA DE REGLAS
    // =========================================================

    @PostMapping("/reglas/{id}/activar")
    public String activarRegla(
            @PathVariable("id") Long id,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            derivationService.activateRule(id);

            // Auditar (no debe interrumpir el flujo si falla)
            try {
                Long adminId = (Long) session.getAttribute("staffUserId");
                auditService.logSecurityEvent(
                    "RULE_ACTIVATED",
                    "SUCCESS",
                    adminId,
                    getUsername(session),
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of("rule_id", id)
                );
            } catch (Exception auditEx) {
                log.error("Failed to log audit event (rule activated), but continuing: {}", auditEx.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Regla activada correctamente");
            return "redirect:/admin/reglas";
        } catch (Exception e) {
            log.error("Error activating rule", e);
            redirectAttributes.addFlashAttribute("error", "Error al activar regla: " + e.getMessage());
            return "redirect:/admin/reglas";
        }
    }

    @PostMapping("/reglas/{id}/desactivar")
    public String desactivarRegla(
            @PathVariable("id") Long id,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes
    ) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            derivationService.deactivateRule(id);

            // Auditar (no debe interrumpir el flujo si falla)
            try {
                Long adminId = (Long) session.getAttribute("staffUserId");
                auditService.logSecurityEvent(
                    "RULE_DEACTIVATED",
                    "SUCCESS",
                    adminId,
                    getUsername(session),
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of("rule_id", id)
                );
            } catch (Exception auditEx) {
                log.error("Failed to log audit event (rule deactivated), but continuing: {}", auditEx.getMessage());
            }

            redirectAttributes.addFlashAttribute("success", "Regla desactivada correctamente");
            return "redirect:/admin/reglas";
        } catch (Exception e) {
            log.error("Error deactivating rule", e);
            redirectAttributes.addFlashAttribute("error", "Error al desactivar regla: " + e.getMessage());
            return "redirect:/admin/reglas";
        }
    }
}

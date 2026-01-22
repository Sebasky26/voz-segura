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

/**
 * Controlador para panel administrativo de Voz Segura.
 * 
 * Proposito:
 * - Gestionar reglas de derivacion de denuncias
 * - Visualizar auditoria de eventos del sistema
 * - Solicitar revelacion excepcional de identidades
 * - Cambiar configuraciones del sistema
 * 
 * Permisos requeridos:
 * - Usuario autenticado como ADMIN
 * - Validacion en cada endpoint con isAuthenticated()
 * 
 * Rutas principales:
 * - GET /admin o /admin/panel -> Dashboard principal
 * - GET /admin/reglas -> Listar reglas de derivacion
 * - POST /admin/reglas/crear -> Crear nueva regla
 * - POST /admin/reglas/{id}/editar -> Editar regla
 * - POST /admin/reglas/{id}/eliminar -> Desactivar regla (soft-delete)
 * - POST /admin/reglas/{id}/activar -> Reactivar regla
 * - GET /admin/logs -> Ver auditoria de eventos
 * - GET /admin/revelacion -> Solicitar revelacion de identidad
 * 
 * Seguridad:
 * - RBAC: Solo usuarios con role ADMIN
 * - Validacion de sesion en cada metodo
 * - Todos los cambios son auditados (DerivationService registra en AuditService)
 * - Soft-delete: Las reglas no se borran, se marcan como inactive
 * 
 * Notas:
 * - Controlador MVC (retorna plantillas HTML, no JSON)
 * - Usa RedirectAttributes para mensajes flash (success/error)
 * - Paginacion de auditoria (50 eventos por pagina)
 * 
 * @see com.vozsegura.vozsegura.service.DerivationService
 * @see com.vozsegura.vozsegura.service.SystemConfigService
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    /**
     * Logger de esta clase.
     */
    private static final Logger log = LoggerFactory.getLogger(AdminController.class);

    /**
     * Servicio de derivacion de denuncias.
     * Maneja la logica de CRUD de reglas de derivacion.
     */
    private final DerivationService derivationService;

    /**
     * Repositorio de eventos de auditoria.
     * Permite consultar logs del sistema.
     */
    private final AuditEventRepository auditEventRepository;

    /**
     * Repositorio de entidades destino.
     * Lista instituciones que pueden recibir derivaciones.
     */
    private final DestinationEntityRepository destinationEntityRepository;

    /**
     * Servicio de configuracion del sistema.
     * Obtiene listas de valores (tipos de denuncia, prioridades, etc.).
     */
    private final SystemConfigService systemConfigService;

    /**
     * Configuracion del gateway.
     * Maneja redireccion a login si sesion esta expirada.
     */
    private final GatewayConfig gatewayConfig;

    /**
     * Constructor con inyeccion de dependencias.
     * 
     * @param derivationService Servicio de derivacion
     * @param auditEventRepository Repositorio de auditoria
     * @param destinationEntityRepository Repositorio de entidades
     * @param systemConfigService Servicio de configuracion
     * @param gatewayConfig Configuracion del gateway
     */
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

    /**
     * GET /admin o /admin/panel - Dashboard principal.
     * 
     * Proposito:
     * - Mostrar resumen del sistema (estadisticas)
     * - Total de reglas de derivacion
     * - Total de reglas activas
     * 
     * @param session Sesion HTTP (para verificar autenticacion)
     * @param model Modelo de Spring para pasar datos a la plantilla
     * @return String: Nombre de plantilla ("admin/panel")
     */
    @GetMapping({"", "/panel"})
    public String panel(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            log.debug("Loading admin panel");

            long totalRules = derivationService.findAllRules().size();
            long activeRules = derivationService.findActiveRules().size();

            model.addAttribute("totalRules", totalRules);
            model.addAttribute("activeRules", activeRules);

            log.debug("Admin panel loaded successfully - Total rules: {}, Active rules: {}", totalRules, activeRules);
            return "admin/panel";
        } catch (Exception e) {
            log.error("Error loading admin panel", e);
            model.addAttribute("errorMessage", "Error al cargar el panel. Por favor intenta nuevamente.");
            return "error/generic-error";
        }
    }

    /**
     * GET /admin/reglas - Listar todas las reglas de derivacion.
     * 
     * Proposito:
     * - Mostrar tabla de reglas de derivacion
     * - Permitir CRUD de reglas
     * - Mostrar entidades destino disponibles
     * 
     * Flujo:
     * 1. Verificar que usuario sea ADMIN
     * 2. Obtener todas las reglas
     * 3. Obtener entidades destino activas
     * 4. Obtener valores de configuracion (tipos de denuncia, prioridades)
     * 5. Pasar todo a la plantilla
     * 
     * @param session Sesion HTTP
     * @param model Modelo de Spring
     * @return String: Nombre de plantilla ("admin/reglas")
     */
    @GetMapping("/reglas")
    public String reglas(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }

        try {
            List<DerivationRule> rules = derivationService.findAllRules();
            List<DestinationEntity> entidades = destinationEntityRepository.findByActiveTrueOrderByNameAsc();

            model.addAttribute("rules", rules);
            model.addAttribute("complaintTypes", systemConfigService.getComplaintTypesList());
            model.addAttribute("severities", systemConfigService.getSeveritiesList());
            model.addAttribute("entidades", entidades);
        } catch (Exception e) {
            log.error("Error loading derivation rules", e);
            model.addAttribute("error", "Error al cargar reglas: " + e.getMessage());
            model.addAttribute("rules", java.util.Collections.emptyList());
            model.addAttribute("entidades", java.util.Collections.emptyList());
            model.addAttribute("complaintTypes", java.util.Collections.emptyList());
            model.addAttribute("severities", java.util.Collections.emptyList());
        }

        return "admin/reglas";
    }

    /**
     * POST /admin/reglas/crear - Crear nueva regla de derivacion.
     * 
     * Proposito:
     * - Crear una nueva regla que define como derivar denuncias
     * 
     * Parametros del formulario:
     * - name: Nombre descriptivo de la regla (ej: "Severidad Alta -> OIJ")
     * - severityMatch: Severidad a la que aplica (LOW, MEDIUM, HIGH, CRITICAL) [opcional]
     * - destinationId: ID de entidad destino que recibe derivaciones
     * - description: Descripcion de la regla (notas, criterios) [opcional]
     *
     * Flujo:
     * 1. Verificar autenticacion
     * 2. Crear objeto DerivationRule
     * 3. Guardar con DerivationService (registra en AuditEvent)
     * 4. Redirigir a /admin/reglas con mensaje de exito
     *
     * Auditoria:
     * - Se registra con DerivationService.createRule(rule, username)
     * - Se guarda username del admin que creo
     *
     * @param name Nombre de la regla
     * @param severityMatch Severidad [opcional]
     * @param destinationId ID de entidad destino
     * @param description Descripcion [opcional]
     * @param session Sesion HTTP
     * @param redirectAttributes Para pasar mensajes flash
     * @return Redireccion a /admin/reglas
     */
    @PostMapping("/reglas/crear")
    public String crearRegla(
            @RequestParam("name") String name,
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
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
        rule.setDestinationId(destinationId);
        rule.setDescription(description);
        rule.setActive(true);

        derivationService.createRule(rule, username);
        redirectAttributes.addFlashAttribute("success", "Regla creada correctamente");
        return "redirect:/admin/reglas";
    }

    /**
     * POST /admin/reglas/{id}/editar - Editar regla de derivacion existente.
     * 
     * Proposito:
     * - Actualizar parametros de una regla
     * - Cambiar entidad destino
     * - Modificar criterios (severidad)
     *
     * Parametros:
     * - id (PathVariable): ID de la regla a editar
     * - name: Nuevo nombre
     * - severityMatch: Nueva severidad [opcional]
     * - destinationId: Nueva entidad destino
     * - description: Nueva descripcion [opcional]
     * - active: true/false para habilitar/deshabilitar
     *
     * @param id ID de la regla (PathVariable)
     * @param name Nuevo nombre
     * @param severityMatch Severidad [opcional]
     * @param destinationId Entidad destino
     * @param description Descripcion [opcional]
     * @param active true/false
     * @param session Sesion HTTP
     * @param redirectAttributes Para mensajes
     * @return Redireccion a /admin/reglas
     */
    @PostMapping("/reglas/{id}/editar")
    public String editarRegla(
            @PathVariable("id") Long id,
            @RequestParam("name") String name,
            @RequestParam(value = "severityMatch", required = false) String severityMatch,
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

    /**
     * POST /admin/reglas/{id}/eliminar - Desactivar regla (soft-delete).
     * 
     * Proposito:
     * - Marcar una regla como inactive sin borrar sus datos
     * - Mantener auditoria historica
     * 
     * Nota:
     * - NO borra la regla de la BD
     * - Solo pone active=false
     * - La regla NO se usara mas en derivaciones
     * 
     * @param id ID de la regla a desactivar
     * @param session Sesion HTTP
     * @param redirectAttributes Para mensajes
     * @return Redireccion a /admin/reglas
     */
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

    /**
     * POST /admin/reglas/{id}/activar - Reactivar regla desactivada.
     * 
     * Proposito:
     * - Cambiar active=false a active=true
     * - Permitir que la regla vuelva a ser usada en derivaciones
     * 
     * @param id ID de la regla a activar
     * @param session Sesion HTTP
     * @param redirectAttributes Para mensajes
     * @return Redireccion a /admin/reglas
     */
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

    /**
     * GET /admin/logs - Ver auditoria de eventos del sistema.
     * 
     * Proposito:
     * - Mostrar log de eventos de auditoria
     * - Filtrar por tipo de evento
     * - Paginacion (50 eventos por pagina)
     * 
     * Parametros:
     * - eventType: Tipo de evento a filtrar [opcional]
     * - page: Numero de pagina (default 0)
     * 
     * Flujo:
     * 1. Verificar autenticacion
     * 2. Si eventType esta especificado: filtrar por tipo
     * 3. Si no: mostrar TODOS los eventos
     * 4. Ordenar por eventTime DESC (mas recientes primero)
     * 5. Paginar: 50 eventos por pagina
     * 6. Pasar a la plantilla
     * 
     * Seguridad:
     * - Los usernames estan hasheados (imposible identificar quien hizo que)
     * - Solo admins pueden ver logs
     * 
     * @param eventType Tipo de evento a filtrar [opcional]
     * @param page Numero de pagina
     * @param session Sesion HTTP
     * @param model Modelo de Spring
     * @return String: Nombre de plantilla ("admin/logs")
     */
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

    /**
     * GET /admin/revelacion - Pagina para solicitar revelacion excepcional de identidad.
     * 
     * Proposito:
     * - Mostrar formulario para solicitar revelacion de identidad de un ciudadano
     * - Requerimientos legales (orden judicial, etc.)
     * - Auditoria reforzada
     * 
     * Nota:
     * - Esta es la pagina de SOLICITUD
     * - El proceso de aprobacion es doble control (dos admins deben aprobar)
     * - Ver IdentityRevealService para logica completa
     * 
     * @param session Sesion HTTP
     * @param model Modelo de Spring
     * @return String: Nombre de plantilla ("admin/revelacion")
     */
    @GetMapping("/revelacion")
    public String revelacion(HttpSession session, Model model) {
        if (!isAuthenticated(session)) {
            return gatewayConfig.redirectToSessionExpired();
        }
        return "admin/revelacion";
    }

    /**
     * Valida que el usuario este autenticado como ADMIN.
     * 
     * @param session Sesion HTTP
     * @return boolean: true si autenticado como ADMIN, false en caso contrario
     */
    private boolean isAuthenticated(HttpSession session) {
        Boolean auth = (Boolean) session.getAttribute("authenticated");
        String userType = (String) session.getAttribute("userType");
        return auth != null && auth && "ADMIN".equals(userType);
    }

    /**
     * Obtiene el username del usuario en sesion.
     * 
     * @param session Sesion HTTP
     * @return String: username, o "ADMIN" si no esta definido
     */
    private String getUsername(HttpSession session) {
        String username = (String) session.getAttribute("username");
        return username != null ? username : "ADMIN";
    }

    /**
     * Convierte cadenas vacias o en blanco a null.
     * 
     * @param value Valor a convertir
     * @return null si value es vacio/blanco, value en caso contrario
     */
    private String emptyToNull(String value) {
        return (value == null || value.isBlank()) ? null : value;
    }
}

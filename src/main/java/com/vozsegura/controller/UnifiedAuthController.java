package com.vozsegura.controller;

import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.dto.forms.SecretKeyForm;
import com.vozsegura.dto.forms.UnifiedLoginForm;
import com.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.domain.entity.StaffUser;
import com.vozsegura.repo.DiditVerificationRepository;
import com.vozsegura.repo.StaffUserRepository;
import com.vozsegura.service.CloudflareTurnstileService;
import com.vozsegura.service.CryptoService;
import com.vozsegura.service.DiditService;
import com.vozsegura.service.JwtTokenProvider;
import com.vozsegura.service.OtpService;
import com.vozsegura.service.AuditService;
import com.vozsegura.service.UnifiedAuthService;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador de autenticación unificada.
 *
 * <p>Este controlador centraliza los flujos de acceso de la aplicación:</p>
 *
 * <ul>
 *   <li><b>Denunciante</b>: completa verificación (incluyendo captcha y términos) y accede al flujo público.</li>
 *   <li><b>Staff/Administración</b>: completa verificación biométrica + clave secreta + OTP (MFA).</li>
 * </ul>
 *
 * <p>La sesión HTTP se usa para mantener estado durante el flujo (por ejemplo: sessionId de Didit,
 * token OTP temporal, rol detectado, etc.). Al finalizar se limpia lo temporal.</p>
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>No se exponen secretos en endpoints de diagnóstico.</li>
 *   <li>Turnstile se valida contra IP para mitigar automatización.</li>
 *   <li>La cédula se convierte a hash para búsquedas y tokens; se evita mantenerla en claro.</li>
 *   <li>JWT se guarda en cookie HttpOnly (ajustar Secure/SameSite según despliegue).</li>
 * </ul>
 */
@Slf4j
@Controller
@RequestMapping("/auth")
public class UnifiedAuthController {

    private static final String ROLE_ADMIN = "ADMIN";
    private static final String ROLE_ANALYST = "ANALYST";
    private static final String ROLE_DENUNCIANTE = "DENUNCIANTE";

    private final UnifiedAuthService unifiedAuthService;
    private final CloudflareTurnstileService turnstileService;
    private final JwtTokenProvider jwtTokenProvider;
    private final DiditService diditService;
    private final DiditVerificationRepository diditVerificationRepository;
    private final StaffUserRepository staffUserRepository;
    private final CryptoService cryptoService;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final OtpService otpService;
    private final AuditService auditService;

    public UnifiedAuthController(
            UnifiedAuthService unifiedAuthService,
            CloudflareTurnstileService turnstileService,
            JwtTokenProvider jwtTokenProvider,
            DiditService diditService,
            DiditVerificationRepository diditVerificationRepository,
            StaffUserRepository staffUserRepository,
            CryptoService cryptoService,
            org.springframework.security.crypto.password.PasswordEncoder passwordEncoder,
            OtpService otpService,
            AuditService auditService) {
        this.unifiedAuthService = unifiedAuthService;
        this.turnstileService = turnstileService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.diditService = diditService;
        this.diditVerificationRepository = diditVerificationRepository;
        this.staffUserRepository = staffUserRepository;
        this.cryptoService = cryptoService;
        this.passwordEncoder = passwordEncoder;
        this.otpService = otpService;
        this.auditService = auditService;
    }

    /**
     * Página principal de login - REDIRIGE AL FLUJO DIDIT.
     *
     * <p>El formulario de cédula/código dactilar está OBSOLETO.
     * Ahora el flujo correcto es verificación biométrica con Didit.</p>
     */
    @GetMapping("/login")
    public String showLoginPage() {
        return "redirect:/verification/inicio";
    }

    /**
     * Procesa login unificado inicial.
     *
     * <p>Valida:</p>
     * <ul>
     *   <li>Formulario</li>
     *   <li>Turnstile</li>
     *   <li>Verificación de identidad contra el servicio interno (Registro Civil / lógica definida en UnifiedAuthService)</li>
     * </ul>
     *
     * <p>En función del tipo de usuario:</p>
     * <ul>
     *   <li>ADMIN/ANALYST: redirige a verificación biométrica (Didit) o a clave secreta, según tu flujo.</li>
     *   <li>DENUNCIANTE: pasa al flujo de verificación/denuncia.</li>
     * </ul>
     */
    /* DESHABILITADO TEMPORALMENTE - REQUIERE REFACTORIZACION
     *
     * Este metodo usa codigo obsoleto:
     * - verifyCitizenIdentity() obsoleto
     * - getUserType() obsoleto
     *
     * NUEVO FLUJO REQUERIDO:
     * - Denunciantes usan flujo Didit directo
     * - Staff usa formulario separado username + password
     *
     * Ver EXECUTION_PLAN.md para codigo completo
     */
    /*
    @PostMapping("/unified-login")
    public String processUnifiedLogin(
            @Valid @ModelAttribute("unifiedLoginForm") UnifiedLoginForm form,
            BindingResult result,
            HttpSession session,
            HttpServletRequest request,
            Model model) {

        if (result.hasErrors()) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Por favor complete todos los campos correctamente.");
            return "public/denuncia-login";
        }

        String turnstileToken = request.getParameter("cf-turnstile-response");
        if (turnstileToken == null || turnstileToken.isBlank()) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Verificación requerida. Intente nuevamente.");
            return "public/denuncia-login";
        }

        String clientIp = getClientIp(request);
        if (!turnstileService.verifyTurnstileToken(turnstileToken, clientIp)) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "No se pudo verificar la solicitud. Intente nuevamente.");
            return "public/denuncia-login";
        }

        try {
            String cedula = form.getCedula();

            // Verificación de identidad (tu servicio define cómo)
            String citizenRef = unifiedAuthService.verifyCitizenIdentity(
                    cedula,
                    form.getCodigoDactilar()
            );

            session.setAttribute("citizenRef", citizenRef);

            UnifiedAuthService.UserType userType = unifiedAuthService.getUserType(cedula);
            session.setAttribute("userType", normalizeUserType(userType.name()));

            // Para búsquedas internas siempre trabajar con hash
            String cedulaHash = cryptoService.hashCedula(cedula);
            session.setAttribute("cedulaHash", cedulaHash);

            // Evitar mantener cédula en claro más tiempo del necesario
            session.removeAttribute("cedula");

            if (userType == UnifiedAuthService.UserType.ADMIN || userType == UnifiedAuthService.UserType.ANALYST) {
                // Si tu flujo staff inicia en Didit:
                return "redirect:/auth/verify-start";
            }

            // Denunciante: puede ir a verificación/confirmación según tu UX
            session.setAttribute("authenticated", true);
            session.setAttribute("citizenHash", cedulaHash);
            return "redirect:/denuncia/opciones";

        } catch (SecurityException e) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", e.getMessage());
            return "public/denuncia-login";
        } catch (Exception e) {
            log.error("Error en unified-login", e);
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Error al procesar la autenticación. Intente nuevamente.");
            return "public/denuncia-login";
        }
    }
    */ // FIN METODO DESHABILITADO

    /**
     * Inicia una sesión de verificación en Didit y redirige al usuario a la URL de verificación.
     *
     * <p>Guarda en sesión el {@code diditSessionId} para correlacionar luego con el webhook y/o callback.</p>
     */
    @GetMapping("/verify-start")
    public String startDiditVerification(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            String vendorData = session.getId();
            Map<String, Object> response = diditService.createVerificationSession(vendorData);

            if (response == null || !response.containsKey("session_id") || !response.containsKey("url")) {
                redirectAttributes.addFlashAttribute("error", "No se pudo iniciar la verificación biométrica.");
                return "redirect:/auth/login";
            }

            String diditSessionId = (String) response.get("session_id");
            String verificationUrl = (String) response.get("url");

            session.setAttribute("diditSessionId", diditSessionId);
            return "redirect:" + verificationUrl;

        } catch (Exception e) {
            log.error("Error iniciando verificación Didit", e);
            redirectAttributes.addFlashAttribute("error", "No se pudo iniciar la verificación. Intente nuevamente.");
            return "redirect:/auth/login";
        }
    }

    /**
     * Callback de Didit después de verificación biométrica.
     *
     * Flujo:
     * 1. Obtiene session_id de Didit (URL param o sesión HTTP)
     * 2. Busca verificación en BD (insertada por webhook)
     * 3. Obtiene documentHash de la verificación
     * 4. Busca si el hash corresponde a staff (cedula_hash_idx)
     * 5. Si es staff → pedir clave secreta
     * 6. Si no es staff (denunciante) → ir a opciones de denuncia
     *
     * Seguridad:
     * - No loguea documentHash completo
     * - Valida que verificación existe y está aprobada
     * - Timeout de 2 minutos para que webhook procese
     */
    @GetMapping("/verify-callback")
    public String handleDiditCallback(
            HttpSession session,
            Model model,
            RedirectAttributes redirectAttributes,
            @RequestParam(name = "session_id", required = false) String sessionIdParam) {

        try {
            // Obtener session_id de Didit (de URL o de sesión HTTP)
            String diditSessionId = (sessionIdParam != null && !sessionIdParam.isBlank())
                    ? sessionIdParam
                    : (String) session.getAttribute("diditSessionId");

            if (diditSessionId == null || diditSessionId.isBlank()) {
                log.warn("verify-callback: No didit session_id found");
                redirectAttributes.addFlashAttribute("error", "Sesión de verificación no encontrada.");
                return "redirect:/verification/inicio";
            }

            // Esperar un poco para que el webhook procese (si aún no lo hizo)
            Optional<DiditVerification> verificationOpt = waitForVerification(diditSessionId);

            if (verificationOpt.isEmpty()) {
                log.warn("verify-callback: Verification not found after waiting");
                redirectAttributes.addFlashAttribute("error",
                        "No se pudo confirmar la verificación. Por favor intente nuevamente.");
                return "redirect:/verification/inicio";
            }

            DiditVerification verification = verificationOpt.get();

            // Validar que la verificación fue aprobada
            if (!"VERIFIED".equalsIgnoreCase(verification.getVerificationStatus())) {
                log.warn("verify-callback: Verification status is not VERIFIED");
                redirectAttributes.addFlashAttribute("error",
                        "La verificación no fue aprobada. Por favor intente nuevamente.");
                return "redirect:/verification/inicio";
            }

            String documentHash = verification.getDocumentNumberHash();
            Long identityVaultId = verification.getIdentityVaultId();

            if (documentHash == null || documentHash.isBlank() || identityVaultId == null) {
                log.error("verify-callback: Invalid verification data");
                redirectAttributes.addFlashAttribute("error", "Verificación inválida. Intente nuevamente.");
                return "redirect:/verification/inicio";
            }

            log.info("verify-callback: Verification found, checking role");

            // Determinar rol: si existe en staff, es staff; si no, es denunciante
            Optional<StaffUser> staffUserOpt = staffUserRepository.findByCedulaHashIdx(documentHash);

            String userRole;
            String redirectButton;
            String redirectUrl;

            if (staffUserOpt.isPresent()) {
                // Es STAFF (admin o analyst)
                StaffUser staffUser = staffUserOpt.get();
                userRole = staffUser.getRole();

                log.info("verify-callback: User is STAFF with role: {}", userRole);

                // Guardar en sesión para flujo de clave secreta
                session.setAttribute("staffCedulaHash", documentHash);
                session.setAttribute("staffRole", userRole);
                session.setAttribute("staffUserId", staffUser.getId());
                session.setAttribute("verifiedStaffRole", userRole);

                // Configurar botón y URL para staff
                if ("ADMIN".equals(userRole)) {
                    redirectButton = "Ir a Panel de Administración";
                    redirectUrl = "/auth/secret-key"; // Primero clave secreta
                } else {
                    redirectButton = "Ir a Panel de Casos";
                    redirectUrl = "/auth/secret-key"; // Primero clave secreta
                }

            } else {
                // Es DENUNCIANTE
                log.info("verify-callback: User is DENUNCIANTE");
                userRole = ROLE_DENUNCIANTE;

                // Guardar en sesión para flujo de denuncia
                session.setAttribute("authenticated", true);
                session.setAttribute("citizenHash", documentHash);
                session.setAttribute("identityVaultId", identityVaultId);
                session.setAttribute("userType", ROLE_DENUNCIANTE);

                // Configurar botón y URL para denunciante
                redirectButton = "Continuar con mi Denuncia";
                redirectUrl = "/denuncia/opciones";
            }

            // Guardar datos para la pantalla de confirmación
            session.setAttribute("verifiedDocumentHash", documentHash);
            session.setAttribute("diditSessionId", diditSessionId);

            // Preparar modelo para la vista de confirmación
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("staffRole", userRole);
            model.addAttribute("staffRedirectUrl", redirectUrl);
            model.addAttribute("staffRedirectButton", redirectButton);

            // TODOS van primero a la pantalla de confirmación/términos
            return "auth/verify-confirm";

        } catch (Exception e) {
            log.error("Error en verify-callback", e);
            redirectAttributes.addFlashAttribute("error", "No se pudo procesar la verificación.");
            return "redirect:/verification/inicio";
        }
    }

    /**
     * Espera hasta 2 minutos para que el webhook de Didit procese la verificación.
     * Reintenta cada 2 segundos.
     */
    private Optional<DiditVerification> waitForVerification(String diditSessionId) {
        int maxAttempts = 60; // 60 intentos * 2 seg = 2 minutos
        int attemptDelay = 2000; // 2 segundos

        for (int i = 0; i < maxAttempts; i++) {
            Optional<DiditVerification> verificationOpt =
                diditVerificationRepository.findByDiditSessionId(diditSessionId);

            if (verificationOpt.isPresent()) {
                return verificationOpt;
            }

            try {
                Thread.sleep(attemptDelay);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return Optional.empty();
            }
        }

        return Optional.empty();
    }

    /**
     * Procesa la confirmación de verificación y aceptación de términos.
     *
     * Este método se ejecuta cuando el usuario hace clic en el botón de la pantalla
     * de confirmación (verify-confirm.html).
     *
     * Flujo:
     * 1. Valida Turnstile (anti-bot)
     * 2. Valida que se aceptaron términos
     * 3. Obtiene rol de la sesión
     * 4. Si es staff → redirige a /auth/secret-key (pedir clave secreta)
     * 5. Si es denunciante → redirige a /denuncia/opciones
     */
    @PostMapping("/verify-complete")
    public String completeVerification(
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {

        try {
            // Validar Turnstile
            String turnstileToken = request.getParameter("cf-turnstile-response");
            if (turnstileToken == null || turnstileToken.isBlank()) {
                model.addAttribute("error", "Verificación anti-bot requerida.");
                return prepareVerifyConfirmModel(session, model);
            }

            String clientIp = getClientIp(request);
            if (!turnstileService.verifyTurnstileToken(turnstileToken, clientIp)) {
                model.addAttribute("error", "Verificación anti-bot fallida. Intente nuevamente.");
                return prepareVerifyConfirmModel(session, model);
            }

            // Validar aceptación de términos
            String acceptTerms = request.getParameter("termsAccepted");
            if (acceptTerms == null || !acceptTerms.equals("on")) {
                model.addAttribute("error", "Debe aceptar los términos y condiciones.");
                return prepareVerifyConfirmModel(session, model);
            }

            // Obtener datos de sesión
            String documentHash = (String) session.getAttribute("verifiedDocumentHash");
            String role = (String) session.getAttribute("verifiedStaffRole");

            if (documentHash == null) {
                redirectAttributes.addFlashAttribute("error", "Sesión expirada. Intente nuevamente.");
                return "redirect:/verification/inicio";
            }

            // Si no hay rol, es denunciante
            if (role == null || ROLE_DENUNCIANTE.equals(role)) {
                // Flujo denunciante: ya tiene sesión configurada, solo redirigir
                Long identityVaultId = (Long) session.getAttribute("identityVaultId");

                if (identityVaultId == null) {
                    // Fallback: buscar por documentHash
                    identityVaultId = diditVerificationRepository.findByDocumentNumberHash(documentHash)
                            .map(DiditVerification::getIdentityVaultId)
                            .orElse(null);
                    session.setAttribute("identityVaultId", identityVaultId);
                }

                session.setAttribute("authenticated", true);
                session.setAttribute("citizenHash", documentHash);
                session.setAttribute("userType", ROLE_DENUNCIANTE);

                // Auditar acceso de denunciante (sin PII, solo rol)
                try {
                    auditService.logSecurityEvent(
                        "USER_ACCESS_GRANTED",
                        "SUCCESS",
                        null,
                        "denunciante",
                        getClientIp(request),
                        request.getHeader("User-Agent"),
                        Map.of(
                            "role", ROLE_DENUNCIANTE,
                            "identity_vault_id", identityVaultId != null ? identityVaultId.toString() : "null"
                        )
                    );
                } catch (Exception e) {
                    // Silent failure
                }

                return "redirect:/denuncia/opciones";
            }

            // Flujo staff: redirigir a clave secreta
            session.setAttribute("staffCedulaHash", documentHash);
            session.setAttribute("staffRole", role);

            return "redirect:/auth/secret-key";

        } catch (Exception e) {
            log.error("Error en verify-complete", e);
            redirectAttributes.addFlashAttribute("error", "No se pudo completar la verificación.");
            return "redirect:/verification/inicio";
        }
    }

    /**
     * Pantalla de clave secreta para Staff/Admin.
     */
    @GetMapping("/secret-key")
    public String showSecretKeyPage(Model model, HttpSession session) {
        String staffCedulaHash = (String) session.getAttribute("staffCedulaHash");
        String citizenRef = (String) session.getAttribute("citizenRef");

        if (staffCedulaHash == null && citizenRef == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("secretKeyForm", new SecretKeyForm());
        return "auth/secret-key";
    }

    /**
     * Procesa la validación de clave secreta para staff.
     *
     * Flujo:
     * 1. Obtiene staffCedulaHash de sesión
     * 2. Busca StaffUser por cedula_hash_idx
     * 3. Valida clave secreta ingresada contra mfa_secret_encrypted (BCrypt)
     * 4. Si válida → envía OTP por email
     * 5. Redirige a pantalla OTP
     *
     * Seguridad:
     * - La clave secreta está almacenada como BCrypt hash en mfa_secret_encrypted
     * - Máximo 3 intentos (implementar en futuro)
     */
    @PostMapping("/verify-secret")
    public String verifySecretKey(
            @Valid @ModelAttribute SecretKeyForm form,
            BindingResult result,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {

        String cedulaHash = (String) session.getAttribute("staffCedulaHash");

        if (cedulaHash == null) {
            log.warn("verify-secret: No staffCedulaHash in session");
            redirectAttributes.addFlashAttribute("error", "Sesión expirada. Intente nuevamente.");
            return "redirect:/verification/inicio";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "La clave secreta es requerida.");
            return "redirect:/auth/secret-key?error";
        }

        try {
            // Control de intentos: máximo 3 intentos
            Integer attempts = (Integer) session.getAttribute("secretKeyAttempts");
            if (attempts == null) {
                attempts = 0;
            }

            if (attempts >= 3) {
                log.warn("verify-secret: Max attempts exceeded");

                // Auditar bloqueo (no debe interrumpir el flujo si falla)
                try {
                    auditService.logSecurityEvent(
                        "SECRET_KEY_LOCKED",
                        "FAILURE",
                        null,
                        cedulaHash,
                        getClientIp(request),
                        request.getHeader("User-Agent"),
                        Map.of(
                            "reason", "max_attempts_exceeded",
                            "attempts", attempts
                        )
                    );
                } catch (Exception auditEx) {
                    log.error("Failed to log audit event (max attempts), but continuing: {}", auditEx.getMessage());
                }

                // Invalidar sesión y bloquear
                session.invalidate();
                redirectAttributes.addFlashAttribute("error",
                    "Máximo de intentos alcanzado. Por seguridad, su sesión ha sido cerrada.");
                return "redirect:/verification/inicio";
            }

            // Buscar staff por cedula_hash_idx
            Optional<StaffUser> staffUserOpt = staffUserRepository.findByCedulaHashIdx(cedulaHash);

            if (staffUserOpt.isEmpty()) {
                log.warn("verify-secret: Staff not found for hash");

                // Auditar intento fallido (no debe interrumpir el flujo si falla)
                try {
                    auditService.logSecurityEvent(
                        "SECRET_KEY_VALIDATION",
                        "FAILURE",
                        null,
                        cedulaHash,
                        getClientIp(request),
                        request.getHeader("User-Agent"),
                        Map.of("reason", "staff_not_found")
                    );
                } catch (Exception auditEx) {
                    log.error("Failed to log audit event (staff not found), but continuing: {}", auditEx.getMessage());
                }

                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
                return "redirect:/auth/secret-key?error";
            }

            StaffUser staffUser = staffUserOpt.get();

            // Validar clave secreta usando BCrypt
            String inputSecret = form.getSecretKey();
            String storedSecretHash = staffUser.getMfaSecretEncrypted();

            if (storedSecretHash == null || storedSecretHash.isBlank()) {
                log.error("verify-secret: No mfa_secret_encrypted configured for user");

                // Auditar configuración faltante (no debe interrumpir el flujo si falla)
                try {
                    auditService.logSecurityEvent(
                        "SECRET_KEY_VALIDATION",
                        "FAILURE",
                        staffUser.getId(),
                        staffUser.getUsername(),
                        getClientIp(request),
                        request.getHeader("User-Agent"),
                        Map.of("reason", "no_secret_configured")
                    );
                } catch (Exception auditEx) {
                    log.error("Failed to log audit event (no secret configured), but continuing: {}", auditEx.getMessage());
                }

                redirectAttributes.addFlashAttribute("error",
                        "No hay clave secreta configurada. Contacte al administrador.");
                return "redirect:/auth/secret-key?error";
            }

            // Validar con BCrypt
            boolean isValid = passwordEncoder.matches(inputSecret, storedSecretHash);

            if (!isValid) {
                // Incrementar contador de intentos
                attempts++;
                session.setAttribute("secretKeyAttempts", attempts);

                log.warn("verify-secret: Invalid secret key (attempt {}/3)", attempts);

                // Auditar intento fallido (no debe interrumpir el flujo si falla)
                try {
                    auditService.logSecurityEvent(
                        "SECRET_KEY_VALIDATION",
                        "FAILURE",
                        staffUser.getId(),
                        staffUser.getUsername(),
                        getClientIp(request),
                        request.getHeader("User-Agent"),
                        Map.of(
                            "reason", "invalid_secret",
                            "attempt", attempts,
                            "max_attempts", 3
                        )
                    );
                } catch (Exception auditEx) {
                    log.error("Failed to log audit event (invalid secret), but continuing: {}", auditEx.getMessage());
                }

                int remainingAttempts = 3 - attempts;
                if (remainingAttempts > 0) {
                    redirectAttributes.addFlashAttribute("error",
                        "Clave secreta incorrecta. Le quedan " + remainingAttempts + " intentos.");
                } else {
                    redirectAttributes.addFlashAttribute("error",
                        "Clave secreta incorrecta. Máximo de intentos alcanzado.");
                }
                return "redirect:/auth/secret-key?error";
            }

            // ÉXITO: Resetear contador de intentos
            session.removeAttribute("secretKeyAttempts");

            log.info("verify-secret: Secret key validated successfully");

            // Auditar éxito (no debe interrumpir el flujo si falla)
            try {
                String clientIp = getClientIp(request);
                log.debug("verify-secret: About to log audit event with IP={}, staffId={}, username={}",
                    clientIp, staffUser.getId(), staffUser.getUsername());

                auditService.logSecurityEvent(
                    "SECRET_KEY_VALIDATION",
                    "SUCCESS",
                    staffUser.getId(),
                    staffUser.getUsername(),
                    clientIp,
                    request.getHeader("User-Agent"),
                    Map.of("role", staffUser.getRole())
                );
            } catch (Exception auditEx) {
                log.error("Failed to log audit event (validation success), but continuing: {}", auditEx.getMessage());
            }

            // Obtener email para enviar OTP
            String emailEncrypted = staffUser.getEmailEncrypted();

            if (emailEncrypted == null || emailEncrypted.isBlank()) {
                log.error("verify-secret: No email configured");
                redirectAttributes.addFlashAttribute("error",
                        "No hay email configurado para MFA. Contacte al administrador.");
                return "redirect:/auth/secret-key?error";
            }

            String email = cryptoService.decryptPII(emailEncrypted);

            if (email == null || email.isBlank()) {
                log.error("verify-secret: Failed to decrypt email");
                redirectAttributes.addFlashAttribute("error",
                        "No se pudo procesar el email para MFA. Intente nuevamente.");
                return "redirect:/auth/secret-key?error";
            }

            // Enviar OTP
            String otpToken = otpService.sendOtp(email);

            if (otpToken == null) {
                log.error("verify-secret: Failed to send OTP");
                redirectAttributes.addFlashAttribute("error",
                        "No se pudo enviar el código. Intente nuevamente.");
                return "redirect:/auth/secret-key?error";
            }

            // Auditar envío de OTP (no debe interrumpir el flujo si falla)
            try {
                auditService.logSecurityEvent(
                    "OTP_SENT",
                    "SUCCESS",
                    staffUser.getId(),
                    staffUser.getUsername(),
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of("method", "email")
                );
            } catch (Exception auditEx) {
                log.error("Failed to log audit event (OTP sent), but continuing: {}", auditEx.getMessage());
            }

            // Guardar en sesión
            session.setAttribute("otpToken", otpToken);
            session.setAttribute("otpEmail", maskEmail(email));
            session.setAttribute("otpCedulaHash", cedulaHash);
            session.setAttribute("otpAttempts", 0); // Inicializar contador OTP

            return "redirect:/auth/verify-otp";

        } catch (Exception e) {
            log.error("Error verificando clave secreta", e);
            redirectAttributes.addFlashAttribute("error", "No se pudo verificar la clave secreta.");
            return "redirect:/auth/secret-key?error";
        }
    }

    /**
     * Pantalla OTP.
     */
    @GetMapping("/verify-otp")
    public String showOtpPage(Model model, HttpSession session) {
        String otpToken = (String) session.getAttribute("otpToken");
        if (otpToken == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("maskedEmail", session.getAttribute("otpEmail"));
        model.addAttribute("otpForm", new OtpForm());
        return "auth/verify-otp";
    }

    /**
     * Procesa la validación del código OTP ingresado por el usuario.
     *
     * Flujo:
     * 1. Obtiene token OTP y cedulaHash de sesión
     * 2. Valida código OTP con OtpService
     * 3. Si válido, busca staff por cedula_hash_idx
     * 4. Genera JWT y cookie
     * 5. Redirige al panel correspondiente según rol
     *
     * Seguridad:
     * - OTP expira en 5 minutos
     * - Token se elimina después de validación
     * - JWT con tiempo de vida limitado
     */
    @PostMapping("/verify-otp")
    public String verifyOtp(
            @ModelAttribute OtpForm form,
            HttpSession session,
            HttpServletRequest request,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {

        String otpToken = (String) session.getAttribute("otpToken");
        String cedulaHash = (String) session.getAttribute("otpCedulaHash");

        if (otpToken == null || cedulaHash == null) {
            log.warn("verify-otp: Missing session data");

            // Auditar sesión expirada
            auditService.logSecurityEvent(
                "OTP_VALIDATION",
                "FAILURE",
                null,
                null,
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of("reason", "session_expired")
            );

            redirectAttributes.addFlashAttribute("error", "Sesión expirada. Intente nuevamente.");
            return "redirect:/verification/inicio";
        }

        try {
            // Control de intentos: máximo 3 intentos
            Integer attempts = (Integer) session.getAttribute("otpAttempts");
            if (attempts == null) {
                attempts = 0;
            }

            if (attempts >= 3) {
                log.warn("verify-otp: Max attempts exceeded");

                // Auditar bloqueo
                auditService.logSecurityEvent(
                    "OTP_LOCKED",
                    "FAILURE",
                    null,
                    cedulaHash,
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of(
                        "reason", "max_attempts_exceeded",
                        "attempts", attempts
                    )
                );

                // Invalidar sesión
                session.invalidate();
                redirectAttributes.addFlashAttribute("error",
                    "Máximo de intentos alcanzado. Por seguridad, su sesión ha sido cerrada.");
                return "redirect:/verification/inicio";
            }

            // Validar OTP con OtpService
            boolean isValid = otpService.verifyOtp(otpToken, form.getOtpCode());

            if (!isValid) {
                // Incrementar contador
                attempts++;
                session.setAttribute("otpAttempts", attempts);

                log.warn("verify-otp: Invalid OTP code (attempt {}/3)", attempts);

                // Auditar intento fallido
                auditService.logSecurityEvent(
                    "OTP_VALIDATION",
                    "FAILURE",
                    null,
                    cedulaHash,
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of(
                        "reason", "invalid_code",
                        "attempt", attempts,
                        "max_attempts", 3
                    )
                );

                int remainingAttempts = 3 - attempts;
                if (remainingAttempts > 0) {
                    redirectAttributes.addFlashAttribute("error",
                        "Código incorrecto. Le quedan " + remainingAttempts + " intentos.");
                } else {
                    redirectAttributes.addFlashAttribute("error",
                        "Código incorrecto. Máximo de intentos alcanzado.");
                }
                return "redirect:/auth/verify-otp?error";
            }

            // ÉXITO: Resetear contador
            session.removeAttribute("otpAttempts");


            // Buscar staff por cedula_hash_idx
            Optional<StaffUser> staffUserOpt = staffUserRepository.findByCedulaHashIdx(cedulaHash);

            if (staffUserOpt.isEmpty()) {
                log.error("verify-otp: Staff not found after OTP validation");

                // Auditar error inesperado
                auditService.logSecurityEvent(
                    "OTP_VALIDATION",
                    "FAILURE",
                    null,
                    cedulaHash,
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of("reason", "staff_not_found_after_validation")
                );

                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado.");
                return "redirect:/verification/inicio";
            }

            StaffUser staffUser = staffUserOpt.get();

            // VALIDAR: Si el usuario está desactivado, bloquear acceso
            if (!staffUser.isEnabled()) {
                log.warn("verify-otp: Staff user is disabled: {}", staffUser.getUsername());

                // Auditar intento bloqueado
                auditService.logSecurityEvent(
                    "DISABLED_USER_BLOCKED",
                    "FAILURE",
                    staffUser.getId(),
                    staffUser.getUsername(),
                    getClientIp(request),
                    request.getHeader("User-Agent"),
                    Map.of("reason", "user_is_disabled", "role", staffUser.getRole())
                );

                // Invalidar sesión
                session.invalidate();

                redirectAttributes.addFlashAttribute("error",
                    "Su cuenta de analista ha sido desactivada. Contacte al administrador del sistema.");
                return "redirect:/verification/inicio";
            }

            String role = staffUser.getRole();


            // Auditar éxito de OTP
            auditService.logSecurityEvent(
                "OTP_VALIDATION",
                "SUCCESS",
                staffUser.getId(),
                staffUser.getUsername(),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of("role", role)
            );

            // Generar JWT
            String jwt = jwtTokenProvider.generateToken(cedulaHash, role, "");

            // Crear cookie con JWT
            Cookie jwtCookie = new Cookie("Authorization", jwt);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // En producción con HTTPS debe ser true
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400); // 24 horas
            response.addCookie(jwtCookie);

            // Configurar sesión
            session.setAttribute("authenticated", true);
            session.setAttribute("userType", role);
            session.setAttribute("cedulaHash", cedulaHash);
            session.setAttribute("username", staffUser.getUsername());

            // Limpieza de datos temporales
            session.removeAttribute("otpToken");
            session.removeAttribute("otpEmail");
            session.removeAttribute("otpCedulaHash");
            session.removeAttribute("staffCedulaHash");
            session.removeAttribute("citizenRef");
            session.removeAttribute("secretKeyAttempts");

            // Redirigir según rol
            String redirectUrl;
            if ("ADMIN".equals(role)) {
                redirectUrl = "/admin/panel";
            } else {
                redirectUrl = "/staff/casos";
            }

            // Auditar LOGIN completo exitoso
            auditService.logSecurityEvent(
                "LOGIN_SUCCESS",
                "SUCCESS",
                staffUser.getId(),
                staffUser.getUsername(),
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of(
                    "role", role,
                    "redirect_to", redirectUrl,
                    "method", "didit_secret_otp"
                )
            );

            return "redirect:" + redirectUrl;

        } catch (Exception e) {
            log.error("Error verificando OTP", e);

            // Auditar error inesperado
            auditService.logSecurityEvent(
                "OTP_VALIDATION",
                "FAILURE",
                null,
                cedulaHash,
                getClientIp(request),
                request.getHeader("User-Agent"),
                Map.of("reason", "exception", "error", e.getMessage())
            );

            redirectAttributes.addFlashAttribute("error", "No se pudo verificar el código. Intente nuevamente.");
            return "redirect:/auth/verify-otp?error";
        }
    }

    /**
     * DESHABILITADO TEMPORALMENTE - REQUIERE REFACTORIZACION
     * Reenvia el OTP si el usuario no lo recibio.
     */
    /* DESHABILITADO - Requiere refactorizacion
    @PostMapping("/resend-otp")
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String cedulaHash = (String) session.getAttribute("otpCedulaHash");
        if (cedulaHash == null) {
            return "redirect:/auth/login";
        }

        try {
            Optional<StaffUser> staffUserOpt = staffUserRepository.findByCedulaHashIdx(cedulaHash);
            if (staffUserOpt.isEmpty()) {
                return "redirect:/auth/login";
            }

            String emailEncrypted = staffUserOpt.get().getEmailEncrypted();
            if (emailEncrypted == null || emailEncrypted.isBlank()) {
                redirectAttributes.addFlashAttribute("error",
                        "No hay email configurado para MFA. Contacte al administrador.");
                return "redirect:/auth/verify-otp?error";
            }

            String email = cryptoService.decryptPII(emailEncrypted);
            if (email == null || email.isBlank()) {
                redirectAttributes.addFlashAttribute("error",
                        "No se pudo reenviar el código. Intente nuevamente.");
                return "redirect:/auth/verify-otp?error";
            }

            String otpToken = unifiedAuthService.sendEmailOtp(email);
            session.setAttribute("otpToken", otpToken);
            session.setAttribute("otpEmail", maskEmail(email));

            redirectAttributes.addFlashAttribute("success", "Código reenviado.");
            return "redirect:/auth/verify-otp";

        } catch (Exception e) {
            log.error("Error reenviando OTP", e);
            redirectAttributes.addFlashAttribute("error", "No se pudo reenviar el código. Intente más tarde.");
            return "redirect:/auth/verify-otp?error";
        }
    }
    */ // FIN METODO DESHABILITADO

    /**
     * Logout: invalida la sesión.
     *
     * <p>Si estás usando cookie JWT, en un entorno real también conviene
     * expirar la cookie explícitamente.</p>
     */
    @GetMapping("/logout")
    public String logout(HttpSession session, HttpServletResponse response) {
        session.invalidate();

        Cookie cookie = new Cookie("Authorization", "");
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // En producción con HTTPS debe ser true
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);

        return "redirect:/auth/login?logout";
    }

    /**
     * DTO simple para formulario OTP.
     */
    public static class OtpForm {
        private String otpCode;

        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    }

    /**
     * Arma nuevamente el modelo de la vista de confirmación cuando hay error.
     *
     * <p>No muestra datos sensibles. Solo rol y el botón de continuación.</p>
     */
    private String prepareVerifyConfirmModel(HttpSession session, Model model) {
        String role = (String) session.getAttribute("verifiedStaffRole");
        role = normalizeRole(role);

        model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
        model.addAttribute("staffRole", role);

        if (ROLE_ADMIN.equals(role)) {
            model.addAttribute("staffRedirectButton", "Ir al panel");
            model.addAttribute("staffRedirectUrl", "/admin/panel");
        } else if (ROLE_ANALYST.equals(role)) {
            model.addAttribute("staffRedirectButton", "Ver casos");
            model.addAttribute("staffRedirectUrl", "/staff/casos");
        } else {
            model.addAttribute("staffRedirectButton", "Continuar");
            model.addAttribute("staffRedirectUrl", "/denuncia/opciones");
        }

        return "auth/verify-confirm";
    }


    /**
     * Obtiene la IP real del cliente considerando headers típicos de proxy.
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            String ip = xForwardedFor.split(",")[0].trim();
            log.debug("getClientIp: X-Forwarded-For = {}", ip);
            return ip;
        }
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            log.debug("getClientIp: X-Real-IP = {}", xRealIp);
            return xRealIp;
        }
        String remoteAddr = request.getRemoteAddr();
        if (remoteAddr == null || remoteAddr.isEmpty()) {
            log.warn("getClientIp: No IP found, using default");
            return "0.0.0.0";
        }
        log.debug("getClientIp: RemoteAddr = {}", remoteAddr);
        return remoteAddr;
    }

    /**
     * Enmascara el email para mostrarlo sin exponerlo completo.
     */
    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "***@***.***";
        }
        String[] parts = email.split("@");
        String local = parts[0];
        String domain = parts[1];

        if (local.length() <= 2) {
            return local.charAt(0) + "***@" + domain;
        }
        return local.charAt(0) + "***" + local.charAt(local.length() - 1) + "@" + domain;
    }

    /**
     * Normaliza el rol desde base de datos para evitar inconsistencias.
     */
    private String normalizeRole(String role) {
        if (role == null) return ROLE_DENUNCIANTE;
        String r = role.trim().toUpperCase();
        if ("ANALISTA".equals(r)) return ROLE_ANALYST;
        if ("ANALYST".equals(r)) return ROLE_ANALYST;
        if ("ADMIN".equals(r)) return ROLE_ADMIN;
        return ROLE_DENUNCIANTE;
    }

    /**
     * Normaliza el tipo de usuario (UserType) a valores esperados por el resto del sistema.
     */
    private String normalizeUserType(String userType) {
        if (userType == null) return ROLE_DENUNCIANTE;
        String t = userType.trim().toUpperCase();
        if ("ANALISTA".equals(t)) return ROLE_ANALYST;
        if ("ANALYST".equals(t)) return ROLE_ANALYST;
        if ("ADMIN".equals(t)) return ROLE_ADMIN;
        return ROLE_DENUNCIANTE;
    }
}

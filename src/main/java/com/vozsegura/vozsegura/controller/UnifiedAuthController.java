package com.vozsegura.vozsegura.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
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
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.vozsegura.dto.forms.SecretKeyForm;
import com.vozsegura.vozsegura.dto.forms.UnifiedLoginForm;
import com.vozsegura.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.vozsegura.domain.entity.Persona;
import com.vozsegura.vozsegura.domain.entity.StaffUser;
import com.vozsegura.vozsegura.repo.PersonaRepository;
import com.vozsegura.vozsegura.repo.StaffUserRepository;
import com.vozsegura.vozsegura.service.CloudflareTurnstileService;
import com.vozsegura.vozsegura.service.UnifiedAuthService;
import com.vozsegura.vozsegura.service.JwtTokenProvider;
import com.vozsegura.vozsegura.service.DiditService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Cookie;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

/**
 * Controlador de Autenticación Unificada (Zero Trust Architecture).
 *
 * Responsabilidades Principales:
 * - Punto de entrada único para todos los usuarios del sistema
 * - Flujo de MFA para Staff/Admin (5 pasos)
 * - Flujo simple para Denunciantes públicos (1-2 pasos)
 * - Integración con Registro Civil (verificación biométrica)
 * - Integración con Didit v3 (QR-based biometric verification)
 * - Validación Cloudflare Turnstile (anti-bot)
 * - Generación de JWT tokens para autenticación
 * - Manejo de sesiones HTTP
 * - Auditoría de todos los intentos de login
 * 
 * Flujo Denunciantes Públicos (Anonymous Complaint Filing):
 * 1. GET /denuncia → showForm()
 * 2. POST verificación inicial → startVerification()
 * 3. Didit biometric verification (QR code)
 * 4. POST callback → verificationCallback()
 * 5. Sesión anónima con SHA-256 hash
 * 6. Redirección a /denuncia/opciones
 * 
 * Flujo Staff/Admin (MFA de 5 pasos):
 * 1. GET /login → loginForm()
 * 2. POST cédula+dactilar → startDiditVerification()
 * 3. Didit biometric verification (QR code)
 * 4. POST callback → handleDiditCallback()
 * 5. POST clave secreta → verifySecretKey()
 * 6. POST código OTP → verifyOtp()
 * 7. JWT token + sesión HTTP
 * 8. Redirección a /staff/casos
 * 
 * Seguridad:
 * - CSRF protection (sincronización de tokens)
 * - Turnstile reCAPTCHA en formularios públicos
 * - Rate limiting en endpoints de autenticación
 * - JWT tokens con TTL (típicamente 1 hora)
 * - Refresh tokens para renovación
 * - Cookies HttpOnly + Secure
 * - Auditoría de intentos fallidos
 * 
 * Enrutamiento por Rol (API Gateway Pattern):
 * - DENUNCIANTE → /denuncia (crear denuncias)
 * - ANALYST → /staff/casos (panel analistas)
 * - ADMIN → /admin (panel administración)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Controller
@RequestMapping("/auth")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;
    private final CloudflareTurnstileService turnstileService;
    private final JwtTokenProvider jwtTokenProvider;
    private final DiditService diditService;
    private final StaffUserRepository staffUserRepository;
    private final PersonaRepository personaRepository;

    public UnifiedAuthController(UnifiedAuthService unifiedAuthService, CloudflareTurnstileService turnstileService, JwtTokenProvider jwtTokenProvider, DiditService diditService, StaffUserRepository staffUserRepository, PersonaRepository personaRepository) {
        this.unifiedAuthService = unifiedAuthService;
        this.turnstileService = turnstileService;
        this.jwtTokenProvider = jwtTokenProvider;
        this.diditService = diditService;
        this.staffUserRepository = staffUserRepository;
        this.personaRepository = personaRepository;
    }

    /**
     * Pantalla de login unificado (punto de entrada único).
     * Nueva versión simplificada: solo botón para iniciar Didit.
     */
    @GetMapping("/login")
    /**
     * Muestra página de login para denunciantes públicos.
     */
    public String showLoginPage(Model model, HttpSession session) {
        return "auth/login-simple";
    }

    /**
     * Endpoint de debug para verificar configuración de Didit
     */
    @GetMapping("/debug/didit-config")
    @ResponseBody
    public Map<String, Object> debugDiditConfig() {
        try {
            Map<String, Object> response = diditService.createVerificationSession("debug-test");
            log.info("Debug: Didit session created: {}", response);
            return Map.of(
                    "status", "success",
                    "response", response
            );
        } catch (Exception e) {
            log.error("Debug: Error creating Didit session", e);
            return Map.of(
                    "status", "error",
                    "error", e.getMessage(),
                    "cause", e.getCause() != null ? e.getCause().getMessage() : "N/A"
            );
        }
    }

    /**
     * Procesar login unificado (Paso 1: Verificación Registro Civil + Turnstile).
     */
    @PostMapping("/unified-login")
    /**
     * Procesa login unificado: verifica cédula+dactilar contra Registro Civil.
     */
    public String processUnifiedLogin(
            @Valid @ModelAttribute UnifiedLoginForm form,
            BindingResult result,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {

        // Validaciones básicas del formulario
        if (result.hasErrors()) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Por favor complete todos los campos correctamente");
            return "public/denuncia-login";
        }

        // Validar token de Turnstile
        String turnstileToken = request.getParameter("cf-turnstile-response");
        if (turnstileToken == null || turnstileToken.isBlank()) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Verificación de Turnstile fallida. Por favor intente nuevamente.");
            return "public/denuncia-login";
        }

        // Obtener IP del cliente para validación adicional
        String clientIp = getClientIp(request);
        
        // Verificar token Turnstile
        if (!turnstileService.verifyTurnstileToken(turnstileToken, clientIp)) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "No se pudo verificar que eres una persona. Por favor intenta de nuevo.");
            return "public/denuncia-login";
        }

        try {
            // Paso 1: Verificar identidad contra Registro Civil (sin CAPTCHA)
            String citizenRef = unifiedAuthService.verifyCitizenIdentity(
                form.getCedula(),
                form.getCodigoDactilar()
            );

            // Guardar citizenRef en sesión
            session.setAttribute("citizenRef", citizenRef);
            session.setAttribute("cedula", form.getCedula());

            // Paso 2: Determinar tipo de usuario
            UnifiedAuthService.UserType userType = unifiedAuthService.getUserType(form.getCedula());
            session.setAttribute("userType", userType.name());

            // Enrutamiento según tipo de usuario (API Gateway Pattern)
            switch (userType) {
                case ADMIN:
                case ANALYST:
                    // Staff/Admin: Solicitar clave secreta
                    return "redirect:/auth/secret-key";

                case DENUNCIANTE:
                default:
                    // Denunciante: Generar hash anónimo y marcar como autenticado
                    String citizenHash = hashCedula(form.getCedula());
                    session.setAttribute("citizenHash", citizenHash);
                    session.setAttribute("authenticated", true);
                    return "redirect:/denuncia/biometric";
            }

        } catch (SecurityException e) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", e.getMessage());
            return "public/denuncia-login";
        } catch (Exception e) {
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Error al procesar la autenticación. Por favor intente nuevamente.");
            return "public/denuncia-login";
        }
    }

    /**
     * Extrae la IP real del cliente considerando proxies (X-Forwarded-For, etc)
     */
    private String getClientIp(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
            // En caso de múltiples proxies, tomar la primera IP
            return xForwardedFor.split(",")[0].trim();
        }

        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty()) {
            return xRealIp;
        }

        return request.getRemoteAddr();
    }

    /**
     * Inicia sesión de verificación con Didit.
     * Crea una sesión interactiva y redirige al usuario a la URL de Didit.
     */
    @GetMapping("/verify-start")
    /**
     * Inicia verificación Didit v3 (QR-based biometric).
     */
    public String startDiditVerification(HttpSession session, RedirectAttributes redirectAttributes) {
        try {
            // Crear sesión con identificador único de sesión HTTP como vendor_data
            String sessionId = session.getId();
            Map<String, Object> response = diditService.createVerificationSession(sessionId);
            
            if (response == null || !response.containsKey("session_id") || !response.containsKey("url")) {
                String errorMsg = "Respuesta inválida de Didit: " + (response != null ? response.toString() : "null");
                redirectAttributes.addFlashAttribute("error", "Error: respuesta inválida de Didit");
                return "redirect:/auth/login";
            }
            
            String diditSessionId = (String) response.get("session_id");
            String verificationUrl = (String) response.get("url");
            
            // Guardar session_id en la sesión HTTP para luego validar el webhook
            session.setAttribute("diditSessionId", diditSessionId);
            
            log.info("Didit verification session created: {} -> {}", diditSessionId, verificationUrl);
            
            // Redirigir a Didit
            return "redirect:" + verificationUrl;
            
        } catch (Exception e) {
            log.error("Error starting Didit verification", e);
            redirectAttributes.addFlashAttribute("error", "Error: " + e.getMessage());
            return "redirect:/auth/login";
        }
    }

    /**
     * Procesa el resultado del webhook de Didit y muestra el formulario de confirmación.
     * Este endpoint se accede después de que el usuario completa la verificación en Didit.
     */
    @GetMapping("/verify-callback")
    /**
     * Webhook callback después de verificación Didit (webhook POST).
     */
    public String handleDiditCallback(
            HttpSession session, 
            Model model, 
            RedirectAttributes redirectAttributes,
            @RequestParam(name = "session_id", required = false) String sessionIdParam) {
        try {
            // Obtener el session_id del parámetro o de la sesión
            String diditSessionId = sessionIdParam != null ? sessionIdParam : (String) session.getAttribute("diditSessionId");
            
            log.info("handleDiditCallback - sessionIdParam: {}, sessionAttribute: {}, final diditSessionId: {}", 
                    sessionIdParam, session.getAttribute("diditSessionId"), diditSessionId);
            
            if (diditSessionId == null || diditSessionId.isBlank()) {
                log.warn("No diditSessionId found. sessionIdParam={}, sessionAttr={}", 
                        sessionIdParam, session.getAttribute("diditSessionId"));
                redirectAttributes.addFlashAttribute("error", "Sesión de verificación no encontrada.");
                return "redirect:/auth/login";
            }
            
            log.info("Verify callback for session: {}", diditSessionId);
            
            // IMPORTANTE: Esperar a que el webhook POST de Didit entregue los datos
            // El webhook tarda algunos milisegundos en llegar
            log.info("Waiting for webhook POST from Didit for sessionId: {}", diditSessionId);
            
            Optional<DiditVerification> verificationOpt = Optional.empty();
            int maxAttempts = 20; // 20 intentos × 250ms = 5 segundos máximo
            
            for (int i = 0; i < maxAttempts; i++) {
                verificationOpt = diditService.getVerificationBySessionId(diditSessionId);
                if (verificationOpt.isPresent()) {
                    log.info("Webhook data received on attempt {}/{}", i + 1, maxAttempts);
                    break;
                }
                
                try {
                    Thread.sleep(250); // Esperar 250ms antes del siguiente intento
                } catch (InterruptedException e) {
                    log.warn("Interrupted while waiting for webhook: {}", e.getMessage());
                    Thread.currentThread().interrupt();
                    break;
                }
            }
            
            if (verificationOpt.isEmpty()) {
                log.error("Webhook data not received after {} attempts (5 seconds) for session: {}", maxAttempts, diditSessionId);
                redirectAttributes.addFlashAttribute("error", "La verificación no fue exitosa. Por favor intente nuevamente.");
                return "redirect:/auth/login";
            }
            
            DiditVerification verification = verificationOpt.get();
            String documentNumber = verification.getDocumentNumber();
            
            log.info("Verification successful for document: {}", documentNumber);
            
            // PASO 1: Verificar si la cédula está en la tabla staff_user (ADMIN o ANALISTA)
            Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(documentNumber);
            
            String staffRole = null;
            String staffRedirectButton = null;
            String staffRedirectUrl = null;
            
            if (staffUser.isPresent()) {
                // Es un usuario staff (ADMIN o ANALISTA)
                StaffUser user = staffUser.get();
                staffRole = user.getRole();
                log.info("👤 Usuario STAFF encontrado: document={}, role={}", documentNumber, staffRole);
                
                if ("ADMIN".equals(staffRole)) {
                    staffRedirectButton = "Ver Panel";
                    staffRedirectUrl = "/admin/panel";
                    log.info("User is ADMIN - redirect to panel");
                } else if ("ANALISTA".equals(staffRole)) {
                    staffRedirectButton = "Ver Denuncias";
                    staffRedirectUrl = "/staff/casos";
                    log.info("User is ANALISTA - redirect to complaints");
                } else {
                    log.warn("⚠️  Unknown staff role: {}", staffRole);
                    staffRedirectButton = "Hacer Denuncia";
                    staffRedirectUrl = "/denuncia/form";
                }
            } else {
                // PASO 2: No es staff - verificar si es una persona válida en registro_civil.personas
                // (es decir, es un denunciante)
                log.info("👤 No es usuario STAFF. Verificando si es persona válida para denuncias: document={}", documentNumber);
                
                // Buscar persona por cédula
                Optional<Persona> persona = personaRepository.findByCedula(documentNumber);
                
                if (persona.isPresent()) {
                    // Es un denunciante válido
                    staffRole = "DENUNCIANTE";
                    staffRedirectButton = "Hacer Denuncia";
                    staffRedirectUrl = "/denuncia/form";
                    log.info("Usuario es DENUNCIANTE válido - redirect to denuncia form");
                } else {
                    // Cédula no encontrada en ninguna tabla
                    log.warn("Cédula no encontrada en sistema: document={}", documentNumber);
                    redirectAttributes.addFlashAttribute("error", "Cédula no registrada en el sistema. Por favor intente nuevamente.");
                    return "redirect:/auth/login";
                }
            }
            
            // Guardar datos en la sesión para POST /auth/verify-complete
            // Solo guardamos la cédula - NO nombres por privacidad
            session.setAttribute("verifiedDocumentNumber", documentNumber);
            session.setAttribute("verifiedStaffRole", staffRole);
            session.setAttribute("diditSessionId", diditSessionId);
            
            // Pasar datos al modelo - NO mostramos cédula ni nombre
            // Solo mostramos "Usuario verificado"
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("staffRole", staffRole);
            model.addAttribute("staffRedirectButton", staffRedirectButton);
            model.addAttribute("staffRedirectUrl", staffRedirectUrl);
            
            return "auth/verify-confirm";
            
        } catch (Exception e) {
            log.error("Error processing verification callback", e);
            redirectAttributes.addFlashAttribute("error", "Error al procesar la verificación. Por favor intente nuevamente.");
            return "redirect:/auth/login";
        }
    }

    /**
     * Completa el proceso de autenticación después de la verificación Didit.
     * Valida el CAPTCHA y los términos, luego establece la sesión y redirige.
     * Si es staff/admin, redirige a /auth/secret-key
     * Si es usuario regular, redirige a /denuncia/form
     */
    @PostMapping("/verify-complete")
    public String completeVerification(
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {
        
        try {
            // Validar CAPTCHA
            String turnstileToken = request.getParameter("cf-turnstile-response");
            if (turnstileToken == null || turnstileToken.isBlank()) {
                model.addAttribute("error", "Verificación de CAPTCHA requerida.");
                return prepareVerifyConfirmModel(session, model, turnstileToken);
            }
            
            String clientIp = getClientIp(request);
            if (!turnstileService.verifyTurnstileToken(turnstileToken, clientIp)) {
                model.addAttribute("error", "Verificación de CAPTCHA fallida. Por favor intente nuevamente.");
                return prepareVerifyConfirmModel(session, model, turnstileToken);
            }
            
            // Validar términos y condiciones
            String acceptTerms = request.getParameter("termsAccepted");
            if (acceptTerms == null || !acceptTerms.equals("on")) {
                model.addAttribute("error", "Debe aceptar los términos y condiciones.");
                return prepareVerifyConfirmModel(session, model, turnstileToken);
            }
            
            // Obtener datos verificados de la sesión
            String documentNumber = (String) session.getAttribute("verifiedDocumentNumber");
            String staffRole = (String) session.getAttribute("verifiedStaffRole");
            
            if (documentNumber == null || staffRole == null) {
                model.addAttribute("error", "Datos de verificación no encontrados. Por favor intente nuevamente.");
                return prepareVerifyConfirmModel(session, model, turnstileToken);
            }
            
            log.info("completeVerification - documentNumber={}, staffRole={}", documentNumber, staffRole);
            
            // Todos los usuarios verificados pasan por el flujo de clave secreta (solo ADMIN y ANALISTA)
            if ("ADMIN".equals(staffRole) || "ANALISTA".equals(staffRole)) {
                log.info("👤 Redirecting staff/admin user to secret-key verification");
                session.setAttribute("staffCedula", documentNumber);
                session.setAttribute("staffRole", staffRole);
                
                // Limpiar datos temporales de Didit
                session.removeAttribute("diditSessionId");
                session.removeAttribute("verifiedDocumentNumber");
                session.removeAttribute("verifiedStaffRole");
                
                return "redirect:/auth/secret-key";
            }
            
            // Si es denunciante, proceder directamente al formulario de denuncia
            log.info("👤 Redirecting denunciante user to denuncia form");
            
            // Generar hash anónimo de la cédula para la denuncia
            String citizenHash = hashCedula(documentNumber);
            
            // Establecer atributos de sesión para denuncias
            session.setAttribute("authenticated", true);
            session.setAttribute("verificationMethod", "DIDIT");
            session.setAttribute("citizenHash", citizenHash);
            session.setAttribute("documentNumber", documentNumber);
            session.setAttribute("userRole", staffRole);
            
            // Limpiar datos temporales
            session.removeAttribute("diditSessionId");
            session.removeAttribute("verifiedDocumentNumber");
            session.removeAttribute("verifiedStaffRole");
            
            // Redirigir al formulario de denuncia
            return "redirect:/denuncia/form";
            
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar la verificación. Por favor intente nuevamente.");
            return prepareVerifyConfirmModel(session, model, null);
        }
    }

    /**
     * Helper para preparar el modelo del formulario de confirmación.
     * NO muestra cédula ni nombre - solo "Usuario verificado"
     */
    private String prepareVerifyConfirmModel(HttpSession session, Model model, String turnstileToken) {
        String documentNumber = (String) session.getAttribute("verifiedDocumentNumber");
        String staffRole = (String) session.getAttribute("verifiedStaffRole");
        
        model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
        
        // Verificar rol y establecer redirección
        if (documentNumber != null && staffRole != null) {
            if ("ADMIN".equals(staffRole)) {
                model.addAttribute("staffRole", staffRole);
                model.addAttribute("staffRedirectButton", "Ver Panel");
                model.addAttribute("staffRedirectUrl", "/admin/panel");
            } else if ("ANALYST".equals(staffRole)) {
                model.addAttribute("staffRole", staffRole);
                model.addAttribute("staffRedirectButton", "Ver Denuncias");
                model.addAttribute("staffRedirectUrl", "/staff/casos");
            } else {
                model.addAttribute("staffRole", staffRole);
                model.addAttribute("staffRedirectButton", "Hacer Denuncia");
                model.addAttribute("staffRedirectUrl", "/denuncia/form");
            }
        }
        
        return "auth/verify-confirm";
    }

    /**
     * Pantalla de clave secreta (Paso 2 para Staff/Admin).
     * Puede llegar desde:
     * 1. Flujo unificado (POST /unified-login) con citizenRef en sesión
     * 2. Flujo Didit (POST /verify-complete con isStaffUser=true) con staffCedula en sesión
     */
    @GetMapping("/secret-key")
    /**
     * Muestra página para ingreso de clave secreta (staff/admin step 3).
     */
    public String showSecretKeyPage(Model model, HttpSession session) {
        // Verificar que sea staff (viene de Didit) O que ya pasó verificación unificada
        String staffCedula = (String) session.getAttribute("staffCedula");
        String citizenRef = (String) session.getAttribute("citizenRef");
        
        if (staffCedula == null && citizenRef == null) {
            return "redirect:/auth/login";
        }
        
        // Si viene del flujo Didit, loggear acceso (NO mostramos datos personales en UI)
        if (staffCedula != null) {
            log.info("🔐 Staff/Admin accessing secret-key page (cedula verificada)");
        }

        model.addAttribute("secretKeyForm", new SecretKeyForm());
        return "auth/secret-key";
    }

    /**
     * Verificar clave secreta (Paso 2 para Staff/Admin).
     * Manejador ambos flujos:
     * 1. Flujo unificado: usa cedula de session
     * 2. Flujo Didit: usa staffCedula de session
     */
    @PostMapping("/verify-secret")
    /**
     * Verifica clave secreta contra BCrypt hash (staff/admin step 3).
     */
    public String verifySecretKey(
            @Valid @ModelAttribute SecretKeyForm form,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        // Obtener cédula desde sesión (puede venir de flujo unificado "cedula" o flujo Didit "staffCedula")
        String cedulaActual = (String) session.getAttribute("cedula");
        if (cedulaActual == null) {
            cedulaActual = (String) session.getAttribute("staffCedula");
        }
        
        if (cedulaActual == null) {
            log.warn("⚠️  verifySecretKey: No cedula found in session");
            return "redirect:/auth/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "La clave secreta es requerida");
            return "redirect:/auth/secret-key?error";
        }

        try {
            // IMPORTANTE: Verificar si la cédula existe en staff_user primero
            // Esto es necesario para casos donde Didit envía formato diferente
            Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(cedulaActual);
            
            if (!staffUser.isPresent()) {
                log.warn("Staff user not found for cedula: {}", cedulaActual);
                redirectAttributes.addFlashAttribute("error", "Clave secreta incorrecta");
                return "redirect:/auth/secret-key?error";
            }
            
            log.info("Staff user found for cedula: {}", cedulaActual);
            
            // Verificar clave secreta contra el hash en la base de datos
            boolean isValid = unifiedAuthService.validateSecretKey(staffUser.get(), form.getSecretKey());

            if (!isValid) {
                log.warn("Invalid secret key for cedula: {}", cedulaActual);
                redirectAttributes.addFlashAttribute("error", "Clave secreta incorrecta");
                return "redirect:/auth/secret-key?error";
            }

            log.info("Secret key validated for cedula: {}", cedulaActual);
            
            // Clave secreta válida - Enviar OTP por email (MFA)
            String email = staffUser.get().getEmail();
            
            if (email == null || email.isBlank()) {
                // Si no tiene email configurado, mostrar error - MFA es obligatorio
                log.error("No email configured for MFA for cedula: {}", cedulaActual);
                redirectAttributes.addFlashAttribute("error", "Error de configuracion: Email no configurado para MFA. Contacte al administrador.");
                return "redirect:/auth/secret-key?error";
            }
            
            log.info("📧 Email found: {}", maskEmail(email));
            
            // Enviar OTP por email
            try {
                String otpToken = unifiedAuthService.sendEmailOtp(email);
                
                if (otpToken == null) {
                    log.error("Failed to send OTP email");
                    redirectAttributes.addFlashAttribute("error", "Error al enviar codigo de verificacion. Intente nuevamente.");
                    return "redirect:/auth/secret-key?error";
                }
                
                log.info("OTP sent to email for cedula: {}", cedulaActual);
                
                session.setAttribute("otpToken", otpToken);
                session.setAttribute("otpEmail", maskEmail(email));
                session.setAttribute("otpCedula", cedulaActual);
                return "redirect:/auth/verify-otp";
            } catch (Exception e) {
                log.error("Error sending OTP email", e);
                redirectAttributes.addFlashAttribute("error", "Error al enviar codigo de verificacion. Intente nuevamente.");
                return "redirect:/auth/secret-key?error";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al verificar la clave secreta");
            return "redirect:/auth/secret-key?error";
        }
    }

    /**
     * Pantalla de verificación OTP (Paso 3 para Staff/Admin - MFA).
     */
    @GetMapping("/verify-otp")
    /**
     * Muestra página para ingreso de código OTP (staff/admin step 4).
     */
    public String showOtpPage(Model model, HttpSession session) {
        String otpToken = (String) session.getAttribute("otpToken");
        if (otpToken == null) {
            return "redirect:/auth/login";
        }
        
        String maskedEmail = (String) session.getAttribute("otpEmail");
        model.addAttribute("maskedEmail", maskedEmail);
        model.addAttribute("otpForm", new OtpForm());
        return "auth/verify-otp";
    }

    /**
     * Verificar código OTP (Paso 3 para Staff/Admin - MFA).
     * Maneja ambos flujos:
     * 1. Flujo unificado: usa cedula de session
     * 2. Flujo Didit: usa otpCedula de session
     */
    @PostMapping("/verify-otp")
    /**
     * Verifica código OTP y crea JWT token (staff/admin step 5, autenticación completa).
     */
    public String verifyOtp(
            @ModelAttribute OtpForm form,
            HttpSession session,
            HttpServletResponse response,
            RedirectAttributes redirectAttributes) {
        
        String otpToken = (String) session.getAttribute("otpToken");
        String cedula = (String) session.getAttribute("cedula");
        String otpCedula = (String) session.getAttribute("otpCedula");
        String cedulaActual = cedula != null ? cedula : otpCedula;
        
        if (otpToken == null || cedulaActual == null) {
            log.warn("⚠️  verifyOtp: Missing otpToken or cedula");
            return "redirect:/auth/login";
        }

        log.info("🔐 Verifying OTP for cedula: {}", cedulaActual);

        try {
            boolean isValid = unifiedAuthService.verifyOtp(otpToken, form.getOtpCode());

            if (!isValid) {
                log.warn("Invalid OTP code for cedula: {}", cedulaActual);
                redirectAttributes.addFlashAttribute("error", "Código incorrecto o expirado");
                return "redirect:/auth/verify-otp?error";
            }

            log.info("OTP verified for cedula: {}", cedulaActual);

            // MFA exitoso - Obtener datos del staff_user
            Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(cedulaActual);
            if (!staffUser.isPresent()) {
                log.error("StaffUser not found for cedula: {}", cedulaActual);
                redirectAttributes.addFlashAttribute("error", "Usuario no encontrado. Contacte al administrador.");
                return "redirect:/auth/login";
            }
            
            String dbRole = staffUser.get().getRole(); // ADMIN o ANALISTA
            // Convertir ANALISTA a ANALYST para compatibilidad con ApiGatewayFilter
            String userType = "ANALISTA".equals(dbRole) ? "ANALYST" : dbRole;
            String apiKey = ""; // o obtenerlo de la configuración
            
            // Generar JWT
            String jwt = jwtTokenProvider.generateToken(cedulaActual, userType, apiKey);

            // Guardar JWT en una cookie HttpOnly que se envíe automáticamente
            // IMPORTANTE: Sin "Bearer " porque las cookies no permiten espacios
            Cookie jwtCookie = new Cookie("Authorization", jwt);
            jwtCookie.setHttpOnly(true);
            jwtCookie.setSecure(false); // true en HTTPS, false en desarrollo local
            jwtCookie.setPath("/");
            jwtCookie.setMaxAge(86400); // 24 horas
            response.addCookie(jwtCookie);
            
            // Limpiar sesión
            session.setAttribute("authenticated", true);
            session.setAttribute("authMethod", "DIDIT_STAFF_MFA");
            session.setAttribute("userType", userType);
            session.setAttribute("cedula", cedulaActual);
            session.removeAttribute("otpToken");
            session.removeAttribute("otpEmail");
            session.removeAttribute("otpCedula");
            session.removeAttribute("staffCedula");
            session.removeAttribute("staffFirstName");
            session.removeAttribute("staffLastName");
            session.removeAttribute("staffFullName");

            log.info("MFA successful for {}: {}. Redirecting to {}", userType, cedulaActual, 
                    "ADMIN".equals(userType) ? "/admin/panel" : "/staff/casos");

            // Redirigir según rol
            if ("ADMIN".equals(userType)) {
                return "redirect:/admin/panel";
            } else {
                return "redirect:/staff/casos";
            }

        } catch (Exception e) {
            log.error("Error verifying OTP", e);
            redirectAttributes.addFlashAttribute("error", "Error al verificar el código. Intente nuevamente.");
            return "redirect:/auth/verify-otp?error";
        }
    }

    /**
     * Reenviar código OTP.
     */
    @PostMapping("/resend-otp")
    /**
     * Reenvía OTP (si usuario no recibió código).
     */
    public String resendOtp(HttpSession session, RedirectAttributes redirectAttributes) {
        String cedula = (String) session.getAttribute("cedula");
        if (cedula == null) {
            return "redirect:/auth/login";
        }

        try {
            String email = unifiedAuthService.getStaffEmail(cedula);
            String otpToken = unifiedAuthService.sendEmailOtp(email);
            session.setAttribute("otpToken", otpToken);
            redirectAttributes.addFlashAttribute("success", "Código reenviado exitosamente");
            return "redirect:/auth/verify-otp";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al reenviar el código. Intente en unos minutos.");
            return "redirect:/auth/verify-otp?error";
        }
    }

    /**
     * Enmascara el email para mostrar al usuario (ej: s***n@epn.edu.ec).
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
     * DTO simple para el formulario OTP.
     */
    public static class OtpForm {
        private String otpCode;
        
        public String getOtpCode() { return otpCode; }
        public void setOtpCode(String otpCode) { this.otpCode = otpCode; }
    }

    /**
     * Logout unificado.
     */
    @GetMapping("/logout")
    /**
     * Logout: Invalida sesión y JWT token.
     */
    public String logout(HttpSession session) {
        session.invalidate();
        return "redirect:/auth/login?logout";
    }
    
    /**
     * Genera un hash SHA-256 de la cédula para identificación anónima.
     */
    private String hashCedula(String cedula) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(cedula.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash", e);
        }
    }
   
}


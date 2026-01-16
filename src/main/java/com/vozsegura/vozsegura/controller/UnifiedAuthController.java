package com.vozsegura.vozsegura.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.vozsegura.dto.forms.SecretKeyForm;
import com.vozsegura.vozsegura.dto.forms.UnifiedLoginForm;
import com.vozsegura.vozsegura.service.CloudflareTurnstileService;
import com.vozsegura.vozsegura.service.UnifiedAuthService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

/**
 * Controlador de Autenticación Unificada (ZTA - Zero Trust Architecture).
 *
 * Flujo de autenticación:
 * 1. Todos los usuarios (denunciantes, staff, admin) ingresan por /denuncia
 * 2. Se verifica contra Registro Civil + Cloudflare Turnstile
 * 3. Si es Staff/Admin -> Solicitar clave secreta AWS
 * 4. Si es Staff/Admin -> Enviar OTP por email (MFA)
 * 5. Si es Denunciante -> Continuar a verificación biométrica
 * 6. Enrutamiento según rol (API Gateway pattern)
 */
@Controller
@RequestMapping("/auth")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;
    private final CloudflareTurnstileService turnstileService;

    public UnifiedAuthController(UnifiedAuthService unifiedAuthService, CloudflareTurnstileService turnstileService) {
        this.unifiedAuthService = unifiedAuthService;
        this.turnstileService = turnstileService;
    }

    /**
     * Pantalla de login unificado (punto de entrada único).
     */
    @GetMapping("/login")
    public String showLoginPage(Model model, HttpSession session) {
        // Inyectar site key de Turnstile (pública, segura en frontend)
        model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
        model.addAttribute("unifiedLoginForm", new UnifiedLoginForm());

        return "public/denuncia-login";
    }

    /**
     * Procesar login unificado (Paso 1: Verificación Registro Civil + Turnstile).
     */
    @PostMapping("/unified-login")
    public String processUnifiedLogin(
            @Valid @ModelAttribute UnifiedLoginForm form,
            BindingResult result,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes,
            Model model) {

        System.out.println("[UNIFIED AUTH] Processing login for cedula: " + form.getCedula());

        // Validaciones básicas del formulario
        if (result.hasErrors()) {
            System.out.println("[UNIFIED AUTH] Validation errors: " + result.getAllErrors());
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Por favor complete todos los campos correctamente");
            return "public/denuncia-login";
        }

        // Validar token de Turnstile
        String turnstileToken = request.getParameter("cf-turnstile-response");
        if (turnstileToken == null || turnstileToken.isBlank()) {
            System.out.println("[UNIFIED AUTH] Turnstile token no proporcionado");
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "Verificación de Turnstile fallida. Por favor intente nuevamente.");
            return "public/denuncia-login";
        }

        // Obtener IP del cliente para validación adicional
        String clientIp = getClientIp(request);
        
        // Verificar token Turnstile
        if (!turnstileService.verifyTurnstileToken(turnstileToken, clientIp)) {
            System.out.println("[UNIFIED AUTH] Turnstile validation failed for IP: " + clientIp);
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", "No se pudo verificar que eres una persona. Por favor intenta de nuevo.");
            return "public/denuncia-login";
        }

        try {
            System.out.println("[UNIFIED AUTH] Verifying identity...");

            // Paso 1: Verificar identidad contra Registro Civil (sin CAPTCHA)
            String citizenRef = unifiedAuthService.verifyCitizenIdentity(
                form.getCedula(),
                form.getCodigoDactilar()
            );

            System.out.println("[UNIFIED AUTH] Identity verified: " + citizenRef);

            // Guardar citizenRef en sesión
            session.setAttribute("citizenRef", citizenRef);
            session.setAttribute("cedula", form.getCedula());

            // Paso 2: Determinar tipo de usuario
            UnifiedAuthService.UserType userType = unifiedAuthService.getUserType(form.getCedula());
            session.setAttribute("userType", userType.name());

            System.out.println("[UNIFIED AUTH] User type: " + userType);

            // Enrutamiento según tipo de usuario (API Gateway Pattern)
            switch (userType) {
                case ADMIN:
                case ANALYST:
                    // Staff/Admin: Solicitar clave secreta
                    System.out.println("[UNIFIED AUTH] Redirecting to secret key page");
                    return "redirect:/auth/secret-key";

                case DENUNCIANTE:
                default:
                    // Denunciante: Generar hash anónimo y continuar a verificación biométrica
                    String citizenHash = hashCedula(form.getCedula());
                    session.setAttribute("citizenHash", citizenHash);
                    System.out.println("[UNIFIED AUTH] Citizen hash generated and saved to session");
                    System.out.println("[UNIFIED AUTH] Redirecting to biometric verification");
                    return "redirect:/denuncia/biometric";
            }

        } catch (SecurityException e) {
            System.err.println("[UNIFIED AUTH] Security exception: " + e.getMessage());
            model.addAttribute("turnstileSiteKey", turnstileService.getSiteKey());
            model.addAttribute("error", e.getMessage());
            return "public/denuncia-login";
        } catch (Exception e) {
            System.err.println("[UNIFIED AUTH] Unexpected error: " + e.getMessage());
            e.printStackTrace();
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
     * Pantalla de clave secreta (Paso 2 para Staff/Admin).
     */
    @GetMapping("/secret-key")
    public String showSecretKeyPage(Model model, HttpSession session) {
        // Verificar que ya pasó la verificación inicial
        String citizenRef = (String) session.getAttribute("citizenRef");
        if (citizenRef == null) {
            return "redirect:/auth/login";
        }

        model.addAttribute("secretKeyForm", new SecretKeyForm());
        return "auth/secret-key";
    }

    /**
     * Verificar clave secreta (Paso 2 para Staff/Admin).
     */
    @PostMapping("/verify-secret")
    public String verifySecretKey(
            @Valid @ModelAttribute SecretKeyForm form,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes) {

        String cedula = (String) session.getAttribute("cedula");
        if (cedula == null) {
            return "redirect:/auth/login";
        }

        if (result.hasErrors()) {
            redirectAttributes.addFlashAttribute("error", "La clave secreta es requerida");
            return "redirect:/auth/secret-key?error";
        }

        try {
            // Verificar clave secreta contra AWS Secrets Manager
            boolean isValid = unifiedAuthService.validateSecretKey(cedula, form.getSecretKey());

            if (!isValid) {
                redirectAttributes.addFlashAttribute("error", "Clave secreta incorrecta");
                return "redirect:/auth/secret-key?error";
            }

            // Clave secreta válida - Enviar OTP por email (MFA)
            String userType = (String) session.getAttribute("userType");
            String email = unifiedAuthService.getStaffEmail(cedula);
            
            System.out.println("[UNIFIED AUTH] Staff email for " + cedula + ": " + (email != null ? maskEmail(email) : "NOT CONFIGURED"));
            
            if (email == null || email.isBlank()) {
                // Si no tiene email configurado, mostrar error - MFA es obligatorio
                System.err.println("[UNIFIED AUTH] ERROR: No email configured for staff " + cedula);
                redirectAttributes.addFlashAttribute("error", "Error de configuracion: Email no configurado para MFA. Contacte al administrador.");
                return "redirect:/auth/secret-key?error";
            }
            
            // Enviar OTP por email
            try {
                System.out.println("[UNIFIED AUTH] Sending OTP to " + maskEmail(email) + "...");
                String otpToken = unifiedAuthService.sendEmailOtp(email);
                
                if (otpToken == null) {
                    System.err.println("[UNIFIED AUTH] ERROR: OTP token is null - email sending failed");
                    redirectAttributes.addFlashAttribute("error", "Error al enviar codigo de verificacion. Intente nuevamente.");
                    return "redirect:/auth/secret-key?error";
                }
                
                session.setAttribute("otpToken", otpToken);
                session.setAttribute("otpEmail", maskEmail(email));
                System.out.println("[UNIFIED AUTH] OTP sent successfully to " + maskEmail(email));
                return "redirect:/auth/verify-otp";
            } catch (Exception e) {
                System.err.println("[UNIFIED AUTH] ERROR sending OTP: " + e.getMessage());
                e.printStackTrace();
                redirectAttributes.addFlashAttribute("error", "Error al enviar codigo de verificacion: " + e.getMessage());
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
     */
    @PostMapping("/verify-otp")
    public String verifyOtp(
            @ModelAttribute OtpForm form,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        
        String otpToken = (String) session.getAttribute("otpToken");
        String cedula = (String) session.getAttribute("cedula");
        
        if (otpToken == null || cedula == null) {
            return "redirect:/auth/login";
        }

        try {
            boolean isValid = unifiedAuthService.verifyOtp(otpToken, form.getOtpCode());
            
            if (!isValid) {
                redirectAttributes.addFlashAttribute("error", "Código incorrecto o expirado");
                return "redirect:/auth/verify-otp?error";
            }

            // MFA exitoso - Crear sesión autenticada
            String userType = (String) session.getAttribute("userType");
            session.setAttribute("authenticated", true);
            session.setAttribute("authMethod", "UNIFIED_ZTA_MFA");
            session.removeAttribute("otpToken");
            session.removeAttribute("otpEmail");

            System.out.println("[UNIFIED AUTH] MFA successful for " + cedula);

            // Redirigir según rol
            if ("ADMIN".equals(userType)) {
                return "redirect:/admin";
            } else {
                return "redirect:/staff/casos";
            }

        } catch (Exception e) {
            System.err.println("[UNIFIED AUTH] Error verifying OTP: " + e.getMessage());
            redirectAttributes.addFlashAttribute("error", "Error al verificar el código");
            return "redirect:/auth/verify-otp?error";
        }
    }

    /**
     * Reenviar código OTP.
     */
    @PostMapping("/resend-otp")
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


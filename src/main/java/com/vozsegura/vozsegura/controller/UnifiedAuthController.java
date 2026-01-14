package com.vozsegura.vozsegura.controller;

import com.vozsegura.vozsegura.dto.forms.SecretKeyForm;
import com.vozsegura.vozsegura.dto.forms.UnifiedLoginForm;
import com.vozsegura.vozsegura.service.CaptchaService;
import com.vozsegura.vozsegura.service.UnifiedAuthService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Controlador de Autenticación Unificada (ZTA - Zero Trust Architecture).
 *
 * Flujo de autenticación:
 * 1. Todos los usuarios (denunciantes, staff, admin) ingresan por /denuncia
 * 2. Se verifica contra Registro Civil + CAPTCHA
 * 3. Si es Staff/Admin -> Solicitar clave secreta AWS
 * 4. Si es Denunciante -> Continuar a verificación biométrica
 * 5. Enrutamiento según rol (API Gateway pattern)
 */
@Controller
@RequestMapping("/auth")
public class UnifiedAuthController {

    private final UnifiedAuthService unifiedAuthService;
    private final CaptchaService captchaService;

    public UnifiedAuthController(UnifiedAuthService unifiedAuthService, CaptchaService captchaService) {
        this.unifiedAuthService = unifiedAuthService;
        this.captchaService = captchaService;
    }

    /**
     * Pantalla de login unificado (punto de entrada único).
     */
    @GetMapping("/login")
    public String showLoginPage(Model model, HttpSession session) {
        // Generar CAPTCHA único para esta sesión
        String captchaCode = captchaService.generateCaptcha(session.getId());

        model.addAttribute("unifiedLoginForm", new UnifiedLoginForm());
        model.addAttribute("captchaCode", captchaCode);

        return "public/denuncia-login";
    }

    /**
     * Procesar login unificado (Paso 1: Verificación Registro Civil).
     */
    @PostMapping("/unified-login")
    public String processUnifiedLogin(
            @Valid @ModelAttribute UnifiedLoginForm form,
            BindingResult result,
            HttpSession session,
            RedirectAttributes redirectAttributes,
            Model model) {

        System.out.println("[UNIFIED AUTH] Processing login for cedula: " + form.getCedula());

        if (result.hasErrors()) {
            System.out.println("[UNIFIED AUTH] Validation errors: " + result.getAllErrors());
            // Regenerar CAPTCHA en caso de error
            String captchaCode = captchaService.generateCaptcha(session.getId());
            model.addAttribute("captchaCode", captchaCode);
            model.addAttribute("error", "Por favor complete todos los campos correctamente");
            return "public/denuncia-login";
        }

        try {
            System.out.println("[UNIFIED AUTH] Verifying identity...");

            // Paso 1: Verificar identidad contra Registro Civil
            String citizenRef = unifiedAuthService.verifyCitizenIdentity(
                form.getCedula(),
                form.getCodigoDactilar(),
                form.getCaptcha(),
                session.getId()
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
            // Regenerar CAPTCHA en caso de error
            String captchaCode = captchaService.generateCaptcha(session.getId());
            model.addAttribute("captchaCode", captchaCode);
            model.addAttribute("error", e.getMessage());
            return "public/denuncia-login";
        } catch (Exception e) {
            System.err.println("[UNIFIED AUTH] Unexpected error: " + e.getMessage());
            e.printStackTrace();
            // Regenerar CAPTCHA en caso de error
            String captchaCode = captchaService.generateCaptcha(session.getId());
            model.addAttribute("captchaCode", captchaCode);
            model.addAttribute("error", "Error al procesar la autenticación. Por favor intente nuevamente.");
            return "public/denuncia-login";
        }
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

            // Autenticación exitosa - Crear sesión Spring Security
            String userType = (String) session.getAttribute("userType");

            // Marcar como autenticado
            session.setAttribute("authenticated", true);
            session.setAttribute("authMethod", "UNIFIED_ZTA");

            // Redirigir según rol
            if ("ADMIN".equals(userType)) {
                return "redirect:/admin";
            } else {
                return "redirect:/staff/casos";
            }

        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("error", "Error al verificar la clave secreta");
            return "redirect:/auth/secret-key?error";
        }
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


package com.vozsegura.vozsegura.controller.publicview;

import com.vozsegura.vozsegura.dto.forms.BiometricOtpForm;
import com.vozsegura.vozsegura.dto.forms.ComplaintForm;
import com.vozsegura.vozsegura.dto.forms.DenunciaAccessForm;
import com.vozsegura.vozsegura.security.RateLimiter;
import com.vozsegura.vozsegura.service.ComplaintService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

@Controller
@SessionAttributes({"denunciaAccessToken", "citizenHash"})
public class PublicComplaintController {

    private final RateLimiter rateLimiter;
    private final ComplaintService complaintService;

    public PublicComplaintController(RateLimiter rateLimiter, ComplaintService complaintService) {
        this.rateLimiter = rateLimiter;
        this.complaintService = complaintService;
    }

    @ModelAttribute("denunciaAccessForm")
    public DenunciaAccessForm accessForm() {
        return new DenunciaAccessForm();
    }

    @GetMapping("/denuncia")
    public String showAccessForm() {
        // Redirigir al login unificado (ZTA)
        return "redirect:/auth/login";
    }

    @PostMapping("/denuncia/verify")
    public String verifyCitizen(@Valid @ModelAttribute("denunciaAccessForm") DenunciaAccessForm form,
                                BindingResult bindingResult,
                                Model model) {
        if (!form.isTermsAccepted()) {
            bindingResult.rejectValue("termsAccepted", "terms.required", "Debe aceptar los términos y condiciones.");
        }
        if (bindingResult.hasErrors()) {
            return "public/denuncia-login";
        }
        // Placeholder: validación con API externa y captcha
        
        // Generar hash del ciudadano para identificación anónima
        String citizenHash = hashCedula(form.getCedula());
        model.addAttribute("citizenHash", citizenHash);
        model.addAttribute("denunciaAccessToken", "ACCESS-GRANTED");
        model.addAttribute("biometricOtpForm", new BiometricOtpForm());
        return "public/denuncia-biometric";
    }

    /**
     * Muestra la página de verificación biométrica.
     * Endpoint GET para acceder después de autenticación unificada.
     */
    @GetMapping("/denuncia/biometric")
    public String showBiometricPage(Model model) {
        model.addAttribute("biometricOtpForm", new BiometricOtpForm());
        return "public/denuncia-biometric";
    }

    @PostMapping("/denuncia/biometric")
    public String verifyBiometric(@Valid @ModelAttribute("biometricOtpForm") BiometricOtpForm form,
                                  BindingResult bindingResult,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "public/denuncia-biometric";
        }
        model.addAttribute("complaintForm", new ComplaintForm());
        return "public/denuncia-form";
    }

    /**
     * Muestra el formulario de denuncia.
     * Endpoint GET para acceder después de verificación biométrica.
     */
    @GetMapping("/denuncia/form")
    public String showComplaintForm(Model model) {
        model.addAttribute("complaintForm", new ComplaintForm());
        return "public/denuncia-form";
    }

    @PostMapping("/denuncia/submit")
    public String submitComplaint(@Valid @ModelAttribute("complaintForm") ComplaintForm form,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  SessionStatus sessionStatus,
                                  RedirectAttributes redirectAttributes,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "public/denuncia-form";
        }
        
        // Obtener citizenHash de la sesión HTTP
        String citizenHash = (String) session.getAttribute("citizenHash");
        if (citizenHash == null) {
            model.addAttribute("error", "Sesión expirada. Por favor inicie sesión nuevamente.");
            return "public/denuncia-form";
        }
        
        try {
            // Guardar denuncia en la base de datos
            String trackingId = complaintService.createComplaint(form, citizenHash);
            
            // Limpiar sesión
            sessionStatus.setComplete();
            
            // Pasar tracking ID a la vista de confirmación
            redirectAttributes.addFlashAttribute("trackingId", trackingId);
            redirectAttributes.addFlashAttribute("success", true);
            
            return "redirect:/denuncia/confirmacion";
        } catch (Exception e) {
            model.addAttribute("error", "Error al procesar la denuncia. Por favor intente nuevamente.");
            return "public/denuncia-form";
        }
    }
    
    @GetMapping("/denuncia/confirmacion")
    public String showConfirmation(Model model) {
        // Los atributos flash (trackingId, success) se agregan automáticamente al model
        return "public/denuncia-confirmacion";
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

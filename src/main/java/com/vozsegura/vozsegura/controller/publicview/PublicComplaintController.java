package com.vozsegura.vozsegura.controller.publicview;

import com.vozsegura.vozsegura.dto.forms.BiometricOtpForm;
import com.vozsegura.vozsegura.dto.forms.ComplaintForm;
import com.vozsegura.vozsegura.dto.forms.DenunciaAccessForm;
import com.vozsegura.vozsegura.security.RateLimiter;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;

@Controller
@SessionAttributes({"denunciaAccessToken"})
public class PublicComplaintController {

    private final RateLimiter rateLimiter;

    public PublicComplaintController(RateLimiter rateLimiter) {
        this.rateLimiter = rateLimiter;
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
                                  SessionStatus sessionStatus) {
        if (bindingResult.hasErrors()) {
            return "public/denuncia-form";
        }
        // Placeholder: creación de denuncia y guardado cifrado
        sessionStatus.setComplete();
        return "redirect:/denuncia?enviada";
    }
}

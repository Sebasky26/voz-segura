package com.vozsegura.vozsegura.controller.publicview;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.Optional;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.dto.forms.AdditionalInfoForm;
import com.vozsegura.vozsegura.dto.forms.BiometricOtpForm;
import com.vozsegura.vozsegura.dto.forms.ComplaintForm;
import com.vozsegura.vozsegura.dto.forms.DenunciaAccessForm;
import com.vozsegura.vozsegura.security.RateLimiter;
import com.vozsegura.vozsegura.service.ComplaintService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;

@Controller
@SessionAttributes({"denunciaAccessToken"})
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
        // Rate limiting para prevenir abuso
        String clientId = "verify-" + form.getCedula();
        if (!rateLimiter.tryConsume(clientId)) {
            model.addAttribute("error", "Demasiados intentos. Por favor espere un momento.");
            return "public/denuncia-login";
        }
        
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
    public String showBiometricPage(HttpSession session, Model model) {
        // Verificar que el usuario esté autenticado
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return "redirect:/auth/login?session_expired";
        }

        model.addAttribute("biometricOtpForm", new BiometricOtpForm());
        return "public/denuncia-biometric";
    }

    @PostMapping("/denuncia/biometric")
    public String verifyBiometric(@Valid @ModelAttribute("biometricOtpForm") BiometricOtpForm form,
                                  BindingResult bindingResult,
                                  HttpSession session,
                                  Model model) {
        if (bindingResult.hasErrors()) {
            return "public/denuncia-biometric";
        }

        // Marcar que el usuario completó la verificación biométrica
        session.setAttribute("biometricVerified", true);

        // Redirigir a página de opciones
        return "redirect:/denuncia/opciones";
    }

    /**
     * Muestra las opciones después de verificación: crear denuncia o consultar seguimiento.
     */
    @GetMapping("/denuncia/opciones")
    public String showOptions(HttpSession session, Model model) {
        // Verificar que el usuario esté autenticado
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return "redirect:/auth/login?session_expired";
        }

        return "public/denuncia-opciones";
    }

    /**
     * Muestra el formulario de denuncia.
     * Endpoint GET para acceder después de verificación biométrica.
     */
    @GetMapping("/denuncia/form")
    public String showComplaintForm(HttpSession session, Model model) {
        // Verificar que el usuario esté autenticado
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return "redirect:/auth/login?session_expired";
        }

        model.addAttribute("complaintForm", new ComplaintForm());
        return "public/denuncia-form";
    }

    @PostMapping("/denuncia/submit")
    public String submitComplaint(@Valid @ModelAttribute("complaintForm") ComplaintForm form,
                                  BindingResult bindingResult,
                                  HttpSession session,
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
     * Muestra formulario para agregar información adicional a una denuncia.
     */
    @GetMapping("/denuncia/editar/{trackingId}")
    public String showEditForm(@PathVariable("trackingId") String trackingId,
                               HttpSession session,
                               Model model,
                               RedirectAttributes redirectAttributes) {
        // Verificar autenticación
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return "redirect:/auth/login?session_expired";
        }

        // Buscar la denuncia
        Optional<Complaint> complaintOpt = complaintService.findByTrackingId(trackingId);
        if (complaintOpt.isEmpty()) {
            redirectAttributes.addFlashAttribute("error", "Denuncia no encontrada");
            return "redirect:/seguimiento";
        }

        Complaint complaint = complaintOpt.get();

        // Verificar que la denuncia requiere más información
        if (!complaint.isRequiresMoreInfo()) {
            redirectAttributes.addFlashAttribute("error", "Esta denuncia no requiere información adicional");
            return "redirect:/seguimiento";
        }

        // Verificar que el ciudadano es el dueño de la denuncia
        String citizenHash = (String) session.getAttribute("citizenHash");
        if (citizenHash == null || !citizenHash.equals(complaint.getIdentityVault().getCitizenHash())) {
            redirectAttributes.addFlashAttribute("error", "No tienes permiso para editar esta denuncia");
            return "redirect:/seguimiento";
        }

        model.addAttribute("trackingId", trackingId);
        model.addAttribute("analystNotes", complaint.getAnalystNotes());
        model.addAttribute("additionalInfoForm", new AdditionalInfoForm());

        return "public/denuncia-editar";
    }

    /**
     * Procesa la información adicional enviada por el denunciante.
     */
    @PostMapping("/denuncia/editar/{trackingId}")
    public String submitAdditionalInfo(@PathVariable("trackingId") String trackingId,
                                       @Valid @ModelAttribute AdditionalInfoForm form,
                                       BindingResult bindingResult,
                                       HttpSession session,
                                       RedirectAttributes redirectAttributes,
                                       Model model) {
        // Verificar autenticación
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            return "redirect:/auth/login?session_expired";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("trackingId", trackingId);
            return "public/denuncia-editar";
        }

        try {
            // Agregar información adicional a la denuncia
            complaintService.addAdditionalInfo(trackingId, form.getAdditionalInfo(), form.getEvidences());

            redirectAttributes.addFlashAttribute("success", "Información adicional enviada correctamente");
            return "redirect:/seguimiento";
        } catch (Exception e) {
            model.addAttribute("error", "Error al enviar la información. Intente nuevamente.");
            model.addAttribute("trackingId", trackingId);
            return "public/denuncia-editar";
        }
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

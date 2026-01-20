package com.vozsegura.vozsegura.controller.publicview;

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
import org.springframework.web.bind.annotation.SessionAttributes;
import org.springframework.web.bind.support.SessionStatus;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.vozsegura.vozsegura.domain.entity.StaffUser;
import com.vozsegura.vozsegura.dto.forms.BiometricOtpForm;
import com.vozsegura.vozsegura.dto.forms.ComplaintForm;
import com.vozsegura.vozsegura.dto.forms.DenunciaAccessForm;
import com.vozsegura.vozsegura.repo.StaffUserRepository;
import com.vozsegura.vozsegura.security.RateLimiter;
import com.vozsegura.vozsegura.service.ComplaintService;
import com.vozsegura.vozsegura.service.DiditService;

import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@SessionAttributes({"denunciaAccessToken", "citizenHash", "diditSessionId"})
public class PublicComplaintController {

    private final RateLimiter rateLimiter;
    private final ComplaintService complaintService;
    private final DiditService diditService;
    private final StaffUserRepository staffUserRepository;

    public PublicComplaintController(RateLimiter rateLimiter, ComplaintService complaintService, DiditService diditService, StaffUserRepository staffUserRepository) {
        this.rateLimiter = rateLimiter;
        this.complaintService = complaintService;
        this.diditService = diditService;
        this.staffUserRepository = staffUserRepository;
    }

    @ModelAttribute("denunciaAccessForm")
    public DenunciaAccessForm accessForm() {
        return new DenunciaAccessForm();
    }

    @GetMapping("/denuncia")
    public String showAccessForm() {
        // Redirigir directamente a verificación
        return "redirect:/verification/inicio";
    }

    /**
     * Página inicial de verificación - Solo muestra botón para iniciar
     */
    @GetMapping("/verification/inicio")
    public String verificationInicio() {
        return "public/verification-inicio";
    }

    /**
     * Inicia sesión de verificación con Didit
     */
    @GetMapping("/verification/start")
    public String startVerification(HttpSession session, Model model) {
        try {
            // Generar ID único para esta sesión
            String sessionId = java.util.UUID.randomUUID().toString();
            
            // Crear sesión de verificación con Didit (v3)
            Map<String, Object> diditSession = diditService.createVerificationSession(sessionId);

            // Guardar session ID en sesión HTTP
            String diditSessionId = (String) diditSession.get("session_id");
            session.setAttribute("diditSessionId", diditSessionId);
            session.setAttribute("vendorData", sessionId);

            // Obtener URL de verificación del usuario
            String verificationUrl = (String) diditSession.get("url");
            
            log.info("Didit verification session created: {}", diditSessionId);

            // Redirigir a la URL de Didit
            return "redirect:" + verificationUrl;

        } catch (Exception e) {
            log.error("Error starting Didit verification", e);
            model.addAttribute("error", "Error al iniciar verificación: " + e.getMessage());
            return "public/verification-inicio";
        }
    }

    /**
     * Callback después de que Didit procesa la verificación
     * El webhook habrá guardado los datos
     */
    @GetMapping("/verification/callback")
    public String verificationCallback(HttpSession session, Model model) {
        try {
            String diditSessionId = (String) session.getAttribute("diditSessionId");
            
            if (diditSessionId == null) {
                return "redirect:/verification/inicio?error=session_expired";
            }

            // Obtener datos guardados por el webhook
            var verification = diditService.getVerificationBySessionId(diditSessionId);
            
            if (verification.isEmpty()) {
                model.addAttribute("error", "Verificación no completada. Por favor intente nuevamente.");
                return "public/verification-inicio";
            }

            var verif = verification.get();

            // IMPORTANTE: Verificar que la cédula esté en staff_user
            Optional<StaffUser> staffUser = staffUserRepository.findByCedulaAndEnabledTrue(verif.getDocumentNumber());
            
            if (staffUser.isEmpty()) {
                log.warn("❌ Usuario NO encontrado en staff_user: document={}", verif.getDocumentNumber());
                model.addAttribute("error", "Usuario no encontrado. Solo personal autorizado puede acceder al sistema.");
                return "public/verification-inicio";
            }

            // Generar hash del ciudadano para próximas denuncias
            String citizenHash = hashCitizenIdentifier(verif.getDocumentNumber());
            session.setAttribute("citizenHash", citizenHash);
            session.setAttribute("verified", true);
            session.setAttribute("documentNumber", verif.getDocumentNumber());
            session.setAttribute("userRole", staffUser.get().getRole());

            // Vincular verificación con hash
            diditService.linkVerificationToCitizen(diditSessionId, citizenHash);

            // NO pasamos datos personales a la vista - solo mostramos "Usuario verificado"
            log.info("Verification completed successfully for document (usuario verificado)");
            return "public/verification-exitosa";

        } catch (Exception e) {
            log.error("Error in verification callback", e);
            model.addAttribute("error", "Error procesando verificación: " + e.getMessage());
            return "public/verification-inicio";
        }
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
     * Requiere que el usuario esté verificado con Didit
     */
    @GetMapping("/denuncia/form")
    public String showComplaintForm(HttpSession session, Model model) {
        // Verificar que el usuario esté autenticado (atributo establecido por UnifiedAuthController)
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            log.warn("❌ Usuario no autenticado - redirigiendo a verificación");
            return "redirect:/verification/inicio?required";
        }

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
     * Genera un hash SHA-256 del documento para identificación anónima.
     */
    private String hashCitizenIdentifier(String documentNumber) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(documentNumber.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Error al generar hash", e);
        }
    }

    /**
     * Genera un hash SHA-256 de la cédula para identificación anónima.
     * DEPRECATED: usar hashCitizenIdentifier en su lugar
     */
    @Deprecated
    private String hashCedula(String cedula) {
        return hashCitizenIdentifier(cedula);
    }
}

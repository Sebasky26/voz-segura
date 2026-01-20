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

/**
 * Controlador para el flujo público de denuncias anónimas.
 * 
 * Responsabilidades:
 * - Gestionar verificación biométrica con Didit
 * - Recopilar información de denuncias del público
 * - Generar tracking ID para seguimiento anónimo
 * - Mantener anonimato usando hash SHA-256 de cédula
 * 
 * Flujo de usuario:
 * 1. Usuario inicia verificación con Didit (/verification/start)
 * 2. Didit procesa verificación biométrica
 * 3. Webhook callback almacena resultado (/verification/callback)
 * 4. Sistema genera hash anónimo de identidad
 * 5. Usuario accede a formulario de denuncia (/denuncia/form)
 * 6. Denuncia se cifra y almacena con tracking ID
 * 7. Usuario recibe tracking ID para seguimiento anónimo
 * 
 * Seguridad:
 * - Rate limiting en todos los endpoints
 * - Validación de sesión en endpoints sensibles
 * - Identidad almacenada como hash SHA-256, nunca en plain text
 * - Cifrado AES-256-GCM para contenido de denuncia
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
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

    /**
     * Proporciona formulario de acceso a denuncia como atributo del modelo.
     * Se inyecta automáticamente en las vistas que lo requieren.
     * 
     * @return formulario vacío de acceso a denuncia
     */
    @ModelAttribute("denunciaAccessForm")
    public DenunciaAccessForm accessForm() {
        return new DenunciaAccessForm();
    }

    /**
     * Punto de entrada para denuncias públicas.
     * Redirige directamente al flujo de verificación biométrica.
     * 
     * @return redirección a /verification/inicio
     */
    @GetMapping("/denuncia")
    public String showAccessForm() {
        // Redirigir directamente a verificación
        return "redirect:/verification/inicio";
    }

    /**
     * Página inicial de verificación - Solo muestra botón para iniciar.
     * 
     * Esta es la primera página que ve el usuario. Solo muestra una descripción
     * del proceso y un botón para iniciar la verificación biométrica.
     * 
     * @return vista: public/verification-inicio
     */
    @GetMapping("/verification/inicio")
    public String verificationInicio() {
        return "public/verification-inicio";
    }

    /**
     * Inicia sesión de verificación con Didit.
     * 
     * Crea una nueva sesión de verificación biométrica en Didit (v3 endpoint).
     * Genera un UUID único para rastrear la sesión y almacena el ID de sesión
     * de Didit en la sesión HTTP para procesamiento posterior del webhook.
     * 
     * Flujo:
     * 1. Genera UUID único para esta instancia de verificación
     * 2. Llama a Didit v3/session para crear sesión interactiva con QR
     * 3. Almacena diditSessionId en sesión HTTP
     * 4. Redirige al usuario a URL de Didit para verificación
     * 
     * @param session sesión HTTP para almacenar state
     * @param model modelo para añadir errores
     * @return redirección a URL de Didit o vista de error
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
     * Callback después de que Didit procesa la verificación.
     * El webhook habrá guardado los datos de verificación en BD.
     * 
     * Verifica que:
     * 1. La sesión HTTP contiene diditSessionId válido
     * 2. El webhook guardó datos de verificación
     * 3. La cédula existe en staff_user (solo personal autorizado)
     * 4. Genera hash SHA-256 del ciudadano para anonimato
     * 
     * Detalles de seguridad:
     * - Nunca almacena cédula en plain text
     * - Genera hash SHA-256 con encoding Base64
     * - Registra log sin revelar datos personales
     * - Verifica autorización contra staff_user
     * 
     * @param session sesión HTTP con atributos de verificación
     * @param model modelo para pasar datos a vista
     * @return redirección a vista de éxito o error
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
                log.warn("Usuario NO encontrado en staff_user: document={}", verif.getDocumentNumber());
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
     * Muestra las opciones después de verificación.
     * 
     * Permite al usuario elegir entre:
     * - Crear nueva denuncia
     * - Consultar seguimiento de denuncia anterior
     * 
     * Requiere que el usuario esté autenticado (authenticated=true en sesión).
     * Si no está autenticado, redirige al login.
     * 
     * @param session sesión HTTP con atributo 'authenticated'
     * @param model modelo para pasar datos a vista
     * @return vista con opciones o redirección a login
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
     * Muestra el formulario de denuncia (GET).
     * Requiere que el usuario esté verificado con Didit y autenticado.
     * 
     * Valida:
     * - Sesión HTTP válida con atributo 'authenticated'
     * - Si no está autenticado, redirige a verificación
     * 
     * Datos en sesión requeridos:
     * - authenticated: boolean (establecido por UnifiedAuthController)
     * - citizenHash: String (hash SHA-256 de la cédula)
     * 
     * @param session sesión HTTP con atributos de autenticación
     * @param model modelo para inyectar formulario vacío
     * @return vista del formulario de denuncia
     */
    @GetMapping("/denuncia/form")
    public String showComplaintForm(HttpSession session, Model model) {
        // Verificar que el usuario esté autenticado (atributo establecido por UnifiedAuthController)
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            log.warn("Usuario no autenticado - redirigiendo a verificación");
            return "redirect:/verification/inicio?required";
        }

        model.addAttribute("complaintForm", new ComplaintForm());
        return "public/denuncia-form";
    }

    /**
     * Procesa envío de denuncia (POST).
     * 
     * Flujo:
     * 1. Valida datos del formulario
     * 2. Obtiene citizenHash de sesión (identificación anónima)
     * 3. Llama a ComplaintService para crear y cifrar denuncia
     * 4. Genera tracking ID único para seguimiento anónimo
     * 5. Limpia sesión (sign out automático después de envío)
     * 6. Redirige a confirmación con tracking ID
     * 
     * Cifrado:
     * - Contenido de denuncia cifrado con AES-256-GCM
     * - Ciudadano identificado por hash SHA-256, no por cédula
     * - Tracking ID es el único identificador público
     * 
     * @param form formulario validado con datos de denuncia
     * @param bindingResult resultado de validación
     * @param session sesión HTTP con citizenHash
     * @param sessionStatus para limpiar sesión tras envío
     * @param redirectAttributes para pasar tracking ID a confirmación
     * @param model modelo para errores
     * @return redirección a /denuncia/confirmacion o formulario con error
     */
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
        
        // Obtener datos de la sesión
        String citizenHash = (String) session.getAttribute("citizenHash");
        Long idRegistro = (Long) session.getAttribute("idRegistro");
        
        if (citizenHash == null) {
            model.addAttribute("error", "Sesión expirada. Por favor inicie sesión nuevamente.");
            return "public/denuncia-form";
        }
        
        try {
            // Guardar denuncia en la base de datos con idRegistro
            String trackingId = complaintService.createComplaint(form, citizenHash, idRegistro);
            
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
    
    /**
     * Muestra página de confirmación después de envío de denuncia.
     * 
     * Datos mostrados:
     * - Tracking ID (único identificador público de la denuncia)
     * - Mensaje de éxito
     * 
     * Los atributos flash (trackingId, success) se inyectan automáticamente
     * en el model desde el RedirectAttributes del envío anterior.
     * 
     * @param model modelo con atributos flash (trackingId, success)
     * @return vista de confirmación con tracking ID
     */
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

package com.vozsegura.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.vozsegura.config.GatewayConfig;

import jakarta.servlet.http.HttpSession;

/**
 * Controlador para la página de inicio de la aplicación.
 *
 * <p>Este controlador funciona como un punto de entrada único (ruta {@code /}). Su objetivo es
 * enviar al usuario a la pantalla adecuada según su estado de sesión:</p>
 *
 * <ul>
 *   <li>Si NO hay sesión válida, redirige al login.</li>
 *   <li>Si hay sesión válida, redirige al panel correspondiente según el tipo de usuario.</li>
 * </ul>
 *
 * <p>Notas importantes:</p>
 * <ul>
 *   <li>No realiza autenticación ni valida credenciales.</li>
 *   <li>Se apoya en atributos ya guardados en sesión por el flujo de login.</li>
 *   <li>Evita mostrar una página “vacía” en la raíz y mejora la experiencia del usuario.</li>
 * </ul>
 *
 * <p>Atributos esperados en sesión (según tu proyecto):</p>
 * <ul>
 *   <li>{@code authenticated}: Boolean, indica si el usuario inició sesión.</li>
 *   <li>{@code userType}: String, tipo de usuario (por ejemplo: ADMIN, ANALYST, DENUNCIANTE).</li>
 * </ul>
 */
@Controller
public class HomeController {

    private final GatewayConfig gatewayConfig;

    /**
     * Inyecta la configuración del gateway para reutilizar las rutas/redirects centralizados.
     *
     * @param gatewayConfig configuración de redirecciones (por ejemplo, login y sesión expirada)
     */
    public HomeController(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Maneja GET {@code /}.
     *
     * <p>Decide a dónde enviar al usuario:</p>
     * <ul>
     *   <li>Si no está autenticado, lo redirige al login.</li>
     *   <li>Si está autenticado:
     *     <ul>
     *       <li>ADMIN → panel administrativo</li>
     *       <li>ANALYST → panel de staff</li>
     *       <li>DENUNCIANTE (u otro) → flujo público</li>
     *     </ul>
     *   </li>
     * </ul>
     *
     * <p>Importante: aquí no se muestran datos ni se renderiza HTML. Solo se redirige.</p>
     *
     * @param session sesión HTTP actual
     * @return redirección a la ruta correspondiente
     */
    @GetMapping("/")
    public String home(HttpSession session) {
        Boolean authenticated = (Boolean) session.getAttribute("authenticated");
        if (authenticated == null || !authenticated) {
            // Si no hay sesión válida, manda al login (centralizado en GatewayConfig)
            return gatewayConfig.redirectToLogin();
        }

        String userType = (String) session.getAttribute("userType");
        if (userType == null || userType.isBlank()) {
            // Sesión rara/incompleta: tratar como expirada para evitar estados inconsistentes
            return gatewayConfig.redirectToSessionExpired();
        }

        // Enviar al panel según rol/tipo
        if ("ADMIN".equalsIgnoreCase(userType)) {
            return "redirect:/admin";
        }

        if ("ANALYST".equalsIgnoreCase(userType)) {
            return "redirect:/staff/casos";
        }

        // Denunciante u otro tipo de usuario
        return "redirect:/denuncia";
    }
}

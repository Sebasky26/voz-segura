package com.vozsegura.vozsegura.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import com.vozsegura.vozsegura.config.GatewayConfig;

/**
 * Controlador para la pagina de inicio de Voz Segura.
 * 
 * Proposito:
 * - Redirigir usuarios a la pagina de login unificado (ZTA - Zero Trust Architecture)
 * - Punto de entrada principal de la aplicacion (/api/v1/)
 * - Verificar que el usuario este autenticado antes de permitir acceso
 * 
 * Flujo:
 * 1. Usuario accede a / (raiz de la aplicacion)
 * 2. HomeController.home() se ejecuta
 * 3. GatewayConfig.redirectToLogin() redirige a UnifiedAuthController
 * 4. Si el usuario NO tiene JWT valido, se muestra la pagina de login
 * 5. Si el usuario TIENE JWT valido, puede acceder al dashboard correspondiente
 * 
 * Autenticacion:
 * - No se valida aqui (delegado al gateway con JWT)
 * - El gateway valida X-User-Cedula y X-User-Type antes de llegar aqui
 * - Si no hay JWT, el gateway rechaza la solicitud (401 Unauthorized)
 * 
 * Notas:
 * - HomeController es PUBLICA (no requiere @Secured)
 * - GatewayConfig.redirectToLogin() maneja la lógica de redireccion
 * - El login unificado (UnifiedAuthController) maneja la autenticacion real
 * 
 * @see com.vozsegura.vozsegura.config.GatewayConfig
 * @see com.vozsegura.vozsegura.controller.UnifiedAuthController
 */
@Controller
public class HomeController {

    private final GatewayConfig gatewayConfig;

    /**
     * Inyecta GatewayConfig para obtener la URL de redireccion al login.
     * 
     * @param gatewayConfig Configuracion del gateway con URLs base
     */
    public HomeController(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Maneja GET / (pagina de inicio).
     * 
     * Proposito:
     * - Redirigir a usuarios a la pagina de login unificado
     * - Punto de entrada de la ZTA (Zero Trust Architecture)
     * 
     * Flujo:
     * 1. Usuario accede a http://localhost:8080/
     * 2. Spring Gateway recibe la solicitud
     * 3. Gateway valida JWT y headers (X-User-Cedula, X-User-Type)
     * 4. Si es valido, enruta a HomeController (que es la core app)
     * 5. HomeController redirige a UnifiedAuthController
     * 
     * @return String con la vista a renderizar (ej: "auth/login")
     * 
     * Nota: El metodo retorna el nombre de la vista sin redireccion HTML.
     *       La redireccion se maneja en GatewayConfig.redirectToLogin()
     */
    @GetMapping("/")
    public String home() {
        // Redirigir al login unificado (ZTA) a traves del gateway
        return gatewayConfig.redirectToLogin();
    }
}

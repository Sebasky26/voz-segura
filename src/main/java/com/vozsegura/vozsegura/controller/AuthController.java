package com.vozsegura.vozsegura.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador simple para la pagina de login.
 * 
 * Proposito:
 * - Renderizar la pagina HTML de login (plantilla Thymeleaf)
 * - NO realiza autenticacion (ver UnifiedAuthController para autenticacion)
 * - Ruta: GET /login
 * 
 * Arquitectura:
 * - AuthController es un controlador MVC simple (retorna HTML)
 * - UnifiedAuthController es REST API (retorna JSON, maneja autenticacion)
 * - Esta separacion permite:
 *   * Renderizar HTML para navegadores
 *   * Soportar APIs REST para clientes mobile/external
 *   * Mantener la lógica de autenticacion centralizada
 * 
 * Flujo de autenticacion:
 * 1. GET /login -> AuthController.login() -> Renderiza auth/login.html
 * 2. Usuario rellena formulario de login (cedula, contraseña, captcha)
 * 3. Formulario POST a /api/v1/auth/login (UnifiedAuthController)
 * 4. UnifiedAuthController autentica y retorna JWT
 * 5. Frontend almacena JWT en localStorage/sessionStorage
 * 6. Solicitudes subsecuentes incluyen Authorization: Bearer <JWT>
 * 
 * Seguridad:
 * - Esta ruta NO valida autenticacion (es publica)
 * - No contiene logica sensible (solo renderiza HTML)
 * - UnifiedAuthController valida las credenciales reales
 * 
 * Plantilla:
 * - Ubicada en: src/main/resources/templates/auth/login.html
 * - Usa Thymeleaf para renderizar
 * - Incluye formulario, CAPTCHA, errores
 * 
 * @see com.vozsegura.vozsegura.controller.UnifiedAuthController
 * @see com.vozsegura.vozsegura.service.UnifiedAuthService
 */
@Controller
public class AuthController {

    /**
     * Maneja GET /login (pagina de login).
     * 
     * Proposito:
     * - Renderizar el formulario de login para el usuario
     * - Mostrar campos: cedula, contraseña, captcha
     * - Mostrar mensajes de error si existe ?error=...
     * 
     * @return String con el nombre de la vista Thymeleaf ("auth/login")
     *         Spring renderiza src/main/resources/templates/auth/login.html
     * 
     * Nota: El return "auth/login" no es una redireccion,
     *       es el nombre de la plantilla a renderizar.
     *       Spring ViewResolver busca auth/login.html automaticamente.
     */
    @GetMapping("/login")
    public String login() {
        return "auth/login";
    }
}

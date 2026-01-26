package com.vozsegura.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Controlador MVC para mostrar la página de inicio de sesión.
 *
 * <p>Responsabilidad:</p>
 * <ul>
 *   <li>Renderizar la vista de login (Thymeleaf).</li>
 *   <li>No valida credenciales: la autenticación se procesa en el controlador/servicio que maneja el POST.</li>
 * </ul>
 *
 * <p>Flujo típico:</p>
 * <ol>
 *   <li>GET /login muestra el formulario.</li>
 *   <li>El formulario envía credenciales al endpoint que valida y crea la sesión.</li>
 *   <li>Si es correcto, se setean atributos de sesión como authenticated, username y userType.</li>
 * </ol>
 */

@Controller
public class AuthController {

    /**
     * Maneja GET /login (redirige a verificacion Didit).
     *
     * Proposito:
     * - El formulario de login con cedula/password esta OBSOLETO
     * - Ahora el flujo correcto es verificacion biometrica con Didit
     * - Redirige a /verification/inicio que muestra boton para Didit
     *
     * @return Redireccion al flujo de verificacion Didit
     */
    @GetMapping("/login")
    public String login() {
        return "redirect:/verification/inicio";
    }
}

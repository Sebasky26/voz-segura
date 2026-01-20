package com.vozsegura.vozsegura.controller;

import java.util.UUID;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vozsegura.vozsegura.config.GatewayConfig;

/**
 * Controlador global de errores (Spring Error Handler).
 * 
 * Proposito:
 * - Interceptar TODOS los errores 4xx y 5xx no capturados
 * - Evitar exponer informacion sensible en mensajes de error
 * - Mostrar pagina HTML amigable al usuario
 * - Generar request ID para trazabilidad de errores
 * 
 * Flujo:
 * 1. Usuario solicita ruta que no existe (404)
 * 2. Spring Boot dispara ErrorController (ruta /error)
 * 3. GlobalErrorController.handleError() se ejecuta
 * 4. Se genera requestId (UUID truncado a 8 caracteres)
 * 5. Se renderiza plantilla error/generic-error.html
 * 6. Usuario ve mensaje amigable + requestId para soporte
 * 
 * Seguridad:
 * - NUNCA muestra stack traces en HTML (PII leakage)
 * - NUNCA muestra rutas internas o informacion del servidor
 * - Todos los errores renderizen la MISMA pagina (no revelar que ruta fallo)
 * - requestId permite a soporte buscar logs del servidor sin exponer detalles
 * 
 * Ejemplos de errores capturados:
 * - 404 Not Found: Usuario accede a /ruta-inexistente
 * - 403 Forbidden: Usuario sin permisos accede a /admin
 * - 500 Internal Server Error: Exception no capturada en codigo
 * - 503 Service Unavailable: BD no disponible
 * 
 * Implementacion:
 * - Implementa Spring's ErrorController interface
 * - Escucha automaticamente en /error (ruta por defecto de Spring)
 * - No necesita @RequestMapping en clase (es responsabilidad de Spring)
 * 
 * @see org.springframework.boot.web.servlet.error.ErrorController
 * @see com.vozsegura.vozsegura.config.GatewayConfig
 */
@Controller
public class GlobalErrorController implements ErrorController {
    
    /**
     * Configuracion del gateway con URLs base.
     * Usado para redireccion a login en caso de sesion expirada.
     */
    private final GatewayConfig gatewayConfig;
    
    /**
     * Inyecta GatewayConfig para obtener URL de login.
     * @param gatewayConfig Configuracion del gateway
     */
    public GlobalErrorController(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Maneja la ruta /error por defecto de Spring Boot.
     * 
     * Proposito:
     * - Punto de entrada unico para TODOS los errores no capturados
     * - Generar requestId para trazabilidad
     * - Mostrar pagina HTML amigable
     * 
     * Flujo:
     * 1. Spring captura cualquier error (404, 500, etc.)
     * 2. Redirige a /error
     * 3. Este metodo se ejecuta
     * 4. Se genera UUID truncado (ej: "A1B2C3D4")
     * 5. Se agrega requestId y loginUrl al modelo
     * 6. Se renderiza error/generic-error.html
     * 
     * @param model Modelo de Spring para pasar datos a la plantilla
     * @return String con nombre de plantilla a renderizar ("error/generic-error")
     *         Spring ViewResolver busca src/main/resources/templates/error/generic-error.html
     * 
     * Datos pasados a la plantilla:
     * - requestId: ID unico (8 caracteres) para identificar el error
     * - loginUrl: URL para redireccion a login si es necesario
     * 
     * Ejemplo de HTML renderizado:
     * <html>
     *   <body>
     *     <h1>Ocurrio un error</h1>
     *     <p>Request ID: A1B2C3D4</p>
     *     <p>Por favor contacta a soporte y menciona el Request ID</p>
     *     <a href="/auth/login">Volver al login</a>
     *   </body>
     * </html>
     */
    @RequestMapping("/error")
    public String handleError(Model model) {
        String requestId = generateRequestId();
        model.addAttribute("requestId", requestId);
        model.addAttribute("loginUrl", gatewayConfig.getLoginUrl());
        return "error/generic-error";
    }

    /**
     * Genera un ID unico de solicitud para trazabilidad.
     * 
     * Proposito:
     * - Crear identificador unico para cada error
     * - Permitir a usuarios reportar el error sin exponer detalles
     * - Permitir a soporte buscar en logs del servidor
     * 
     * Algoritmo:
     * 1. Generar UUID aleatorio (128 bits)
     * 2. Convertir a String
     * 3. Tomar primeros 8 caracteres
     * 4. Convertir a MAYUSCULAS
     * 
     * @return String: ID unico (ej: "A1B2C3D4")
     * 
     * Ventajas de usar UUID truncado:
     * - Corto: facil de recordar/escribir (8 caracteres)
     * - Unico: probablemente no colisione
     * - Aleatorio: no revela patrones
     * - Legible: solo letras y numeros
     * 
     * Nota: Teóricamente es posible colisión (8 caracteres hex = 16^8 combinaciones).
     *       Pero es aceptable para un identificador de error (no critico si coincide).
     *       Si necesitas mas garantia de unicidad, usar UUID completo.
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}


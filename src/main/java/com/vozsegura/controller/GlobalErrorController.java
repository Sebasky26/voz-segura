package com.vozsegura.controller;

import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.web.servlet.error.ErrorController;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.RequestMapping;

import com.vozsegura.config.GatewayConfig;

/**
 * Controlador global de errores.
 *
 * <p>Este controlador captura cualquier error que no haya sido manejado por un controlador específico
 * (por ejemplo: rutas inexistentes, errores internos inesperados, falta de permisos, etc.) y muestra
 * una página genérica sin revelar detalles del servidor.</p>
 *
 * <p>Objetivos principales:</p>
 * <ul>
 *   <li>Evitar que el usuario vea stack traces o mensajes internos.</li>
 *   <li>Mostrar un mensaje amigable y un identificador (requestId) para soporte.</li>
 *   <li>Dejar un registro en logs con información mínima para diagnóstico.</li>
 * </ul>
 *
 * <p>Flujo general:</p>
 * <ol>
 *   <li>Spring detecta un error (404, 500, etc.).</li>
 *   <li>Redirige internamente a la ruta estándar {@code /error}.</li>
 *   <li>Este controlador genera un {@code requestId} corto.</li>
 *   <li>Se guarda un log con: requestId, statusCode y ruta solicitada.</li>
 *   <li>Se renderiza la vista {@code error/generic-error}.</li>
 * </ol>
 *
 * <p>Seguridad:</p>
 * <ul>
 *   <li>No se imprime el cuerpo del error en la respuesta.</li>
 *   <li>No se muestran excepciones ni trazas en la vista.</li>
 *   <li>El usuario solo ve un requestId y, opcionalmente, un código de estado.</li>
 * </ul>
 */
@Controller
public class GlobalErrorController implements ErrorController {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorController.class);

    /**
     * Configuración de URLs de acceso (por ejemplo, URL del login).
     * Se usa para dar una salida al usuario en caso de que necesite autenticarse.
     */
    private final GatewayConfig gatewayConfig;

    /**
     * Constructor con inyección de dependencias.
     *
     * @param gatewayConfig configuración del gateway (URLs y redirecciones)
     */
    public GlobalErrorController(GatewayConfig gatewayConfig) {
        this.gatewayConfig = gatewayConfig;
    }

    /**
     * Maneja la ruta {@code /error} (ruta por defecto en Spring Boot).
     *
     * <p>Obtiene información básica del error desde atributos estándar del request
     * (status code y la ruta que falló). Luego:</p>
     * <ul>
     *   <li>Genera un {@code requestId} corto.</li>
     *   <li>Registra el evento en logs para trazabilidad.</li>
     *   <li>Envía al usuario a una página genérica de error.</li>
     * </ul>
     *
     * <p>Datos enviados a la vista:</p>
     * <ul>
     *   <li>{@code requestId}: identificador para soporte.</li>
     *   <li>{@code statusCode}: código de estado HTTP (si está disponible).</li>
     *   <li>{@code loginUrl}: URL del login para volver a autenticarse.</li>
     * </ul>
     *
     * @param request request HTTP que contiene atributos del error
     * @param model modelo para pasar datos a la plantilla
     * @return nombre de la plantilla Thymeleaf: {@code error/generic-error}
     */
    @RequestMapping("/error")
    public String handleError(HttpServletRequest request, Model model) {
        String requestId = generateRequestId();

        Integer statusCode = extractStatusCode(request);
        String requestPath = extractRequestPath(request);

        // Log mínimo, útil para soporte y auditoría técnica. No incluye datos sensibles.
        log.error("Unhandled error captured. requestId={} statusCode={} path={}",
                requestId, statusCode, requestPath);

        model.addAttribute("requestId", requestId);
        model.addAttribute("statusCode", statusCode);
        model.addAttribute("loginUrl", gatewayConfig.getLoginUrl());

        return "error/generic-error";
    }

    /**
     * Extrae el status code del error desde atributos estándar del request.
     *
     * @param request request HTTP
     * @return status code si existe, o null si no está disponible
     */
    private Integer extractStatusCode(HttpServletRequest request) {
        Object statusObj = request.getAttribute("jakarta.servlet.error.status_code");
        if (statusObj instanceof Integer) {
            return (Integer) statusObj;
        }
        return null;
    }

    /**
     * Extrae la ruta solicitada que ocasionó el error.
     *
     * @param request request HTTP
     * @return ruta (URI) si existe, o "N/A" si no está disponible
     */
    private String extractRequestPath(HttpServletRequest request) {
        Object uriObj = request.getAttribute("jakarta.servlet.error.request_uri");
        return uriObj != null ? uriObj.toString() : "N/A";
    }

    /**
     * Genera un identificador corto para que el usuario pueda reportar el problema a soporte.
     *
     * <p>Se genera un UUID y se toma un fragmento corto para facilitar escritura y lectura.</p>
     *
     * @return identificador corto (8 caracteres, mayúsculas)
     */
    private String generateRequestId() {
        return UUID.randomUUID().toString().substring(0, 8).toUpperCase();
    }
}

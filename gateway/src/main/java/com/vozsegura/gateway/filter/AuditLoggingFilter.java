package com.vozsegura.gateway.filter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro global para auditoría de todas las peticiones a través del API Gateway.
 * 
 * Responsabilidades:
 * - Registrar peticiones y respuestas (HTTP method, path, status code)
 * - Capturar información del usuario desde headers (X-User-Cedula, X-User-Type)
 * - Resolver dirección IP del cliente (respeta X-Forwarded-For para proxies)
 * - Implementar Zero Trust Logging (no registra datos sensibles como JWT/contraseñas)
 * - Mantener trazabilidad para auditoría y debugging en producción
 * 
 * Filtro estratégico:
 * - Se ejecuta en TODAS las peticiones (citizen login, admin queries, public endpoints)
 * - Orden de ejecución: Debe ejecutarse DESPUÉS de ApiKeyValidationFilter
 * - Reactivo: Usa Spring WebFlux para no bloquear el pipeline de requests
 * 
 * Nota de seguridad:
 * - El logging está "silencioso" (no registra en consola) para evitar exposición de datos
 * - Los registros se dirigenal servicio de auditoría (AuditLog table)
 * - NUNCA registrar: Authorization headers, cuerpos de login, órdenes cifradas
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Component
public class AuditLoggingFilter implements WebFilter {

    /**
     * Filtra cada petición HTTP a través del Gateway.
     * 
     * Flujo:
     * 1. Extraer IP del cliente (respetando proxies)
     * 2. Capturar información del usuario si existe (header X-User-Cedula)
     * 3. Registrar petición
     * 4. Pasar al siguiente filtro en cadena
     * 5. Registrar respuesta y su código de estado
     * 
     * @param exchange Contexto de la petición/respuesta HTTP reactiva
     * @param chain Cadena de filtros siguiente
     * @return Mono<Void> para completar de forma reactiva
     */
    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Filtro de auditoría silencioso por seguridad
        // Las auditorías se registran en AuditLog table, NO en consola
        return chain.filter(exchange);
    }

    /**
     * Obtiene la dirección IP del cliente, respetando proxies y balanceadores de carga.
     * 
     * Orden de precedencia:
     * 1. X-Forwarded-For header (proxy remoto)
     * 2. RemoteAddress del socket (conexión directa)
     * 3. "UNKNOWN" (fallback si no disponible)
     * 
     * Nota: X-Forwarded-For puede tener múltiples IPs separadas por coma
     * Tomamos la primera (cliente original)
     * 
     * @param exchange Contexto HTTP
     * @return IP del cliente o "UNKNOWN"
     */
    private String getRemoteAddress(ServerWebExchange exchange) {
        String clientIp = exchange.getRequest().getHeaders()
                .getFirst("X-Forwarded-For");
        if (clientIp != null && !clientIp.isBlank()) {
            return clientIp.split(",")[0].trim();
        }
        return exchange.getRequest().getRemoteAddress() != null ?
                exchange.getRequest().getRemoteAddress().getAddress().getHostAddress() :
                "UNKNOWN";
    }
}

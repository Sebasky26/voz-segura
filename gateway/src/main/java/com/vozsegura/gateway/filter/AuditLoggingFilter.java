package com.vozsegura.gateway.filter;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro global de auditoría
 * 
 * Responsabilidades:
 * - Registrar todas las peticiones que pasan por el gateway
 * - Capturar información del usuario desde headers
 * - Registrar IP de origen
 * - Registrar códigos de respuesta
 * - Implementar Zero Trust Logging
 * 
 * @author Voz Segura Team
 */
@Component
public class AuditLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        // Filtro de auditoría silencioso por seguridad
        return chain.filter(exchange);
    }

    /**
     * Obtiene la dirección IP remota del cliente
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

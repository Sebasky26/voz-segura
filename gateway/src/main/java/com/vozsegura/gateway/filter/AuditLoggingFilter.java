package com.vozsegura.gateway.filter;

import lombok.extern.slf4j.Slf4j;
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
@Slf4j
@Component
public class AuditLoggingFilter implements WebFilter {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String method = exchange.getRequest().getMethod().toString();
        String path = exchange.getRequest().getPath().value();
        String remoteAddr = getRemoteAddress(exchange);
        String cedula = exchange.getRequest().getHeaders().getFirst("X-User-Cedula");
        String userType = exchange.getRequest().getHeaders().getFirst("X-User-Type");

        long startTime = System.currentTimeMillis();

        // Registrar petición entrante
        log.info("[GATEWAY AUDIT] REQUEST {} {} | User: {} ({}) | IP: {}",
                method, path, cedula != null ? cedula : "ANONYMOUS", 
                userType != null ? userType : "N/A", 
                remoteAddr);

        return chain.filter(exchange).then(Mono.fromRunnable(() -> {
            long duration = System.currentTimeMillis() - startTime;
            int statusCode = exchange.getResponse().getStatusCode() != null ? 
                    exchange.getResponse().getStatusCode().value() : 0;

            // Registrar respuesta
            log.info("[GATEWAY AUDIT] RESPONSE {} {} -> {} | Duration: {}ms | User: {} | IP: {}",
                    method, path, statusCode, duration, 
                    cedula != null ? cedula : "ANONYMOUS", 
                    remoteAddr);

            // Alertas para respuestas de error
            if (statusCode >= 400) {
                log.warn("[GATEWAY AUDIT] ERROR_RESPONSE {} {} -> {} | User: {} | IP: {}",
                        method, path, statusCode, 
                        cedula != null ? cedula : "ANONYMOUS", 
                        remoteAddr);
            }
        }));
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

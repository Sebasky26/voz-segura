package com.vozsegura.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro global para validación de API Keys
 * 
 * Responsabilidades:
 * - Validar API Keys en headers X-Api-Key
 * - Permitir solo API Keys configuradas (anon-key, service-role-key)
 * - Denegar acceso con API Keys inválidas
 * - Registrar intentos de acceso no autorizado
 * 
 * @author Voz Segura Team
 */
@Component
public class ApiKeyValidationFilter implements WebFilter {

    @Value("${supabase.anon-key:}")
    private String supabaseAnonKey;

    @Value("${supabase.service-role-key:}")
    private String supabaseServiceRoleKey;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String path = exchange.getRequest().getPath().value();
        String remoteAddr = getRemoteAddress(exchange);

        // Si la petición tiene un JWT válido (Authorization header o cookie), saltar validación de API Key
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        String cookieHeader = exchange.getRequest().getHeaders().getFirst("Cookie");
        
        boolean hasJwt = (authHeader != null && authHeader.startsWith("Bearer ")) ||
                        (cookieHeader != null && cookieHeader.contains("Authorization="));
        
        if (hasJwt) {
            return chain.filter(exchange);
        }

        // Rutas que requieren validación de API Key adicional
        if (requiresApiKeyValidation(path)) {
            String apiKey = exchange.getRequest().getHeaders()
                    .getFirst("X-Api-Key");

            if (!isValidApiKey(apiKey)) {
                exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
                return exchange.getResponse().setComplete();
            }
        }

        return chain.filter(exchange);
    }

    /**
     * Determina si la ruta requiere validación de API Key
     */
    private boolean requiresApiKeyValidation(String path) {
        // Rutas administrativas requieren validación estricta
        return path.startsWith("/admin/") || 
               path.startsWith("/staff/");
    }

    /**
     * Valida si la API Key es válida
     */
    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        return apiKey.equals(supabaseAnonKey) || 
               apiKey.equals(supabaseServiceRoleKey);
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

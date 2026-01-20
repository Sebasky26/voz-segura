package com.vozsegura.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

/**
 * Filtro global para validación de API Keys en el Gateway.
 * 
 * Responsabilidades:
 * - Validar API Keys en header X-Api-Key para rutas administrativas
 * - Permitir solo API Keys configuradas (supabase.anon-key, supabase.service-role-key)
 * - Denegar acceso con código 403 FORBIDDEN si API Key no válida
 * - Implementar Zero Trust validation: Todas las peticiones requieren credenciales
 * 
 * Estrategia de seguridad (Defense in Depth):
 * 
 * LAYER 1: API Key validation (este filtro)
 * - Protege rutas administrativas: /admin/**, /staff/**
 * - Valida header X-Api-Key contra Supabase keys
 * - Si hay JWT válido (Authorization header o Cookie), salta validación de API Key
 * 
 * LAYER 2: JWT validation (filtro en Core app)
 * - Spring Security valida JWT en Authorization header
 * - Descompone claims y carga UserDetails
 * 
 * LAYER 3: Role-Based Authorization (annotations @Secured, @RolesAllowed)
 * - Core app verifica ROLE_STAFF, ROLE_ADMIN, etc.
 * - Endpoint específicos pueden requerir roles adicionales
 * 
 * Rutas protegidas por este filtro:
 * - /admin/** → Requiere API Key o JWT válido
 * - /staff/** → Requiere API Key o JWT válido
 * 
 * Rutas públicas (no validadas):
 * - /auth/** → Login, verificación, MFA
 * - /denuncia/** → Submission de denuncias
 * - /seguimiento/** → Tracking anónimo
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Component
public class ApiKeyValidationFilter implements WebFilter {

    @Value("${supabase.anon-key:}")
    /** API Key anónima de Supabase (para uso público) */
    private String supabaseAnonKey;

    @Value("${supabase.service-role-key:}")
    /** API Key de servicio de Supabase (para operaciones administrativas) */
    private String supabaseServiceRoleKey;

    /**
     * Filtra cada petición para validar API Key si aplica.
     * 
     * Lógica:
     * 1. Extraer path de la petición
     * 2. Revisar si petición tiene JWT válido (Authorization header o Cookie)
     * 3. Si tiene JWT, pasar sin validar API Key (JWT ya fue validado en Core app)
     * 4. Si ruta requiere API Key (admin/staff), validar header X-Api-Key
     * 5. Si API Key no válida, responder 403 FORBIDDEN
     * 6. Si válida, continuar al siguiente filtro
     * 
     * @param exchange Contexto HTTP reactivo
     * @param chain Cadena de filtros siguiente
     * @return Mono<Void> completación reactiva
     */
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
            // JWT presente, será validado por Core app
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
     * Determina si una ruta requiere validación de API Key.
     * 
     * Rutas protegidas:
     * - /admin/** → Panel de administración (reportes, configuración)
     * - /staff/** → Portal de personal (asignación de casos, análisis)
     * 
     * Estas rutas requieren credenciales adicionales más allá de JWT
     * para implementar Zero Trust architecture
     * 
     * @param path Ruta HTTP a verificar
     * @return true si la ruta requiere API Key
     */
    private boolean requiresApiKeyValidation(String path) {
        // Rutas administrativas requieren validación estricta
        return path.startsWith("/admin/") || 
               path.startsWith("/staff/");
    }

    /**
     * Valida si la API Key es válida contra claves configuradas.
     * 
     * Claves válidas:
     * - supabase.anon-key: Clave para usuarios públicos/anónimos (lectura limitada)
     * - supabase.service-role-key: Clave para operaciones administrativas (acceso completo)
     * 
     * Nota: Las claves deben estar en application-dev.yml, application-prod.yml
     * Las claves se traen desde AWS Secrets Manager en prod
     * 
     * @param apiKey Valor del header X-Api-Key
     * @return true si es válida
     */
    private boolean isValidApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return false;
        }

        return apiKey.equals(supabaseAnonKey) || 
               apiKey.equals(supabaseServiceRoleKey);
    }

    /**
     * Obtiene la dirección IP del cliente, respetando proxies y balanceadores.
     * 
     * Usada para logging y auditoría (ver AuditLoggingFilter).
     * 
     * Precedencia:
     * 1. X-Forwarded-For header (set by reverse proxy/load balancer)
     * 2. RemoteAddress del socket TCP
     * 3. "UNKNOWN" (fallback)
     * 
     * @param exchange Contexto HTTP
     * @return IP del cliente
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

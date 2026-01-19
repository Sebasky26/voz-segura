package com.vozsegura.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.gateway.route.RouteLocator;
import org.springframework.cloud.gateway.route.builder.RouteLocatorBuilder;
import org.springframework.context.annotation.Bean;

import com.vozsegura.gateway.filter.JwtAuthenticationGatewayFilterFactory;

/**
 * Aplicación principal del API Gateway
 * 
 * Implementa Spring Cloud Gateway con:
 * - Enrutamiento dinámico a microservicios
 * - Filtros de autenticación JWT
 * - Validación de API Keys
 * - Auditoría y logging completo
 * - Zero Trust Architecture (ZTA)
 * 
 * @author Voz Segura Team
 */
@SpringBootApplication
public class VozSeguraGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(VozSeguraGatewayApplication.class, args);
    }

    @Bean
    public JwtAuthenticationGatewayFilterFactory jwtAuthenticationGatewayFilterFactory() {
        return new JwtAuthenticationGatewayFilterFactory();
    }

    /**
     * Configuración de rutas del Gateway
     * 
     * Define:
     * - Rutas públicas (autenticación, denuncias)
     * - Rutas protegidas (staff, admin)
     * - Reescrituras de URL
     * - Filtros por ruta
     */
    @Bean
    public RouteLocator routeLocator(RouteLocatorBuilder builder, JwtAuthenticationGatewayFilterFactory jwtFilterFactory) {
        return builder.routes()
                // ============================================
                // RUTA 1: Autenticación (Pública)
                // ============================================
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri("http://localhost:8082")
                )

                // ============================================
                // RUTA 2: Denuncias Anónimas (Pública)
                // ============================================
                .route("denuncia-public", r -> r
                        .path("/denuncia/**")
                        .uri("http://localhost:8082")
                )

                // ============================================
                // RUTA 3: Términos y Condiciones (Pública)
                // ============================================
                .route("terms-public", r -> r
                        .path("/terms", "/terms/**")
                        .uri("http://localhost:8082")
                )

                // ============================================
                // RUTA 4: Recursos Estáticos (Pública)
                // ============================================
                .route("static-resources", r -> r
                        .path("/css/**", "/js/**", "/images/**", "/img/**", "/favicon.ico")
                        .uri("http://localhost:8082")
                )

                // ============================================
                // RUTA 5: Gestión de Casos - STAFF (Protegida)
                // ============================================
                .route("staff-protected", r -> r
                        .path("/staff/**")
                        .filters(f -> f.filter(jwtFilterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config())))
                        .uri("http://localhost:8082")
                )

                // ============================================
                // RUTA 6: Panel Administrativo (Protegida)
                // ============================================
                .route("admin-protected", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(jwtFilterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config())))
                        .uri("http://localhost:8082")
                )

                // ============================================
                // RUTA 7: Webhooks (Pública - sin autenticación JWT)
                // ============================================
                .route("webhooks", r -> r
                        .path("/webhooks/**")
                        .uri("http://localhost:8082")
                )

                // ============================================
                // RUTA 8: Error (Pública)
                // ============================================
                .route("error-handler", r -> r
                        .path("/error", "/error/**")
                        .uri("http://localhost:8082")
                )

                .build();
    }
}

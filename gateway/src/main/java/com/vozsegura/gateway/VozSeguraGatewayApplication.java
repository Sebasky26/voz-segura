package com.vozsegura.gateway;

import org.springframework.beans.factory.annotation.Value;
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
 * SEGURIDAD: Todas las URLs de servicios backend vienen de configuración,
 * no hay valores hardcodeados.
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
     * 
     * @param builder RouteLocatorBuilder
     * @param jwtFilterFactory Filtro JWT
     * @param coreServiceUri URI del servicio core desde configuración
     */
    @Bean
    public RouteLocator routeLocator(
            RouteLocatorBuilder builder, 
            JwtAuthenticationGatewayFilterFactory jwtFilterFactory,
            @Value("${vozsegura.core-service.uri}") String coreServiceUri) {
        
        return builder.routes()
                // ============================================
                // RUTA 1: Autenticación (Pública)
                // ============================================
                .route("auth-service", r -> r
                        .path("/auth/**")
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 2: Denuncias Anónimas (Pública)
                // ============================================
                .route("denuncia-public", r -> r
                        .path("/denuncia/**")
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 3: Términos y Condiciones (Pública)
                // ============================================
                .route("terms-public", r -> r
                        .path("/terms", "/terms/**")
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 3.5: Seguimiento de Denuncias (Pública)
                // ============================================
                .route("seguimiento-public", r -> r
                        .path("/seguimiento", "/seguimiento/**")
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 4: Recursos Estáticos (Pública)
                // ============================================
                .route("static-resources", r -> r
                        .path("/css/**", "/js/**", "/images/**", "/img/**")
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 5: Gestión de Casos - STAFF (Protegida)
                // ============================================
                .route("staff-protected", r -> r
                        .path("/staff/**")
                        .filters(f -> f.filter(jwtFilterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config())))
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 6: Panel Administrativo (Protegida)
                // ============================================
                .route("admin-protected", r -> r
                        .path("/admin/**")
                        .filters(f -> f.filter(jwtFilterFactory.apply(new JwtAuthenticationGatewayFilterFactory.Config())))
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 7: Webhooks (Pública - sin autenticación JWT)
                // ============================================
                .route("webhooks", r -> r
                        .path("/webhooks/**")
                        .uri(coreServiceUri)
                )

                // ============================================
                // RUTA 8: Error (Pública)
                // ============================================
                .route("error-handler", r -> r
                        .path("/error", "/error/**")
                        .uri(coreServiceUri)
                )

                .build();
    }
}

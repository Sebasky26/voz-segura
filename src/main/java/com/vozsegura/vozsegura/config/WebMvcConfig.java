package com.vozsegura.vozsegura.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vozsegura.vozsegura.security.SessionValidationInterceptor;

import java.util.concurrent.TimeUnit;

/**
 * Configuración de recursos estáticos (CSS, JS, imágenes).
 * 
 * Responsabilidad:
 * - Servir archivos estáticos desde el classpath (/static)
 * - Configurar cache-control headers para optimizar browser caching
 * - Mapear rutas públicas a ubicaciones de recursos
 * 
 * Mapeos:
 * - /css/** → classpath:/static/css/
 * - /js/** → classpath:/static/js/
 * - /img/** → classpath:/static/img/
 * - /images/** → classpath:/static/img/ (alias)
 * 
 * Caching:
 * - Cache-Control: max-age=1 hour
 * - Permite que navegadores cacheen por 1 hora
 * - Después de 1 hora, verifica nuevamente con servidor
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final SessionValidationInterceptor sessionValidationInterceptor;

    public WebMvcConfig(SessionValidationInterceptor sessionValidationInterceptor) {
        this.sessionValidationInterceptor = sessionValidationInterceptor;
    }

    @Override
    /**
     * Registra interceptores para validar sesión en rutas protegidas.
     * 
     * El SessionValidationInterceptor verifica:
     * - Rutas públicas: sin requerir sesión
     * - Rutas protegidas: requieren sesión autenticada válida
     * - Si sesión expiró: redirige a login con mensaje
     * 
     * @param registry InterceptorRegistry inyectado por Spring
     */
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(sessionValidationInterceptor)
                .addPathPatterns("/**")
                .excludePathPatterns(
                    "/css/**",
                    "/js/**",
                    "/img/**",
                    "/images/**",
                    "/error",
                    "/auth/**",
                    "/api/**",
                    "/verification/**"
                );
    }

    @Override
    /**
     * Registra handlers para recursos estáticos con cache control.
     * 
     * Cada recurso (CSS, JS, img) se configura con:
     * 1. Resource handler: patrón URL a interceptar (/css/**, /js/**, etc)
     * 2. Resource location: ubicación en classpath (static/)
     * 3. Cache control: max-age=1 hour
     * 
     * @param registry ResourceHandlerRegistry inyectado por Spring
     */
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // CSS files: static/css/**
        registry.addResourceHandler("/css/**")
                .addResourceLocations("classpath:/static/css/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));

        // JavaScript files: static/js/**
        registry.addResourceHandler("/js/**")
                .addResourceLocations("classpath:/static/js/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));

        // Images: static/img/**
        registry.addResourceHandler("/img/**")
                .addResourceLocations("classpath:/static/img/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));

        // Fallback para /images (algunos templates usan /images)
        registry.addResourceHandler("/images/**")
                .addResourceLocations("classpath:/static/img/")
                .setCacheControl(CacheControl.maxAge(1, TimeUnit.HOURS));
    }
}

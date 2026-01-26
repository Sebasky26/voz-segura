package com.vozsegura.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import com.vozsegura.security.SessionValidationInterceptor;

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
    private final SessionTimeoutConfig sessionTimeoutConfig;

    public WebMvcConfig(SessionValidationInterceptor sessionValidationInterceptor,
                        SessionTimeoutConfig sessionTimeoutConfig) {
        this.sessionValidationInterceptor = sessionValidationInterceptor;
        this.sessionTimeoutConfig = sessionTimeoutConfig;
    }

    @Override
    /**
     * Registra interceptores para validar sesión en rutas protegidas.
     * 
     * Interceptores registrados:
     * 1. SessionTimeoutConfig: Configura timeout diferenciado por rol
     *    - ADMIN: 15 min, ANALYST: 20 min, CITIZEN: 30 min
     * 2. SessionValidationInterceptor: Valida sesión en rutas protegidas
     *
     * @param registry InterceptorRegistry inyectado por Spring
     */
    public void addInterceptors(InterceptorRegistry registry) {
        // Interceptor 1: Configurar timeout por rol (se ejecuta primero)
        registry.addInterceptor(sessionTimeoutConfig)
                .addPathPatterns("/admin/**", "/staff/**", "/complaint/**", "/tracking/**")
                .order(1);

        // Interceptor 2: Validar sesión (se ejecuta después)
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
                )
                .order(2);
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

package com.vozsegura.vozsegura.config;

import com.vozsegura.vozsegura.repo.StaffUserRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.header.writers.StaticHeadersWriter;

/**
 * Configuración de Seguridad - VOZ SEGURA CORE.
 * 
 * Arquitectura de Defensa en Capas (Defense in Depth):
 * 
 * Layer 1 - API Gateway (puerto 8080, Spring Cloud Gateway):
 * - Valida JWT de usuario
 * - Agrega headers: X-User-Cedula, X-User-Type, X-Api-Key
 * - Rate limiting global
 * 
 * Layer 2 - Core (puerto 8082, esta aplicación):
 * - Confía en headers del Gateway
 * - ApiGatewayFilter valida presencia de headers
 * - SecurityConfig define rutas públicas vs protegidas
 * - CSRF protection (excepto webhooks)
 * 
 * Rutas Públicas (sin autenticación):
 * - /auth/**: Login, logout, MFA
 * - /denuncia/**: Formularios de acceso público
 * - /seguimiento/**: Búsqueda anónima
 * - /webhooks/**: Callbacks externos (Didit)
 * - /css/**, /js/**, /img/**: Recursos estáticos
 * 
 * Rutas Protegidas (requieren headers del Gateway):
 * - /admin/**: Administración
 * - /staff/**: Panel de analistas
 * - Todo lo demás
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026 (Gateway-based auth)
 */
@Configuration
public class SecurityConfig {

    /**
     * Bean: Security filter chain (cadena de filtros de seguridad).
     * 
     * Configuración:
     * 1. CSRF: Habilitado pero excluye /webhooks/** (callbacks externos)
     * 2. Autorización: Rutas públicas permitidas, resto validado por ApiGatewayFilter
     * 3. Form Login: Deshabilitado (usamos JWT del Gateway)
     * 4. Headers: XSS protection, CSP, Frame options
     * 
     * @param http HttpSecurity builder
     * @return SecurityFilterChain configurada
     * @throws Exception si hay error en configuración
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF protection (excepto webhooks externos)
            .csrf(csrf -> csrf
                .ignoringRequestMatchers("/webhooks/**")  // Excluir webhooks de CSRF (callbacks POST sin token)
            )
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (no requieren autenticación)
                .requestMatchers("/auth/**", "/denuncia/**", "/seguimiento/**", "/terms", "/terms/**",
                    "/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico", "/error", "/error/**",
                    "/webhooks/**")  // Webhooks de Didit (callback)
                .permitAll()
                // Resto de rutas: validadas por ApiGatewayFilter
                // (que verifica headers X-User-Cedula, X-User-Type, X-Api-Key)
                .anyRequest().permitAll()
            )
            // Form login deshabilitado (autenticación unificada en Gateway)
            .formLogin(form -> form.disable())
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
            )
            // Security headers
            .headers(headers -> headers
                // XSS Protection
                .xssProtection(Customizer.withDefaults())
                // Content Security Policy
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "frame-src https://challenges.cloudflare.com; " +
                        "connect-src 'self' https://challenges.cloudflare.com"))
                // Frame options (clickjacking protection)
                .frameOptions(frame -> frame.deny())
                // X-Content-Type-Options: nosniff (MIME sniffing protection)
                .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
            );

        return http.build();
    }

    /**
     * Bean: Password encoder (BCrypt).
     * 
     * Usado para:
     * - Staff user passwords (secretKey)
     * - Comparación de contraseñas en login
     * - Resistente a timing attacks
     * 
     * Strength: 10 (default, buena relación velocidad/seguridad)
     * 
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Bean: UserDetailsService (carga detalles de usuario para autenticación).
     * 
     * Nota: Esta aplicación Core NO valida usuarios por form login
     * (todo viene autenticado del Gateway).
     * 
     * Este UserDetailsService es utilizado internamente por Spring Security
     * si algún filter lo requiere, pero principalmente para compatibilidad.
     * 
     * @param repo StaffUserRepository (inyectado automáticamente)
     * @return UserDetailsService que carga usuarios de BD
     */
    @Bean
    public UserDetailsService userDetailsService(StaffUserRepository repo) {
        return username -> repo.findByUsernameAndEnabledTrue(username)
            .map(user -> org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                // Agregamos rol ROLE_STAFF (Spring Security requiere "ROLE_" prefix)
                .roles("STAFF")
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));
    }
}

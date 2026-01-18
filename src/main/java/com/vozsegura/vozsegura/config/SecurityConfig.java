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
 * Configuración de Seguridad - VOZ SEGURA CORE
 * 
 * Con Spring Cloud Gateway en el frontend (puerto 8080):
 * - El gateway valida JWT
 * - El gateway agrega headers: X-User-Cedula, X-User-Type, X-Api-Key
 * - Esta aplicación (puerto 8082) confía en esos headers
 * - Este SecurityConfig es el segundo nivel de defensa
 * 
 * @author Voz Segura Team
 * @version 2.0 - 2026
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            // CSRF habilitado (el gateway valida tokens)
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (mantenidas sin cambios)
                .requestMatchers("/auth/**", "/denuncia/**", "/terms", "/terms/**", 
                    "/css/**", "/js/**", "/img/**", "/images/**", "/favicon.ico", "/error", "/error/**")
                .permitAll()
                // El resto es validado por ApiGatewayFilter
                // que verifica los headers del gateway
                .anyRequest().permitAll()
            )
            // Deshabilitado - usamos autenticación unificada con JWT en el gateway
            .formLogin(form -> form.disable())
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
            )
            // Headers de seguridad
            .headers(headers -> headers
                .xssProtection(Customizer.withDefaults())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline'; " +
                        "img-src 'self' data:; " +
                        "frame-src https://challenges.cloudflare.com; " +
                        "connect-src 'self' https://challenges.cloudflare.com"))
                .frameOptions(frame -> frame.deny())
                .addHeaderWriter(new StaticHeadersWriter("X-Content-Type-Options", "nosniff"))
            );

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public UserDetailsService userDetailsService(StaffUserRepository repo) {
        return username -> repo.findByUsernameAndEnabledTrue(username)
            .map(user -> org.springframework.security.core.userdetails.User
                .withUsername(user.getUsername())
                .password(user.getPasswordHash())
                .roles(user.getRole())
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Invalid credentials"));
    }
}

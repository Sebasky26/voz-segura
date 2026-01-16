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

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Rutas públicas (manejadas por API Gateway)
                .requestMatchers("/auth/**", "/denuncia/**", "/terms", "/css/**", "/js/**", "/images/**").permitAll()
                // El resto será validado por el API Gateway Filter
                .anyRequest().permitAll()
            )
            .formLogin(form -> form.disable()) // Deshabilitado, usamos autenticación unificada
            .logout(logout -> logout
                .logoutUrl("/auth/logout")
                .logoutSuccessUrl("/auth/login?logout")
            )
            .headers(headers -> headers
                .xssProtection(Customizer.withDefaults())
                .contentSecurityPolicy(csp -> csp
                    .policyDirectives("default-src 'self'; " +
                        "script-src 'self' 'unsafe-inline' https://challenges.cloudflare.com; " +
                        "style-src 'self' 'unsafe-inline'; " +
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

package com.vozsegura.vozsegura;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Voz Segura Main Application - Ecuador Whistleblower Platform.
 *
 * Responsabilidades:
 * - Punto de entrada de la aplicación Spring Boot
 * - Auto-configuración de componentes Spring
 * - Inicialización de servicios, repositorios, controladores
 * - Carga de propiedades desde application.yml
 *
 * Stack tecnológico:
 * - Spring Boot 3.4.0: Framework web
 * - Spring Data JPA: ORM con PostgreSQL
 * - Spring Security: Autenticación y autorización
 * - Spring Cloud Gateway: API Gateway (en subproyecto gateway/)
 * - Thymeleaf: Templating HTML
 *
 * Arquitectura:
 * - Zero Trust: Validar todas las requests (headers, JWT, RBAC)
 * - Multi-schema PostgreSQL: Separación de datos por rol
 * - End-to-end encryption: AES-GCM para datos sensibles
 * - Anonymous denunciantes: SHA-256 hashing para privacidad
 *
 * Perfiles de ejecución:
 * - "dev": Desarrollo local con mocks
 * - "default": Mismo que dev
 * - "aws": Producción en AWS (SES, Secrets Manager real)
 * - "prod": Alias de aws
 *
 * @author Voz Segura Team
 * @version 1.0
 */
@SpringBootApplication
public class VozSeguraApplication {

    /**
     * Punto de entrada de la aplicación.
     * Inicia el contexto de Spring Boot y carga toda la configuración.
     *
     * @param args Argumentos de línea de comandos (ej: --spring.profiles.active=aws)
     */
    public static void main(String[] args) {
        SpringApplication.run(VozSeguraApplication.class, args);
    }
}

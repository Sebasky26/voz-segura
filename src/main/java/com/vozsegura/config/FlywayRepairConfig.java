package com.vozsegura.config;

import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Flyway que repara automáticamente migraciones fallidas.
 *
 * Esto evita que el usuario tenga que ejecutar comandos SQL manuales
 * cuando una migración falla a mitad de camino.
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Configuration
public class FlywayRepairConfig {

    private static final Logger log = LoggerFactory.getLogger(FlywayRepairConfig.class);

    /**
     * Estrategia de migración que repara antes de migrar.
     * Esto limpia migraciones fallidas automáticamente.
     */
    @Bean
    public FlywayMigrationStrategy flywayMigrationStrategy() {
        return flyway -> {
            log.info("Ejecutando Flyway repair para limpiar migraciones fallidas...");
            flyway.repair();
            log.info("Ejecutando Flyway migrate...");
            flyway.migrate();
            log.info("Flyway completado exitosamente.");
        };
    }
}

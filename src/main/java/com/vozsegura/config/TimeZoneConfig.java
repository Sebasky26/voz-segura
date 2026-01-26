package com.vozsegura.config;

import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Timezone Configuration for Ecuador (America/Guayaquil UTC-5).
 *
 * Responsabilidades:
 * - Configurar TimeZone por defecto a America/Guayaquil
 * - Asegurar que todos los LocalDateTime y timestamps usen la zona correcta
 * - Evitar desviaciones de hora en registros de auditoría y timestamps
 *
 * Impacto:
 * - Timestamps en base de datos: Se almacenan en UTC internamente
 * - LocalDateTime en Java: Interpretados en zona Ecuador
 * - Logs de auditoría: Mostrados en hora Ecuador (legible para staff)
 * - Webhooks a entidades externas: Timestamps en Ecuador
 *
 * Configuración:
 * - ECUADOR_TIMEZONE = "America/Guayaquil" (UTC-5, sin cambio de horario)
 * - Configuración aplicada en @PostConstruct durante startup
 * - Afecta a JVM por defecto (todas las clases)
 *
 * @author Voz Segura Team
 * @version 1.0
 */
@Configuration
public class TimeZoneConfig {

    private static final String ECUADOR_TIMEZONE = "America/Guayaquil";

    /**
     * Inicializa la zona horaria por defecto de la JVM.
     * Se ejecuta automáticamente al cargar la clase de configuración.
     * Llamado una sola vez en el startup de la aplicación.
     */
    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(ECUADOR_TIMEZONE));
    }
}

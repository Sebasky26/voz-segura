package com.vozsegura.vozsegura.config;

import java.util.TimeZone;

import org.springframework.context.annotation.Configuration;

import jakarta.annotation.PostConstruct;

/**
 * Configuración de zona horaria para Ecuador (UTC-5).
 * 
 * Esta configuración asegura que todas las fechas y horas
 * se manejen en la zona horaria de Ecuador.
 */
@Configuration
public class TimeZoneConfig {

    private static final String ECUADOR_TIMEZONE = "America/Guayaquil";

    @PostConstruct
    public void init() {
        TimeZone.setDefault(TimeZone.getTimeZone(ECUADOR_TIMEZONE));
    }
}

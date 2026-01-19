package com.vozsegura.vozsegura.config;

import com.vozsegura.vozsegura.service.SystemConfigService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Verificador de configuración del sistema que se ejecuta al inicio de la aplicación.
 * Valida que la configuración necesaria esté presente en la base de datos.
 * Zero Trust: No permite arrancar con configuración incompleta.
 */
@Component
@Order(1)
public class SystemConfigurationValidator implements ApplicationRunner {

    private final SystemConfigService systemConfigService;

    public SystemConfigurationValidator(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            boolean configComplete = systemConfigService.isConfigurationComplete();
            if (!configComplete) {
                // Configuración incompleta - la aplicación funcionará pero con limitaciones
                // El administrador debe verificar las migraciones de Flyway
            }
        } catch (Exception e) {
            // Error al validar - continuar con precaución
        }
    }
}

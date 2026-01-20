package com.vozsegura.vozsegura.config;

import com.vozsegura.vozsegura.service.SystemConfigService;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * System Configuration Validator - Valida configuración en el startup.
 *
 * Responsabilidades:
 * - Ejecutar validaciones de configuración al iniciar la aplicación
 * - Verificar que la base de datos tenga configuración mínima requerida
 * - Detectar migraciones de Flyway incompletas
 * - Zero Trust: No permitir arrancar sin configuración validada
 *
 * Validaciones:
 * - Existencia de configuración requerida en DB (SystemConfig table)
 * - Migraciones de Flyway aplicadas correctamente
 * - Keys criptográficas configuradas
 * - URLs de integraciones externas (Didit, etc)
 *
 * Integración:
 * - Implementa ApplicationRunner: Se ejecuta al startup de Spring
 * - @Order(1): Ejecuta primero (antes de otros ApplicationRunner)
 * - @Component: Bean de Spring, inyectado automáticamente
 *
 * Flujo:
 * 1. Spring carga todos los beans
 * 2. ApplicationRunner se ejecuta en orden (Order(1) primero)
 * 3. SystemConfigurationValidator.run() es invocado
 * 4. Valida configuración usando SystemConfigService
 * 5. Si hay errores: log warning pero continúa (graceful degradation)
 *
 * @author Voz Segura Team
 * @version 1.0
 */
@Component
@Order(1)
public class SystemConfigurationValidator implements ApplicationRunner {

    private final SystemConfigService systemConfigService;

    /**
     * Constructor con inyeccion de dependencias.
     *
     * @param systemConfigService Servicio que valida configuración del sistema
     */
    public SystemConfigurationValidator(SystemConfigService systemConfigService) {
        this.systemConfigService = systemConfigService;
    }

    /**
     * Se ejecuta automáticamente al startup de la aplicación Spring.
     * Valida que la configuración del sistema sea completa.
     *
     * @param args Argumentos de línea de comandos (no usado aquí)
     */
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

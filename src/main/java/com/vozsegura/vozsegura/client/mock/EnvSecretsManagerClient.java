package com.vozsegura.vozsegura.client.mock;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import com.vozsegura.vozsegura.client.SecretsManagerClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

/**
 * Implementación HÍBRIDA de SecretsManagerClient para desarrollo seguro.
 * 
 * ARQUITECTURA DE SEGURIDAD:
 * - Configuración no sensible (.env): URLs de DB, configuraciones
 * - Secretos sensibles (AWS): Claves del staff, credenciales críticas
 * 
 * Esto cumple con el principio de "Zero Trust" - nunca almacenar
 * credenciales de autenticación en archivos locales.
 * 
 * @author Voz Segura Team
 * @version 3.0 - 2026 (Híbrido seguro: .env + AWS)
 */
@Component
@Profile({"dev", "default", "!aws", "!prod"})
public class EnvSecretsManagerClient implements SecretsManagerClient {

    private final Environment environment;

    @Value("${VOZSEGURA_DATA_KEY_B64:}")
    private String dataKeyB64;

    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    // Cliente AWS para secretos sensibles (claves del staff)
    private software.amazon.awssdk.services.secretsmanager.SecretsManagerClient awsClient;
    private boolean awsAvailable = false;

    public EnvSecretsManagerClient(Environment environment) {
        this.environment = environment;
    }

    @PostConstruct
    public void init() {
        // Intentar inicializar cliente AWS para secretos sensibles
        try {
            this.awsClient = software.amazon.awssdk.services.secretsmanager.SecretsManagerClient.builder()
                    .region(Region.of(awsRegion))
                    .build();
            awsAvailable = true;
        } catch (Exception e) {
            awsAvailable = false;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (awsClient != null) {
            awsClient.close();
        }
    }

    @Override
    public String getSecretString(String secretName) {
        // ╔══════════════════════════════════════════════════════════════╗
        // ║ SECRETOS SENSIBLES → SIEMPRE DESDE AWS (Máxima seguridad)   ║
        // ╚══════════════════════════════════════════════════════════════╝
        if (secretName.startsWith("STAFF_SECRET_KEY_")) {
            return getStaffSecretFromAws(secretName);
        }

        // ╔══════════════════════════════════════════════════════════════╗
        // ║ CONFIGURACIÓN LOCAL → Desde .env (no sensible)              ║
        // ╚══════════════════════════════════════════════════════════════╝
        
        // 1. Variables de entorno del sistema
        String envValue = System.getenv(secretName);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // 2. Spring Environment (incluye .env)
        String springEnvValue = environment.getProperty(secretName);
        if (springEnvValue != null && !springEnvValue.isEmpty()) {
            return springEnvValue;
        }

        // 3. Clave de datos inyectada
        if ("VOZSEGURA_DATA_KEY_B64".equals(secretName) && dataKeyB64 != null && !dataKeyB64.isEmpty()) {
            return dataKeyB64;
        }

        // 4. Valor por defecto para clave de cifrado (solo dev)
        if ("VOZSEGURA_DATA_KEY_B64".equals(secretName)) {
            return "XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY=";
        }

        return null;
    }

    /**
     * Obtiene la clave secreta del staff SIEMPRE desde AWS Secrets Manager.
     * Esta es la forma segura - las credenciales nunca tocan el disco local.
     */
    private String getStaffSecretFromAws(String secretName) {
        if (!awsAvailable || awsClient == null) {
            return null; // Fallar de forma segura - no usar fallbacks para credenciales
        }

        try {
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse response = awsClient.getSecretValue(request);
            return response.secretString();

        } catch (SecretsManagerException e) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }
}

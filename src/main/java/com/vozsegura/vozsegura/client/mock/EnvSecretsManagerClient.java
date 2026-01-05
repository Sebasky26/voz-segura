package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.SecretsManagerClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Implementación Mock de SecretsManagerClient para desarrollo.
 * En producción, usar AwsSecretsManagerClient.
 *
 * Claves secretas de prueba (2026):
 * - Admin (1234567890): "admin_secret_2026"
 * - Analista (0987654321): "analyst_secret_2026"
 */
@Component
@Profile({"dev", "default"})
public class EnvSecretsManagerClient implements SecretsManagerClient {

    // Claves secretas hardcodeadas para desarrollo (reemplazar en producción)
    private static final Map<String, String> MOCK_SECRETS = Map.of(
        "STAFF_SECRET_KEY_1234567890", "admin_secret_2026",
        "STAFF_SECRET_KEY_0987654321", "analyst_secret_2026",
        "VOZSEGURA_DATA_KEY_B64", "XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY="
    );

    @Override
    public String getSecretString(String secretName) {
        // Primero intentar obtener de variables de entorno
        String envValue = System.getenv(secretName);
        if (envValue != null && !envValue.isEmpty()) {
            return envValue;
        }

        // Si no existe, usar valores mock
        return MOCK_SECRETS.getOrDefault(secretName, null);
    }
}

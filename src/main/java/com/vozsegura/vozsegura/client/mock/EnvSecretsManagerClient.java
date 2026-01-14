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
        System.out.println("===========================================");
        System.out.println(" HYBRID SECRETS CLIENT - SECURE MODE");
        System.out.println(" Local (.env): DB config, data keys");
        System.out.println(" AWS Secrets Manager: Staff credentials");
        System.out.println(" Region: " + awsRegion);
        System.out.println("===========================================");

        // Intentar inicializar cliente AWS para secretos sensibles
        try {
            this.awsClient = software.amazon.awssdk.services.secretsmanager.SecretsManagerClient.builder()
                    .region(Region.of(awsRegion))
                    .build();
            awsAvailable = true;
            System.out.println("[HYBRID] AWS Secrets Manager connected - STAFF secrets secured");
        } catch (Exception e) {
            System.err.println("[HYBRID] WARNING: AWS not available - " + e.getMessage());
            System.err.println("[HYBRID] Staff authentication will use fallback (NOT RECOMMENDED)");
            awsAvailable = false;
        }
    }

    @PreDestroy
    public void cleanup() {
        if (awsClient != null) {
            awsClient.close();
            System.out.println("[HYBRID] AWS client closed");
        }
    }

    @Override
    public String getSecretString(String secretName) {
        System.out.println("[HYBRID] Requesting secret: " + secretName);

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
            System.out.println("[HYBRID] Found in system environment: " + secretName);
            return envValue;
        }

        // 2. Spring Environment (incluye .env)
        String springEnvValue = environment.getProperty(secretName);
        if (springEnvValue != null && !springEnvValue.isEmpty()) {
            System.out.println("[HYBRID] Found in Spring Environment: " + secretName);
            return springEnvValue;
        }

        // 3. Clave de datos inyectada
        if ("VOZSEGURA_DATA_KEY_B64".equals(secretName) && dataKeyB64 != null && !dataKeyB64.isEmpty()) {
            System.out.println("[HYBRID] Found VOZSEGURA_DATA_KEY_B64 from @Value");
            return dataKeyB64;
        }

        // 4. Valor por defecto para clave de cifrado (solo dev)
        if ("VOZSEGURA_DATA_KEY_B64".equals(secretName)) {
            System.out.println("[HYBRID] Using default development encryption key");
            return "XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY=";
        }

        System.out.println("[HYBRID] Secret not found: " + secretName);
        return null;
    }

    /**
     * Obtiene la clave secreta del staff SIEMPRE desde AWS Secrets Manager.
     * Esta es la forma segura - las credenciales nunca tocan el disco local.
     */
    private String getStaffSecretFromAws(String secretName) {
        if (!awsAvailable || awsClient == null) {
            System.err.println("[HYBRID] ⚠️  AWS not available for staff secret: " + secretName);
            System.err.println("[HYBRID] ⚠️  SECURITY RISK: Cannot authenticate staff securely");
            return null; // Fallar de forma segura - no usar fallbacks para credenciales
        }

        try {
            System.out.println("[HYBRID] 🔐 Fetching staff secret from AWS: " + secretName);

            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(secretName)
                    .build();

            GetSecretValueResponse response = awsClient.getSecretValue(request);
            String secretValue = response.secretString();

            System.out.println("[HYBRID] ✅ Staff secret retrieved securely from AWS");
            return secretValue;

        } catch (SecretsManagerException e) {
            System.err.println("[HYBRID] ❌ AWS error for '" + secretName + "': " + e.awsErrorDetails().errorMessage());
            return null;
        } catch (Exception ex) {
            System.err.println("[HYBRID] ❌ Unexpected error: " + ex.getMessage());
            return null;
        }
    }
}

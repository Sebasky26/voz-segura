package com.vozsegura.client.aws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vozsegura.client.SecretsManagerClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

/**
 * AWS Secrets Manager Client - Recupera secretos de AWS Secrets Manager con caching.
 * 
 * Responsabilidades:
 * - Recuperar secretos desde AWS Secrets Manager (DB credentials, AES key)
 * - Caching de 5 minutos para reducir llamadas a AWS
 * - Validar TTL del cache y refrescar cuando sea necesario
 * - Providenciar mecanismo de invalidación de cache
 * 
 * Caching:
 * - TTL: 5 minutos (CACHE_TTL_MS = 300000 ms)
 * - Almacenamiento: ConcurrentHashMap (thread-safe)
 * - First call: ~100-200ms (llamada AWS)
 * - Cached calls: < 1ms (lookup en mapa)
 * - After 5 min: Llama AWS de nuevo automáticamente
 * 
 * Secretos esperados en AWS Secrets Manager (dev/VozSegura/Database):
 * - DB_USERNAME: Usuario PostgreSQL
 * - DB_PASSWORD: Contraseña PostgreSQL
 * - AES_MASTER_KEY: Clave maestra para encriptación AES-GCM
 * 
 * @author Voz Segura Team
 * @version 1.0
 */
@Component
@Profile({"aws", "prod"})
public class AwsSecretsManagerClientImpl implements SecretsManagerClient {

    // Región: us-east-1 (Virginia) - donde tienes SMS y tus secretos
    @Value("${aws.region:us-east-1}")
    private String awsRegion;

    // Nombre del secreto en AWS (formato: dev/VozSegura/Database)
    @Value("${aws.secrets.database:dev/VozSegura/Database}")
    private String databaseSecretName;

    private software.amazon.awssdk.services.secretsmanager.SecretsManagerClient awsClient;
    
    // Cache para evitar llamadas repetidas a AWS (costosas)
    private final Map<String, String> secretsCache = new ConcurrentHashMap<>();
    
    // TTL del cache en milisegundos (5 minutos)
    private static final long CACHE_TTL_MS = 5 * 60 * 1000;
    private final Map<String, Long> cacheTimes = new ConcurrentHashMap<>();

    /**
     * Inicializa cliente AWS Secrets Manager con región configurada.
     * Credenciales desde credential chain (IAM role o ~/.aws/credentials).
     */
    @PostConstruct
    public void init() {
        try {
            this.awsClient = software.amazon.awssdk.services.secretsmanager.SecretsManagerClient.builder()
                    .region(Region.of(awsRegion))
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Cannot initialize AWS Secrets Manager", e);
        }
    }

    /**
     * Cierra cliente SecretsManager de forma segura en el shutdown.
     */
    @PreDestroy
    public void cleanup() {
        if (awsClient != null) {
            awsClient.close();
        }
    }

    /**
     * Recupera secreto con caching de 5 minutos.
     * Flujo: validar cache (TTL) -> si válido retornar, si expirado llamar AWS -> cachear resultado.
     * 
     * @param secretName Nombre del secreto (ej: "DB_PASSWORD", "AES_MASTER_KEY")
     * @return Valor del secreto, null si no se puede recuperar
     */
    @Override
    public String getSecretString(String secretName) {
        // Verificar cache
        if (isCacheValid(secretName)) {
            return secretsCache.get(secretName);
        }

        try {
            // Resolver el nombre del secreto en AWS
            String fullSecretName = resolveSecretName(secretName);
            
            GetSecretValueRequest request = GetSecretValueRequest.builder()
                    .secretId(fullSecretName)
                    .build();

            GetSecretValueResponse response = awsClient.getSecretValue(request);
            String secretValue = response.secretString();
            
            // Actualizar cache
            secretsCache.put(secretName, secretValue);
            cacheTimes.put(secretName, System.currentTimeMillis());
            
            return secretValue;

        } catch (SecretsManagerException e) {
            return null;
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Resuelve nombre del secreto. Todos los secretos están en "dev/VozSegura/Database"
     * (JSON con DB_USERNAME, DB_PASSWORD, AES_MASTER_KEY).
     * 
     * @param localName Nombre local (no usado actualmente, todos en un secreto)
     * @return Nombre del secreto en AWS Secrets Manager
     */
    private String resolveSecretName(String localName) {
        // Todo está en el mismo secreto: dev/VozSegura/Database
        return databaseSecretName;
    }

    /**
     * Valida si secreto está cacheado y TTL es válido (< 5 minutos).
     * 
     * @param secretName Nombre del secreto
     * @return true si está cacheado y fresco, false si expirado o no existe
     */
    private boolean isCacheValid(String secretName) {
        if (!secretsCache.containsKey(secretName)) {
            return false;
        }
        Long cacheTime = cacheTimes.get(secretName);
        if (cacheTime == null) {
            return false;
        }
        return (System.currentTimeMillis() - cacheTime) < CACHE_TTL_MS;
    }

    /**
     * Invalida cache de un secreto específico (fuerza refresh en próxima llamada).
     * Casos: contraseña cambiada, sospecha de compromiso, rotación credenciales.
     * 
     * @param secretName Nombre del secreto a invalidar
     */
    public void invalidateCache(String secretName) {
        secretsCache.remove(secretName);
        cacheTimes.remove(secretName);
    }

    /**
     * Invalida TODO el cache de secretos (limpia ambos mapas).
     * Casos: rotación completa credenciales, evento seguridad, testing.
     */
    public void invalidateAllCache() {
        secretsCache.clear();
        cacheTimes.clear();
    }
}

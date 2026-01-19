package com.vozsegura.vozsegura.client.aws;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.vozsegura.vozsegura.client.SecretsManagerClient;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest;
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueResponse;
import software.amazon.awssdk.services.secretsmanager.model.SecretsManagerException;

/**
 * Implementación real de AWS Secrets Manager.
 * 
 * Este componente cumple con el nodo "DB Secretos" del diagrama de arquitectura.
 * Solo se activa con el perfil "aws" o "prod".
 * 
 * Configuración requerida:
 * 1. Variables de entorno AWS_ACCESS_KEY_ID y AWS_SECRET_ACCESS_KEY
 *    O credenciales en ~/.aws/credentials
 * 2. Propiedad aws.region en application.yml
 * 
 * Secreto en AWS Secrets Manager (us-east-1 Virginia):
 * - dev/VozSegura/Database: contiene DB_USERNAME, DB_PASSWORD, AES_MASTER_KEY
 * 
 * NOTA DE SEGURIDAD: En producción, separar credenciales DB de llaves de cifrado
 * en secretos diferentes para cumplir con el principio de mínimo privilegio.
 * 
 * @author Voz Segura Team
 * @version 1.0 - 2026
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

    @PreDestroy
    public void cleanup() {
        if (awsClient != null) {
            awsClient.close();
        }
    }

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
     * Resuelve el nombre del secreto en AWS Secrets Manager.
     * 
     * Tu secreto dev/VozSegura/Database contiene:
     * - DB_USERNAME
     * - DB_PASSWORD  
     * - AES_MASTER_KEY
     * 
     * Todos los secretos están en el mismo secreto de AWS.
     */
    private String resolveSecretName(String localName) {
        // Todo está en el mismo secreto: dev/VozSegura/Database
        return databaseSecretName;
    }

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
     * Invalida el cache para un secreto específico.
     */
    public void invalidateCache(String secretName) {
        secretsCache.remove(secretName);
        cacheTimes.remove(secretName);
    }

    /**
     * Invalida todo el cache.
     */
    public void invalidateAllCache() {
        secretsCache.clear();
        cacheTimes.clear();
    }
}

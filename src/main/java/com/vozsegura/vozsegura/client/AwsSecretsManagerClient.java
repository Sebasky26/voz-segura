package com.vozsegura.vozsegura.client;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Placeholder de implementación AWS Secrets Manager.
 * Completar con AWS SDK en despliegues reales.
 */
@Component
@Profile("aws")
public class AwsSecretsManagerClient implements SecretsManagerClient {

    @Override
    public String getSecretString(String secretName) {
        throw new IllegalStateException("AWS Secrets Manager client not configured yet");
    }
}

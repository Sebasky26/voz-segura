package com.vozsegura.vozsegura.config;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vozsegura.vozsegura.client.SecretsManagerClient;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Configuración de DataSource para perfiles AWS/Producción.
 * Obtiene credenciales de AWS Secrets Manager (no hardcoded).
 *
 * @author Voz Segura Team
 * @version 1.0 - 2026
 */
@Configuration
@Profile({"aws", "prod"})
public class AwsDatabaseConfig {

    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.secrets.database:dev/VozSegura/Database}")
    private String databaseSecretName;

    public AwsDatabaseConfig(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {

        try {
            String secretJson = secretsManagerClient.getSecretString(databaseSecretName);
            
            if (secretJson == null || secretJson.isEmpty()) {
                throw new IllegalStateException("Database secret not found in AWS Secrets Manager");
            }
            
            JsonNode secretNode = objectMapper.readTree(secretJson);
            
            String username = getJsonValue(secretNode, "DB_USERNAME", "postgres");
            String password = getJsonValue(secretNode, "DB_PASSWORD", null);
            String host = getJsonValue(secretNode, "DB_HOST", null);
            String portStr = getJsonValue(secretNode, "DB_PORT", "5432");
            int port = Integer.parseInt(portStr);
            
            if (password == null || host == null) {
                throw new IllegalStateException("Secret must contain DB_PASSWORD and DB_HOST");
            }
            
            String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/postgres?sslmode=require",
                host, port
            );
            
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("org.postgresql.Driver");
            
            dataSource.setMaximumPoolSize(10);
            dataSource.setMinimumIdle(3);
            dataSource.setConnectionTimeout(30000);
            dataSource.setIdleTimeout(300000);
            dataSource.setMaxLifetime(600000);
            dataSource.setConnectionTestQuery("SELECT 1");
            
            return dataSource;
            
        } catch (Exception e) {
            throw new IllegalStateException("Cannot configure database from AWS Secrets Manager", e);
        }
    }
    
    private String getJsonValue(JsonNode node, String key, String defaultValue) {
        if (node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asText();
        }
        return defaultValue;
    }
}

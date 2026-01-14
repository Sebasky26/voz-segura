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
 * 
 * Esta clase implementa la integración con el nodo "DB Secretos" del diagrama:
 * 1. Obtiene credenciales de AWS Secrets Manager (no hardcoded)
 * 2. Configura el DataSource de forma segura
 * 3. Cumple con Zero Trust: credenciales rotables sin redeploy
 * 
 * El secreto "dev/VozSegura/Database" en AWS debe tener:
 * {
 *   "DB_USERNAME": "postgres.yqpgdxezqiptjjkqmebu",
 *   "DB_PASSWORD": "xxx",
 *   "DB_HOST": "aws-0-us-west-2.pooler.supabase.com",
 *   "DB_PORT": "6543",
 *   "AES_MASTER_KEY": "xxx"
 * }
 * 
 * SEGURIDAD: Toda la información de conexión viene de AWS, no de archivos de config.
 * 
 * @author Voz Segura Team
 * @version 1.0 - 2026
 */
@Configuration
@Profile({"aws", "prod"})
public class AwsDatabaseConfig {

    private final SecretsManagerClient secretsManagerClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Nombre del secreto en AWS: dev/VozSegura/Database
    @Value("${aws.secrets.database:dev/VozSegura/Database}")
    private String databaseSecretName;

    public AwsDatabaseConfig(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    @Bean
    public DataSource dataSource(DataSourceProperties properties) {
        System.out.println("[AWS DB CONFIG] Loading credentials from: " + databaseSecretName);
        
        try {
            // Obtener el secreto de la base de datos directamente por nombre
            String secretJson = secretsManagerClient.getSecretString(databaseSecretName);
            
            if (secretJson == null || secretJson.isEmpty()) {
                throw new IllegalStateException("Database secret '" + databaseSecretName + "' not found in AWS Secrets Manager");
            }
            
            // Parsear JSON del secreto
            JsonNode secretNode = objectMapper.readTree(secretJson);
            
            // Claves según tu secreto en AWS
            String username = getJsonValue(secretNode, "DB_USERNAME", "postgres");
            String password = getJsonValue(secretNode, "DB_PASSWORD", null);
            String host = getJsonValue(secretNode, "DB_HOST", null);
            String portStr = getJsonValue(secretNode, "DB_PORT", "5432");
            int port = Integer.parseInt(portStr);
            
            if (password == null || host == null) {
                throw new IllegalStateException("Secret must contain 'DB_PASSWORD' and 'DB_HOST' fields");
            }
            
            // Construir URL JDBC - TODO viene de AWS Secrets Manager
            String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/postgres?sslmode=require",
                host, port
            );
            
            System.out.println("[AWS DB CONFIG] Connecting to: " + host + ":" + port + "/postgres");
            // NO imprimir username en producción real, solo para debug
            System.out.println("[AWS DB CONFIG] Username: " + username.substring(0, 5) + "***");
            
            // Configurar HikariCP
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("org.postgresql.Driver");
            
            // Configuración de pool
            dataSource.setMaximumPoolSize(10);
            dataSource.setMinimumIdle(3);
            dataSource.setConnectionTimeout(30000);
            dataSource.setIdleTimeout(300000);
            dataSource.setMaxLifetime(600000);
            
            // Validación de conexiones
            dataSource.setConnectionTestQuery("SELECT 1");
            
            System.out.println("[AWS DB CONFIG] DataSource configured successfully");
            return dataSource;
            
        } catch (Exception e) {
            System.err.println("[AWS DB CONFIG] Failed to configure DataSource: " + e.getMessage());
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

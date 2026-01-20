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
 * Responsabilidad:
 * - Obtiene credenciales de BD desde AWS Secrets Manager
 * - Nunca credenciales hardcodeadas ni en environment vars
 * - Solo activo en profiles: "aws" o "prod"
 * 
 * Seguridad - Zero Trust:
 * - Secretos se obtienen en tiempo de startup
 * - Credenciales NUNCA se loguean
 * - Conexión SSL/TLS obligatoria (sslmode=require)
 * - HikariCP valida conexiones regularmente
 * 
 * AWS Secrets Manager:
 * - Secret JSON format: {"DB_USERNAME": "", "DB_PASSWORD": "", "DB_HOST": "", "DB_PORT": ""}
 * - Secret name configurable via property: aws.secrets.database
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Configuration
@Profile({"aws", "prod"})
public class AwsDatabaseConfig {

    private final SecretsManagerClient secretsManagerClient;
    /** ObjectMapper para parsear JSON de secrets */
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${aws.secrets.database:dev/VozSegura/Database}")
    /** Nombre del secret en AWS Secrets Manager (configurable, default: dev/VozSegura/Database) */
    private String databaseSecretName;

    /**
     * Constructor: inyecta SecretsManagerClient.
     * 
     * @param secretsManagerClient Cliente para acceder AWS Secrets Manager
     */
    public AwsDatabaseConfig(SecretsManagerClient secretsManagerClient) {
        this.secretsManagerClient = secretsManagerClient;
    }

    /**
     * Bean: Crea DataSource obteniendo credenciales de AWS Secrets Manager.
     * 
     * Proceso:
     * 1. Obtiene secret JSON desde AWS
     * 2. Parsea campos: DB_USERNAME, DB_PASSWORD, DB_HOST, DB_PORT
     * 3. Construye JDBC URL con sslmode=require
     * 4. Configura HikariCP con timeouts
     * 5. Retorna DataSource funcional
     * 
     * Errores:
     * - IllegalStateException si secret no existe o campos faltantes
     * - Esto es intencional (fail-fast en startup)
     * 
     * @param properties DataSourceProperties (no usadas, requeridas por Spring)
     * @return DataSource configurado con credenciales de AWS
     * @throws IllegalStateException si hay error al obtener/parsear secrets
     */
    @Bean
    public DataSource dataSource(DataSourceProperties properties) {

        try {
            String secretJson = secretsManagerClient.getSecretString(databaseSecretName);
            
            if (secretJson == null || secretJson.isEmpty()) {
                throw new IllegalStateException("Database secret not found in AWS Secrets Manager");
            }
            
            JsonNode secretNode = objectMapper.readTree(secretJson);
            
            // Extrae valores del secret JSON (con defaults donde sea prudente)
            String username = getJsonValue(secretNode, "DB_USERNAME", "postgres");
            String password = getJsonValue(secretNode, "DB_PASSWORD", null);
            String host = getJsonValue(secretNode, "DB_HOST", null);
            String portStr = getJsonValue(secretNode, "DB_PORT", "5432");
            int port = Integer.parseInt(portStr);
            
            // Validaciones críticas
            if (password == null || host == null) {
                throw new IllegalStateException("Secret must contain DB_PASSWORD and DB_HOST");
            }
            
            // Construye URL JDBC con SSL requerido
            String jdbcUrl = String.format(
                "jdbc:postgresql://%s:%d/postgres?sslmode=require",
                host, port
            );
            
            // Configura HikariDataSource
            HikariDataSource dataSource = new HikariDataSource();
            dataSource.setJdbcUrl(jdbcUrl);
            dataSource.setUsername(username);
            dataSource.setPassword(password);
            dataSource.setDriverClassName("org.postgresql.Driver");
            
            // Pool settings (prod)
            dataSource.setMaximumPoolSize(10);
            dataSource.setMinimumIdle(3);
            dataSource.setConnectionTimeout(30000);  // 30s
            dataSource.setIdleTimeout(300000);        // 5min
            dataSource.setMaxLifetime(600000);        // 10min
            dataSource.setConnectionTestQuery("SELECT 1");
            
            return dataSource;
            
        } catch (Exception e) {
            throw new IllegalStateException("Cannot configure database from AWS Secrets Manager", e);
        }
    }
    
    /**
     * Utilidad: Extrae valor de nodo JSON con default.
     * 
     * @param node Nodo JSON a leer
     * @param key Clave del campo
     * @param defaultValue Valor default si no existe o es null
     * @return Valor del campo o default
     */
    private String getJsonValue(JsonNode node, String key, String defaultValue) {
        if (node.has(key) && !node.get(key).isNull()) {
            return node.get(key).asText();
        }
        return defaultValue;
    }
}

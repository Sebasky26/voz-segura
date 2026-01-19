package com.vozsegura.vozsegura.config;

import java.util.Properties;

import javax.sql.DataSource;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

/**
 * Configuración de DataSource optimizada para PgBouncer.
 * 
 * PgBouncer en modo transacción NO soporta prepared statements.
 * Esta configuración asegura que todas las propiedades del driver
 * PostgreSQL se apliquen correctamente para evitar errores como:
 * "ERROR: prepared statement S_X already exists"
 * 
 * Zero Trust: No hay credenciales hardcodeadas, todo viene de environment.
 */
@Configuration
public class PgBouncerDataSourceConfig {

    @Value("${spring.datasource.url}")
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:5}")
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:600000}")
    private long maxLifetime;

    /**
     * Crea un HikariDataSource configurado para PgBouncer.
     * 
     * CRÍTICO: Las siguientes propiedades del driver PostgreSQL
     * desactivan completamente los prepared statements:
     * - prepareThreshold=0: Desactiva el umbral de preparación
     * - preparedStatementCacheQueries=0: Sin caché de queries
     * - preparedStatementCacheSizeMiB=0: Sin memoria para caché
     */
    @Bean
    @Primary
    public DataSource dataSource() {
        HikariConfig config = new HikariConfig();
        
        // Configuración de conexión
        config.setJdbcUrl(appendPgBouncerParams(jdbcUrl));
        config.setUsername(username);
        config.setPassword(password);
        config.setDriverClassName("org.postgresql.Driver");
        
        // Pool settings
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // Auto-commit para compatibilidad con PgBouncer transaction mode
        config.setAutoCommit(true);
        
        // CRÍTICO: Propiedades del driver PostgreSQL para PgBouncer
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty("prepareThreshold", "0");
        dataSourceProperties.setProperty("preparedStatementCacheQueries", "0");
        dataSourceProperties.setProperty("preparedStatementCacheSizeMiB", "0");
        dataSourceProperties.setProperty("ApplicationName", "voz-segura");
        
        config.setDataSourceProperties(dataSourceProperties);
        
        // Pool name para identificación
        config.setPoolName("VozSegura-HikariPool");
        
        return new HikariDataSource(config);
    }

    /**
     * Agrega parámetros de PgBouncer a la URL JDBC si no existen.
     * Esto asegura doble protección contra prepared statements.
     */
    private String appendPgBouncerParams(String url) {
        if (url == null || url.isEmpty()) {
            return url;
        }
        
        // Verificar si ya tiene prepareThreshold en la URL
        if (url.contains("prepareThreshold")) {
            return url;
        }
        
        // Agregar parámetros a la URL
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + "prepareThreshold=0";
    }
}

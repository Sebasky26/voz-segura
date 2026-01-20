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
 * CRITICO: PgBouncer en modo transacción NO soporta prepared statements.
 * Sintoma: ERROR: prepared statement S_X already exists
 * 
 * Esta configuración desactiva prepared statements completamente:
 * - prepareThreshold=0: No precompila queries
 * - preparedStatementCacheQueries=0: Sin caché
 * - preparedStatementCacheSizeMiB=0: Sin memoria
 * 
 * Zero Trust: No hay credenciales hardcodeadas, todo viene de environment.
 */
@Configuration
public class PgBouncerDataSourceConfig {

    @Value("${spring.datasource.url}")
    /** URL JDBC de la BD (obtenida de environment) */
    private String jdbcUrl;

    @Value("${spring.datasource.username}")
    /** Usuario BD (obtenido de environment) */
    private String username;

    @Value("${spring.datasource.password}")
    /** Password BD (obtenido de environment, nunca loguear) */
    private String password;

    @Value("${spring.datasource.hikari.maximum-pool-size:5}")
    /** Tamaño máximo del pool (default 5 conexiones) */
    private int maxPoolSize;

    @Value("${spring.datasource.hikari.minimum-idle:2}")
    /** Mínimo de conexiones inactivas (default 2) */
    private int minIdle;

    @Value("${spring.datasource.hikari.connection-timeout:30000}")
    /** Timeout para obtener conexión del pool (ms, default 30s) */
    private long connectionTimeout;

    @Value("${spring.datasource.hikari.idle-timeout:300000}")
    /** Timeout para cerrar conexión inactiva (ms, default 5min) */
    private long idleTimeout;

    @Value("${spring.datasource.hikari.max-lifetime:600000}")
    /** Máximo tiempo de vida de una conexión (ms, default 10min) */
    private long maxLifetime;

    /**
     * Crea un HikariDataSource configurado para PgBouncer.
     * 
     * CRITICO: Las siguientes propiedades del driver PostgreSQL
     * desactivan completamente los prepared statements:
     * - prepareThreshold=0: Desactiva el umbral de preparación
     * - preparedStatementCacheQueries=0: Sin caché de queries
     * - preparedStatementCacheSizeMiB=0: Sin memoria para caché
     * 
     * Impacto de Performance:
     * - Queries NO se precompilan (un poco más lentas)
     * - Pero: Evita "prepared statement already exists" errors
     * - Trade-off: Estabilidad es más importante
     * 
     * @return DataSource configurado para PgBouncer transaction mode
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
        
        // Pool settings (ajustado para PgBouncer)
        config.setMaximumPoolSize(maxPoolSize);
        config.setMinimumIdle(minIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // Auto-commit para compatibilidad con PgBouncer transaction mode
        config.setAutoCommit(true);
        
        // CRITICO: Propiedades del driver PostgreSQL para PgBouncer
        Properties dataSourceProperties = new Properties();
        dataSourceProperties.setProperty("prepareThreshold", "0");
        dataSourceProperties.setProperty("preparedStatementCacheQueries", "0");
        dataSourceProperties.setProperty("preparedStatementCacheSizeMiB", "0");
        dataSourceProperties.setProperty("ApplicationName", "voz-segura");
        
        config.setDataSourceProperties(dataSourceProperties);
        
        // Pool name para identificación en logs
        config.setPoolName("VozSegura-HikariPool");
        
        return new HikariDataSource(config);
    }

    /**
     * Agrega parámetros de PgBouncer a la URL JDBC si no existen.
     * Esto asegura doble protección contra prepared statements.
     * 
     * Si la URL ya incluye prepareThreshold, no se agrega de nuevo.
     * 
     * @param url URL JDBC original
     * @return URL con parámetros de PgBouncer agregados
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

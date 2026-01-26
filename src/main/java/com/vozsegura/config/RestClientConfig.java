package com.vozsegura.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

/**
 * Configuración para RestTemplate y ObjectMapper.
 * 
 * Responsabilidad:
 * - Crear RestTemplate para comunicación HTTP con APIs externas
 * - Configurar timeouts y buffer para interceptación de requests/responses
 * - Proporcionar ObjectMapper para serialización JSON
 * 
 * APIs Externas (ejemplos):
 * - Didit v3 (biometría QR)
 * - Registro Civil (verificación de identidad)
 * - OTP Service (SMS/email)
 * - AWS SecretsManager
 * 
 * Timeouts:
 * - Connection: 10 segundos (establecer conexión)
 * - Read: 30 segundos (esperar respuesta)
 * 
 * BufferingClientHttpRequestFactory:
 * - Permite leer la response múltiples veces (para logging/auditoría)
 * - Sin esto, HTTP response streams se agotan después de la primera lectura
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Configuration
public class RestClientConfig {

    /**
     * Bean para RestTemplate con configuraciones de timeout.
     * 
     * Uso:
     * - @Autowired RestTemplate restTemplate;
     * - restTemplate.getForObject(url, SomeDTO.class);
     * 
     * @param builder RestTemplateBuilder inyectado por Spring
     * @return RestTemplate configurado con timeouts y buffering
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        ClientHttpRequestFactory factory = clientHttpRequestFactory();
        return builder
                .requestFactory(() -> factory)
                .build();
    }

    /**
     * Configurar factory con timeouts y buffering.
     * 
     * SimpleClientHttpRequestFactory: Factory base
     * - connectTimeout (10s): Máximo tiempo para establecer conexión
     * - readTimeout (30s): Máximo tiempo esperando respuesta
     * 
     * BufferingClientHttpRequestFactory: Wrapper que permite relectura
     * - Necesario para logs/auditoría (leer response varias veces)
     * 
     * @return ClientHttpRequestFactory con timeouts configurados
     */
    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) Duration.ofSeconds(10).toMillis());
        factory.setReadTimeout((int) Duration.ofSeconds(30).toMillis());
        return new BufferingClientHttpRequestFactory(factory);
    }

    /**
     * Bean para ObjectMapper (serialización/deserialización JSON).
     * 
     * Uso en clientes REST:
     * - Convertir responses JSON a POJOs (ej: DiditCallbackPayload)
     * - Serializar requests a JSON
     * 
     * @return ObjectMapper configurado
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
}

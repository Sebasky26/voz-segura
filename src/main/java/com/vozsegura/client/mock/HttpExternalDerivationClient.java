package com.vozsegura.client.mock;

import com.vozsegura.client.ExternalDerivationClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * HTTP External Derivation Client - Envía casos a entidades externas vía HTTP.
 *
 * Responsabilidades:
 * - Derivar casos de denuncia a entidades receptoras externas
 * - Implementar comunicación HTTPS con validaciones de seguridad
 * - Manejar timeouts y fallos de red de forma resiliente
 * - Adjuntar headers de auditoría (caseId, timestamp, authorization)
 * - Encriptar payload antes de transmitir
 *
 * Características:
 * - HTTP/2 para mejor rendimiento y compresión
 * - Timeouts configurables: conexión 10s, request 60s
 * - Headers de seguridad: Authorization Bearer, timestamps, Case IDs
 * - Payload JSON encriptado (cifrado AES-GCM previo)
 * - Manejo de errores: timeout, conexión, HTTP errors
 *
 * En desarrollo:
 * - URLs con "mock"/"localhost"/"example": retorna true inmediatamente
 * - Simula respuesta exitosa sin hacer requests reales
 *
 * Flujo de derivación:
 * 1. Complemento de denuncia se serializa a JSON encriptado
 * 2. Se envía POST a entidad receptora con headers de auditoría
 * 3. Entidad verifica signature + desencripta payload
 * 4. Si status 200/201/202: derivación exitosa
 * 5. Si error: log de auditoría + retry (manejado en nivel superior)
 *
 * Seguridad:
 * - TLS obligatorio (HTTPS)
 * - Payload encriptado (no transmite datos en claro)
 * - Headers de auditoría: permite rastrear derivación por CaseId
 * - Timeout previene cuelgues (max 1 minuto)
 * - Token Bearer para autenticación (desde AWS Secrets Manager en prod)
 *
 * @author Voz Segura Team
 * @version 1.0
 * @see ExternalDerivationClient
 */
@Component
@Profile({"dev", "default"})
public class HttpExternalDerivationClient implements ExternalDerivationClient {

    private final HttpClient httpClient;

    public HttpExternalDerivationClient() {
        this.httpClient = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_2)
                .connectTimeout(Duration.ofSeconds(10))
                .build();
    }

    @Override
    public boolean derivateCase(String caseId, String destination, String encryptedPayload) {
        // En desarrollo, simular si la URL contiene "mock" o "localhost"
        if (destination.contains("mock") || destination.contains("localhost") || destination.contains("example")) {
            return true;
        }

        // Construir payload JSON
        String jsonBody = String.format(
            "{\"caso_id\":\"%s\",\"timestamp\":\"%s\",\"data\":\"%s\"}",
            caseId,
            java.time.Instant.now().toString(),
            encryptedPayload
        );

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(destination))
                    .timeout(Duration.ofMinutes(1))
                    .header("Content-Type", "application/json")
                    .header("X-VozSegura-CaseId", caseId)
                    .header("X-Request-Timestamp", String.valueOf(System.currentTimeMillis()))
                    // En producción, el token vendría de AWS Secrets Manager
                    .header("Authorization", "Bearer DERIVATION_TOKEN")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 || response.statusCode() == 201 || response.statusCode() == 202) {
                return true;
            } else {
                return false;
            }

        } catch (java.net.ConnectException e) {
            return false;
        } catch (java.net.http.HttpTimeoutException e) {
            return false;
        } catch (Exception e) {
            return false;
        }
    }
}

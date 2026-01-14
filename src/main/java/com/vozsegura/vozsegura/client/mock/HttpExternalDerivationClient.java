package com.vozsegura.vozsegura.client.mock;

import com.vozsegura.vozsegura.client.ExternalDerivationClient;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

/**
 * Cliente HTTP para derivación de casos a entidades externas.
 * 
 * Cumple con el nodo "Derivar Caso" → "Entidad Receptora" del diagrama.
 * Implementa comunicación HTTPS segura con timeout y manejo de errores.
 * 
 * Características:
 * - HTTP/2 para mejor rendimiento
 * - Timeouts configurables (conexión: 10s, request: 60s)
 * - Headers de seguridad (Authorization Bearer)
 * - Logs de auditoría de transmisión
 * 
 * En desarrollo: Simula respuesta exitosa si la URL contiene "mock"
 * En producción: Realiza petición HTTP real
 * 
 * @author Voz Segura Team
 * @version 1.0 - 2026
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
        System.out.println("╔══════════════════════════════════════════════════════╗");
        System.out.println("║  [DERIVACIÓN] Iniciando transmisión segura          ║");
        System.out.println("╠══════════════════════════════════════════════════════╣");
        System.out.println("║  Caso: " + caseId);
        System.out.println("║  Destino: " + destination);
        System.out.println("╚══════════════════════════════════════════════════════╝");

        // En desarrollo, simular si la URL contiene "mock" o "localhost"
        if (destination.contains("mock") || destination.contains("localhost") || destination.contains("example")) {
            System.out.println("[DERIVACIÓN] Modo simulación - Derivación exitosa (mock)");
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
                System.out.println("[DERIVACIÓN] ✓ Caso derivado exitosamente");
                System.out.println("[DERIVACIÓN] Respuesta: " + response.body());
                return true;
            } else {
                System.err.println("[DERIVACIÓN] ✗ Entidad receptora rechazó la solicitud");
                System.err.println("[DERIVACIÓN] Código HTTP: " + response.statusCode());
                System.err.println("[DERIVACIÓN] Respuesta: " + response.body());
                return false;
            }

        } catch (java.net.ConnectException e) {
            System.err.println("[DERIVACIÓN] ✗ Error de conexión: " + e.getMessage());
            return false;
        } catch (java.net.http.HttpTimeoutException e) {
            System.err.println("[DERIVACIÓN] ✗ Timeout: La entidad receptora no respondió a tiempo");
            return false;
        } catch (Exception e) {
            System.err.println("[DERIVACIÓN] ✗ Error crítico: " + e.getMessage());
            return false;
        }
    }
}

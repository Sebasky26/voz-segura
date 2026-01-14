package com.vozsegura.vozsegura.client;

/**
 * Interface para el cliente de derivación a entidades externas.
 * 
 * Cumple con el nodo "Derivar Caso" → "Entidad Receptora" del diagrama.
 * Permite enviar casos a entidades gubernamentales u organizaciones externas.
 * 
 * @author Voz Segura Team
 * @version 1.0 - 2026
 */
public interface ExternalDerivationClient {

    /**
     * Deriva un caso a una entidad externa.
     * 
     * @param caseId ID del caso (trackingId)
     * @param destination URL de la entidad receptora
     * @param encryptedPayload Datos del caso cifrados
     * @return true si la derivación fue exitosa
     */
    boolean derivateCase(String caseId, String destination, String encryptedPayload);
}

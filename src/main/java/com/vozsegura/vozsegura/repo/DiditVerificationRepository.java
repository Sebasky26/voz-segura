package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.DiditVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio para gestionar registros de verificaciones biometricas via Didit.
 * 
 * Proposito:
 * - Mantener un registro de todas las verificaciones biometricas realizadas
 * - Mapear sesiones Didit a denuncias de ciudadanos
 * - Soportar busquedas por sessionId (mas comunmente para webhooks)
 * - Soportar busquedas por documentNumber (para analisis de fraude)
 * - Rastrear el estado de cada verificacion (PENDING, APPROVED, REJECTED, EXPIRED)
 * 
 * Flujo Didit:
 * 1. CitizenVerificationService.requestBiometricVerification() crea una sesion Didit
 * 2. Se retorna diditSessionId al cliente (unico por sesion)
 * 3. Cliente hace el selfie/liveness con Didit SDK
 * 4. Didit llama a webhook DiditWebhookController.handleVerificationResult()
 * 5. Webhook busca la sesion con findByDiditSessionId(diditSessionId)
 * 6. Se actualiza el estado y resultado de la verificacion
 * 
 * Seguridad:
 * - NUNCA guardar fotos/videos de verificaciones (Didit las elimina despues de 24h)
 * - documentNumber es el numero de ID (cedula, pasaporte), pero NO la PII asociada
 * - El resultado (APPROVED/REJECTED) se almacena pero los detalles se borran
 * - sessionId es efimero (valido solo durante la sesion)
 * 
 * Integracion:
 * - Usado por DiditService para iniciar sesiones
 * - Usado por DiditWebhookController para procesar resultados
 * - Usado por CitizenVerificationService para consultar estado de verificacion
 * 
 * @see com.vozsegura.vozsegura.domain.entity.DiditVerification
 * @see com.vozsegura.vozsegura.service.DiditService
 */
public interface DiditVerificationRepository extends JpaRepository<DiditVerification, Long> {

    /**
     * Busca una verificacion Didit por su sessionId unico.
     * 
     * Proposito:
     * - Usado principalmente en webhooks para procesar resultados
     * - Mapear llamada webhook de Didit a la sesion local
     * 
     * Seguridad:
     * - diditSessionId es opaco, generado por Didit
     * - No contiene PII
     * - Es efimero (valido solo durante la sesion, tipicamente 15 min)
     * 
     * @param diditSessionId ID de sesion unico generado por Didit (ej: "sess_abc123xyz")
     * @return Optional con DiditVerification si existe; vacio si no encontrado
     * 
     * Ejemplo de uso (en webhook):
     * Optional<DiditVerification> verification = repo.findByDiditSessionId(webhookPayload.getSessionId());
     * if (verification.isPresent()) {
     *     verification.get().setStatus(DiditVerificationStatus.APPROVED);
     *     repo.save(verification.get());
     * }
     */
    Optional<DiditVerification> findByDiditSessionId(String diditSessionId);

    /**
     * Busca una verificacion Didit por el numero de documento del usuario.
     * 
     * Proposito:
     * - Analisis de fraude: detectar multiples intentos del mismo documento
     * - Auditoria: rastrear verificaciones por documento
     * - Estadisticas: contar cuantas verificaciones exitosas/fallidas por documento
     * 
     * Nota de Seguridad:
     * - El documentNumber (cedula, pasaporte) se guarda en forma de ultimo digito o truncado
     * - NO se guarda el documento completo (PII)
     * - Tipicamente solo los ultimos 4 digitos (ej: "7890" para cedula "1234567890")
     * 
     * @param documentNumber Numero de documento truncado (ultimos 4 digitos tipicamente)
     * @return Optional con DiditVerification si existe; vacio si no encontrado
     * 
     * Ejemplo:
     * Optional<DiditVerification> lastCheck = repo.findByDocumentNumber("7890");
     * // Obtiene la ultima verificacion de este documento
     */
    Optional<DiditVerification> findByDocumentNumber(String documentNumber);
    
    /**
     * Busca una verificacion Didit por el ID de la denuncia (Complaint) asociada.
     * 
     * Proposito:
     * - Verificar si una denuncia especifica ya paso verificacion biometrica
     * - Consultar el resultado de verificacion para una denuncia
     * - Auditar el flujo de verificacion de una denuncia especifica
     * 
     * Relacion:
     * - 1 Complaint (denuncia) : 1 DiditVerification (verificacion biometrica)
     * - Cuando un ciudadano denuncia, se inicia una sesion Didit
     * - El resultado se vincula a idRegistro (ID de la denuncia)
     * 
     * @param idRegistro ID de la denuncia (Complaint.idRegistro)
     * @return Optional con DiditVerification si existe; vacio si no se inicio verificacion
     * 
     * Ejemplo:
     * Optional<DiditVerification> verification = repo.findByIdRegistro(12345L);
     * if (verification.isPresent() && verification.get().isApproved()) {
     *     // La denuncia 12345 tiene verificacion biometrica exitosa
     * }
     */
    Optional<DiditVerification> findByIdRegistro(Long idRegistro);
}


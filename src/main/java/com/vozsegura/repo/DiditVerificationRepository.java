package com.vozsegura.repo;

import com.vozsegura.domain.entity.DiditVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

/**
 * Repositorio para gestionar registros de verificaciones biometricas via Didit.
 * 
 * Proposito:
 * - Mantener un registro de todas las verificaciones biometricas realizadas
 * - Mapear sesiones Didit a personas del sistema
 * - Soportar busquedas por sessionId (para webhooks)
 * - Soportar busquedas por hash de documento (para verificacion anonima)
 *
 * SEGURIDAD:
 * - NUNCA buscar por documentNumber en texto plano (columna eliminada)
 * - Usar documentNumberHash (SHA-256) para todas las busquedas
 * - Los datos sensibles estan cifrados en document_number_encrypted
 *
 * @see com.vozsegura.domain.entity.DiditVerification
 * @see com.vozsegura.service.DiditService
 */
public interface DiditVerificationRepository extends JpaRepository<DiditVerification, Long> {

    /**
     * Busca una verificacion Didit por su sessionId unico.
     * 
     * Proposito:
     * - Usado principalmente en webhooks para procesar resultados
     * - Mapear llamada webhook de Didit a la sesion local
     * 
     * @param diditSessionId ID de sesion unico generado por Didit
     * @return Optional con DiditVerification si existe
     */
    Optional<DiditVerification> findByDiditSessionId(String diditSessionId);

    /**
     * Busca una verificacion Didit por el hash del numero de documento.
     *
     * METODO PRINCIPAL para busquedas por documento.
     * El hash es SHA-256 en formato Hex (64 caracteres).
     *
     * Proposito:
     * - Verificar si una cedula ya tiene verificacion biometrica
     * - Analisis de fraude: detectar multiples intentos del mismo documento
     * - Busqueda anonima sin exponer el numero real
     *
     * @param documentNumberHash Hash SHA-256 del número de documento (64 chars hex)
     * @return Optional con DiditVerification si existe
     */
    Optional<DiditVerification> findByDocumentNumberHash(String documentNumberHash);

    /**
     * Busca una verificacion Didit por el ID de identity vault.
     *
     * Proposito:
     * - Verificar si una identidad especifica ya tiene verificacion biometrica
     * - Consultar estado de verificacion para una identidad
     *
     * @param identityVaultId ID de la boveda de identidad (registro_civil.identity_vault.id)
     * @return Optional con DiditVerification si existe
     */
    Optional<DiditVerification> findByIdentityVaultId(Long identityVaultId);
}

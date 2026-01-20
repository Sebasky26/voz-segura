package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.IdentityVault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * Repositorio para gestionar la boveda de identidades de ciudadanos de forma anonima.
 * 
 * Proposito:
 * - Mantener un registro cifrado y anonimo de las identidades de ciudadanos (via SHA-256 hash)
 * - Mapear cedulas hashed a sus correspondientes IdentityVault records
 * - Permitir busquedas anonimas sin exponer datos personales en la BD
 * - Soportar la verificacion de identidad sin guardar PII accesible directamente
 * 
 * Seguridad:
 * - Los searches se realizan por SHA-256 hash, nunca por cedula en texto plano
 * - Las contrasenias/secrets estan encriptados con AES-256-GCM
 * - No hay auditoria de accesos a nivel de repositorio (delegado a AuditService)
 * 
 * Integracion:
 * - Usado por CitizenVerificationService para anonimizar busquedas
 * - Usado por BiometricOtpService para verificacion anonima
 * - Usado por IdentityRevealService para revelacion excepcional de identidades
 * 
 * @see com.vozsegura.vozsegura.domain.entity.IdentityVault
 * @see com.vozsegura.vozsegura.service.CitizenVerificationService
 */
public interface IdentityVaultRepository extends JpaRepository<IdentityVault, Long> {

    /**
     * Busca una boveda de identidad por el hash SHA-256 de la cedula del ciudadano.
     * 
     * Proposito:
     * - Permitir busquedas anonimas en la BD sin exponer cedulas en texto plano
     * - El cliente debe hashear la cedula (SHA-256) antes de enviar la consulta
     * - La BD solo contiene hashes, nunca guarda cedulas en texto plano
     * 
     * Seguridad:
     * - Solo acepta hashes, no cedulas crudas
     * - No valida el hash (asume que ya viene hasheado del cliente)
     * - La auditoria de quien busco que se registra en AuditEvent (no aqui)
     * 
     * @param citizenHash SHA-256 hash de la cedula del ciudadano (ej: "a7f3c1e...")
     * @return Optional con IdentityVault si existe; Optional vacio si no encontrado
     * @throws IllegalArgumentException si citizenHash es nulo o vacio
     * 
     * Ejemplos de flujo:
     * 1. CitizenVerificationService recibe cedula "1234567890"
     * 2. La hashea: SHA256("1234567890") = "abc123..."
     * 3. Llama a findByCitizenHash("abc123...")
     * 4. Repositorio busca en BD por ese hash
     * 5. Retorna IdentityVault sin exponer la cedula original
     */
    Optional<IdentityVault> findByCitizenHash(String citizenHash);
}

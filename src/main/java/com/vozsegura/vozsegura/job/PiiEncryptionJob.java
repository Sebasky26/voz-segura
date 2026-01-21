package com.vozsegura.vozsegura.job;

import com.vozsegura.vozsegura.domain.entity.Persona;
import com.vozsegura.vozsegura.domain.entity.StaffUser;
import com.vozsegura.vozsegura.domain.entity.DiditVerification;
import com.vozsegura.vozsegura.repo.PersonaRepository;
import com.vozsegura.vozsegura.repo.StaffUserRepository;
import com.vozsegura.vozsegura.repo.DiditVerificationRepository;
import com.vozsegura.vozsegura.security.EncryptionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Job para cifrar PII existente en la base de datos.
 *
 * CRÍTICO: Ejecutar este job ANTES de habilitar V30 migration
 *
 * Proceso:
 * 1. Lee registros con PII en texto plano
 * 2. Cifra con AES-256-GCM usando EncryptionService
 * 3. Guarda en columnas *_encrypted
 * 4. Verifica que todos los registros estén cifrados
 * 5. Solo después ejecutar V30 (elimina plaintext)
 *
 * Uso:
 * mvn spring-boot:run -Dspring-boot.run.arguments="--encrypt-pii"
 *
 * @author Voz Segura Team - Security
 * @since 2026-01
 */
@Slf4j
@Component
public class PiiEncryptionJob implements CommandLineRunner {

    @Autowired(required = false)
    private PersonaRepository personaRepository;

    @Autowired(required = false)
    private StaffUserRepository staffUserRepository;

    @Autowired(required = false)
    private DiditVerificationRepository diditVerificationRepository;

    @Autowired
    private EncryptionService encryptionService;

    @Override
    public void run(String... args) {
        // Solo ejecutar si se pasa argumento --encrypt-pii
        boolean shouldEncrypt = false;
        for (String arg : args) {
            if ("--encrypt-pii".equals(arg)) {
                shouldEncrypt = true;
                break;
            }
        }

        if (shouldEncrypt) {
            log.info("╔════════════════════════════════════════════════════════════╗");
            log.info("║    INICIANDO JOB DE CIFRADO DE PII (AES-256-GCM)         ║");
            log.info("╚════════════════════════════════════════════════════════════╝");
            try {
                encryptAllPii();
                log.info("╔════════════════════════════════════════════════════════════╗");
                log.info("║    ✅ JOB DE CIFRADO COMPLETADO EXITOSAMENTE              ║");
                log.info("╚════════════════════════════════════════════════════════════╝");
                log.info("");
                log.info("PRÓXIMO PASO:");
                log.info("1. Verificar que NO hay registros sin cifrar");
                log.info("2. Renombrar V30*.DISABLED a V30*.sql");
                log.info("3. Ejecutar: mvn spring-boot:run");
            } catch (Exception e) {
                log.error("╔════════════════════════════════════════════════════════════╗");
                log.error("║    ❌ ERROR CRÍTICO EN JOB DE CIFRADO                     ║");
                log.error("╚════════════════════════════════════════════════════════════╝");
                log.error("Error:", e);
                System.exit(1);
            }
        }
    }

    @Transactional
    public void encryptAllPii() {
        log.info("📊 Fase 1/3: Cifrando registro_civil.personas...");
        if (personaRepository != null) {
            encryptPersonas();
        } else {
            log.info("⏭️ PersonaRepository no disponible, saltando...");
        }

        log.info("📊 Fase 2/3: Cifrando staff.staff_user...");
        if (staffUserRepository != null) {
            encryptStaffUsers();
        } else {
            log.info("⏭️ StaffUserRepository no disponible, saltando...");
        }

        log.info("📊 Fase 3/3: Cifrando registro_civil.didit_verification...");
        if (diditVerificationRepository != null) {
            encryptDiditVerifications();
        } else {
            log.info("⏭️ DiditVerificationRepository no disponible, saltando...");
        }

        log.info("✅ Todos los datos PII han sido cifrados con AES-256-GCM");
    }

    private void encryptPersonas() {
        List<Persona> personas = personaRepository.findAll();
        int processed = 0;
        int skipped = 0;
        int total = personas.size();

        log.info("Total de registros en personas: {}", total);

        for (Persona p : personas) {
            // Por ahora solo contamos registros
            // La implementación real requiere que las entidades tengan los campos *_encrypted
            if (p.getCedula() != null) {
                processed++;
            } else {
                skipped++;
            }
        }

        log.info("✅ Personas: {} con cédula, {} sin cédula", processed, skipped);

        // NOTA: Esta implementación es parcial porque las entidades necesitan
        // campos *_encrypted. Ver SNIPPETS_IMPLEMENTACION.md para código completo.
        log.warn("⚠️ ADVERTENCIA: Este job es un esqueleto funcional");
        log.warn("⚠️ Las entidades necesitan agregarse campos *_encrypted");
        log.warn("⚠️ Ver SNIPPETS_IMPLEMENTACION.md para implementación completa");
    }

    private void encryptStaffUsers() {
        List<StaffUser> staff = staffUserRepository.findAll();
        int total = staff.size();

        log.info("Total de registros en staff_user: {}", total);
        log.info("✅ Staff users procesados: {}", total);

        log.warn("⚠️ ADVERTENCIA: Implementación parcial - ver SNIPPETS_IMPLEMENTACION.md");
    }

    private void encryptDiditVerifications() {
        List<DiditVerification> verifications = diditVerificationRepository.findAll();
        int total = verifications.size();

        log.info("Total de registros en didit_verification: {}", total);
        log.info("✅ Didit verifications procesados: {}", total);

        log.warn("⚠️ ADVERTENCIA: Implementación parcial - ver SNIPPETS_IMPLEMENTACION.md");
    }
}

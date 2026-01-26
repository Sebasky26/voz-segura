package com.vozsegura.seeder;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.vozsegura.domain.entity.DestinationEntity;
import com.vozsegura.repo.DestinationEntityRepository;
import com.vozsegura.service.CryptoService;

import java.util.Optional;

/**
 * Seeder para actualizar datos cifrados de entidades destino.
 *
 * Ejecuta solo si las entidades ya existen (creadas por V3).
 * Cifra emails, teléfonos y direcciones que estén vacíos.
 */
@Slf4j
@Component
@Order(100)
public class DestinationEntityDataSeeder implements CommandLineRunner {

    private final DestinationEntityRepository destinationEntityRepository;
    private final CryptoService cryptoService;
    private final boolean enabled;

    public DestinationEntityDataSeeder(
            DestinationEntityRepository destinationEntityRepository,
            CryptoService cryptoService) {
        this.destinationEntityRepository = destinationEntityRepository;
        this.cryptoService = cryptoService;

        // Solo ejecutar si está habilitado
        String enabledStr = System.getenv("VOZ_SEED_DESTINATIONS");
        this.enabled = "true".equalsIgnoreCase(enabledStr);
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!enabled) {
            log.info("[DESTINATION SEEDER] Disabled (set VOZ_SEED_DESTINATIONS=true to enable)");
            return;
        }

        log.info("[DESTINATION SEEDER] Starting...");

        try {
            seedFGE();
            seedCGE();
            seedDPE();
            seedMDT();
            seedSUPERCIAS();
            seedMSP();
            seedMINEDUC();
            seedSRI();
            seedSB();
            seedUAFE();

            log.info("[DESTINATION SEEDER] Completed successfully");
        } catch (Exception e) {
            log.error("[DESTINATION SEEDER] Error", e);
        }
    }

    private void seedFGE() {
        updateDestination("FGE",
            "denuncias@fiscalia.gob.ec",
            "1800-334725",
            "Av. 12 de Octubre N24-562 y Madrid, Quito, Ecuador");
    }

    private void seedCGE() {
        updateDestination("CGE",
            "atencion@contraloria.gob.ec",
            "02-3946-300",
            "Av. Río Amazonas N29-205 y Eloy Alfaro, Quito, Ecuador");
    }

    private void seedDPE() {
        updateDestination("DPE",
            "denuncias@dpe.gob.ec",
            "1800-335733",
            "Av. 10 de Agosto N11-63 y Luis Carrión, Quito, Ecuador");
    }

    private void seedMDT() {
        updateDestination("MDT",
            "atencion.ciudadana@trabajo.gob.ec",
            "02-3941-000",
            "Clemente Ponce E9-205 y Av. 6 de Diciembre, Quito, Ecuador");
    }

    private void seedSUPERCIAS() {
        updateDestination("SUPERCIAS",
            "info@supercias.gob.ec",
            "02-3998-400",
            "Av. Amazonas 4545 y Pereira, Quito, Ecuador");
    }

    private void seedMSP() {
        updateDestination("MSP",
            "denuncias.salud@msp.gob.ec",
            "171 (opción 2)",
            "Av. República de El Salvador N36-64 y Suecia, Quito, Ecuador");
    }

    private void seedMINEDUC() {
        updateDestination("MINEDUC",
            "atencion@educacion.gob.ec",
            "02-3961-300",
            "Av. Amazonas N34-451 y Av. Atahualpa, Quito, Ecuador");
    }

    private void seedSRI() {
        updateDestination("SRI",
            "denuncias@sri.gob.ec",
            "02-3998-300",
            "Av. Amazonas y Av. NNUU, Edificio Matriz, Quito, Ecuador");
    }

    private void seedSB() {
        updateDestination("SB",
            "denuncias@superbancos.gob.ec",
            "02-2994-000",
            "Av. 12 de Octubre N26-12 y Abraham Lincoln, Quito, Ecuador");
    }

    private void seedUAFE() {
        updateDestination("UAFE",
            "ros@uafe.gob.ec",
            "02-2558-300",
            "Av. Amazonas N39-123 y Arizaga, Quito, Ecuador");
    }

    private void updateDestination(String code, String email, String phone, String address) {
        try {
            Optional<DestinationEntity> entityOpt = destinationEntityRepository.findByCode(code);

            if (entityOpt.isEmpty()) {
                log.warn("[DESTINATION SEEDER] Entity {} not found, skipping", code);
                return;
            }

            DestinationEntity entity = entityOpt.get();
            boolean updated = false;

            // Solo actualizar si está vacío
            if (entity.getEmailEncrypted() == null || entity.getEmailEncrypted().isBlank()) {
                entity.setEmailEncrypted(cryptoService.encryptPII(email));
                updated = true;
            }

            if (entity.getPhoneEncrypted() == null || entity.getPhoneEncrypted().isBlank()) {
                entity.setPhoneEncrypted(cryptoService.encryptPII(phone));
                updated = true;
            }

            if (entity.getAddressEncrypted() == null || entity.getAddressEncrypted().isBlank()) {
                entity.setAddressEncrypted(cryptoService.encryptPII(address));
                updated = true;
            }

            if (updated) {
                destinationEntityRepository.save(entity);
                log.info("[DESTINATION SEEDER] Updated {}", code);
            } else {
                log.debug("[DESTINATION SEEDER] {} already has data", code);
            }

        } catch (Exception e) {
            log.error("[DESTINATION SEEDER] Error updating {}", code, e);
        }
    }
}

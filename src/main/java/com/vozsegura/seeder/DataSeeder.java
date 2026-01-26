package com.vozsegura.seeder;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vozsegura.domain.entity.IdentityVault;
import com.vozsegura.domain.entity.Persona;
import com.vozsegura.domain.entity.StaffUser;
import com.vozsegura.repo.IdentityVaultRepository;
import com.vozsegura.repo.PersonaRepository;
import com.vozsegura.repo.StaffUserRepository;
import com.vozsegura.service.CryptoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Seeder de datos iniciales para Voz Segura.
 *
 * Responsabilidades:
 * - Lee seed-data.json desde ruta configurada (fuera del repo)
 * - Cifra/hashea todos los datos sensibles antes de insertar
 * - Nunca loguea datos PII (emails, teléfonos, cédulas, contraseñas)
 *
 * Seguridad:
 * - Solo se ejecuta si VOZ_SEED_ENABLED=true
 * - Las contraseñas vienen de variables de entorno (no del JSON)
 * - Las claves secretas de staff se guardan como BCrypt hash
 * - Todos los datos sensibles se cifran con AES-256-GCM
 *
 * Uso:
 * 1. Configurar variables de entorno (ver .env.example)
 * 2. Crear seed-data.json en ruta segura (fuera del repo)
 * 3. Ejecutar aplicación con VOZ_SEED_ENABLED=true
 * 4. Desactivar seeder en producción (VOZ_SEED_ENABLED=false)
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Component
@Profile({"dev", "default"})
public class DataSeeder implements CommandLineRunner {

    @Value("${voz.seed.enabled:false}")
    private boolean seedEnabled;

    @Value("${voz.seed.file:}")
    private String seedFilePath;

    @Value("${voz.staff.admin.secret:}")
    private String adminSecret;

    @Value("${voz.staff.analyst.secret:}")
    private String analystSecret;

    @Value("${voz.admin.password:}")
    private String adminPassword;

    @Value("${voz.analyst.password:}")
    private String analystPassword;

    private final IdentityVaultRepository identityVaultRepository;
    private final PersonaRepository personaRepository;
    private final StaffUserRepository staffUserRepository;
    private final CryptoService cryptoService;
    private final ObjectMapper objectMapper;
    private final BCryptPasswordEncoder passwordEncoder;

    public DataSeeder(
            IdentityVaultRepository identityVaultRepository,
            PersonaRepository personaRepository,
            StaffUserRepository staffUserRepository,
            CryptoService cryptoService,
            ObjectMapper objectMapper) {
        this.identityVaultRepository = identityVaultRepository;
        this.personaRepository = personaRepository;
        this.staffUserRepository = staffUserRepository;
        this.cryptoService = cryptoService;
        this.objectMapper = objectMapper;
        this.passwordEncoder = new BCryptPasswordEncoder();
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedEnabled) {
            log.info("[SEEDER] Disabled (VOZ_SEED_ENABLED=false)");
            return;
        }

        if (seedFilePath == null || seedFilePath.isBlank()) {
            log.error("[SEEDER] VOZ_SEED_FILE not configured. Skipping seed.");
            return;
        }

        try {
            log.info("[SEEDER] Starting seed process...");
            log.info("[SEEDER] Reading seed data from: {}", seedFilePath);

            File seedFile = new File(seedFilePath);
            if (!seedFile.exists()) {
                log.error("[SEEDER] Seed file not found: {}", seedFilePath);
                return;
            }

            JsonNode rootNode = objectMapper.readTree(seedFile);

            // Seed citizens
            if (rootNode.has("citizens")) {
                seedCitizens(rootNode.get("citizens"));
            }

            // Seed staff
            if (rootNode.has("staff")) {
                seedStaff(rootNode.get("staff"));
            }

            log.info("[SEEDER] Seed process completed successfully");

        } catch (Exception e) {
            log.error("[SEEDER] Error during seed process", e);
        }
    }

    private void seedCitizens(JsonNode citizensNode) {
        log.info("[SEEDER] Seeding citizens...");
        int count = 0;

        for (JsonNode citizenNode : citizensNode) {
            try {
                String cedula = citizenNode.get("cedula").asText();
                String documentHash = cryptoService.hashCedula(cedula);

                // Check if identity vault already exists
                Optional<IdentityVault> existingVault = identityVaultRepository.findByDocumentHash(documentHash);
                if (existingVault.isPresent()) {
                    log.debug("[SEEDER] Identity vault already exists for document hash (skipping)");
                    continue;
                }

                // Build identity blob JSON
                Map<String, String> identityData = new HashMap<>();
                identityData.put("cedula", cedula);
                identityData.put("primerNombre", citizenNode.get("primerNombre").asText());
                identityData.put("primerApellido", citizenNode.get("primerApellido").asText());
                if (citizenNode.has("segundoNombre")) {
                    identityData.put("segundoNombre", citizenNode.get("segundoNombre").asText());
                }
                if (citizenNode.has("segundoApellido")) {
                    identityData.put("segundoApellido", citizenNode.get("segundoApellido").asText());
                }
                identityData.put("email", citizenNode.get("email").asText());
                identityData.put("telefono", citizenNode.get("telefono").asText());
                identityData.put("sexo", citizenNode.get("sexo").asText());

                String identityJson = objectMapper.writeValueAsString(identityData);
                String encryptedBlob = cryptoService.encryptPII(identityJson);

                // Create identity vault
                IdentityVault vault = new IdentityVault();
                vault.setDocumentHash(documentHash);
                vault.setIdentityBlobEncrypted(encryptedBlob);
                vault.setKeyVersion(1);
                vault = identityVaultRepository.save(vault);

                // Create persona
                Persona persona = new Persona();
                persona.setIdentityVaultId(vault.getId());
                persona.setCedulaHash(documentHash);
                persona.setCedulaEncrypted(cryptoService.encryptPII(cedula));
                persona.setPrimerNombreEncrypted(cryptoService.encryptPII(citizenNode.get("primerNombre").asText()));
                persona.setPrimerApellidoEncrypted(cryptoService.encryptPII(citizenNode.get("primerApellido").asText()));
                if (citizenNode.has("segundoNombre")) {
                    persona.setSegundoNombreEncrypted(cryptoService.encryptPII(citizenNode.get("segundoNombre").asText()));
                }
                if (citizenNode.has("segundoApellido")) {
                    persona.setSegundoApellidoEncrypted(cryptoService.encryptPII(citizenNode.get("segundoApellido").asText()));
                }
                persona.setSexo(citizenNode.get("sexo").asText());

                String nombreCompleto = citizenNode.get("primerNombre").asText() + " " +
                        (citizenNode.has("segundoNombre") ? citizenNode.get("segundoNombre").asText() + " " : "") +
                        citizenNode.get("primerApellido").asText() + " " +
                        (citizenNode.has("segundoApellido") ? citizenNode.get("segundoApellido").asText() : "");
                persona.setNombreCompletoHash(cryptoService.hashCedula(nombreCompleto.trim()));

                personaRepository.save(persona);
                count++;
                log.debug("[SEEDER] Created citizen identity (vault_id: {})", vault.getId());

            } catch (Exception e) {
                log.error("[SEEDER] Error seeding citizen", e);
            }
        }

        log.info("[SEEDER] Seeded {} citizens", count);
    }

    private void seedStaff(JsonNode staffNode) {
        log.info("[SEEDER] Seeding staff users...");

        // Seed admin
        if (staffNode.has("admin")) {
            seedStaffUser(staffNode.get("admin"), adminPassword, adminSecret);
        }

        // Seed analysts
        if (staffNode.has("analysts")) {
            for (JsonNode analystNode : staffNode.get("analysts")) {
                seedStaffUser(analystNode, analystPassword, analystSecret);
            }
        }
    }

    private void seedStaffUser(JsonNode staffNode, String password, String secret) {
        try {
            String username = staffNode.get("username").asText();

            // Check if user already exists
            Optional<StaffUser> existing = staffUserRepository.findByUsernameAndEnabledTrue(username);
            if (existing.isPresent()) {
                log.debug("[SEEDER] Staff user already exists: {}", username);
                return;
            }

            String role = staffNode.get("role").asText();
            String cedula = staffNode.get("cedula").asText();
            String email = staffNode.get("email").asText();
            String phone = staffNode.get("phone").asText();

            // Create staff user
            StaffUser staff = new StaffUser();
            staff.setUsername(username);
            staff.setRole(role);
            staff.setEnabled(true);

            // Hash password
            if (password == null || password.isBlank()) {
                log.warn("[SEEDER] No password configured for staff. Using default (CHANGE THIS!)");
                password = "ChangeMe123!";
            }
            staff.setPasswordHash(passwordEncoder.encode(password));

            // Encrypt email and phone
            staff.setEmailEncrypted(cryptoService.encryptPII(email));
            staff.setPhoneEncrypted(cryptoService.encryptPII(phone));

            // Hash cedula for Didit linkage
            staff.setCedulaHashIdx(cryptoService.hashCedula(cedula));

            // Hash secret key (stored as BCrypt for validation)
            if (secret != null && !secret.isBlank()) {
                staff.setMfaSecretEncrypted(passwordEncoder.encode(secret));
            }

            staffUserRepository.save(staff);
            log.info("[SEEDER] Created staff user: {} (role: {})", username, role);

        } catch (Exception e) {
            log.error("[SEEDER] Error seeding staff user", e);
        }
    }
}

# 🔧 SNIPPETS LISTOS PARA PEGAR

## 📦 Corrección Core: Validación HMAC en ApiGatewayFilter

**Archivo a crear/modificar:** `src/main/java/com/vozsegura/vozsegura/config/ApiGatewayFilter.java`

```java
package com.vozsegura.vozsegura.config;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * Filtro para validar requests desde API Gateway (Zero Trust).
 * 
 * SEGURIDAD CRÍTICA:
 * - Valida firma HMAC-SHA256 del Gateway
 * - NO confía en headers X-User-* sin validación
 * - Implementa arquitectura Zero Trust
 */
@Slf4j
@Component
@Order(1)
public class ApiGatewayFilter implements Filter {

    @Value("${vozsegura.gateway.shared-secret}")
    private String sharedSecret;
    
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Rutas públicas (no requieren Gateway)
        String requestUri = httpRequest.getRequestURI();
        if (isPublicRoute(requestUri)) {
            chain.doFilter(request, response);
            return;
        }
        
        // Extraer headers del Gateway
        String cedula = httpRequest.getHeader("X-User-Cedula");
        String userType = httpRequest.getHeader("X-User-Type");
        String gatewaySignature = httpRequest.getHeader("X-Gateway-Signature");
        String timestamp = httpRequest.getHeader("X-Request-Timestamp");
        
        // Validar que todos los headers están presentes
        if (cedula == null || userType == null || gatewaySignature == null || timestamp == null) {
            log.warn("Access denied: missing Gateway headers (uri={})", requestUri);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, 
                                  "Access denied: requests must come through API Gateway");
            return;
        }
        
        // Validar timestamp (no más de 5 minutos de antigüedad)
        try {
            long requestTime = Long.parseLong(timestamp);
            long now = System.currentTimeMillis();
            if (Math.abs(now - requestTime) > 300000) { // 5 minutos
                log.warn("Access denied: timestamp too old (uri={})", requestUri);
                httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, 
                                      "Request timestamp expired");
                return;
            }
        } catch (NumberFormatException e) {
            log.warn("Access denied: invalid timestamp (uri={})", requestUri);
            httpResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid timestamp");
            return;
        }
        
        // Generar firma esperada (mismo algoritmo que Gateway)
        String expectedSignature = generateHmacSignature(
            timestamp,
            httpRequest.getMethod(),
            requestUri,
            cedula,
            userType
        );
        
        // Comparar firmas (timing-attack safe)
        if (!MessageDigest.isEqual(
            expectedSignature.getBytes(StandardCharsets.UTF_8),
            gatewaySignature.getBytes(StandardCharsets.UTF_8)
        )) {
            log.warn("Access denied: invalid Gateway signature (uri={}, user={})", 
                    requestUri, cedula);
            httpResponse.sendError(HttpServletResponse.SC_FORBIDDEN, 
                                  "Invalid gateway signature");
            return;
        }
        
        log.debug("✅ Request validated from Gateway: {} {} (user: {})", 
                 httpRequest.getMethod(), requestUri, cedula);
        
        // Request válido → continuar
        chain.doFilter(request, response);
    }
    
    /**
     * Genera firma HMAC-SHA256 (mismo algoritmo que Gateway).
     */
    private String generateHmacSignature(String timestamp, String method, String path,
                                        String cedula, String userType) {
        try {
            String message = String.join(":", timestamp, method, path, cedula, userType);
            
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec keySpec = new SecretKeySpec(
                sharedSecret.getBytes(StandardCharsets.UTF_8),
                "HmacSHA256"
            );
            mac.init(keySpec);
            
            byte[] hmacBytes = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate HMAC signature", e);
        }
    }
    
    /**
     * Verifica si la ruta es pública (no requiere Gateway).
     */
    private boolean isPublicRoute(String uri) {
        return uri.startsWith("/auth/") ||
               uri.startsWith("/public/") ||
               uri.startsWith("/webhooks/") ||
               uri.startsWith("/actuator/health") ||
               uri.startsWith("/error");
    }
}
```

---

## 📦 Job de Cifrado de PII

**Archivo a crear:** `src/main/java/com/vozsegura/vozsegura/job/PiiEncryptionJob.java`

```java
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
 * 2. Cifra con AES-256-GCM
 * 3. Guarda en columnas *_encrypted
 * 4. Verifica que todos los registros estén cifrados
 * 5. Solo después ejecutar V30 (elimina plaintext)
 */
@Slf4j
@Component
public class PiiEncryptionJob implements CommandLineRunner {

    @Autowired
    private PersonaRepository personaRepository;
    
    @Autowired
    private StaffUserRepository staffUserRepository;
    
    @Autowired
    private DiditVerificationRepository diditVerificationRepository;
    
    @Autowired
    private EncryptionService encryptionService;
    
    @Override
    public void run(String... args) {
        // Solo ejecutar si se pasa argumento --encrypt-pii
        if (args.length > 0 && args[0].equals("--encrypt-pii")) {
            log.info("=== INICIANDO JOB DE CIFRADO DE PII ===");
            try {
                encryptAllPii();
                log.info("=== JOB DE CIFRADO COMPLETADO ===");
            } catch (Exception e) {
                log.error("ERROR CRÍTICO en job de cifrado", e);
                System.exit(1);
            }
        }
    }
    
    @Transactional
    public void encryptAllPii() {
        log.info("1/3 Cifrando personas...");
        encryptPersonas();
        
        log.info("2/3 Cifrando staff users...");
        encryptStaffUsers();
        
        log.info("3/3 Cifrando didit verifications...");
        encryptDiditVerifications();
        
        log.info("✅ Todos los datos PII han sido cifrados");
    }
    
    private void encryptPersonas() {
        List<Persona> personas = personaRepository.findAll();
        int processed = 0;
        int skipped = 0;
        
        for (Persona p : personas) {
            boolean needsEncryption = false;
            
            // Cifrar cedula si no está cifrada
            if (p.getCedula() != null && p.getCedulaEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(p.getCedula());
                p.setCedulaEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar primer nombre
            if (p.getPrimerNombre() != null && p.getPrimerNombreEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(p.getPrimerNombre());
                p.setPrimerNombreEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar segundo nombre
            if (p.getSegundoNombre() != null && p.getSegundoNombreEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(p.getSegundoNombre());
                p.setSegundoNombreEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar primer apellido
            if (p.getPrimerApellido() != null && p.getPrimerApellidoEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(p.getPrimerApellido());
                p.setPrimerApellidoEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar segundo apellido
            if (p.getSegundoApellido() != null && p.getSegundoApellidoEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(p.getSegundoApellido());
                p.setSegundoApellidoEncrypted(encrypted);
                needsEncryption = true;
            }
            
            if (needsEncryption) {
                personaRepository.save(p);
                processed++;
            } else {
                skipped++;
            }
        }
        
        log.info("Personas: {} cifrados, {} ya estaban cifrados", processed, skipped);
    }
    
    private void encryptStaffUsers() {
        List<StaffUser> staff = staffUserRepository.findAll();
        int processed = 0;
        int skipped = 0;
        
        for (StaffUser s : staff) {
            boolean needsEncryption = false;
            
            // Cifrar cedula
            if (s.getCedula() != null && s.getCedulaEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(s.getCedula());
                s.setCedulaEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar email
            if (s.getEmail() != null && s.getEmailEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(s.getEmail());
                s.setEmailEncrypted(encrypted);
                needsEncryption = true;
            }
            
            if (needsEncryption) {
                staffUserRepository.save(s);
                processed++;
            } else {
                skipped++;
            }
        }
        
        log.info("Staff users: {} cifrados, {} ya estaban cifrados", processed, skipped);
    }
    
    private void encryptDiditVerifications() {
        List<DiditVerification> verifications = diditVerificationRepository.findAll();
        int processed = 0;
        int skipped = 0;
        
        for (DiditVerification d : verifications) {
            boolean needsEncryption = false;
            
            // Cifrar document_number
            if (d.getDocumentNumber() != null && d.getDocumentNumberEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(d.getDocumentNumber());
                d.setDocumentNumberEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar full_name
            if (d.getFullName() != null && d.getFullNameEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(d.getFullName());
                d.setFullNameEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar first_name
            if (d.getFirstName() != null && d.getFirstNameEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(d.getFirstName());
                d.setFirstNameEncrypted(encrypted);
                needsEncryption = true;
            }
            
            // Cifrar last_name
            if (d.getLastName() != null && d.getLastNameEncrypted() == null) {
                String encrypted = encryptionService.encryptToBase64(d.getLastName());
                d.setLastNameEncrypted(encrypted);
                needsEncryption = true;
            }
            
            if (needsEncryption) {
                diditVerificationRepository.save(d);
                processed++;
            } else {
                skipped++;
            }
        }
        
        log.info("Didit verifications: {} cifrados, {} ya estaban cifrados", processed, skipped);
    }
}
```

**Ejecución:**
```bash
# 1. Hacer backup de la BD
pg_dump $SUPABASE_DB_URL > backup_before_encryption.sql

# 2. Ejecutar job
mvn spring-boot:run -Dspring-boot.run.arguments="--encrypt-pii"

# 3. Verificar que no hay registros sin cifrar
psql $SUPABASE_DB_URL -c "
SELECT COUNT(*) FROM registro_civil.personas 
WHERE cedula IS NOT NULL AND cedula_encrypted IS NULL;
"

# 4. Si COUNT = 0, habilitar V30
mv src/main/resources/db/migration/V30__drop_plaintext_pii_columns.sql.DISABLED_UNTIL_ENCRYPTION_COMPLETE \
   src/main/resources/db/migration/V30__drop_plaintext_pii_columns.sql

# 5. Ejecutar migración
mvn spring-boot:run
```

---

## 📦 Actualización .env

Agregar al archivo `.env`:

```bash
# ============================================
# ZERO TRUST - Gateway Shared Secret
# ============================================
# CRÍTICO: Debe ser el MISMO valor en Gateway y Core
# Genera con: openssl rand -base64 32
VOZSEGURA_GATEWAY_SHARED_SECRET=<generar_con_openssl>

# ============================================
# JWT Configuration
# ============================================
# CRÍTICO: Mínimo 32 caracteres
JWT_SECRET=<ya_existente>
JWT_EXPIRATION=86400000

# ============================================
# Encryption Key
# ============================================
# Para cifrado AES-256-GCM de PII
VOZSEGURA_DATA_KEY_B64=<ya_existente>
```

**Generar shared-secret:**
```bash
openssl rand -base64 32
```

---

## ✅ VERIFICACIÓN FINAL

### Test 1: Secrets obligatorios

```bash
# Eliminar JWT_SECRET del .env temporalmente
# Iniciar app
mvn spring-boot:run

# Resultado esperado: IllegalStateException en startup
```

### Test 2: Validación HMAC

```bash
# Con Core actualizado (ApiGatewayFilter con validación)
curl -X GET http://localhost:8082/staff/casos \
  -H "X-User-Cedula: 1234567890" \
  -H "X-User-Type: ADMIN" \
  -H "X-Gateway-Signature: firma_falsa"

# Resultado esperado: 403 Forbidden
```

### Test 3: Upload malicioso

```bash
# Renombrar malware.exe a documento.pdf
# Intentar upload

# Resultado esperado: Rechazo con "magic bytes don't match"
```

---

**Estado:** ✅ Todos los snippets listos para implementación  
**Prioridad:** CRÍTICO - Implementar ApiGatewayFilter y job de cifrado


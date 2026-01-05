# 🏗️ Arquitectura del Proyecto - Voz Segura

## 📁 Estructura de Directorios

```
vozSegura/
├── src/
│   ├── main/
│   │   ├── java/com/vozsegura/vozsegura/
│   │   │   ├── client/              # Clientes para servicios externos
│   │   │   ├── config/              # Configuraciones de Spring
│   │   │   ├── controller/          # Controladores MVC
│   │   │   ├── domain/entity/       # Entidades JPA (modelos de BD)
│   │   │   ├── dto/forms/           # Data Transfer Objects
│   │   │   ├── repo/                # Repositorios JPA
│   │   │   ├── security/            # Servicios de seguridad
│   │   │   ├── service/             # Lógica de negocio
│   │   │   └── VozSeguraApplication.java  # Clase principal
│   │   └── resources/
│   │       ├── db/migration/        # Migraciones Flyway (PostgreSQL)
│   │       ├── static/              # Recursos estáticos (CSS, JS, imágenes)
│   │       ├── templates/           # Plantillas Thymeleaf (HTML)
│   │       ├── application.yml      # Configuración principal
│   │       └── application-dev.yml  # Configuración desarrollo
│   └── test/                        # Tests unitarios e integración
├── target/                          # Archivos compilados (generado)
├── pom.xml                          # Dependencias Maven
└── README.md                        # Documentación principal
```

---

## 📦 Descripción de Paquetes

### 1. `client/` - Clientes de Servicios Externos

**Propósito:** Interfaces y implementaciones para comunicación con APIs externas.

#### Interfaces:
- **`CivilRegistryClient`**: Contrato para verificación contra Registro Civil del Ecuador
- **`OtpClient`**: Contrato para envío y validación de códigos OTP
- **`SecretsManagerClient`**: Contrato para obtener secretos de AWS Secrets Manager

#### Implementaciones (`mock/`):
- **`MockCivilRegistryClient`**: Implementación simulada del Registro Civil (desarrollo)
  - `verifyCitizen()`: Retorna `CITIZEN-{cedula}` para cualquier cédula válida
  - `verifyBiometric()`: Siempre retorna `true` (en producción validaría foto real)
  - `getEmailForCitizen()`: Retorna email mock basado en citizenRef

- **`MockOtpClient`**: Implementación simulada de OTP (desarrollo)
  - `sendOtp()`: Simula envío, retorna ID mock
  - `verifyOtp()`: Acepta código "123456" como válido

- **`EnvSecretsManagerClient`**: Implementación que lee de variables de entorno
  - En desarrollo: Lee de Map hardcodeado con valores de prueba
  - En producción: Se reemplaza por `AwsSecretsManagerClient` real

**¿Por qué mocks?** 
- Permite desarrollo sin dependencias externas
- Tests más rápidos y predecibles
- Fácil cambio a servicios reales cambiando el perfil de Spring

---

### 2. `config/` - Configuraciones de Spring

#### `SecurityConfig.java`
**Propósito:** Configuración de Spring Security

**Responsabilidades:**
- Definir qué rutas son públicas (`/auth/**`, `/css/**`)
- Configurar headers de seguridad (CSP, XSS Protection)
- Deshabilitar login form (usamos autenticación unificada)
- Configurar logout

**Características clave:**
```java
// CSRF habilitado por defecto
csrf(Customizer.withDefaults())

// Headers de seguridad
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
Content-Security-Policy: default-src 'self'
```

#### `ApiGatewayFilter.java`
**Propósito:** Filtro que implementa Zero Trust Architecture (ZTA)

**Responsabilidades:**
1. **Interceptar TODAS las peticiones** antes de que lleguen a los controladores
2. **Validar autenticación**: Verificar que exista sesión válida
3. **Verificar autorización**: Control de Acceso Basado en Roles (RBAC)
4. **Aplicar headers de seguridad**: Agregar headers a cada respuesta
5. **Registrar auditoría**: Log de cada acceso (IP, sesión, ruta, resultado)

**Flujo de decisión:**
```
Petición → ¿Es ruta pública? 
           ├─→ SÍ: Permitir
           └─→ NO: ¿Tiene sesión autenticada?
                   ├─→ NO: BLOCKED → /auth/login
                   └─→ SÍ: ¿Tiene permisos para este recurso?
                           ├─→ NO: 403 Forbidden
                           └─→ SÍ: ALLOWED + Log + Agregar headers
```

**Principios ZTA implementados:**
- **Never Trust, Always Verify**: Cada petición se valida
- **Least Privilege**: Solo se otorgan permisos mínimos necesarios
- **Assume Breach**: Sistema defensivo por defecto

---

### 3. `controller/` - Controladores MVC

#### `UnifiedAuthController.java`
**Propósito:** Gestiona el flujo de autenticación unificada

**Endpoints:**
- `GET /auth/login`: Muestra página de login con CAPTCHA
- `POST /auth/unified-login`: Procesa login (verifica CI + CAPTCHA)
- `GET /auth/secret-key`: Pantalla de clave secreta (Staff/Admin)
- `POST /auth/verify-secret`: Valida clave secreta AWS
- `GET /auth/logout`: Cierra sesión

**Flujo:**
```
1. Usuario → GET /auth/login
   → Genera CAPTCHA único
   → Muestra términos y formulario

2. Usuario → POST /auth/unified-login
   → Valida CAPTCHA
   → Verifica contra Registro Civil
   → Determina tipo de usuario (Admin/Analyst/Denunciante)
   → Redirige según rol:
      - Admin/Analyst → /auth/secret-key
      - Denunciante → /denuncia/biometric

3. Staff → POST /auth/verify-secret
   → Valida clave secreta AWS
   → Marca sesión como autenticada
   → Redirige a panel correspondiente
```

#### `publicview/` - Controladores Públicos
- **`PublicComplaintController`**: Gestiona denuncias anónimas
  - Verificación biométrica
  - Formulario de denuncia
  - Envío y cifrado de evidencias

- **`TermsController`**: Muestra términos y condiciones

#### `staff/` - Controladores Staff
- **`StaffComplaintController`**: Gestión de denuncias para analistas
  - Listar denuncias
  - Ver detalles
  - Cambiar estado
  - Derivar casos

#### `admin/` - Controladores Admin
- **`AdminPanelController`**: Panel de administración
  - Gestión de reglas
  - Visualización de logs
  - Configuración del sistema

---

### 4. `domain/entity/` - Entidades JPA

Las entidades representan tablas en PostgreSQL.

#### `StaffUser.java`
**Tabla:** `staff_user`

**Campos:**
- `id`: Primary key (BIGSERIAL)
- `username`: Usuario único
- `cedula`: Cédula del Ecuador (nuevo, vincula con Registro Civil)
- `passwordHash`: Hash BCrypt de la contraseña
- `role`: ADMIN | ANALYST
- `enabled`: boolean (activo/inactivo)

**Propósito:** Usuarios internos del sistema (no denunciantes)

#### `IdentityVault.java`
**Tabla:** `identity_vault`

**Campos:**
- `id`: Primary key
- `citizenHash`: SHA-256 hash de (cédula + código dactilar)
- `createdAt`: Timestamp

**Propósito:** Almacenar hash de identidades SIN guardar datos personales

**¿Por qué?** 
- Permite vincular denuncias con ciudadano sin exponer identidad
- En caso de revelación judicial, se puede buscar por hash

#### `Complaint.java`
**Tabla:** `complaint`

**Campos:**
- `id`: Primary key
- `trackingId`: Código único para seguimiento (40 chars)
- `identityVault`: Foreign key a IdentityVault
- `status`: PENDING | IN_REVIEW | RESOLVED | ARCHIVED
- `severity`: LOW | MEDIUM | HIGH | CRITICAL
- `encryptedText`: Texto cifrado AES-256-GCM
- `companyName`, `companyAddress`, `companyContact`: Datos empresa denunciada
- `companyEmail`, `companyPhone`: Contactos opcionales
- `createdAt`, `updatedAt`: Timestamps

**Propósito:** Denuncia anónima cifrada

**Relaciones:**
- `1-N` con `Evidence` (una denuncia puede tener múltiples evidencias)
- `N-1` con `IdentityVault` (muchas denuncias pueden venir del mismo ciudadano)

#### Otras entidades:
- **`Evidence`**: Evidencias adjuntas (archivos cifrados)
- **`DerivationRule`**: Reglas de derivación automática
- **`AuditEvent`**: Logs de auditoría
- **`TermsAcceptance`**: Registro de aceptación de términos

---

### 5. `dto/forms/` - Data Transfer Objects

Los DTOs transportan datos entre capas sin exponer entidades.

#### `UnifiedLoginForm.java`
**Campos:**
- `cedula`: String (10 dígitos)
- `codigoDactilar`: String
- `captcha`: String (6 caracteres)

**Validaciones:**
- `@NotBlank` en todos los campos
- `@Size(min=10, max=10)` en cédula
- `@Size(min=6, max=6)` en captcha

**Uso:** Formulario de login unificado

#### `SecretKeyForm.java`
**Campos:**
- `secretKey`: String

**Uso:** Formulario de clave secreta AWS (Staff/Admin)

#### `ComplaintForm.java`
**Campos:**
- `detail`: Texto de la denuncia
- `evidences`: MultipartFile[] (archivos adjuntos)
- `companyName`, `companyAddress`, `companyContact`: Datos empresa
- `companyEmail`, `companyPhone`: Opcionales

**Validaciones:**
- `@NotBlank` en campos obligatorios
- `@Email` en companyEmail
- `@Size` en todos los campos

---

### 6. `repo/` - Repositorios JPA

Los repositorios proporcionan acceso a la base de datos.

#### Interfaces Spring Data JPA:
- **`StaffUserRepository`**: 
  - `findByUsernameAndEnabledTrue()`
  - `findByCedulaAndEnabledTrue()` ← Nuevo para auth unificada

- **`ComplaintRepository`**:
  - `findByTrackingId()`
  - `findAll()`
  - `findByStatus()`

- **`IdentityVaultRepository`**:
  - `findByCitizenHash()`

**¿Por qué interfaces?** Spring Data JPA genera la implementación automáticamente.

---

### 7. `security/` - Servicios de Seguridad

#### `AesGcmEncryptionService.java`
**Propósito:** Cifrar/descifrar denuncias y evidencias

**Algoritmo:** AES-256-GCM (Galois/Counter Mode)

**Características:**
- Autenticación: Detecta modificaciones
- Nonce único: Cada cifrado usa IV diferente
- Clave de 256 bits: Máxima seguridad

**Métodos:**
- `encrypt(plainText)`: String → Ciphertext Base64
- `decrypt(cipherText)`: Ciphertext Base64 → String
- `encryptFile(bytes)`: byte[] → byte[] cifrados

---

### 8. `service/` - Lógica de Negocio

#### `UnifiedAuthService.java`
**Propósito:** Orquestar autenticación unificada

**Métodos:**
1. `verifyCitizenIdentity()`: 
   - Valida CAPTCHA
   - Llama a CivilRegistryClient
   - Retorna citizenRef

2. `getUserType()`:
   - Busca cédula en `staff_user`
   - Retorna ADMIN | ANALYST | DENUNCIANTE

3. `validateSecretKey()`:
   - Obtiene clave esperada desde AWS/ENV
   - Compara con input del usuario

**Colaboradores:**
- CaptchaService
- CivilRegistryClient
- SecretsManagerClient
- StaffUserRepository

#### `CaptchaService.java`
**Propósito:** Generar y validar CAPTCHAs

**Características:**
- CAPTCHA de 6 caracteres
- Sin caracteres ambiguos (0, O, I, 1, l)
- SecureRandom para generación
- Un solo uso (se elimina al validar)

#### `ComplaintService.java`
**Propósito:** Gestionar denuncias

**Operaciones:**
- Crear denuncia (cifrado + tracking ID)
- Procesar evidencias
- Cambiar estado
- Derivar casos
- Descifrar para visualización (solo staff autorizado)

---

## 🗄️ Base de Datos - PostgreSQL

### Esquema

```sql
-- Usuarios Staff
staff_user (id, username, cedula, password_hash, role, enabled)

-- Identidades hasheadas
identity_vault (id, citizen_hash, created_at)

-- Denuncias cifradas
complaint (id, tracking_id, identity_vault_id, status, severity, 
           encrypted_text, company_*, created_at, updated_at)

-- Evidencias
evidence (id, complaint_id, file_name, encrypted_data, ...)

-- Reglas de derivación
derivation_rule (id, criteria, target_unit, priority)

-- Auditoría
audit_event (id, actor_role, actor_username, action, ...)

-- Términos
terms_acceptance_log (id, session_id, ip_address, accepted_at, ...)
```

### Migraciones Flyway

**V1__init.sql**: Crea todas las tablas iniciales
**V2__seed_users.sql**: Inserta usuarios de prueba
**V3__add_company_contact_fields.sql**: Agrega email y teléfono
**V4__add_cedula_to_staff.sql**: Agrega campo cédula a staff_user
**V5__create_terms_log.sql**: Crea tabla de términos

---

## 🔄 Flujo Completo de Autenticación

```
┌─────────────────────────────────────────────────────┐
│ 1. Usuario → GET /auth/login                        │
│    ├─→ CaptchaService.generateCaptcha()            │
│    ├─→ Guarda en sesión                             │
│    └─→ Muestra términos + formulario                │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ 2. Usuario → POST /auth/unified-login               │
│    ├─→ CaptchaService.validateCaptcha()            │
│    ├─→ CivilRegistryClient.verifyCitizen()         │
│    ├─→ UnifiedAuthService.getUserType()            │
│    └─→ Redirige según tipo:                         │
│        ├─→ Staff/Admin: /auth/secret-key            │
│        └─→ Denunciante: /denuncia/biometric         │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ 3a. Staff/Admin → POST /auth/verify-secret          │
│     ├─→ SecretsManagerClient.getSecretString()     │
│     ├─→ Valida contra input                         │
│     ├─→ session.setAttribute("authenticated", true) │
│     └─→ Redirige a /admin o /staff/casos            │
└─────────────────────────────────────────────────────┘
                        ↓
┌─────────────────────────────────────────────────────┐
│ 4. Cada Petición → ApiGatewayFilter                 │
│    ├─→ ¿Es pública? → Permitir                      │
│    ├─→ ¿Tiene sesión? → BLOCKED si no               │
│    ├─→ ¿Tiene permisos? → 403 si no                 │
│    ├─→ Agregar headers de seguridad                 │
│    └─→ Log de auditoría                             │
└─────────────────────────────────────────────────────┘
```

---

## 🎨 Capa de Presentación

### Templates Thymeleaf

**`auth/`**
- `secret-key.html`: Pantalla de clave secreta AWS

**`public/`**
- `denuncia-login.html`: Login unificado con términos
- `denuncia-biometric.html`: Verificación biométrica
- `denuncia-form.html`: Formulario de denuncia

**`staff/`**
- `casos-list.html`: Listado de denuncias
- `caso-detalle.html`: Detalle de una denuncia

**`admin/`**
- `panel.html`: Panel principal
- `reglas.html`: Gestión de reglas
- `logs.html`: Visualización de logs

### CSS

**`main.css`** - Estilos únicos con diseño consistente:
- Paleta azul oscuro profesional (#1e3a8a, #2563eb)
- Componentes reutilizables (.vs-button-primary, .vs-card-main)
- Responsive design
- Cumple principios de Nielsen y Leyes de UX

---

## 🔐 Principios de Seguridad

### 1. Defense in Depth (Defensa en Profundidad)
- Múltiples capas de seguridad
- Si una falla, otras protegen

### 2. Zero Trust Architecture
- Nunca confiar, siempre verificar
- Cada petición es validada

### 3. Least Privilege
- Usuarios solo acceden a lo necesario
- Roles granulares (ADMIN vs ANALYST)

### 4. Separation of Duties
- Identity Vault separa identidad de denuncias
- No se puede relacionar denuncia con persona fácilmente

### 5. Audit Trail
- Cada acción queda registrada
- Logs inmutables en `audit_event`

---

## 📊 Métricas de Seguridad

- **Cifrado**: AES-256-GCM (FIPS 140-2 compliant)
- **Hashing**: SHA-256 para identidades, BCrypt para passwords
- **Session Timeout**: 30 minutos
- **CAPTCHA**: 6 caracteres, un solo uso
- **MFA**: 2 factores para Staff (Cédula + Clave AWS)

---

## 🚀 Deployment

### Desarrollo
```bash
mvn spring-boot:run -Dspring.profiles.active=dev
```

### Producción
```bash
java -jar voz-segura.jar \
  --spring.profiles.active=prod \
  --server.port=443 \
  --spring.datasource.url=jdbc:postgresql://prod-db:5432/voz_segura
```

---

**Voz Segura - 2026**  
**Arquitectura Zero Trust | ISO 27001 | GDPR Compliant**


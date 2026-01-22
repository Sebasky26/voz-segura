# VOZ SEGURA - Plataforma de Denuncias Anónimas

## 🚀 Ejecución Local y Despliegue

### Requisitos

- Tener Java 17 y Maven instalados
- Tener el archivo `.env` completo en la raíz del proyecto (no se sube a git)
- PowerShell habilitado (Windows)

### Ejecución local de ambos módulos

1. Abre dos terminales en la raíz del proyecto.
2. En el primer terminal (core):
   ```powershell
   cd "d:\Octavo Semestre\Desarrollo Seguro\Project End\voz-segura"
   .\run-local.ps1
   ```
3. En el segundo terminal (gateway):
   ```powershell
   cd "d:\Octavo Semestre\Desarrollo Seguro\Project End\voz-segura\gateway"
   .\run-local.ps1
   ```

Ambos scripts cargan automáticamente las variables del `.env` y activan el perfil `dev` para desarrollo local.

### Despliegue en producción

- Para Render u otros entornos, define las variables de entorno necesarias en el panel de configuración del servicio.
- El perfil activo se puede sobreescribir con la variable `SPRING_PROFILES_ACTIVE` según el entorno (`prod`, `dev`, etc).

---

**Versión:** 2.0  
**Fecha:** Enero 2026

---

## Descripción del Proyecto

**Voz Segura** es una plataforma gubernamental de denuncias anónimas desarrollada bajo principios de **Zero Trust Architecture**, diseñada para garantizar la máxima seguridad y privacidad de los denunciantes en Ecuador.

### Características Principales

- **Verificación Biométrica con DIDIT v3:** Autenticación facial contra Registro Civil
- **Cifrado de Extremo a Extremo:** AES-256-GCM para denuncias y evidencias
- **Anonimato Total:** Identity Vault separa identidad real de denuncias
- **Zero Trust:** Validación HMAC-SHA256 entre Gateway y Core con anti-replay
- **MFA para Staff:** Autenticación de dos factores con OTP por email (AWS SES)
- **PII Cifrado en BD:** Datos sensibles cifrados automáticamente con AES-256-GCM
- **Auditoría Completa:** Todos los accesos registrados sin exposición de PII
- **Cloud Native:** Supabase PostgreSQL, AWS SES, Cloudflare Turnstile
- **Validación de Archivos:** Magic bytes + whitelist (PDF, DOCX, JPG, PNG)
- **Rate Limiting:** Protección anti-brute-force
- **Headers de Seguridad:** CSP, XSS protection, anti-clickjacking

---

## 🏗️ Arquitectura del Sistema

### Componentes Principales

```
┌─────────────┐
│  USUARIO    │
└──────┬──────┘
       │ HTTPS
       ↓
┌─────────────────────────────────┐
│   GATEWAY (Puerto 8080)         │
│   - Validación JWT              │
│   - Firma HMAC-SHA256           │
│   - Rate Limiting               │
│   - CORS/Security Headers       │
└──────────┬──────────────────────┘
           │ Zero Trust (HMAC)
           ↓
┌─────────────────────────────────┐
│   CORE (Puerto 8082)            │
│   - Validación HMAC             │
│   - Lógica de Negocio           │
│   - Cifrado/Descifrado PII      │
│   - Flyway Migrations (Auto)    │
└──────────┬──────────────────────┘
           │
           ↓
┌─────────────────────────────────┐
│   SUPABASE POSTGRESQL           │
│   - Schemas: registro_civil,    │
│     staff, denuncias,           │
│     evidencias, logs            │
│   - PII Cifrado en Reposo       │
└─────────────────────────────────┘
```

### Zero Trust Architecture

- **Clave compartida** 256-bit entre Gateway y Core
- **Timestamps** con TTL para prevenir replay attacks
- **Comparación constante** en tiempo (anti-timing attack)
- **Rate limiting:** 30 requests/minuto por IP
- **Logs seguros:** Sin exposición de datos sensibles

---

## 💻 Tecnologías Utilizadas

### Backend Core

| Tecnología               | Versión | Propósito                    | Detalles de Implementación              |
| ------------------------ | ------- | ---------------------------- | --------------------------------------- |
| **Java**                 | 21 LTS  | Lenguaje principal           | JDK con soporte hasta 2029              |
| **Spring Boot**          | 3.4.0   | Framework de aplicación      | Auto-configuración, embedded server     |
| **Spring Security**      | 6.x     | Autenticación y autorización | BCrypt, JWT validation, CSRF protection |
| **Spring Cloud Gateway** | 4.x     | API Gateway reactivo         | WebFlux, filtros de autenticación       |
| **Spring Data JPA**      | 3.x     | Persistencia ORM             | Hibernate + PostgreSQL optimizations    |
| **Spring Validation**    | 3.x     | Validación de DTOs           | Jakarta Bean Validation                 |

### Seguridad y Criptografía

| Tecnología      | Versión         | Propósito           | Implementación                                        |
| --------------- | --------------- | ------------------- | ----------------------------------------------------- |
| **JWT (jjwt)**  | 0.12.3          | Tokens de sesión    | HS256, 24h expiración, claims: cedula/userType/apiKey |
| **BCrypt**      | Spring Security | Hash de contraseñas | Strength 10 (2^10 = 1024 rounds)                      |
| **AES-256-GCM** | Java Crypto     | Cifrado de PII      | IV 12 bytes, tag 128 bits, AEAD                       |
| **HMAC-SHA256** | Java Crypto     | Firma Zero Trust    | Gateway-Core validation, TTL 60s                      |
| **SHA-256**     | Java Security   | Hash de identidades | Irreversible, usado para anonimato                    |

### Base de Datos

| Componente              | Propósito               | Configuración                        |
| ----------------------- | ----------------------- | ------------------------------------ |
| **Supabase PostgreSQL** | BD principal            | Versión 17, 6 schemas separados      |
| **Flyway**              | Migraciones automáticas | V1-V32, baseline-on-migrate enabled  |
| **PgBouncer**           | Connection pooling      | Modo transacción, prepareThreshold=0 |
| **HikariCP**            | Pool de conexiones      | Pool size: 3 (dev), 10 (prod)        |

#### Schemas de Base de Datos:

1. **`registro_civil`**: Personas verificadas (PII cifrado)
2. **`staff`**: Usuarios Admin/Analyst (PII cifrado)
3. **`denuncias`**: Denuncias (texto cifrado AES-256-GCM)
4. **`evidencias`**: Archivos adjuntos (cifrados)
5. **`logs`**: Auditoría (sin PII, username hasheado)
6. **`reglas_derivacion`**: Reglas de clasificación automática

### Integraciones Externas

| Servicio                     | Propósito                      | Configuración         | Seguridad                   |
| ---------------------------- | ------------------------------ | --------------------- | --------------------------- |
| **DIDIT API v3**             | Verificación biométrica facial | API Key desde .env    | Webhook HMAC validation     |
| **Registro Civil (Ecuador)** | Validación de identidad        | API REST con OAuth    | Credenciales en AWS SM      |
| **AWS SES**                  | Envío de OTP por email         | Region: us-east-1     | IAM credentials, rate limit |
| **AWS Secrets Manager**      | Gestión de secretos (prod)     | KMS encryption        | IAM Role, cache 2h          |
| **Cloudflare Turnstile**     | CAPTCHA anti-bot               | Site Key + Secret Key | Validación server-side      |

### Frontend y UI

| Tecnología               | Propósito                                 |
| ------------------------ | ----------------------------------------- |
| **Thymeleaf**            | Motor de templates server-side            |
| **CSS Custom**           | Estilos personalizados (main.css)         |
| **JavaScript Vanilla**   | Validaciones client-side (sin frameworks) |
| **Cloudflare Turnstile** | CAPTCHA en formularios públicos           |

### DevOps y Deployment

| Herramienta        | Propósito                                          |
| ------------------ | -------------------------------------------------- |
| **Maven**          | Gestión de dependencias y build                    |
| **Docker**         | Containerización (Dockerfile + docker-compose.yml) |
| **GitHub Actions** | CI/CD (opcional)                                   |
| **AWS EC2**        | Hosting de producción (recomendado)                |

### Observabilidad y Monitoreo

| Componente          | Propósito                 | Configuración                     |
| ------------------- | ------------------------- | --------------------------------- |
| **Logback**         | Logging framework         | Configurado en logback-spring.xml |
| **SLF4J + Lombok**  | Logging API               | `@Slf4j` annotation en clases     |
| **Spring Actuator** | Health checks             | `/actuator/health` endpoint       |
| **AWS CloudWatch**  | Logs centralizados (prod) | Logs exportados desde EC2         |

---

## 📊 Esquemas de Base de Datos

### 1. `registro_civil` - Identidades

- **`personas`**: Ciudadanos verificados (PII cifrado con AES-256-GCM)
- **`didit_verification`**: Registros de verificación biométrica

### 2. `staff` - Personal del Sistema

- **`staff_user`**: Usuarios Admin/Analista (PII cifrado)

### 3. `denuncias` - Denuncias

- **`denuncia`**: Denuncias con texto cifrado
- **`complaint_status_log`**: Historial de cambios

### 4. `evidencias` - Archivos Adjuntos

- **`evidencia`**: Archivos PDF/DOCX/IMG cifrados

### 5. `logs` - Auditoría

- **`evento_auditoria`**: Registro de todas las acciones (sin PII)

### 6. `reglas_derivacion` - Configuración

- **`derivation_rule`**: Reglas de derivación automática
- **`destination_entity`**: Entidades destino
- **`configuracion`**: Configuración del sistema

---

## 🚀 Instalación y Configuración

### Requisitos Previos

- **Java 21 JDK**
- **Maven 3.8+**
- Cuenta **Supabase** (PostgreSQL)
- Cuenta **AWS** (SES)
- Cuenta **Cloudflare** (Turnstile)
- Cuenta **DIDIT** (Verificación biométrica)

### 1. Clonar Repositorio

```bash
git clone https://github.com/tu-org/voz-segura.git
cd voz-segura
```

### 2. Configurar Variables de Entorno

```bash
# Copiar plantilla
cp .env.example .env

# Editar .env con tus credenciales
```

**Variables Obligatorias en `.env`:**

```bash
# === SUPABASE (PostgreSQL) ===
SUPABASE_DB_URL=jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0
SUPABASE_DB_USERNAME=postgres.tu-proyecto-id
SUPABASE_DB_PASSWORD=tu-password
SUPABASE_PROJECT_URL=https://tu-proyecto.supabase.co
SUPABASE_ANON_KEY=tu-anon-key
SUPABASE_SERVICE_ROLE_KEY=tu-service-role-key

# === SEGURIDAD (Generar con: openssl rand -base64 32) ===
JWT_SECRET=tu-jwt-secret-base64
JWT_EXPIRATION=86400000
VOZSEGURA_DATA_KEY_B64=tu-encryption-key-base64

# === AWS SES ===
AWS_SES_FROM_EMAIL=noreply@tudominio.com
AWS_ACCESS_KEY_ID=tu-access-key
AWS_SECRET_ACCESS_KEY=tu-secret-key
AWS_REGION=us-east-1

# === CLOUDFLARE TURNSTILE ===
CLOUDFLARE_SITE_KEY=tu-site-key
CLOUDFLARE_SECRET_KEY=tu-secret-key

# === DIDIT (Biometría) ===
DIDIT_APP_ID=tu-app-id
DIDIT_API_KEY=tu-api-key
DIDIT_WEBHOOK_URL=https://tu-dominio.com/webhooks/didit
DIDIT_WEBHOOK_SECRET_KEY=tu-webhook-secret
DIDIT_WORKFLOW_ID=tu-workflow-id
DIDIT_API_URL=https://verification.didit.me
```

### 3. Configurar Base de Datos

1. Crear proyecto en [Supabase](https://supabase.com)
2. Obtener credenciales de conexión (usar **Pooler** para mejor rendimiento)
3. Las migraciones Flyway se ejecutan **automáticamente** al iniciar la aplicación

**Migraciones automáticas:**

- V1 a V27: Estructura de BD
- V28: Agregar columnas PII cifradas
- V29: Migración de datos existentes (si hay)
- V30: Eliminar columnas texto plano
- V31-V32: Optimizaciones y limpieza

⚠️ **IMPORTANTE:** Las migraciones se ejecutan automáticamente. NO se requiere intervención manual.

### 4. Compilar Proyecto

```bash
./mvnw clean install
```

---

## ▶️ Ejecutar la Aplicación

### Modo Desarrollo (Local)

#### Opción 1: Ejecutar ambos servicios

```bash
# Terminal 1: Core Service (Puerto 8082)
./mvnw spring-boot:run

# Terminal 2: Gateway (Puerto 8080)
cd gateway
../mvnw spring-boot:run
```

---

## 🔧 Comandos Útiles

### Maven

```bash
# Compilar
./mvnw clean compile

# Ejecutar tests
./mvnw test

# Package
./mvnw package

# Limpiar y compilar
./mvnw clean install
```

---

## 🐛 Troubleshooting

### Error: "JWT_SECRET not found"

```bash
# Solución: Agregar a .env
JWT_SECRET=$(openssl rand -base64 32)
```

### Error: "Database connection failed"

```bash
# Verificar credenciales Supabase
echo $SUPABASE_DB_URL

# Probar conexión
psql "$SUPABASE_DB_URL" -U "$SUPABASE_DB_USERNAME"
```

### Error: "Invalid gateway signature"

```bash
# El Core solo acepta peticiones del Gateway
# Accede a http://localhost:8080 (NO a :8082)
```

### Error en migraciones Flyway

```bash
# Las migraciones son automáticas
# Si falla, revisar logs en logs/core-dev.log
tail -f logs/core-dev.log
```

---

## 📋 Flujos del Sistema

### Flujo de Denuncia

1. Usuario accede a `/denuncia`
2. Verificación biométrica DIDIT
3. Validación contra Registro Civil
4. Aceptación de términos y condiciones
5. Formulario de denuncia (máx 4000 caracteres)
6. Upload de evidencias (PDF/DOCX/JPG/PNG, máx 25MB)
7. **Cifrado automático** de texto y archivos
8. Generación de tracking ID (UUID)
9. Almacenamiento en `denuncias.denuncia`
10. Retorno de código de seguimiento

### Flujo de Análisis (Staff)

1. Login con biometría + clave secreta + OTP
2. Lista de casos en estado PENDING
3. Visualización de caso (**descifrado automático**)
4. Clasificación (tipo, prioridad, severidad)
5. Derivación automática según reglas
6. Actualización de estado
7. Registro en auditoría (sin PII)

---

## 🔒 Seguridad

### 🛡️ Arquitectura Zero Trust Implementada

#### 1. **API Gateway (Puerto 8080)**

**Responsabilidades:**

- Validación de JWT (firma HS256, expiración 24h)
- Generación de firma HMAC-SHA256 para peticiones al Core
- Rate limiting (30 req/min por IP)
- CORS y headers de seguridad

**Clase Principal:** `JwtAuthenticationGatewayFilterFactory`

- Extrae claims del JWT (cedula, userType, apiKey)
- Genera timestamp + HMAC signature
- Agrega headers: `X-User-Cedula`, `X-User-Type`, `X-Gateway-Signature`, `X-Request-Timestamp`

```java
// Generación de firma HMAC
String message = timestamp + ":" + method + ":" + path + ":" + cedula + ":" + userType;
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(sharedSecret.getBytes(), "HmacSHA256"));
String signature = Base64.encode(mac.doFinal(message.getBytes()));
```

#### 2. **Core Service (Puerto 8082)**

**Responsabilidades:**

- Validación de firma HMAC del Gateway (Zero Trust)
- Anti-replay: TTL 60 segundos en timestamp
- Cifrado/descifrado de PII con AES-256-GCM
- Lógica de negocio y persistencia

**Clase Principal:** `ZeroTrustGatewayFilter` + `GatewayRequestValidator`

- Valida firma HMAC contra clave compartida
- Compara con timing-attack safe (`MessageDigest.isEqual`)
- Rechaza peticiones directas al Core (sin pasar por Gateway)

```java
// Validación Zero Trust
String expectedSignature = generateHmacSignature(timestamp, method, path, cedula, userType);
if (!MessageDigest.isEqual(
    expectedSignature.getBytes(),
    gatewaySignature.getBytes())) {
    response.sendError(403, "Invalid gateway signature");
}
```

#### 3. **Cifrado de Datos (AES-256-GCM)**

**Clase Principal:** `AesGcmEncryptionService`

- **Algoritmo:** AES-256-GCM (AEAD - Authenticated Encryption with Associated Data)
- **IV:** 12 bytes aleatorios por operación (`SecureRandom`)
- **Tag:** 128 bits de autenticación (detecta manipulación)
- **Clave:** 256 bits desde AWS Secrets Manager o variable de entorno

**Flujo de Cifrado:**

```
Texto Plain → IV Aleatorio → AES-GCM → Tag Auth → Base64 → BD
                 (12 bytes)   (256-bit)  (128 bits)
```

**Implementación:**

```java
// Cifrado
byte[] iv = new byte[12];
secureRandom.nextBytes(iv);  // IV aleatorio
Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
GCMParameterSpec spec = new GCMParameterSpec(128, iv);
cipher.init(Cipher.ENCRYPT_MODE, key, spec);
byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
return Base64.encode(IV + ciphertext + tag);
```

**Datos Cifrados:**

- Texto completo de denuncias
- Archivos adjuntos (evidencias)
- PII en columnas `*_encrypted` de BD: nombres, emails, cédulas
- Notas de analistas (opcional)

#### 4. **Validación de Archivos**

**Clase Principal:** `FileValidationService`

- **Whitelist MIME types:** PDF, JPEG, PNG, DOCX, MP4
- **Validación de magic bytes** (firma real del archivo, no spoofeable)
- **Path traversal bloqueado:** `..`, `/`, `\`
- **Tamaño máximo:** 25 MB por archivo

**Magic Bytes Validados:**
| Formato | Magic Bytes | Offset |
|---------|-------------|--------|
| PDF | `%PDF` (0x25504446) | 0 |
| JPEG | `FFD8FF` | 0 |
| PNG | `89504E47` | 0 |
| DOCX | `PK` (0x504B) | 0 |
| MP4 | `ftyp` | 4-7 |

```java
// Validación exhaustiva
boolean isValidEvidence(MultipartFile file) {
    return isValidSize(file) &&           // Max 25MB
           isAllowedMimeType(file) &&     // Whitelist MIME
           isAllowedFileName(file) &&     // Path traversal blocked
           isValidMagicBytes(file);       // Firma real del archivo
}
```

#### 5. **Auditoría Sin PII**

**Clase Principal:** `AuditService`

- Username hasheado con SHA-256 (8 caracteres): `USR-Xy7kP0Qz`
- Sin cédulas, tokens, contraseñas en logs
- Timestamp con timezone offset (UTC)
- Detalles truncados a 500 caracteres

**Eventos Auditados:**

- `LOGIN`: Acceso al sistema
- `LOGOUT`: Cierre de sesión
- `CREATE`: Creación de denuncia/usuario
- `UPDATE`: Actualización de estado/clasificación
- `DELETE`: Eliminación (soft-delete)
- `ACCESS`: Acceso a recurso (visualización)
- `REVEAL`: Solicitud de revelación de identidad
- `ERROR`: Error del sistema

---

**Última actualización:** Enero 21, 2026  
**Versión:** 2.0

# VOZ SEGURA - Plataforma de Denuncias Anónimas

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
| Tecnología | Versión | Propósito | Detalles de Implementación |
|------------|---------|-----------|----------------------------|
| **Java** | 21 LTS | Lenguaje principal | JDK con soporte hasta 2029 |
| **Spring Boot** | 3.4.0 | Framework de aplicación | Auto-configuración, embedded server |
| **Spring Security** | 6.x | Autenticación y autorización | BCrypt, JWT validation, CSRF protection |
| **Spring Cloud Gateway** | 4.x | API Gateway reactivo | WebFlux, filtros de autenticación |
| **Spring Data JPA** | 3.x | Persistencia ORM | Hibernate + PostgreSQL optimizations |
| **Spring Validation** | 3.x | Validación de DTOs | Jakarta Bean Validation |

### Seguridad y Criptografía
| Tecnología | Versión | Propósito | Implementación |
|------------|---------|-----------|----------------|
| **JWT (jjwt)** | 0.12.3 | Tokens de sesión | HS256, 24h expiración, claims: cedula/userType/apiKey |
| **BCrypt** | Spring Security | Hash de contraseñas | Strength 10 (2^10 = 1024 rounds) |
| **AES-256-GCM** | Java Crypto | Cifrado de PII | IV 12 bytes, tag 128 bits, AEAD |
| **HMAC-SHA256** | Java Crypto | Firma Zero Trust | Gateway-Core validation, TTL 60s |
| **SHA-256** | Java Security | Hash de identidades | Irreversible, usado para anonimato |

### Base de Datos
| Componente | Propósito | Configuración |
|------------|-----------|---------------|
| **Supabase PostgreSQL** | BD principal | Versión 17, 6 schemas separados |
| **Flyway** | Migraciones automáticas | V1-V32, baseline-on-migrate enabled |
| **PgBouncer** | Connection pooling | Modo transacción, prepareThreshold=0 |
| **HikariCP** | Pool de conexiones | Pool size: 3 (dev), 10 (prod) |

#### Schemas de Base de Datos:
1. **`registro_civil`**: Personas verificadas (PII cifrado)
2. **`staff`**: Usuarios Admin/Analyst (PII cifrado)
3. **`denuncias`**: Denuncias (texto cifrado AES-256-GCM)
4. **`evidencias`**: Archivos adjuntos (cifrados)
5. **`logs`**: Auditoría (sin PII, username hasheado)
6. **`reglas_derivacion`**: Reglas de clasificación automática

### Integraciones Externas
| Servicio | Propósito | Configuración | Seguridad |
|----------|-----------|---------------|-----------|
| **DIDIT API v3** | Verificación biométrica facial | API Key desde .env | Webhook HMAC validation |
| **Registro Civil (Ecuador)** | Validación de identidad | API REST con OAuth | Credenciales en AWS SM |
| **AWS SES** | Envío de OTP por email | Region: us-east-1 | IAM credentials, rate limit |
| **AWS Secrets Manager** | Gestión de secretos (prod) | KMS encryption | IAM Role, cache 2h |
| **Cloudflare Turnstile** | CAPTCHA anti-bot | Site Key + Secret Key | Validación server-side |

### Frontend y UI
| Tecnología | Propósito |
|------------|-----------|
| **Thymeleaf** | Motor de templates server-side |
| **CSS Custom** | Estilos personalizados (main.css) |
| **JavaScript Vanilla** | Validaciones client-side (sin frameworks) |
| **Cloudflare Turnstile** | CAPTCHA en formularios públicos |

### DevOps y Deployment
| Herramienta | Propósito |
|-------------|-----------|
| **Maven** | Gestión de dependencias y build |
| **Docker** | Containerización (Dockerfile + docker-compose.yml) |
| **GitHub Actions** | CI/CD (opcional) |
| **AWS EC2** | Hosting de producción (recomendado) |

### Observabilidad y Monitoreo
| Componente | Propósito | Configuración |
|------------|-----------|---------------|
| **Logback** | Logging framework | Configurado en logback-spring.xml |
| **SLF4J + Lombok** | Logging API | `@Slf4j` annotation en clases |
| **Spring Actuator** | Health checks | `/actuator/health` endpoint |
| **AWS CloudWatch** | Logs centralizados (prod) | Logs exportados desde EC2 |

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

**⚠️ IMPORTANTE: Desarrollo Local con Webhooks de Didit**

Si vas a desarrollar localmente (localhost), los **webhooks de Didit NO funcionarán** porque Didit no puede enviar solicitudes HTTP a tu máquina local. Tienes dos opciones:

**Opción 1: Usar ngrok (Recomendado para desarrollo)**
```bash
# 1. Instalar ngrok: https://ngrok.com/download

# 2. Ejecutar el script incluido
.\start-ngrok.bat  # Windows
# O manualmente:
ngrok http 8082

# 3. Copiar la URL pública (ej: https://abc123.ngrok.io)

# 4. Actualizar .env
DIDIT_WEBHOOK_URL=https://abc123.ngrok.io/webhooks/didit
```

**Opción 2: La aplicación usará fallback automático**

El código incluye fallback que consulta la API de Didit directamente si el webhook no llega. Sin embargo, esto puede fallar si la sesión expira.

📖 **Para más detalles, consulta**: [`WEBHOOK_DEVELOPMENT_GUIDE.md`](WEBHOOK_DEVELOPMENT_GUIDE.md)

---

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
VOZSEGURA_GATEWAY_SHARED_SECRET=tu-shared-secret-base64

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

---

## ⚙️ Variables de Entorno - Documentación Completa

### 📂 Categorías de Variables

#### 1. **Base de Datos (Supabase PostgreSQL)**

| Variable | Descripción | Ejemplo | Obligatoria |
|----------|-------------|---------|-------------|
| `SUPABASE_DB_URL` | JDBC URL de conexión con SSL | `jdbc:postgresql://...pooler.supabase.com:6543/postgres?sslmode=require&prepareThreshold=0` | ✅ Sí |
| `SUPABASE_DB_USERNAME` | Usuario de BD (formato: postgres.proyecto) | `postgres.abcdefghijk` | ✅ Sí |
| `SUPABASE_DB_PASSWORD` | Contraseña de BD | `********` | ✅ Sí |
| `SUPABASE_PROJECT_URL` | URL base del proyecto Supabase | `https://abcdefghijk.supabase.co` | ✅ Sí |
| `SUPABASE_ANON_KEY` | API Key anónima (para operaciones públicas) | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` | ✅ Sí |
| `SUPABASE_SERVICE_ROLE_KEY` | API Key con privilegios admin | `eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...` | ✅ Sí |

**Notas:**
- Usar **Pooler URL** (puerto 6543) en lugar de conexión directa para mejor rendimiento
- `prepareThreshold=0` es necesario con PgBouncer en modo transacción
- `sslmode=require` obliga a conexiones cifradas

#### 2. **Seguridad y Cifrado**

| Variable | Descripción | Cómo Generar | Obligatoria |
|----------|-------------|--------------|-------------|
| `JWT_SECRET` | Clave secreta para firmar tokens JWT (HS256) | `openssl rand -base64 32` | ✅ Sí |
| `JWT_EXPIRATION` | Tiempo de expiración del JWT en milisegundos | `86400000` (24 horas) | ⚪ No (default: 24h) |
| `VOZSEGURA_DATA_KEY_B64` | Clave AES-256 para cifrado de PII | `openssl rand -base64 32` | ✅ Sí |
| `VOZSEGURA_GATEWAY_SHARED_SECRET` | Clave compartida Gateway↔Core (Zero Trust) | `openssl rand -base64 32` | ✅ Sí |

**Comandos de Generación:**
```bash
# JWT Secret (256 bits)
export JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET"

# AES-256 Encryption Key (256 bits)
export VOZSEGURA_DATA_KEY_B64=$(openssl rand -base64 32)
echo "VOZSEGURA_DATA_KEY_B64=$VOZSEGURA_DATA_KEY_B64"

# Gateway Shared Secret (256 bits) - DEBE SER LA MISMA en Gateway y Core
export VOZSEGURA_GATEWAY_SHARED_SECRET=$(openssl rand -base64 32)
echo "VOZSEGURA_GATEWAY_SHARED_SECRET=$VOZSEGURA_GATEWAY_SHARED_SECRET"
```

**⚠️ CRÍTICO:**
- `VOZSEGURA_GATEWAY_SHARED_SECRET` **DEBE ser la misma** en Gateway (8080) y Core (8082)
- Si no coinciden, todas las peticiones serán rechazadas con HTTP 403
- NUNCA commitear estas claves en el repositorio

#### 3. **AWS SES (Email OTP)**

| Variable | Descripción | Dónde Obtener | Obligatoria |
|----------|-------------|---------------|-------------|
| `AWS_SES_FROM_EMAIL` | Email verificado para enviar OTP | AWS SES Console → Verified Identities | ✅ Sí |
| `AWS_ACCESS_KEY_ID` | AWS Access Key ID | AWS IAM Console → Users → Security Credentials | ✅ Sí |
| `AWS_SECRET_ACCESS_KEY` | AWS Secret Access Key | AWS IAM Console (solo visible al crear) | ✅ Sí |
| `AWS_REGION` | Región de AWS SES | `us-east-1` (recomendado) | ✅ Sí |

**Permisos IAM Requeridos:**
```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "ses:SendEmail",
        "ses:SendRawEmail"
      ],
      "Resource": "*"
    }
  ]
}
```

**Verificar email en AWS SES:**
1. Ir a AWS Console → SES → Verified Identities
2. Click "Create Identity" → Email Address
3. Ingresar `noreply@tudominio.com`
4. Verificar email recibido

#### 4. **Cloudflare Turnstile (CAPTCHA)**

| Variable | Descripción | Dónde Obtener | Obligatoria |
|----------|-------------|---------------|-------------|
| `CLOUDFLARE_SITE_KEY` | Site Key pública (usada en frontend) | Cloudflare Dashboard → Turnstile | ✅ Sí |
| `CLOUDFLARE_SECRET_KEY` | Secret Key para validación server-side | Cloudflare Dashboard → Turnstile | ✅ Sí |

**Configuración en Cloudflare:**
1. Ir a [Cloudflare Dashboard](https://dash.cloudflare.com/)
2. Turnstile → Add Site
3. Configurar dominios permitidos: `vozsegura.gob.ec`, `localhost` (dev)
4. Copiar Site Key y Secret Key

#### 5. **DIDIT API (Verificación Biométrica)**

| Variable | Descripción | Dónde Obtener | Obligatoria |
|----------|-------------|---------------|-------------|
| `DIDIT_APP_ID` | ID de la aplicación DIDIT | DIDIT Dashboard → Applications | ✅ Sí |
| `DIDIT_API_KEY` | API Key para crear sesiones | DIDIT Dashboard → API Keys | ✅ Sí |
| `DIDIT_WEBHOOK_URL` | URL de callback para resultados | `https://tudominio.com/webhooks/didit` | ✅ Sí |
| `DIDIT_WEBHOOK_SECRET_KEY` | Secret para validar firma HMAC de webhooks | DIDIT Dashboard → Webhooks | ✅ Sí |
| `DIDIT_WORKFLOW_ID` | ID del workflow (documento + biometría) | DIDIT Dashboard → Workflows | ✅ Sí |
| `DIDIT_API_URL` | URL base de DIDIT API | `https://verification.didit.me` | ⚪ No (default) |

**Configuración en DIDIT:**
1. Crear cuenta en [DIDIT](https://www.didit.me)
2. Crear aplicación → Seleccionar workflow (Document + Selfie)
3. Configurar webhook URL: `https://vozsegura.gob.ec/webhooks/didit`
4. Guardar Webhook Secret para validación HMAC

#### 6. **Aplicación (Opcional)**

| Variable | Descripción | Default | Obligatoria |
|----------|-------------|---------|-------------|
| `SERVER_PORT` | Puerto del Core | `8082` | ⚪ No |
| `GATEWAY_PORT` | Puerto del Gateway | `8080` | ⚪ No |
| `SPRING_PROFILES_ACTIVE` | Perfil de Spring | `dev` | ⚪ No |
| `SESSION_TIMEOUT` | Timeout de sesión HTTP (segundos) | `1800` (30 min) | ⚪ No |
| `MAX_FILE_SIZE` | Tamaño máximo de archivo upload | `25MB` | ⚪ No |

### 🔐 Generar Todas las Claves (Script Completo)

```bash
#!/bin/bash
# Generar variables de seguridad para Voz Segura

echo "=== GENERANDO CLAVES DE SEGURIDAD ==="
echo ""

# JWT Secret
JWT_SECRET=$(openssl rand -base64 32)
echo "JWT_SECRET=$JWT_SECRET"
echo ""

# AES-256 Encryption Key
VOZSEGURA_DATA_KEY_B64=$(openssl rand -base64 32)
echo "VOZSEGURA_DATA_KEY_B64=$VOZSEGURA_DATA_KEY_B64"
echo ""

# Gateway Shared Secret (MISMA en Gateway y Core)
VOZSEGURA_GATEWAY_SHARED_SECRET=$(openssl rand -base64 32)
echo "VOZSEGURA_GATEWAY_SHARED_SECRET=$VOZSEGURA_GATEWAY_SHARED_SECRET"
echo ""

echo "⚠️  IMPORTANTE:"
echo "1. Agregar estas variables al archivo .env"
echo "2. NUNCA commitear el archivo .env"
echo "3. VOZSEGURA_GATEWAY_SHARED_SECRET debe ser LA MISMA en Gateway y Core"
echo "4. En producción, usar AWS Secrets Manager para estas claves"
```

Guardar como `generate-keys.sh` y ejecutar:
```bash
chmod +x generate-keys.sh
./generate-keys.sh >> .env
```

### 🚨 Seguridad de Variables

**NUNCA hacer esto:**
```bash
# ❌ MAL: Hardcodear en código
String jwtSecret = "mi-clave-secreta-123";

# ❌ MAL: Commitear .env al repositorio
git add .env

# ❌ MAL: Compartir claves por email/Slack
```

**✅ Hacer esto:**
```bash
# ✅ BIEN: Leer desde variable de entorno
String jwtSecret = System.getenv("JWT_SECRET");

# ✅ BIEN: .env en .gitignore
echo ".env" >> .gitignore

# ✅ BIEN: AWS Secrets Manager en producción
@Value("${jwt.secret}")
private String jwtSecret;
```

---

## 📚 Documentación de APIs Internas

### Endpoints Públicos (Sin Autenticación)

#### 1. Health Check
**GET** `/health/config`

**Descripción:** Verificar configuración de DIDIT y estado del sistema

**Response (200 OK):**
```json
{
  "didit": {
    "appId": "app_1234567...",
    "apiKeySet": true,
    "workflowId": "wf_abcdefg...",
    "apiUrl": "https://verification.didit.me",
    "webhookUrl": "https://vozsegura.gob.ec/webhooks/didit"
  },
  "status": "OK"
}
```

---

#### 2. Crear Denuncia - Paso 1: Verificación Biométrica
**GET** `/denuncia`

**Descripción:** Punto de entrada para denuncias públicas. Redirige a verificación DIDIT.

**Response:** Redirect a `/verification/inicio`

---

#### 3. Inicio de Verificación Biométrica
**POST** `/verification/inicio`

**Request Body (form-data):**
```
cedula: 1234567890
turnstileToken: 0.ABC123XYZ...
```

**Response (200 OK):**
```json
{
  "success": true,
  "diditSessionUrl": "https://didit.me/session/abc123",
  "qrCodeUrl": "https://api.didit.me/qr/abc123",
  "sessionId": "session_xyz789",
  "message": "Escanea el código QR con tu móvil"
}
```

**Errores:**
- `400 Bad Request`: CAPTCHA inválido o cédula mal formada
- `429 Too Many Requests`: Límite de intentos excedido (rate limiting)
- `500 Internal Server Error`: Error al crear sesión DIDIT

---

#### 4. Verificar Estado de Sesión DIDIT
**GET** `/verification/status?sessionId={sessionId}`

**Query Parameters:**
- `sessionId`: ID de sesión DIDIT retornado en paso 1

**Response (200 OK - Pendiente):**
```json
{
  "status": "PENDING",
  "message": "Esperando verificación biométrica"
}
```

**Response (200 OK - Aprobado):**
```json
{
  "status": "APPROVED",
  "citizenHash": "sha256_hash_abc...",
  "message": "Verificación completada",
  "nextStep": "/denuncia/formulario"
}
```

**Response (200 OK - Rechazado):**
```json
{
  "status": "REJECTED",
  "message": "Verificación biométrica fallida. Intenta nuevamente."
}
```

---

#### 5. Formulario de Denuncia
**GET** `/denuncia/formulario`

**Descripción:** Muestra formulario HTML para crear denuncia (requiere verificación biométrica previa)

**Validaciones:**
- Session debe contener `citizenHash` (generado en verificación)
- Si no hay hash → Redirect a `/verification/inicio`

---

#### 6. Crear Denuncia - Paso 2: Envío de Formulario
**POST** `/denuncia/submit`

**Request Body (multipart/form-data):**
```
tipoDelito: CORRUPCION
descripcion: Texto de la denuncia (max 4000 caracteres)
evidencia: [File] (opcional, max 25MB, tipos: PDF|DOCX|JPG|PNG|MP4)
```

**Headers:**
- `Cookie: JSESSIONID=...` (con citizenHash en session)

**Response (200 OK):**
```json
{
  "success": true,
  "trackingId": "DEN-2026-ABC123XYZ",
  "message": "Denuncia creada exitosamente. Guarda tu código de seguimiento.",
  "tracking_url": "/seguimiento?code=DEN-2026-ABC123XYZ"
}
```

**Errores:**
- `400 Bad Request`: Campos faltantes o archivo inválido
- `401 Unauthorized`: Sesión sin verificación biométrica
- `413 Payload Too Large`: Archivo > 25 MB
- `415 Unsupported Media Type`: Tipo de archivo no permitido

**Validaciones de archivo:**
- MIME types permitidos: `application/pdf`, `image/jpeg`, `image/png`, `application/vnd.openxmlformats-officedocument.wordprocessingml.document`, `video/mp4`
- Validación de magic bytes (firma real del archivo)
- Path traversal bloqueado (`..`, `/`, `\`)

---

#### 7. Seguimiento de Denuncia
**GET** `/seguimiento?code={trackingId}`

**Query Parameters:**
- `code`: Tracking ID de la denuncia (ej: `DEN-2026-ABC123XYZ`)

**Response (200 OK):**
```html
<!-- Página HTML con estado de la denuncia -->
<div class="status">
  <h2>Estado de tu denuncia</h2>
  <p>Tracking ID: DEN-2026-ABC123XYZ</p>
  <p>Estado: EN_REVISION</p>
  <p>Fecha: 2026-01-22 14:30:00</p>
  <p>Entidad asignada: Fiscalía General del Estado</p>
</div>
```

**Errores:**
- `404 Not Found`: Tracking ID no existe

---

### Endpoints de Autenticación (Staff/Admin)

#### 8. Login - Paso 1: Verificación Biométrica
**POST** `/auth/unified-login`

**Request Body (form-data):**
```
cedula: 1234567890
codigoDactilar: 123456
turnstileToken: 0.ABC123XYZ...
```

**Response (200 OK):**
```json
{
  "success": true,
  "step": "PASSWORD_REQUIRED",
  "userType": "ADMIN",
  "message": "Verificación biométrica exitosa. Ingresa tu clave secreta."
}
```

**Errores:**
- `400 Bad Request`: CAPTCHA inválido
- `401 Unauthorized`: Cédula no encontrada en BD de staff
- `403 Forbidden`: Usuario deshabilitado

---

#### 9. Login - Paso 2: Validación de Contraseña
**POST** `/auth/verify-secret`

**Request Body (form-data):**
```
cedula: 1234567890
secretKey: VozSegura2026Admin!
```

**Response (200 OK):**
```json
{
  "success": true,
  "step": "OTP_REQUIRED",
  "message": "Clave correcta. Código OTP enviado a tu email."
}
```

**Errores:**
- `401 Unauthorized`: Contraseña incorrecta (registra intento fallido)
- `423 Locked`: Cuenta bloqueada tras 3 intentos fallidos

---

#### 10. Login - Paso 3: Verificación OTP
**POST** `/auth/verify-otp`

**Request Body (form-data):**
```
cedula: 1234567890
otpCode: 123456
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Login exitoso",
  "userType": "ADMIN",
  "redirectUrl": "/admin"
}
```

**Response Headers:**
```
Set-Cookie: JSESSIONID=ABC123XYZ; HttpOnly; Secure; SameSite=Strict
```

**Errores:**
- `401 Unauthorized`: Código OTP incorrecto
- `410 Gone`: Código OTP expirado (TTL: 5 minutos)
- `429 Too Many Requests`: Máximo 3 intentos de OTP

---

### Endpoints de Staff (Requiere Auth + Role ANALYST)

#### 11. Listar Casos
**GET** `/staff/casos`

**Query Parameters (opcionales):**
```
estado: PENDING|IN_REVIEW|ASSIGNED|COMPLETED
tipo: CORRUPCION|ACOSO|DISCRIMINACION|...
prioridad: LOW|MEDIUM|HIGH|CRITICAL
page: 0
size: 20
```

**Headers:**
```
Cookie: JSESSIONID=...
```

**Response (200 OK):**
```html
<!-- Página HTML con tabla de casos -->
```

---

#### 12. Ver Detalle de Caso
**GET** `/staff/casos/{trackingId}`

**Path Parameters:**
- `trackingId`: ID de la denuncia

**Response (200 OK):**
```html
<!-- Página HTML con:
  - Texto de denuncia (DESCIFRADO automáticamente)
  - Archivos adjuntos (links de descarga)
  - Historia de cambios de estado
  - Botones de acción (aprobar, rechazar, derivar)
-->
```

**Nota:** El texto cifrado en BD se descifra automáticamente por `ComplaintService.findByTrackingId()`

---

#### 13. Descargar Evidencia
**GET** `/staff/casos/{trackingId}/evidencias/{evidenciaId}`

**Response (200 OK):**
```
Content-Type: application/pdf (o según tipo)
Content-Disposition: attachment; filename="evidencia_001.pdf"

[Binary data - archivo descifrado]
```

---

#### 14. Actualizar Estado de Caso
**POST** `/staff/casos/{trackingId}/estado`

**Request Body (form-data):**
```
newStatus: IN_REVIEW|COMPLETED|REJECTED
```

**Response:** Redirect a `/staff/casos/{trackingId}` con mensaje flash

---

#### 15. Aprobar y Derivar Caso
**POST** `/staff/casos/{trackingId}/aprobar-derivar`

**Descripción:** Aprueba la denuncia y la deriva automáticamente según reglas configuradas

**Response (302 Redirect):**
```
Location: /staff/casos/{trackingId}
Flash: "Denuncia aprobada y enviada a: Fiscalía General del Estado"
```

---

### Endpoints de Admin (Requiere Auth + Role ADMIN)

#### 16. Ver Logs de Auditoría
**GET** `/admin/logs`

**Query Parameters (opcionales):**
```
username: USR-Xy7kP0Qz (hash SHA-256)
eventType: LOGIN|LOGOUT|CREATE|UPDATE|DELETE|ACCESS|REVEAL|ERROR
startDate: 2026-01-01
endDate: 2026-01-31
page: 0
size: 50
```

**Response (200 OK):**
```html
<!-- Tabla HTML con logs:
  - Timestamp (UTC)
  - Event Type
  - Username hasheado (8 caracteres)
  - Details (truncado 500 chars)
  - Request ID
-->
```

---

#### 17. Listar Reglas de Derivación
**GET** `/admin/reglas`

**Response (200 OK):**
```html
<!-- Tabla HTML con:
  - ID
  - Nombre de regla
  - Severidad match
  - Entidad destino
  - Estado (active/inactive)
  - Botones: Editar, Desactivar, Activar
-->
```

---

#### 18. Crear Regla de Derivación
**POST** `/admin/reglas/crear`

**Request Body (form-data):**
```
name: Severidad Alta -> OIJ
severityMatch: HIGH (opcional)
destinationId: 3
description: Denuncias de alta gravedad van a OIJ
```

**Response:** Redirect a `/admin/reglas` con mensaje de éxito

---

#### 19. Editar Regla de Derivación
**POST** `/admin/reglas/{id}/editar`

**Request Body (form-data):**
```
name: Nuevo nombre
severityMatch: CRITICAL
destinationId: 5
description: Descripción actualizada
active: true
```

**Response:** Redirect a `/admin/reglas` con mensaje de éxito

---

#### 20. Desactivar Regla
**POST** `/admin/reglas/{id}/eliminar`

**Descripción:** Soft-delete (marca como `active=false`)

**Response:** Redirect a `/admin/reglas`

---

#### 21. Activar Regla
**POST** `/admin/reglas/{id}/activar`

**Descripción:** Reactiva una regla desactivada

**Response:** Redirect a `/admin/reglas`

---

### Webhooks (Callbacks Externos)

#### 22. Webhook DIDIT
**POST** `/webhooks/didit`

**Headers:**
```
X-Didit-Signature: hmac_sha256_signature
Content-Type: application/json
```

**Request Body:**
```json
{
  "session_id": "session_abc123",
  "status": "Approved",
  "webhook_type": "VERIFICATION_COMPLETED",
  "document_data": {
    "personal_number": "1234567890",
    "full_name": "Juan Pérez",
    "nationality": "ECU",
    "birth_date": "1990-01-01"
  }
}
```

**Response (200 OK):**
```json
{
  "received": true,
  "session_id": "session_abc123"
}
```

**Validaciones:**
- Firma HMAC-SHA256 en header `X-Didit-Signature`
- Validación timing-safe contra `DIDIT_WEBHOOK_SECRET_KEY`
- Si firma inválida → HTTP 403

---

### Códigos de Error HTTP

| Código | Significado | Cuándo Ocurre |
|--------|-------------|---------------|
| `400 Bad Request` | Parámetros faltantes o inválidos | Formulario incompleto, CAPTCHA inválido |
| `401 Unauthorized` | Credenciales inválidas | Login fallido, JWT expirado |
| `403 Forbidden` | Sin permisos | ANALYST intenta acceder a `/admin` |
| `404 Not Found` | Recurso no existe | Tracking ID inválido |
| `410 Gone` | Recurso expirado | OTP expirado (>5 min) |
| `413 Payload Too Large` | Archivo muy grande | Evidencia > 25 MB |
| `415 Unsupported Media Type` | Tipo de archivo no permitido | Upload de .exe o .zip |
| `423 Locked` | Cuenta bloqueada | 3 intentos fallidos de login |
| `429 Too Many Requests` | Rate limit excedido | >30 req/min por IP |
| `500 Internal Server Error` | Error del servidor | Fallo de BD, cifrado, etc. |

---

### Ejemplos con cURL

#### Crear Denuncia (flujo completo)
```bash
# Paso 1: Verificación biométrica
curl -X POST https://vozsegura.gob.ec/verification/inicio \
  -F "cedula=1234567890" \
  -F "turnstileToken=0.ABC123XYZ" \
  -c cookies.txt

# Paso 2: Verificar estado (esperar aprobación)
curl -X GET "https://vozsegura.gob.ec/verification/status?sessionId=session_xyz" \
  -b cookies.txt

# Paso 3: Enviar denuncia
curl -X POST https://vozsegura.gob.ec/denuncia/submit \
  -b cookies.txt \
  -F "tipoDelito=CORRUPCION" \
  -F "descripcion=Descripción de la denuncia" \
  -F "evidencia=@documento.pdf"
```

#### Login Staff (flujo MFA)
```bash
# Paso 1: Verificación biométrica
curl -X POST https://vozsegura.gob.ec/auth/unified-login \
  -F "cedula=1234567890" \
  -F "codigoDactilar=123456" \
  -F "turnstileToken=0.ABC123" \
  -c cookies.txt

# Paso 2: Contraseña
curl -X POST https://vozsegura.gob.ec/auth/verify-secret \
  -b cookies.txt \
  -F "cedula=1234567890" \
  -F "secretKey=VozSegura2026Admin!"

# Paso 3: OTP
curl -X POST https://vozsegura.gob.ec/auth/verify-otp \
  -b cookies.txt \
  -F "cedula=1234567890" \
  -F "otpCode=123456"
```

---

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

## 🚀 Despliegue en Producción

### Compilación del Proyecto

#### Compilar Gateway
```bash
cd gateway
../mvnw clean package -DskipTests
# Archivo generado: gateway/target/voz-segura-gateway-0.0.1-SNAPSHOT.jar
```

#### Compilar Core
```bash
./mvnw clean package -DskipTests
# Archivo generado: target/voz-segura-core-0.0.1-SNAPSHOT.jar
```

### Opción 1: Despliegue con Docker

```bash
# Construir imágenes
docker-compose build

# Iniciar servicios
docker-compose up -d

# Verificar estado
docker-compose ps

# Ver logs
docker-compose logs -f gateway
docker-compose logs -f core
```

### Opción 2: Despliegue en AWS EC2

#### Instalar Java 21
```bash
sudo amazon-linux-extras install java-openjdk21 -y
java -version
```

#### Configurar variables de entorno
```bash
sudo vim /etc/environment
```

Agregar:
```bash
# Gateway
GATEWAY_PORT=8080
CORE_SERVICE_URI=http://localhost:8082
JWT_SECRET=<generar_con_openssl_rand_base64_32>
VOZSEGURA_GATEWAY_SHARED_SECRET=<mismo_en_gateway_y_core>

# Core
SERVER_PORT=8082
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://aws-0-us-west-2.pooler.supabase.com:6543/postgres?sslmode=require
DB_USERNAME=postgres.<tu-proyecto>
DB_PASSWORD=<tu-password-seguro>
VOZSEGURA_DATA_KEY_B64=<clave-aes-256-base64>
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=<tu-access-key>
AWS_SECRET_ACCESS_KEY=<tu-secret-key>
AWS_SES_FROM_EMAIL=noreply@vozsegura.gob.ec
DIDIT_APP_ID=<tu-app-id>
DIDIT_API_KEY=<tu-api-key>
DIDIT_WEBHOOK_SECRET_KEY=<tu-webhook-secret>
CLOUDFLARE_SITE_KEY=<tu-site-key>
CLOUDFLARE_SECRET_KEY=<tu-secret-key>
```

#### Crear servicios systemd

**Gateway Service (`/etc/systemd/system/voz-segura-gateway.service`):**
```ini
[Unit]
Description=Voz Segura API Gateway
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/vozsegura
ExecStart=/usr/bin/java -jar -Xmx512m -Xms256m /opt/vozsegura/gateway.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=voz-segura-gateway
EnvironmentFile=/etc/environment

[Install]
WantedBy=multi-user.target
```

**Core Service (`/etc/systemd/system/voz-segura-core.service`):**
```ini
[Unit]
Description=Voz Segura Core Application
After=network.target

[Service]
Type=simple
User=ec2-user
WorkingDirectory=/opt/vozsegura
ExecStart=/usr/bin/java -jar -Xmx2g -Xms1g /opt/vozsegura/core.jar
Restart=always
RestartSec=10
StandardOutput=journal
StandardError=journal
SyslogIdentifier=voz-segura-core
EnvironmentFile=/etc/environment

[Install]
WantedBy=multi-user.target
```

#### Iniciar servicios
```bash
# Recargar systemd
sudo systemctl daemon-reload

# Habilitar inicio automático
sudo systemctl enable voz-segura-core
sudo systemctl enable voz-segura-gateway

# Iniciar servicios
sudo systemctl start voz-segura-core
sudo systemctl start voz-segura-gateway

# Verificar estado
sudo systemctl status voz-segura-core
sudo systemctl status voz-segura-gateway

# Ver logs
sudo journalctl -u voz-segura-core -f
sudo journalctl -u voz-segura-gateway -f
```

### Opción 3: Configuración de Nginx (Reverse Proxy)

#### Instalar Nginx
```bash
sudo yum install nginx -y
```

#### Configurar SSL con Let's Encrypt
```bash
sudo yum install certbot python3-certbot-nginx -y
sudo certbot --nginx -d vozsegura.gob.ec
```

#### Configuración Nginx (`/etc/nginx/conf.d/vozsegura.conf`)
```nginx
# Upstream para Gateway
upstream gateway_backend {
    server localhost:8080 max_fails=3 fail_timeout=30s;
}

# Redirigir HTTP a HTTPS
server {
    listen 80;
    server_name vozsegura.gob.ec;
    return 301 https://$server_name$request_uri;
}

# Servidor HTTPS principal
server {
    listen 443 ssl http2;
    server_name vozsegura.gob.ec;

    # Certificados SSL (Let's Encrypt)
    ssl_certificate /etc/letsencrypt/live/vozsegura.gob.ec/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/vozsegura.gob.ec/privkey.pem;

    # Configuración SSL moderna
    ssl_protocols TLSv1.2 TLSv1.3;
    ssl_ciphers 'ECDHE-RSA-AES256-GCM-SHA384:ECDHE-RSA-AES128-GCM-SHA256';
    ssl_prefer_server_ciphers on;
    ssl_session_cache shared:SSL:10m;
    ssl_session_timeout 10m;

    # Headers de seguridad
    add_header Strict-Transport-Security "max-age=31536000; includeSubDomains" always;
    add_header X-Frame-Options "DENY" always;
    add_header X-Content-Type-Options "nosniff" always;
    add_header X-XSS-Protection "1; mode=block" always;
    add_header Referrer-Policy "strict-origin-when-cross-origin" always;

    # Limitar tamaño de archivos (denuncias con evidencias)
    client_max_body_size 30M;

    # Proxy al Gateway
    location / {
        proxy_pass http://gateway_backend;
        proxy_http_version 1.1;
        
        # Headers para proxy
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;
        proxy_set_header X-Forwarded-Host $host;
        
        # Timeouts
        proxy_connect_timeout 60s;
        proxy_send_timeout 60s;
        proxy_read_timeout 60s;
        
        # Buffering
        proxy_buffering on;
        proxy_buffer_size 4k;
        proxy_buffers 8 4k;
    }

    # Health check endpoint (no cachear)
    location /actuator/health {
        proxy_pass http://gateway_backend;
        proxy_http_version 1.1;
        proxy_set_header Host $host;
        add_header Cache-Control "no-cache, no-store, must-revalidate";
    }

    # Archivos estáticos (cachear por 7 días)
    location ~* \.(css|js|jpg|jpeg|png|gif|svg|ico|woff|woff2|ttf)$ {
        proxy_pass http://gateway_backend;
        expires 7d;
        add_header Cache-Control "public, immutable";
    }
}
```

#### Reiniciar Nginx
```bash
sudo nginx -t  # Verificar configuración
sudo systemctl restart nginx
sudo systemctl enable nginx
```

### Monitoreo y Logs

#### Ver logs de aplicación
```bash
# Logs de systemd
sudo journalctl -u voz-segura-core -f
sudo journalctl -u voz-segura-gateway -f

# Logs de aplicación (si están en archivos)
tail -f /opt/vozsegura/logs/core.log
tail -f /opt/vozsegura/logs/gateway.log
```

#### Configurar logrotate
```bash
sudo vim /etc/logrotate.d/vozsegura
```

```
/opt/vozsegura/logs/*.log {
    daily
    rotate 30
    compress
    delaycompress
    missingok
    notifempty
    create 0644 ec2-user ec2-user
    sharedscripts
    postrotate
        systemctl reload voz-segura-core
        systemctl reload voz-segura-gateway
    endscript
}
```

### Actualización en Producción (Zero Downtime)

```bash
# 1. Subir nuevos JARs
scp target/core.jar ec2-user@servidor:/opt/vozsegura/core-new.jar
scp gateway/target/gateway.jar ec2-user@servidor:/opt/vozsegura/gateway-new.jar

# 2. Verificar que funcionan
java -jar /opt/vozsegura/core-new.jar --server.port=8083 &
java -jar /opt/vozsegura/gateway-new.jar --server.port=8081 &

# 3. Reemplazar y reiniciar
sudo systemctl stop voz-segura-core
sudo mv /opt/vozsegura/core-new.jar /opt/vozsegura/core.jar
sudo systemctl start voz-segura-core

sudo systemctl stop voz-segura-gateway
sudo mv /opt/vozsegura/gateway-new.jar /opt/vozsegura/gateway.jar
sudo systemctl start voz-segura-gateway

# 4. Verificar logs
sudo journalctl -u voz-segura-core -n 100
sudo journalctl -u voz-segura-gateway -n 100
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

## 🧪 Pruebas Unitarias y de Seguridad

### Ejecutar Todos los Tests

```bash
# Ejecutar todos los tests
./mvnw test

# Ejecutar con cobertura (JaCoCo)
./mvnw test jacoco:report

# Ver reporte de cobertura
open target/site/jacoco/index.html
# o en Windows: start target\site\jacoco\index.html
```

### Ejecutar Tests por Categoría

```bash
# Solo tests de seguridad
./mvnw test -Dtest="*SecurityTest,*AccessControlTest,SecuritySmokeTests"

# Solo tests de servicios
./mvnw test -Dtest="*ServiceTest"

# Solo tests de validación
./mvnw test -Dtest="*ValidationTest"

# Tests de un archivo específico
./mvnw test -Dtest=SecuritySmokeTests
```

### Tests Implementados

#### 1. **SecuritySmokeTests** - Validación de Controles de Acceso

**Archivo:** `src/test/java/com/vozsegura/vozsegura/SecuritySmokeTests.java`

**Propósito:** Verificar que los controles de acceso básicos funcionan correctamente

**Tests:**

```java
@Test
void publicDenunciaRequiresAuth() throws Exception {
    // Verifica que /denuncia requiere autenticación o verif. biométrica
    mockMvc.perform(get("/denuncia"))
            .andExpect(status().is3xxRedirection());
}
```
- ✅ **Valida:** Rutas de denuncias redirigen si no hay verificación
- ✅ **Control:** Zero Trust - No hay acceso sin autenticación

```java
@Test
void authLoginIsAccessible() throws Exception {
    // Verifica que la página de login es pública
    mockMvc.perform(get("/auth/login"))
            .andExpect(status().isOk());
}
```
- ✅ **Valida:** Ruta `/auth/login` es pública
- ✅ **Control:** Endpoints de autenticación accesibles sin login

```java
@Test
void staffCasosRequiresAuth() throws Exception {
    // Verifica que /staff requiere autenticación
    mockMvc.perform(get("/staff/casos"))
            .andExpect(status().is3xxRedirection());
}
```
- ✅ **Valida:** Panel de staff requiere login
- ✅ **Control:** RBAC - Solo usuarios autenticados

---

#### 2. **VozSeguraApplicationTests** - Tests de Integración

**Archivo:** `src/test/java/com/vozsegura/vozsegura/VozSeguraApplicationTests.java`

**Propósito:** Verificar que la aplicación inicia correctamente

```java
@Test
void contextLoads() {
    // Verifica que Spring Boot context se carga sin errores
}
```
- ✅ **Valida:** Configuración de Spring Boot correcta
- ✅ **Valida:** Todas las dependencias inyectables

---

### Tests de Seguridad Recomendados (Para Implementar)

#### A. **ApiGatewayFilterTest** - Autorización por Ruta

```java
@Test
void analystCannotAccessAdmin() throws Exception {
    // Simular sesión con userType=ANALYST
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userType", "ANALYST");
    session.setAttribute("authenticated", true);
    
    // Intentar acceder a ruta admin
    mockMvc.perform(get("/admin/logs").session(session))
            .andExpect(status().isForbidden());
}

@Test
void adminCanAccessBothAdminAndStaff() throws Exception {
    MockHttpSession session = new MockHttpSession();
    session.setAttribute("userType", "ADMIN");
    session.setAttribute("authenticated", true);
    
    mockMvc.perform(get("/admin/logs").session(session))
            .andExpect(status().isOk());
    mockMvc.perform(get("/staff/casos").session(session))
            .andExpect(status().isOk());
}

@Test
void unauthenticatedUserCannotAccessProtectedRoutes() throws Exception {
    mockMvc.perform(get("/staff/casos"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrlPattern("**/auth/login**"));
}
```

**Controles Validados:**
- ✅ ANALYST NO puede acceder a `/admin/**`
- ✅ ADMIN puede acceder a `/admin/**` y `/staff/**`
- ✅ Sin autenticación → redirect a login

---

#### B. **ZeroTrustGatewayFilterTest** - Validación HMAC

```java
@Test
void requestWithoutHmacSignatureIsRejected() throws Exception {
    mockMvc.perform(post("/staff/casos")
            .header("X-User-Cedula", "1234567890")
            .header("X-User-Type", "ANALYST"))
            // Sin X-Gateway-Signature
            .andExpect(status().isUnauthorized());
}

@Test
void requestWithInvalidHmacSignatureIsRejected() throws Exception {
    String timestamp = String.valueOf(System.currentTimeMillis());
    String fakeSignature = "invalid_signature_123";
    
    mockMvc.perform(post("/staff/casos")
            .header("X-Gateway-Signature", fakeSignature)
            .header("X-Request-Timestamp", timestamp)
            .header("X-User-Cedula", "1234567890")
            .header("X-User-Type", "ANALYST"))
            .andExpect(status().isForbidden());
}

@Test
void requestWithExpiredTimestampIsRejected() throws Exception {
    // Timestamp de hace 2 minutos (TTL: 60 segundos)
    long expiredTimestamp = System.currentTimeMillis() - 120_000;
    String signature = generateValidHmac(expiredTimestamp, "GET", "/staff/casos");
    
    mockMvc.perform(get("/staff/casos")
            .header("X-Gateway-Signature", signature)
            .header("X-Request-Timestamp", String.valueOf(expiredTimestamp)))
            .andExpect(status().isForbidden());
}
```

**Controles Validados:**
- ✅ Sin firma HMAC → HTTP 401
- ✅ Firma HMAC inválida → HTTP 403
- ✅ Timestamp expirado (>60s) → HTTP 403 (anti-replay)

---

#### C. **UnifiedAuthServiceTest** - Flujo MFA

```java
@Test
void mfaFlowCompletesSuccessfully() {
    String cedula = "1234567890";
    String password = "VozSegura2026Admin!";
    
    // Paso 1: Verificación biométrica (mock)
    when(diditService.createVerificationSession(cedula))
            .thenReturn("session_abc123");
    
    // Paso 2: Validación de contraseña
    StaffUser user = createMockStaffUser(cedula, password);
    when(staffUserRepository.findByCedulaAndEnabledTrue(cedula))
            .thenReturn(Optional.of(user));
    
    boolean passwordValid = authService.validateSecretKey(user, password);
    assertTrue(passwordValid);
    
    // Paso 3: Envío de OTP
    String otpToken = authService.sendEmailOtp(cedula);
    assertNotNull(otpToken);
    
    // Paso 4: Verificación OTP
    boolean otpValid = otpClient.verifyOtp(otpToken, "123456");
    assertTrue(otpValid);
}

@Test
void accountIsLockedAfterThreeFailedAttempts() {
    String cedula = "1234567890";
    StaffUser user = createMockStaffUser(cedula, "correct_password");
    
    // 3 intentos fallidos
    for (int i = 0; i < 3; i++) {
        authService.validateSecretKey(user, "wrong_password");
    }
    
    // Verificar que cuenta está bloqueada
    assertTrue(user.isLocked());
    
    // Intentar login con contraseña correcta debe fallar
    assertThrows(AccountLockedException.class, () -> {
        authService.validateSecretKey(user, "correct_password");
    });
}

@Test
void otpExpiresAfterFiveMinutes() throws InterruptedException {
    String otpToken = otpClient.sendOtp("user@example.com");
    
    // Simular paso de 6 minutos
    Thread.sleep(6 * 60 * 1000);
    
    // Verificación debe fallar
    boolean valid = otpClient.verifyOtp(otpToken, "123456");
    assertFalse(valid);
}
```

**Controles Validados:**
- ✅ Flujo MFA completo (Biometría + Password + OTP)
- ✅ Bloqueo de cuenta tras 3 intentos fallidos
- ✅ Expiración de OTP (5 minutos)

---

#### D. **ComplaintServiceTest** - Cifrado y Auditoría

```java
@Test
void complaintTextIsEncryptedBeforePersisting() {
    String plainText = "Descripción de la denuncia sensible";
    
    Complaint complaint = new Complaint();
    complaint.setEncryptedText(plainText);
    
    // Guardar (debe cifrar automáticamente)
    complaintService.save(complaint);
    
    // Verificar que en BD está cifrado
    String encryptedInDb = complaintRepository
            .findById(complaint.getId())
            .get()
            .getEncryptedText();
    
    assertNotEquals(plainText, encryptedInDb);
    assertTrue(encryptedInDb.startsWith("base64:")); // Base64 encoded
}

@Test
void complaintTextIsDecryptedWhenRetrieved() {
    String originalText = "Texto original de la denuncia";
    
    // Crear y guardar
    Complaint complaint = complaintService.createComplaint(
            originalText, "CORRUPCION", "citizenHash123");
    
    // Recuperar (debe descifrar automáticamente)
    Complaint retrieved = complaintService
            .findByTrackingId(complaint.getTrackingId())
            .orElseThrow();
    
    assertEquals(originalText, retrieved.getDecryptedText());
}

@Test
void onlyAnalystCanAccessComplaint() {
    String trackingId = "DEN-2026-ABC123";
    
    // ANALYST puede acceder
    when(session.getAttribute("userType")).thenReturn("ANALYST");
    Complaint complaint = complaintService.findByTrackingId(trackingId);
    assertNotNull(complaint);
    
    // DENUNCIANTE NO puede acceder a otras denuncias
    when(session.getAttribute("userType")).thenReturn("DENUNCIANTE");
    assertThrows(AccessDeniedException.class, () -> {
        complaintService.findByTrackingId(trackingId);
    });
}

@Test
void complaintAccessIsAudited() {
    String trackingId = "DEN-2026-ABC123";
    String username = "USR-Xy7kP0Qz";
    
    complaintService.findByTrackingId(trackingId);
    
    // Verificar que se registró en auditoría
    verify(auditService).logEvent(
            eq("ACCESS"),
            contains(trackingId),
            eq(username)
    );
}
```

**Controles Validados:**
- ✅ Texto cifrado antes de guardar en BD
- ✅ Texto descifrado automáticamente al recuperar
- ✅ Solo ANALYST puede leer denuncias
- ✅ Accesos registrados en auditoría (sin PII)

---

#### E. **FileValidationServiceTest** - Validación de Archivos

```java
@Test
void pdfFilePassesValidation() {
    byte[] pdfBytes = createMockPdfFile(); // Comienza con %PDF
    MultipartFile file = new MockMultipartFile(
            "evidencia", "documento.pdf", "application/pdf", pdfBytes);
    
    boolean valid = fileValidationService.isValidEvidence(file);
    assertTrue(valid);
}

@Test
void exeFileIsRejected() {
    byte[] exeBytes = createMockExeFile(); // Magic bytes: MZ
    MultipartFile file = new MockMultipartFile(
            "malware", "virus.exe", "application/exe", exeBytes);
    
    boolean valid = fileValidationService.isValidEvidence(file);
    assertFalse(valid);
}

@Test
void fileTooLargeIsRejected() {
    byte[] largeFile = new byte[30 * 1024 * 1024]; // 30 MB
    MultipartFile file = new MockMultipartFile(
            "evidencia", "huge.pdf", "application/pdf", largeFile);
    
    boolean valid = fileValidationService.isValidEvidence(file);
    assertFalse(valid); // Max: 25 MB
}

@Test
void pathTraversalIsBlocked() {
    byte[] pdfBytes = createMockPdfFile();
    MultipartFile file = new MockMultipartFile(
            "evidencia", "../../../etc/passwd", "application/pdf", pdfBytes);
    
    assertThrows(SecurityException.class, () -> {
        fileValidationService.isValidEvidence(file);
    });
}
```

**Controles Validados:**
- ✅ Whitelist de MIME types (PDF, DOCX, JPG, PNG, MP4)
- ✅ Validación de magic bytes (firma real del archivo)
- ✅ Tamaño máximo 25 MB
- ✅ Path traversal bloqueado

---

### Cobertura de Código

```bash
# Generar reporte de cobertura
./mvnw clean test jacoco:report

# Ver reporte HTML
open target/site/jacoco/index.html
```

**Métricas de Cobertura Objetivo:**
- Clases: > 80%
- Métodos: > 75%
- Líneas: > 70%
- Branches: > 65%

**Áreas Críticas (cobertura > 90%):**
- `EncryptionService`
- `GatewayRequestValidator`
- `ZeroTrustGatewayFilter`
- `ApiGatewayFilter`
- `UnifiedAuthService`

---

### Ejecutar Tests en CI/CD (GitHub Actions)

```yaml
# .github/workflows/tests.yml
name: Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    
    steps:
      - uses: actions/checkout@v3
      
      - name: Set up JDK 21
        uses: actions/setup-java@v3
        with:
          java-version: '21'
          distribution: 'temurin'
      
      - name: Run tests
        run: ./mvnw clean test
      
      - name: Generate coverage report
        run: ./mvnw jacoco:report
      
      - name: Upload coverage to Codecov
        uses: codecov/codecov-action@v3
        with:
          file: ./target/site/jacoco/jacoco.xml
```

---

### Tests de Penetración (Pentesting)

#### Herramientas Recomendadas:
- **OWASP ZAP:** Escaneo automatizado de vulnerabilidades
- **Burp Suite:** Pruebas manuales de seguridad
- **sqlmap:** Detección de inyecciones SQL
- **Nikto:** Escaneo de servidor web

#### Checklist de Seguridad:
- ✅ Inyección SQL (JPA con prepared statements)
- ✅ XSS (Thymeleaf escapa por defecto)
- ✅ CSRF (Spring Security CSRF enabled)
- ✅ Clickjacking (X-Frame-Options: DENY)
- ✅ HTTPS (TLS 1.2+)
- ✅ Rate Limiting (30 req/min)
- ✅ Autenticación (MFA obligatorio)
- ✅ Autorización (RBAC granular)
- ✅ Cifrado (AES-256-GCM)
- ✅ Auditoría (todos los accesos)

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

**Tablas Cifradas:**

| Tabla | Registros | Columnas Encrypted | Hashes SHA-256 |
|-------|-----------|-------------------|----------------|
| `registro_civil.personas` | 5 | 5 (cedula, nombres, apellidos) | 2 (cedula, nombre_completo) |
| `staff.staff_user` | 2 | 2 (cedula, email) | 2 (cedula, email) |
| `denuncias.denuncia` | 41 | 4 (text, analyst_notes, company_contact, company_address) | - |
| `reglas_derivacion.entidad_destino` | 12 | 3 (email, phone, address) | - |
| `evidencias.evidencia` | N | 1 (encrypted_content - binario) | - |
| `registro_civil.didit_verification` | N | 1 (document_number) | - |

**Job de Cifrado Automático:**
```bash
#### 4. **Validación de Archivos**
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
**Última actualización:** Enero 22, 2026  
**Versión:** 2.0

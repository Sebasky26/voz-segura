# 🔐 VOZ SEGURA - Plataforma de Denuncias Anónimas

**Versión:** 2.0  
**Fecha:** Enero 2026  
**Arquitectura:** Zero Trust Architecture (ZTA)  
**Base de Datos:** Supabase PostgreSQL  
**Estado de Seguridad:** ✅ Auditado - Enero 2026

---

## 📖 Descripción del Proyecto

**Voz Segura** es una plataforma gubernamental de denuncias anónimas desarrollada bajo principios de **Zero Trust Architecture**, diseñada para garantizar la máxima seguridad y privacidad de los denunciantes en Ecuador.

### 🎯 Características Principales

- ✅ **Verificación Biométrica con DIDIT v3:** Autenticación facial contra Registro Civil de Ecuador
- 🔐 **Cifrado de Extremo a Extremo:** AES-256-GCM para todas las denuncias y evidencias
- 👤 **Anonimato Total:** Identity Vault separa identidad real de las denuncias
- 🛡️ **Zero Trust:** Validación HMAC-SHA256 entre Gateway y Core con anti-replay
- 📱 **MFA para Staff:** Autenticación de dos factores con OTP por email (AWS SES)
- 🔒 **PII Cifrado en BD:** Datos sensibles cifrados automáticamente con AES-256-GCM
- 📊 **Auditoría Completa:** Todos los accesos registrados sin exposición de datos sensibles
- ☁️ **Cloud Native:** Supabase PostgreSQL, AWS SES, Cloudflare Turnstile
- 🛡️ **Validación de Archivos:** Magic bytes + whitelist estricta (PDF, DOCX, JPG, PNG)
- ⚡ **Rate Limiting:** Protección anti-brute-force con ventanas deslizantes
- 🔍 **Headers de Seguridad:** CSP, XSS protection, anti-clickjacking

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
│   - Flyway Migrations           │
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

```
Usuario → JWT válido → Gateway (puerto 8080)
                         ↓
          Firma HMAC: SHA256(timestamp:method:path:user:type)
          Anti-replay: TTL 5 minutos
                         ↓
          Headers: X-Gateway-Signature
                   X-Request-Timestamp (TTL 60s)
                   X-User-Cedula (masked en logs)
                   X-User-Type
                         ↓
                      Core (puerto 8082)
                         ↓
          Valida HMAC → Si inválido: 403 Forbidden
                      → Si válido: Procesa petición
          
🔒 Características de Seguridad:
- Clave compartida 256-bit entre Gateway y Core
- Timestamps con TTL para prevenir replay attacks
- Comparación constante en tiempo (anti-timing attack)
- Rate limiting: 30 requests/minuto por IP
- Logs seguros: enmascaramiento de datos PII
```

### Flujo de Autenticación

#### Denunciantes
1. Verificación biométrica DIDIT → Cédula + Nombre
2. Validación contra Registro Civil
3. Aceptación de términos y condiciones
4. Acceso al panel de denuncias

#### Staff (Admin/Analista)
1. Verificación biométrica DIDIT
2. Validación contra Registro Civil
3. Verificación en tabla `staff_user`
4. Ingreso de clave secreta (BCrypt)
5. OTP por email (AWS SES)
6. JWT token (24h)
7. Acceso al panel correspondiente

---

## 💻 Tecnologías Utilizadas

### Backend
- **Java 21** - LTS
- **Spring Boot 3.3.4** - Framework principal
- **Spring Security** - Autenticación y autorización
- **Spring Cloud Gateway** - API Gateway con filtros
- **Spring Data JPA** - Persistencia
- **Flyway** - Migraciones de base de datos (automáticas)

### Seguridad
- **JWT (jjwt 0.12.3)** - Tokens de sesión
- **BCrypt** - Hashing de contraseñas
- **AES-256-GCM** - Cifrado de PII
- **HMAC-SHA256** - Validación Zero Trust
- **Cloudflare Turnstile** - Anti-bot

### Base de Datos
- **Supabase PostgreSQL 16** - Base de datos principal
- **PgBouncer** - Connection pooling
- **6 Schemas:** registro_civil, staff, denuncias, evidencias, logs, reglas_derivacion

### Integraciones Externas
- **DIDIT API v3** - Verificación biométrica
- **AWS SES** - Envío de emails OTP
- **AWS Secrets Manager** - Gestión de secretos (producción)
- **Cloudflare Turnstile** - CAPTCHA

### Frontend
- **Thymeleaf** - Motor de plantillas
- **HTML5 + CSS3** - UI responsive
- **JavaScript Vanilla** - Interactividad

---

## 📊 Esquemas de Base de Datos

### 1. `registro_civil` - Identidades
- **`personas`**: Ciudadanos verificados (PII cifrado)
- **`didit_verification`**: Registros de verificación biométrica

### 2. `staff` - Personal del Sistema
- **`staff_user`**: Usuarios Admin/Analista (PII cifrado)

### 3. `denuncias` - Denuncias
- **`complaint`**: Denuncias con texto cifrado
- **`complaint_status_log`**: Historial de cambios

### 4. `evidencias` - Archivos Adjuntos
- **`evidence`**: Archivos PDF/DOCX/IMG cifrados

### 5. `logs` - Auditoría
- **`audit_event`**: Registro de todas las acciones

### 6. `reglas_derivacion` - Configuración
- **`derivation_rule`**: Reglas de derivación automática
- **`destination_entity`**: Entidades destino

---

## 🚀 Instalación y Configuración

### Requisitos Previos

- Java 21 JDK
- Maven 3.8+
- Cuenta Supabase (PostgreSQL)
- Cuenta AWS (SES + Secrets Manager para prod)
- Cuenta Cloudflare (Turnstile)
- Cuenta DIDIT (Verificación biométrica)

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
nano .env
```

**Variables Obligatorias:**

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
VOZSEGURA_DATA_KEY_B64=tu-encryption-key-base64
VOZSEGURA_GATEWAY_SHARED_SECRET=tu-shared-secret-base64

# === AWS ===
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

# === URLs (Opcional para desarrollo) ===
GATEWAY_BASE_URL=http://localhost:8080
CORE_SERVICE_URI=http://localhost:8082
```

### 3. Configurar Base de Datos en Supabase

1. Crear proyecto en [Supabase](https://supabase.com)
2. Obtener credenciales de conexión (usar Pooler para mejor rendimiento)
3. Las migraciones se ejecutan **automáticamente** al iniciar la aplicación

**Nota:** Las migraciones Flyway se ejecutan automáticamente en orden:
- V1 a V27: Estructura de BD
- V28: Agregar columnas PII cifradas
- V29: Cifrado automático de datos existentes (si existen)
- V30: Eliminar columnas texto plano

### 4. Compilar Proyecto

```bash
./mvnw clean install
```

---

## ▶️ Ejecutar la Aplicación

### Modo Desarrollo (Local)

#### Opción 1: Ejecutar ambos servicios en terminales separadas

```bash
# Terminal 1: Core Service (Puerto 8082)
./mvnw spring-boot:run

# Terminal 2: Gateway (Puerto 8080)
cd gateway
../mvnw spring-boot:run
```

#### Opción 2: Usar Docker Compose

```bash
docker-compose up --build
```

### Acceso a la Aplicación

- **URL Principal:** http://localhost:8080
- **Gateway:** http://localhost:8080
- **Core (interno):** http://localhost:8082 (no accesible directamente por Zero Trust)

---

## 🔒 Análisis de Seguridad Completo

### ✅ **FORTALEZAS DE SEGURIDAD IMPLEMENTADAS**

#### 1. **Arquitectura Zero Trust**
- ✅ Validación HMAC-SHA256 Gateway ↔ Core con clave compartida
- ✅ Anti-replay attacks: timestamps con TTL de 60 segundos
- ✅ Comparaciones constantes en tiempo (anti-timing attacks)
- ✅ Headers inmutables: imposible falsificar peticiones

#### 2. **Cifrado y Protección de Datos**
- ✅ **AES-256-GCM** (AEAD): Cifrado autenticado con detección de manipulación
- ✅ **BCrypt strength 10**: Hashing resistente de contraseñas
- ✅ **JWT HS256**: Tokens firmados con expiración 24h
- ✅ **SHA-256**: Hashing de cédulas para anonimización
- ✅ **IV aleatorio**: 12 bytes por encriptación (nunca reutilizado)

#### 3. **Validación de Archivos**
- ✅ **Magic bytes verification**: Validación real vs MIME declarado
- ✅ **Whitelist estricta**: Solo PDF, DOCX, JPG, PNG (máx 25MB)
- ✅ **Path traversal protection**: Sanitización de nombres
- ✅ **Eliminación de riesgos**: NO permite DOC (macros) ni ejecutables

#### 4. **Rate Limiting y Anti-Abuse**
- ✅ **Ventanas deslizantes**: 30 intentos/60 segundos por IP/usuario
- ✅ **Thread-safe**: ConcurrentHashMap + sincronización
- ✅ **Endpoints protegidos**: Login, OTP, admin panel

#### 5. **Headers de Seguridad Web**
- ✅ **CSP**: Content Security Policy contra XSS
- ✅ **X-Frame-Options: DENY**: Anti-clickjacking
- ✅ **X-XSS-Protection**: Activada
- ✅ **Strict-Transport-Security**: HTTPS obligatorio
- ✅ **X-Content-Type-Options: nosniff**: Anti-MIME sniffing

### ⚠️ **VULNERABILIDADES Y ÁREAS DE MEJORA**

#### 1. **Configuración y Bypass de Seguridad**
🔴 **CRÍTICO**: `NOT_USED_AWS_SECRET` bypass en `UnifiedAuthService.validateSecretKey()`:
```java
if ("NOT_USED_AWS_SECRET".equals(passwordHash)) {
    return true; // ⚠️ Permite cualquier contraseña
}
```
**Riesgo**: Acceso no autorizado si un usuario tiene este marcador  
**Solución**: Eliminar en producción, forzar contraseñas reales

#### 2. **Configuración Hardcodeada**
🟡 **MEDIO**: Valores no configurables en:
- `InMemoryRateLimiter`: MAX_ATTEMPTS=30, WINDOW_SECONDS=60
- `ZeroTrustGatewayFilter`: TTL=300000 (5 min) 
- `FileValidationService`: MAX_SIZE=25MB
- `AesGcmEncryptionService`: IV_LENGTH=12, TAG_LENGTH=128

**Riesgo**: Inflexibilidad para ajustar según amenazas  
**Solución**: Mover a `application.yml` o base de datos

#### 3. **Exposición de Datos en Logs**
🟡 **MEDIO**: Logs con información parcialmente sensible:
```java
log.warn("❌ Validación fallida: Firma no coincide (Path: {}, User: {})",
         path, maskCedula(userCedula)); // ⚠️ Aún muestra primeros/últimos dígitos
```
**Riesgo**: Correlación de identidades  
**Solución**: Hash completo de identificadores en logs

#### 4. **Validación de Entrada Incompleta**
🟡 **MEDIO**: Algunos endpoints no validan completamente:
- Longitudes máximas de campos
- Caracteres especiales en entradas de usuario
- Validación numérica estricta

**Riesgo**: Injection attacks, buffer overflow  
**Solución**: Validación exhaustiva con Bean Validation

### 📊 **EVALUACIÓN DE SEGURIDAD POR COMPONENTE**

| Componente | Seguridad | Observaciones |
|------------|-----------|---------------|
| **Gateway JWT Validation** | 🟢 EXCELENTE | HMAC correcto, validación completa |
| **Zero Trust Filter** | 🟢 EXCELENTE | Implementación robusta |
| **AES-GCM Encryption** | 🟢 EXCELENTE | Algoritmo moderno, IV aleatorio |
| **File Validation** | 🟢 EXCELENTE | Magic bytes + whitelist |
| **Rate Limiter** | 🟢 BUENO | Thread-safe, pero hardcodeado |
| **Password Security** | 🟡 REGULAR | BCrypt bien, pero bypass crítico |
| **Logging Security** | 🟡 REGULAR | Máscaras parciales |
| **Input Validation** | 🟡 REGULAR | Básica, necesita mejoras |

### 🔧 **RECOMENDACIONES PRIORITARIAS**

#### **CRÍTICAS (Implementar YA)**
1. **Eliminar bypass de contraseñas**: Remover `NOT_USED_AWS_SECRET`
2. **Configurar producción**: Usar AWS Secrets Manager para todas las claves
3. **Validar configuración**: Fail-fast si faltan variables críticas

#### **IMPORTANTES (Próximas semanas)**
1. **Hacer configurables límites hardcodeados**
2. **Mejorar masking de logs**: SHA-256 completo de IDs
3. **Validación estricta de entrada**: Bean Validation exhaustiva
4. **Auditoría de permisos**: Review de roles y accesos

#### **RECOMENDADAS (Siguientes meses)**
1. **Implementar Redis Rate Limiter** para clusterización
2. **Agregar alertas de seguridad** para intentos maliciosos
3. **Security headers adicionales**: Permissions Policy
4. **Tests de penetración** automatizados

---

## 📁 Estructura del Proyecto

```
voz-segura/
├── src/main/
│   ├── java/com/vozsegura/vozsegura/
│   │   ├── client/          # Integraciones externas
│   │   ├── config/          # Configuración Spring + Zero Trust
│   │   ├── controller/      # Controladores REST/MVC
│   │   ├── domain/entity/   # Entidades JPA
│   │   ├── dto/             # DTOs y Forms
│   │   ├── repo/            # Repositories
│   │   ├── security/        # Cifrado y validación HMAC
│   │   └── service/         # Lógica de negocio
│   │       └── migration/   # Migraciones automáticas PII
│   └── resources/
│       ├── db/migration/    # Flyway (ejecución automática)
│       ├── static/          # CSS, JS, imágenes
│       ├── templates/       # Thymeleaf
│       └── application.yml
├── gateway/                 # Spring Cloud Gateway
│   └── src/main/
│       ├── java/com/vozsegura/gateway/
│       │   └── filter/      # JWT + HMAC
│       └── resources/
│           └── application.yml
├── .env.example             # Plantilla de variables
├── docker-compose.yml
├── pom.xml
└── README.md               # Este archivo
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

# Ver estado de migraciones
./mvnw flyway:info
```

### Docker

```bash
# Construir e iniciar
docker-compose up --build

# Detener
docker-compose down

# Ver logs
docker-compose logs -f core
docker-compose logs -f gateway
```

---

## 🐛 Troubleshooting

### Error: "JWT_SECRET not found"
```bash
# Solución: Agregar a .env
JWT_SECRET=$(openssl rand -base64 32)
```

### Error: "VOZSEGURA_GATEWAY_SHARED_SECRET not configured"
```bash
# Solución: Generar y agregar a .env (MISMO en Gateway y Core)
openssl rand -base64 32
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
# Verificar que shared secret es el MISMO
grep VOZSEGURA_GATEWAY_SHARED_SECRET .env
```

---

## 📋 Flujos del Sistema

### Flujo de Denuncia

1. Usuario accede a `/denuncia`
2. Verificación biométrica DIDIT
3. Validación contra Registro Civil
4. Aceptación de términos y condiciones
5. Formulario de denuncia (máx 4000 caracteres)
6. Upload de evidencias (PDF/DOCX/JPG/PNG)
7. **Cifrado automático** de texto y archivos
8. Generación de tracking ID (UUID)
9. Almacenamiento en `denuncias.complaint`
10. Retorno de código de seguimiento

### Flujo de Análisis (Staff)

1. Login con biometría + clave secreta + OTP
2. Lista de casos en estado PENDING
3. Visualización de caso (**descifrado automático**)
4. Clasificación (tipo, prioridad)
5. Derivación automática según reglas
6. Actualización de estado
7. Registro en auditoría

---

## 🚀 Despliegue a Producción

### 1. Preparación

```bash
# Compilar para producción
./mvnw clean package -Pprod

# Crear backup de BD Supabase
pg_dump "$SUPABASE_DB_URL" > backup_$(date +%Y%m%d).sql
```

### 2. Variables de Entorno

```bash
# Producción usa AWS Secrets Manager
export SPRING_PROFILES_ACTIVE=prod
export AWS_REGION=us-east-1
```

### 3. Ejecutar

```bash
# Core
java -jar target/voz-segura-2.0.jar --spring.profiles.active=prod

# Gateway
java -jar gateway/target/gateway-2.0.jar --spring.profiles.active=prod
```

### 4. Docker (Recomendado)

```bash
docker-compose -f docker-compose.yml up -d
```

---

## 📊 Monitoreo

### Health Checks

```bash
# Gateway
curl http://localhost:8080/actuator/health

# Core
curl http://localhost:8082/actuator/health
```

### Logs

```bash
# Core
tail -f logs/core-dev.log

# Gateway
tail -f gateway/logs/gateway.log

# Filtrar errores
grep ERROR logs/core-dev.log
```

---

## 📝 Licencia

Propiedad del Gobierno de Ecuador - Uso Gubernamental Exclusivo

---

## 👥 Equipo

- **Arquitectura y Desarrollo:** Equipo Voz Segura
- **Auditoría de Seguridad:** Enero 2026
- **Stack:** Java 21 + Spring Boot 3 + Supabase PostgreSQL

---

## 🔄 Changelog

### v2.0 (Enero 2026) - **Auditoría de Seguridad Completa**
- ✅ **Zero Trust Architecture** implementada y validada
- ✅ **Cifrado automático de PII** en BD (Flyway automático)
- ✅ **Migraciones automáticas** al iniciar aplicación
- ✅ **Validación HMAC** Gateway ↔ Core con anti-replay
- ✅ **Integración completa** con Supabase PostgreSQL
- ✅ **AWS SES** para MFA via OTP
- ✅ **DIDIT v3** verificación biométrica
- ✅ **Headers de seguridad** completos (CSP, XSS, HSTS)
- ✅ **Validación de archivos** con magic bytes
- ✅ **Rate limiting** anti-brute-force
- ✅ **Logs seguros** con masking de PII
- ⚠️ **Vulnerabilidades documentadas** (bypass contraseñas, config hardcodeada)
- 📋 **Plan de remediación** definido

### v1.0 (Noviembre 2025)
- Primera versión funcional

---

**Última actualización:** Enero 21, 2026  
**Versión:** 2.0  
**Estado:** ✅ Producción Ready (con plan de remediación de seguridad)  
**Auditoría:** ✅ Completada - Enero 2026  
**Próxima revisión:** Abril 2026

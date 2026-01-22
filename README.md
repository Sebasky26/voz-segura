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

- ✅ **Verificación Biométrica con DIDIT v3:** Autenticación facial contra Registro Civil
- 🔐 **Cifrado de Extremo a Extremo:** AES-256-GCM para denuncias y evidencias
- 👤 **Anonimato Total:** Identity Vault separa identidad real de denuncias
- 🛡️ **Zero Trust:** Validación HMAC-SHA256 entre Gateway y Core con anti-replay
- 📱 **MFA para Staff:** Autenticación de dos factores con OTP por email (AWS SES)
- 🔒 **PII Cifrado en BD:** Datos sensibles cifrados automáticamente con AES-256-GCM
- 📊 **Auditoría Completa:** Todos los accesos registrados sin exposición de PII
- ☁️ **Cloud Native:** Supabase PostgreSQL, AWS SES, Cloudflare Turnstile
- 🛡️ **Validación de Archivos:** Magic bytes + whitelist (PDF, DOCX, JPG, PNG)
- ⚡ **Rate Limiting:** Protección anti-brute-force
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

### Backend
- **Java 21** - LTS
- **Spring Boot 3.4.0** - Framework principal
- **Spring Security** - Autenticación y autorización
- **Spring Cloud Gateway** - API Gateway
- **Spring Data JPA** - Persistencia
- **Flyway** - Migraciones automáticas

### Seguridad
- **JWT (jjwt 0.12.3)** - Tokens de sesión
- **BCrypt** - Hashing de contraseñas
- **AES-256-GCM** - Cifrado de PII
- **HMAC-SHA256** - Validación Zero Trust
- **Cloudflare Turnstile** - Anti-bot

### Base de Datos
- **Supabase PostgreSQL 17** - Base de datos principal
- **PgBouncer** - Connection pooling
- **6 Schemas:** registro_civil, staff, denuncias, evidencias, logs, reglas_derivacion

### Integraciones
- **DIDIT API v3** - Verificación biométrica
- **AWS SES** - Envío de emails OTP
- **Cloudflare Turnstile** - CAPTCHA

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

#### Opción 2: Docker Compose

```bash
docker-compose up --build
```

### Acceso a la Aplicación

- **URL Principal:** http://localhost:8080
- **Gateway:** http://localhost:8080
- **Core (interno):** http://localhost:8082 (no accesible directamente)

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
│   └── resources/
│       ├── db/migration/    # Flyway (ejecución automática)
│       ├── static/          # CSS, JS, imágenes
│       ├── templates/       # Thymeleaf
│       ├── application.yml
│       └── application-dev.yml
├── gateway/                 # Spring Cloud Gateway
│   └── src/main/
│       ├── java/com/vozsegura/gateway/
│       └── resources/
│           └── application.yml
├── logs/                    # Logs (en .gitignore)
├── .env.example             # Plantilla de variables
├── .gitignore
├── docker-compose.yml
├── pom.xml
└── README.md
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

### Cifrado
- **AES-256-GCM**: PII en BD
- **BCrypt strength 10**: Contraseñas
- **JWT HS256**: Tokens de sesión
- **SHA-256**: Hashing de identificadores

### Validación
- **Magic bytes**: Archivos adjuntos
- **Turnstile**: Anti-bot
- **Rate limiting**: Anti-brute-force
- **HMAC-SHA256**: Zero Trust Gateway-Core

### Headers de Seguridad
- Content Security Policy (CSP)
- X-Frame-Options: DENY
- X-XSS-Protection
- Strict-Transport-Security
- X-Content-Type-Options: nosniff

### Logs Seguros
- ✅ Sin exposición de PII
- ✅ Sin datos de sesión
- ✅ Sin credenciales
- ✅ Solo errores críticos

---

## 🚀 Despliegue a Producción

### 1. Compilar para producción

```bash
./mvnw clean package -Pprod
```

### 2. Ejecutar

```bash
# Core
java -jar target/voz-segura-2.0.jar --spring.profiles.active=prod

# Gateway
java -jar gateway/target/gateway-2.0.jar --spring.profiles.active=prod
```

### 3. Docker (Recomendado)

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

# Filtrar errores
grep ERROR logs/core-dev.log
```

---

## 🗑️ Archivos Removibles

Los siguientes archivos **NO** son necesarios para ejecutar la aplicación:

- `mvnw`, `mvnw.cmd`, `mvnwDebug`, `mvnwDebug.cmd` - Solo si usas Maven instalado localmente
- `logs/*.log` - Archivos temporales (se regeneran automáticamente)
- `.idea/` - IDE IntelliJ IDEA (en .gitignore)

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
- ✅ **Zero Trust Architecture** implementada
- ✅ **Cifrado automático de PII** en BD
- ✅ **Migraciones automáticas** Flyway
- ✅ **Logs seguros** sin exposición de datos
- ✅ **Integración Supabase PostgreSQL**
- ✅ **AWS SES** para MFA via OTP
- ✅ **DIDIT v3** verificación biométrica
- ✅ **Documentación consolidada**

### v1.0 (Noviembre 2025)
- Primera versión funcional

---

**Última actualización:** Enero 21, 2026  
**Versión:** 2.0  
**Estado:** ✅ Producción Ready  
**Auditoría:** ✅ Completada - Enero 2026

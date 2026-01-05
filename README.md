# Voz Segura - Plataforma Segura de Denuncias Anónimas

**Versión:** 1.0.0  
**Año:** 2026  
**Arquitectura:** Zero Trust Architecture (ZTA)  
**Seguridad:** ISO 27001 | GDPR Compliant

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.4-green) ![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-blue) ![ZTA](https://img.shields.io/badge/Architecture-Zero%20Trust-red)

---

## 📋 Tabla de Contenidos

- [Descripción](#-descripción)
- [Arquitectura Zero Trust (ZTA)](#-arquitectura-zero-trust-zta)
- [Requisitos Previos](#-requisitos-previos)
- [Instalación en Windows](#-instalación-en-windows)
- [Configuración PostgreSQL Local](#-configuración-postgresql-local)
- [Variables de Entorno](#-variables-de-entorno)
- [Ejecución](#-ejecución)
- [Sistema de Autenticación Unificado](#-sistema-de-autenticación-unificado)
- [Usuarios de Prueba](#-usuarios-de-prueba)
- [API Gateway ZTA](#-api-gateway-zta)
- [Endpoints y Rutas](#-endpoints-y-rutas)
- [Seguridad](#-seguridad)
- [Términos y Condiciones](#-términos-y-condiciones)
- [Troubleshooting](#-troubleshooting)

---

## 📖 Descripción

**Voz Segura** es una plataforma de denuncias anónimas diseñada bajo los principios de **Zero Trust Architecture (ZTA)** implementada en 2026, que garantiza:

### 🎯 Características Principales

- **✅ Autenticación Unificada**: Todos los usuarios (denunciantes, staff, admin) se autentican por el mismo punto de entrada contra el **Registro Civil del Ecuador**
- **🛡️ Zero Trust Architecture**: Implementación completa de ZTA con API Gateway que valida cada petición
- **🔐 Cifrado de Extremo a Extremo**: AES-256-GCM para todas las denuncias y evidencias
- **👤 Identity Vault**: Separación total entre identidad real y denuncias
- **📱 Verificación Biométrica**: Autenticación facial (integrable con servicios reales)
- **🔑 MFA para Staff/Admin**: Clave secreta de AWS Secrets Manager
- **📊 Auditoría Completa**: Todos los accesos y acciones son registrados
- **📜 Términos y Condiciones**: Aceptación obligatoria con explicaciones claras

---

## 🏗️ Arquitectura Zero Trust (ZTA)

### Principios Implementados

1. **Never Trust, Always Verify** - Cada petición es verificada, sin importar el origen
2. **Assume Breach** - El sistema asume que puede estar comprometido
3. **Verify Explicitly** - Autenticación multifactor y continua
4. **Least Privilege Access** - Solo permisos necesarios por rol
5. **Microsegmentation** - Cada recurso está protegido individualmente

### Componentes de la Arquitectura

```
┌─────────────────────────────────────────────────────────┐
│                    REGISTRO CIVIL                       │
│              (Verificación de Identidad)                │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────────────────┐
│            PUNTO DE ENTRADA ÚNICO                       │
│         /auth/login (Todos los usuarios)                │
│    ┌──────────────────────────────────────┐            │
│    │  • Cédula + Código Dactilar          │            │
│    │  • CAPTCHA Dinámico                  │            │
│    │  • Términos y Condiciones            │            │
│    └──────────────────────────────────────┘            │
└─────────────────┬───────────────────────────────────────┘
                  │
                  ↓
┌─────────────────────────────────────────────────────────┐
│              API GATEWAY FILTER (ZTA)                   │
│     ┌────────────────────────────────────────┐         │
│     │  1. Validar Autenticación              │         │
│     │  2. Verificar Permisos                 │         │
│     │  3. Enrutar según Rol                  │         │
│     │  4. Registrar Auditoría                │         │
│     └────────────────────────────────────────┘         │
└─────────┬──────────────┬──────────────┬────────────────┘
          │              │              │
          ↓              ↓              ↓
┌─────────────┐  ┌──────────────┐  ┌──────────┐
│ DENUNCIANTE │  │   ANALYST    │  │  ADMIN   │
│             │  │              │  │          │
│ + Biométrica│  │ + AWS Secret │  │+ AWS     │
│             │  │    Key       │  │  Secret  │
└─────────────┘  └──────────────┘  └──────────┘
```

---

## 🔧 Requisitos Previos

### Software Requerido

- **Java 21** ([Descargar](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.8+** ([Descargar](https://maven.apache.org/download.cgi))
- **PostgreSQL 16** ([Descargar para Windows](https://www.postgresql.org/download/windows/))
- **Git** (opcional)

### Verificar Instalaciones

```powershell
# Verificar Java
java -version
# Debe mostrar: java version "21.x.x"

# Verificar Maven
mvn -version
# Debe mostrar: Apache Maven 3.x.x

# Verificar PostgreSQL
psql --version
# Debe mostrar: psql (PostgreSQL) 16.x
```

---

## 💻 Instalación en Windows

### Paso 1: Descargar el Proyecto

```powershell
cd "C:\SOFTWARE SEGURO"
git clone https://github.com/tu-usuario/voz-segura.git
cd voz-segura
```

O descarga el ZIP y extráelo en `C:\SOFTWARE SEGURO\vozSegura`

---

## 🗄️ Configuración PostgreSQL Local

### Paso 1: Instalar PostgreSQL 16

1. Descarga el instalador desde [postgresql.org](https://www.postgresql.org/download/windows/)
2. Ejecuta el instalador
3. Durante la instalación:
   - **Puerto**: `5432` (dejar por defecto)
   - **Superuser password**: Crea una contraseña (ej: `postgres`)
   - **Locale**: Spanish, Ecuador
4. Instala Stack Builder (opcional, para pgAdmin)

### Paso 2: Configurar pgAdmin

1. Abre **pgAdmin 4**
2. Conéctate al servidor local:
   - **Host**: localhost
   - **Port**: 5432
   - **Username**: postgres
   - **Password**: (la que configuraste)

### Paso 3: Crear la Base de Datos

Opción A - Usando pgAdmin:
1. Clic derecho en "Databases" → Create → Database
2. **Name**: `voz_segura`
3. **Owner**: postgres
4. Save

Opción B - Usando psql:
```powershell
# Abrir psql
psql -U postgres

# Crear base de datos
CREATE DATABASE voz_segura;

# Crear usuario
CREATE USER voz_segura WITH PASSWORD 'voz_segura_dev';

# Dar permisos
GRANT ALL PRIVILEGES ON DATABASE voz_segura TO voz_segura;

# Salir
\q
```

### Paso 4: Configurar la Aplicación

Edita `src/main/resources/application.yml`:

```yaml
spring:
  profiles:
    active: dev  # Cambiar de 'h2' a 'dev'
```

Edita `src/main/resources/application-dev.yml` (si necesitas cambiar credenciales):

```yaml
spring:
  datasource:
    url: jdbc:postgresql://localhost:5432/voz_segura
    username: voz_segura
    password: voz_segura_dev
    driver-class-name: org.postgresql.Driver
```

---

## 🔐 Variables de Entorno

### Variable Obligatoria: Clave de Cifrado

**Windows PowerShell:**
```powershell
$env:VOZSEGURA_DATA_KEY_B64="XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY="
```

**CMD:**
```cmd
set VOZSEGURA_DATA_KEY_B64=XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY=
```

### Variables Opcionales

```powershell
# Puerto del servidor (opcional, default: 8080)
$env:SERVER_PORT="8080"

# Perfil activo (opcional, default: h2)
$env:SPRING_PROFILES_ACTIVE="dev"
```

### Configurar Permanentemente (Opcional)

1. Buscar "Variables de entorno" en Windows
2. Variables de entorno del sistema
3. Nueva variable de usuario:
   - **Nombre**: `VOZSEGURA_DATA_KEY_B64`
   - **Valor**: `XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY=`

---

## ▶️ Ejecución

### Método 1: Maven Direct

```powershell
# Configurar variable de entorno
$env:VOZSEGURA_DATA_KEY_B64="XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY="

# Navegar al proyecto
cd "C:\SOFTWARE SEGURO\vozSegura"

# Ejecutar
mvn spring-boot:run
```

### Método 2: JAR Ejecutable

```powershell
# Compilar
mvn clean package

# Ejecutar
java -jar target\voz-segura-0.0.1-SNAPSHOT.jar
```

### Verificar que Inició Correctamente

Debes ver:
```
===========================================
 API GATEWAY ZTA INITIALIZED - 2026
 Zero Trust Architecture Active
 All requests will be verified
===========================================

Started VozSeguraApplication in X.XXX seconds

Tomcat started on port 8080 (http)
```

Accede a: **http://localhost:8080/auth/login**

---

## 🔑 Sistema de Autenticación Unificado

### Flujo Completo (ZTA)

```
1. Usuario accede a /auth/login
   ↓
2. Ingresa: Cédula + Código Dactilar + CAPTCHA
   ↓
3. Sistema verifica contra Registro Civil
   ↓
4. ¿Es Staff/Admin?
   │
   ├─→ SÍ: Solicitar Clave Secreta AWS
   │   ↓
   │   Verificar contra AWS Secrets Manager
   │   ↓
   │   Acceso a Panel Staff/Admin
   │
   └─→ NO: Continuar con Verificación Biométrica
       ↓
       Tomar fotografía facial
       ↓
       Acceso a Formulario de Denuncia
```

### Términos y Condiciones

**TODOS** los usuarios deben aceptar antes de ingresar:

✅ Datos legítimos y verídicos  
✅ Uso responsable de evidencias  
✅ Posible contacto por entidad receptora  
✅ Proceso anónimo (salvo orden judicial)  
✅ Consecuencias legales por mal uso

---

## 👥 Usuarios de Prueba

### Datos de Acceso (2026)

| Cédula | Código Dactilar | Rol | Clave Secreta AWS | Acceso |
|--------|----------------|-----|-------------------|--------|
| `1234567890` | Cualquiera (ej: `ABC123`) | **ADMIN** | `admin_secret_2026` | Panel completo |
| `0987654321` | Cualquiera (ej: `XYZ789`) | **ANALYST** | `analyst_secret_2026` | Gestión denuncias |
| Cualquier otra | Cualquiera | **DENUNCIANTE** | - | Formulario denuncia |

### Pasos para Probar

#### Como Admin:
1. Ir a http://localhost:8080/auth/login
2. Aceptar términos y condiciones
3. Ingresar:
   - **Cédula**: `1234567890`
   - **Código Dactilar**: `ABC123`
   - **CAPTCHA**: (el que aparece en pantalla)
4. Clic en "Iniciar Sesión"
5. Aparecerá pantalla de Clave Secreta
6. Ingresar: `admin_secret_2026`
7. Acceso a `/admin`

#### Como Analista:
1-4. Igual que admin, pero con cédula `0987654321`
5-6. Clave secreta: `analyst_secret_2026`
7. Acceso a `/staff/casos`

#### Como Denunciante:
1-4. Igual, pero con cualquier otra cédula (ej: `9999999999`)
5. No solicita clave secreta
6. Verificación biométrica (subir cualquier foto)
7. Acceso a formulario de denuncia

---

## 🚪 API Gateway ZTA

### Implementación

El `ApiGatewayFilter` intercepta **TODAS** las peticiones y aplica:

#### 1. Validación de Autenticación
```java
// Verifica que el usuario tenga sesión autenticada
if (session == null || session.getAttribute("authenticated") == null) {
    // BLOCKED
}
```

#### 2. Verificación de Autorización (RBAC)
```java
// Control de Acceso Basado en Roles
switch (userType) {
    case "ADMIN":    return true;  // Acceso total
    case "ANALYST":  return !isAdminPath;  // No acceso a admin
    case "DENUNCIANTE": return isDenunciaPath;  // Solo /denuncia
}
```

#### 3. Validación de Método de Autenticación
```java
// Staff/Admin DEBEN usar autenticación ZTA completa
if (isStaff && authMethod != "UNIFIED_ZTA") {
    // BLOCKED
}
```

#### 4. Headers de Seguridad
```java
X-Content-Type-Options: nosniff
X-Frame-Options: DENY
X-XSS-Protection: 1; mode=block
Strict-Transport-Security: max-age=31536000
Content-Security-Policy: default-src 'self'
```

### Logs de Auditoría

Todos los accesos se registran:
```
[API GATEWAY ZTA] GET /staff/casos | Session: abc123 | IP: 192.168.1.100
[API GATEWAY ZTA] ALLOWED - ANALYST accessing /staff/casos
```

---

## 🌐 Endpoints y Rutas

### Públicas (No requieren autenticación)

| Ruta | Descripción |
|------|-------------|
| `/auth/login` | Login unificado (punto de entrada) |
| `/auth/unified-login` | Procesar login POST |
| `/css/**`, `/js/**` | Recursos estáticos |

### Requieren Autenticación

| Ruta | Rol | Descripción |
|------|-----|-------------|
| `/auth/secret-key` | Staff/Admin | Pantalla clave secreta |
| `/denuncia/biometric` | Denunciante | Verificación biométrica |
| `/denuncia/submit` | Denunciante | Enviar denuncia |
| `/staff/casos` | Analyst, Admin | Listado denuncias |
| `/admin` | Admin | Panel administración |

---

## 🛡️ Seguridad

### Cifrado

- **Algoritmo**: AES-256-GCM
- **Modo**: Galois/Counter Mode (autenticado)
- **Scope**: Denuncias y evidencias
- **Gestión**: Variables de entorno (dev) / AWS (prod)

### Identity Vault

```
Identidad Real → SHA-256 Hash → Identity Vault
                       ↓
              (nunca se almacena)
                       
Denuncia → Linked to → Hash (no a identidad real)
```

### Autenticación Multifactor

1. **Factor 1**: Cédula + Código Dactilar (Registro Civil)
2. **Factor 2 (Denunciante)**: Biometría facial
3. **Factor 2 (Staff/Admin)**: Clave secreta AWS

### Headers de Seguridad

Implementados en API Gateway:
- Content Security Policy (CSP)
- X-Frame-Options: DENY
- X-Content-Type-Options: nosniff
- Strict-Transport-Security (HSTS)

### Auditoría

Todos los eventos se registran en `audit_event`:
- Login attempts
- Accesos a recursos
- Cambios de estado
- Derivaciones
- Revelaciones de identidad

---

## 📜 Términos y Condiciones

### Puntos Clave

1. **Legitimidad de Datos**: Todo dato debe ser verídico
2. **Uso Responsable**: Evidencias solo para fines legítimos
3. **Contacto Posible**: Entidad receptora puede solicitar contacto
4. **Anonimato Garantizado**: Salvo orden judicial
5. **Revelación Excepcional**: Solo con aprobación de Comité de Ética
6. **Protección Legal**: Ley de Datos Personales Ecuador 2026

### Aceptación Obligatoria

El checkbox de términos **DEBE** estar marcado para habilitar el botón de login.

---

## 🐛 Troubleshooting

### La aplicación inicia pero no puedo acceder a /auth/login

**Síntoma:** El API Gateway bloquea todas las peticiones

**Solución:** Esto es normal si intentas acceder sin aceptar términos. Verifica los logs:
```
[API GATEWAY ZTA] GET /auth/login | Session: XXX | IP: XXX
```

Si ves "BLOCKED", verifica que:
1. La ruta `/auth/` esté en la lista de rutas públicas
2. El navegador no tenga cache antiguo (Ctrl + F5)
3. Revisa los logs de la aplicación para más detalles

### Puerto 8080 ya en uso

```powershell
# Ver qué proceso usa el puerto
netstat -ano | findstr :8080

# Matar el proceso
taskkill /PID <PID> /F
```

### PostgreSQL no se conecta

**Verificar servicio:**
```powershell
Get-Service postgresql*
```

Si está detenido:
```powershell
Start-Service postgresql-x64-16
```

**Verificar puerto:**
```powershell
netstat -an | findstr :5432
```

**Probar conexión:**
```powershell
psql -U voz_segura -d voz_segura -h localhost
```

### Migraciones Flyway fallan

**Limpiar base de datos:**
```sql
-- Conectar a PostgreSQL
psql -U postgres

-- Eliminar y recrear
DROP DATABASE IF EXISTS voz_segura;
CREATE DATABASE voz_segura;
GRANT ALL PRIVILEGES ON DATABASE voz_segura TO voz_segura;
```

### Variable de entorno no encontrada

```powershell
# Verificar que está configurada
echo $env:VOZSEGURA_DATA_KEY_B64

# Si no aparece nada, configurarla:
$env:VOZSEGURA_DATA_KEY_B64="XP0OU/9rhJRkPgjUp1ncpQwCu+GwesQNwCQuv5gNkpY="
```

### CAPTCHA inválido

El CAPTCHA es único por sesión y se regenera en cada carga de página.
- Copiar el código exactamente como aparece
- Si da error, recarga la página para obtener uno nuevo
- Los espacios o mayúsculas/minúsculas importan

### Error al procesar autenticación

**Síntomas:**
- Vuelve al login después de enviar
- Mensaje "Error al procesar la autenticación"

**Causas comunes:**
1. **CAPTCHA incorrecto**: El código debe coincidir exactamente
2. **Sesión expirada**: Recarga la página para nueva sesión
3. **Servicios mock no funcionan**: Revisa los logs para detalles

**Solución:**
```powershell
# Ver logs detallados
mvn spring-boot:run

# Buscar líneas con [UNIFIED AUTH]
```

Los logs mostrarán:
```
[UNIFIED AUTH] Processing login for cedula: XXXXXXXXXX
[UNIFIED AUTH] Verifying identity...
[UNIFIED AUTH] Identity verified: CITIZEN-XXXXXXXXXX
[UNIFIED AUTH] User type: DENUNCIANTE
```

### Clave secreta incorrecta

**Valores mock para desarrollo:**
- Admin: `admin_secret_2026`
- Analista: `analyst_secret_2026`

---

## 📞 Soporte

### Documentación Adicional

- **README.md** - Este archivo
- **QUICKSTART.md** - Guía rápida
- **CHANGELOG.md** - Historial de cambios

### Logs

Los logs se muestran en consola. Para guardarlos:
```powershell
mvn spring-boot:run > logs.txt 2>&1
```

---

## 📄 Licencia

MIT License - 2026

---

## 🏆 Desarrollado con

- Spring Boot 3.3.4
- PostgreSQL 16
- Java 21
- Thymeleaf
- Flyway
- BCrypt
- AES-256-GCM

---

**Voz Segura - Plataforma Segura de Denuncias Anónimas**  
**Zero Trust Architecture - 2026**  
**¡Protegiendo tu identidad, protegiendo tu voz!** 🛡️


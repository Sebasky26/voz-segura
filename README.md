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
- [🚀 Setup e Instalación](#-setup-e-instalación)
  - [Requisitos Previos](#requisitos-previos)
  - [Pasos de Instalación](#pasos-de-instalación)
  - [Configuración de Supabase](#configuración-de-supabase)
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

## � Setup e Instalación

### Requisitos Previos

#### Software Requerido

- **Java 21+** ([Descargar desde Adoptium](https://adoptium.net/) o [Oracle](https://www.oracle.com/java/technologies/downloads/))
- **Maven 3.6+** ([Descargar](https://maven.apache.org/download.cgi))
- **Git** (opcional)

#### Verificar Instalaciones

```bash
# Verificar Java
java -version
# Debe mostrar: openjdk version "21.x.x" o superior

# Verificar Maven
mvn -version
# Debe mostrar: Apache Maven 3.6.x o superior

# Verificar Git
git --version
# Debe mostrar: git version 2.x.x o superior
```

⚠️ **Si no tienes Java 21 o Maven:**
- **Java 21**: Descargar desde [Adoptium](https://adoptium.net/)
- **Maven**: Descargar desde [Apache Maven](https://maven.apache.org/download.cgi)

---

### Pasos de Instalación

#### 1️⃣ Clonar el repositorio

```bash
git clone <url-del-repo>
cd voz-segura
```

#### 2️⃣ Configurar credenciales de Supabase

1. **Copia el archivo de ejemplo:**
   ```bash
   # En Linux/Mac:
   cp .env.example .env
   
   # En Windows PowerShell:
   Copy-Item .env.example .env
   ```

2. **Edita el archivo `.env`** en la raíz del proyecto:
   ```env
   SUPABASE_DB_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?sslmode=require
   SUPABASE_DB_USERNAME=postgres
   SUPABASE_DB_PASSWORD=tu-password-aqui
   VOZSEGURA_DATA_KEY_B64=clave-de-cifrado-aqui
   ```

   ⚠️ **Solicita estas credenciales al líder del proyecto por correo** (no están en Git por seguridad)

   📝 **Nota:** El proyecto usa `spring-dotenv` que carga automáticamente el archivo `.env` al iniciar.

#### 3️⃣ Ejecutar la aplicación

```bash
# Descargar dependencias (opcional, mvn spring-boot:run lo hace automáticamente)
mvn clean install

# Ejecutar la aplicación
mvn spring-boot:run
```

**O desde el IDE:**
- **IntelliJ IDEA**: Botón Run en la clase principal `VozSeguraApplication`
- **Eclipse**: Run As → Spring Boot App

La aplicación arrancará en: **http://localhost:8080**

#### ✅ Verificar que está funcionando

Deberías ver en los logs:
```
Database: jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres (PostgreSQL 17.6)
Started VozSeguraApplication in X.XXX seconds
Tomcat started on port 8080 (http)
```

---

### Configuración de Supabase

#### Arquitectura de Datos Separados

La aplicación usa **Supabase** (PostgreSQL en la nube) con **esquemas separados** para diferentes tipos de datos:

##### 📦 Esquemas de Base de Datos

1. **`public`** (schema por defecto)
   - `staff_user`: Usuarios del sistema
   - `complaint`: Denuncias (solo texto cifrado)
   - `derivation_rule`: Reglas de derivación
   - `terms_acceptance`: Aceptación de términos

2. **`secure_identities`** (datos del registro civil)
   - `identity_vault`: Solo IDs y hashes de ciudadanos
   - ⚠️ **NO guarda datos personales**, solo hashes SHA-256
   - Con Row Level Security (RLS) habilitado

3. **`evidence_vault`** (evidencias cifradas)
   - `evidence`: Archivos y contenido cifrado
   - Todo el contenido está cifrado con AES-256-GCM
   - Con RLS habilitado

4. **`audit_logs`** (logs de auditoría)
   - `audit_event`: Registro de todas las acciones
   - Con RLS habilitado para acceso restringido

##### 🔒 Seguridad de Supabase

**Cifrado en múltiples capas:**
- **Cifrado en tránsito**: SSL/TLS obligatorio (`sslmode=require`)
- **Cifrado en reposo**: Supabase cifra todos los datos en disco
- **Cifrado a nivel de aplicación**: 
  - Textos de denuncias cifrados con AES-256-GCM
  - Evidencias cifradas con AES-256-GCM
  - Hashes SHA-256 para identidades

**Row Level Security (RLS):**
- Solo la aplicación (service_role) puede acceder a datos sensibles
- Imposible acceso directo desde consola SQL sin permisos

**Separación de datos:**
- Identidades del registro civil en schema separado
- Evidencias en vault separado
- Logs de auditoría aislados

##### 🚀 Migraciones Automáticas

Las migraciones de **Flyway** crean automáticamente al iniciar la aplicación:
- ✅ Los esquemas separados (`secure_identities`, `evidence_vault`, `audit_logs`)
- ✅ Las tablas en cada schema
- ✅ Los índices de seguridad
- ✅ Las políticas RLS

**No necesitas ejecutar nada manualmente**, Flyway se encarga de todo.

##### 🔍 Verificar en Supabase

Ve a tu proyecto en Supabase:
1. **SQL Editor** → Verifica que existen los schemas: `secure_identities`, `evidence_vault`, `audit_logs`
2. **Database** → **Policies** → Verifica que RLS está habilitado

##### ⚠️ Notas Importantes

- ❌ **NUNCA** commitees el archivo `.env` con credenciales reales
- ✅ El archivo `.env` ya está en `.gitignore`
- ✅ La clave de cifrado (`VOZSEGURA_DATA_KEY_B64`) debe ser diferente en cada ambiente
- ✅ Supabase hace backups automáticos
- ✅ Revisa regularmente los `audit_logs` para detectar accesos no autorizados

---

## �🔧 Requisitos Previos

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

### Error: "Could not resolve placeholder 'SUPABASE_DB_URL'"
- **Causa:** El archivo `.env` no existe o está mal ubicado
- **Solución:** Asegúrate de que `.env` esté en la **raíz del proyecto** (mismo nivel que `pom.xml`)

### Error: "Connection refused"
- Verifica que las credenciales en `.env` sean correctas
- Asegúrate de tener conexión a internet
- Verifica que la URL de Supabase sea correcta (debe incluir `?sslmode=require`)

### Error: "Flyway validation failed"
- La base de datos ya tiene las migraciones aplicadas
- Esto es normal, la app continuará normalmente

### Las variables de entorno no se cargan
- Verifica que el archivo `.env` no tenga espacios extra en las líneas
- No uses comillas en los valores: `PASSWORD=abc123` (✅) vs `PASSWORD="abc123"` (❌)
- Reinicia el IDE después de crear el archivo `.env`

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

Para más información sobre arquitectura del sistema, consulta [ARQUITECTURA.md](ARQUITECTURA.md)

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


# Voz Segura — README (análisis del proyecto)


## 1) Cómo ejecutar el proyecto completo

El repo tiene **dos apps Spring Boot**:

- **Core** (la aplicación principal): `./` (root) → corre en **:8082**
- **Gateway** (API Gateway): `./gateway` → corre en **:8080**

### Requisitos
- Java (recomendado **17**)
- Maven
- Una base Postgres (en el proyecto se asume **Supabase Postgres**)

### Paso a paso (modo local)
1) Crea el archivo `.env` en la raíz (plantilla más abajo).
2) Exporta variables de entorno (Linux/Mac):
   ```bash
   set -a
   source .env
   set +a
   ```
3) Levanta el **Core**:
   ```bash
   mvn -f pom.xml spring-boot:run
   ```
4) En otra terminal, levanta el **Gateway**:
   ```bash
   mvn -f gateway/pom.xml spring-boot:run
   ```

### URLs rápidas
- Gateway (entrada principal): `http://localhost:8080`
- Core (solo si lo abres directo): `http://localhost:8082`

> Nota: si vas a usar **Didit** con webhooks, “localhost” no recibe webhooks desde afuera. En local normalmente se usa **ngrok** para exponer la URL pública y ponerla en `DIDIT_WEBHOOK_URL`.


## 2) `.env` esencial (limpio y agrupado)

> Deja los valores según tu entorno. Aquí van solo las claves importantes (sin comentarios largos).

```env
#####################################
# APP / PERFILES
#####################################
SPRING_PROFILES_ACTIVE=dev

#####################################
# BASE DE DATOS (Supabase Postgres)
#####################################
SUPABASE_DB_URL=jdbc:postgresql://<host>:5432/<db>
SUPABASE_DB_USERNAME=<user>
SUPABASE_DB_PASSWORD=<password>

#####################################
# SUPABASE (opcional: claves del proyecto)
#####################################
SUPABASE_PROJECT_URL=https://<project>.supabase.co
SUPABASE_ANON_KEY=
SUPABASE_SERVICE_ROLE_KEY=

#####################################
# JWT (login + cookies)
#####################################
JWT_SECRET=
JWT_EXPIRATION=86400000

#####################################
# ZTA / GATEWAY (firma interna)
#####################################
VOZSEGURA_GATEWAY_SHARED_SECRET=

#####################################
# CIFRADO PII (AES-GCM) / SECRET MANAGER
#####################################
VOZSEGURA_DATA_KEY_B64=

#####################################
# DIDIT (verificación de identidad)
#####################################
DIDIT_API_KEY=
DIDIT_WEBHOOK_SECRET_KEY=
DIDIT_WORKFLOW_ID=
DIDIT_API_URL=https://api.didit.me
DIDIT_WEBHOOK_URL=https://<tu-dominio>/webhooks/didit

#####################################
# CLOUDFLARE TURNSTILE (captcha)
#####################################
CLOUDFLARE_TURNSTILE_SITE_KEY=
CLOUDFLARE_TURNSTILE_SECRET_KEY=

#####################################
# AWS (OTP por correo con SES)
#####################################
AWS_REGION=us-east-1
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
AWS_SES_FROM_EMAIL=

#####################################
# SEED (datos iniciales para demo)
#####################################
VOZ_SEED_ENABLED=false
VOZ_SEED_FILE=docs/seed-data.example.json

VOZ_ADMIN_PASSWORD=
VOZ_ANALYST_PASSWORD=
VOZ_STAFF_ADMIN_SECRET=
VOZ_STAFF_ANALYST_SECRET=
```


## 3) Seed / datos iniciales (simulación)

El proyecto trae un ejemplo en: `docs/seed-data.example.json`.

El **seeder** vive en `com.vozsegura.seeder.DataSeeder` y se ejecuta al iniciar si:

- `VOZ_SEED_ENABLED=true`
- `VOZ_SEED_FILE` apunta al JSON

Qué carga (resumen):
- Personas del “registro civil” (tabla `registro_civil.personas`) usando **hash** de cédula.
- Identity vault (tabla `identity_vault`) con “blob” cifrado.
- Usuarios staff (admin/analyst) y datos mínimos para probar.

Importante:
- Las contraseñas y secretos del staff se toman de variables como `VOZ_ADMIN_PASSWORD`, `VOZ_ANALYST_PASSWORD`, `VOZ_STAFF_ADMIN_SECRET`, `VOZ_STAFF_ANALYST_SECRET`.
- El seeder **cifra** y/o **hashea** los datos sensibles antes de guardarlos.


## 4) Tecnologías usadas (explicado en sencillo)

- **Spring Boot**: es el motor principal del backend. Arranca el servidor, maneja rutas web, inyección de dependencias, etc.
- **Spring MVC + Thymeleaf**: para las páginas HTML (formularios de denuncia, login, panel staff/admin).
- **Spring Security**: para cookies seguras, CSRF, headers de seguridad (CSP, X-Frame-Options, etc.).
- **Spring Cloud Gateway** (módulo `gateway/`): es la “puerta de entrada”. Filtra y valida las peticiones antes de dejarlas pasar al Core.
- **JPA (Spring Data) + Hibernate**: para hablar con la base de datos con repositorios (sin escribir SQL a mano casi nunca).
- **Flyway**: aplica migraciones SQL automáticas al iniciar (carpeta `src/main/resources/db/migration`).
- **Supabase**: aquí se usa principalmente como **Postgres administrado** (la app se conecta por JDBC).
- **JWT (JSON Web Token)**: token firmado para identificar al usuario (sobre todo staff).
- **ZTA (Zero Trust Architecture)**: el Core no confía en “porque viene de adentro”, sino que valida firma y tiempo en cada request del Gateway.
- **Didit**: verificación de identidad (sesión + webhook) para confirmar a la persona sin guardar su cédula en texto plano.
- **Cloudflare Turnstile**: captcha moderno para frenar bots (se valida server-side).
- **AWS SES**: envío de OTP por correo (segundo factor).
- **Cifrado AES-GCM + hash SHA-256**: para proteger PII (cédula, nombres, correos, etc.) y mantener anonimato.


## 5) Diagrama de componentes (en palabras)

El diagrama está en `docs/c4-component-diagram.puml`. Traducido a “qué hace cada pieza”:

- **Navegador (usuario)**: abre la web, llena formularios y ve estados.
- **Gateway (Spring Cloud Gateway)**:
    - recibe TODO lo que viene de afuera,
    - revisa el **JWT** (si aplica),
    - valida que haya API Key (cuando corresponde),
    - y además firma la petición (HMAC + timestamp) para que el Core sepa que es “legítima y reciente”.
- **Core (Spring Boot app principal)**:
    - sirve las páginas (Thymeleaf),
    - expone endpoints,
    - guarda/consulta en base de datos,
    - cifra y descifra lo necesario,
    - y vuelve a validar que la request realmente venga del Gateway (ZTA).
- **Supabase Postgres**:
    - guarda denuncias, evidencias cifradas, reglas de derivación, staff, auditoría…
- **Didit**:
    - hace el “check” de identidad,
    - manda un webhook al Core cuando la verificación termina.
- **Cloudflare Turnstile**:
    - valida que no sea un bot (token del frontend → verificación server-side).
- **AWS SES**:
    - envía códigos OTP al correo para MFA.
- **(Opcional) Secret Manager / llaves**:
    - la llave AES-GCM se carga como secreto (en el código existe `SecretsManagerClient`).


## 6) Gateway + ZTA (cómo funciona en este proyecto)

### 6.1 Qué hace el Gateway (módulo `gateway/`)
La idea es que **nadie** le pegue directo al Core “porque sí”. El Gateway es el filtro principal.

En el código, la pieza importante es:
- `com.vozsegura.gateway.filter.JwtAuthenticationGatewayFilterFactory`

Lo que hace, resumido:
1) Lee el header `Authorization: Bearer <jwt>`
2) Valida firma y expiración del JWT con `jwt.secret`
3) Extrae claims importantes:
    - `sub` (usuario)
    - `role` (rol)
    - `apiKey` (llave para servicios internos)
4) Calcula una firma **HMAC-SHA256** con:
    - método HTTP
    - path
    - timestamp
    - userId
    - role
5) Inyecta headers internos:
    - `X-Gateway-User-Id`
    - `X-Gateway-User-Role`
    - `X-Gateway-Timestamp`
    - `X-Gateway-Signature`
    - (y también pasa `X-Api-Key` si aplica)

### 6.2 Qué es “ZTA” aquí (la parte Zero Trust)
ZTA en este proyecto significa: **el Core valida cada request “interna”**, aunque venga del Gateway.

En el Core hay dos filtros relevantes:
- `com.vozsegura.config.ZeroTrustGatewayFilter`
- `com.vozsegura.config.ApiGatewayFilter`

Ambos van en el mismo espíritu:
- si la request es “de API / sensible”, exige headers del Gateway,
- verifica firma,
- verifica timestamp (anti-replay).

Además, existe un validador re-usable:
- `com.vozsegura.security.GatewayRequestValidator`

Detalle importante:
- `ZeroTrustGatewayFilter` acepta una ventana de **5 minutos**.
- `GatewayRequestValidator` usa **60 segundos**.
- Esto es intencional o una inconsistencia: el README original menciona 60s, pero el filtro “ZeroTrust” deja 5 min. Si estás explicándolo en defensa, menciona esta diferencia y cuál usas como “regla final”.

### 6.3 Cómo viaja una petición (ejemplo simple)
- Usuario → Gateway:
    - manda cookie / token (según el caso)
- Gateway:
    - valida JWT
    - agrega headers + firma
- Core:
    - valida firma + timestamp (ZTA)
    - recién ahí procesa (guardar denuncia, ver casos, etc.)


## 7) Validaciones (entrada, API, credenciales, archivos)

Aquí te dejo “qué valida qué”, y **en qué clase** pasa (para que lo expliques con seguridad).

### 7.1 Validaciones de formularios (Bean Validation)
Están en `com.vozsegura.dto.forms.*` usando anotaciones como `@NotBlank`, `@Size`, `@Pattern`, `@Email`.

Ejemplos claros:
- `UnifiedLoginForm`: valida formato de cédula y código dactilar (largo y patrón).
- `ComplaintForm`: exige que el detalle tenga mínimo 50 caracteres, y valida campos de empresa.
- `TrackingForm`: exige UUID (36 chars + regex).
- `SecretKeyForm`: mínimo 8 caracteres.
- `AdditionalInfoForm`: texto entre 50 y 5000 caracteres.

Dónde se “aplican”:
- En controladores, con `@Valid` + `BindingResult` (por ejemplo en `PublicComplaintController`, `TrackingController`, `UnifiedAuthController`).

### 7.2 Validación de credenciales (staff)
Se hace principalmente en `com.vozsegura.controller.UnifiedAuthController`:

- **Password (login staff)**:
    - usa `PasswordEncoder.matches(...)`
    - cuenta intentos y bloquea temporalmente por sesión
- **Secret Key (2do factor staff)**:
    - también con `PasswordEncoder.matches(...)`
    - con contador de intentos `secretKeyAttempts` en sesión

La creación/almacenamiento seguro se ve en:
- `com.vozsegura.controller.admin.AdminController` (creación de analistas)
    - genera password temporal, lo hashea con BCrypt
    - toma secretKey de `VOZ_STAFF_ADMIN_SECRET` / `VOZ_STAFF_ANALYST_SECRET` y guarda hash en `mfaSecretEncrypted`

### 7.3 OTP (código por correo)
- Envío: `com.vozsegura.service.OtpService.sendOtp(...)`
    - valida formato básico de email
- Verificación: `com.vozsegura.service.OtpService.verifyOtp(...)`
- Cliente real: `com.vozsegura.client.aws.AwsSesOtpClient`
    - rate limiting: 3 solicitudes/minuto por destino
    - TTL: 5 minutos
    - máximo 3 intentos fallidos
    - anti-replay: token se consume una vez
- Cliente mock: `com.vozsegura.client.mock.MockOtpClient`
    - valida igual, pero no envía correo real (solo simula en memoria)

> Ojo: en el código actual, `AwsSesOtpClient` está marcado como `@Primary`, entonces en profile `dev` se usa ese primero. Para que OTP funcione en local, o configuras AWS SES, o ajustas el perfil/primary para usar el mock.

### 7.4 Validación de archivos (evidencias)
Hay dos niveles:
- `com.vozsegura.security.FileValidationService.isValidEvidence(...)` (la validación “seria”)
    - tamaño máximo
    - extensión permitida
    - MIME type permitido
    - **magic bytes** (firma real del archivo) para evitar “.pdf que es un .exe”
- `com.vozsegura.service.ComplaintService.processEvidences(...)`
    - limita a **máx 5** evidencias
    - sanitiza nombre de archivo (`sanitizeFileName`)
    - cifra el contenido antes de guardar

### 7.5 Validación de API (Gateway → Core)
En el Gateway:
- `JwtAuthenticationGatewayFilterFactory`
    - rechaza si falta token, si está mal firmado, o si le faltan claims (`sub`, `role`, `apiKey`)

En el Core:
- `ZeroTrustGatewayFilter` + `ApiGatewayFilter`
    - exigen headers `X-Gateway-*`
    - verifican firma HMAC
    - verifican timestamp (anti-replay)
- `GatewayRequestValidator` centraliza la lógica (firma + ventana)


## 8) Seguridad (JWT, XSS/CSRF, SQLi, etc.)

### 8.1 Cómo se generan los JWT (exacto en el código)
La clase es:
- `com.vozsegura.service.JwtTokenProvider`

Métodos:
- `generateToken(String subject, String role, String apiKey)`
- `generateTokenWithScopes(String subject, String role, String apiKey, List<String> scopes)`

Dónde se usan:
- En `UnifiedAuthController`, después de completar MFA, se crea el JWT y se mete en cookie con:
    - `createSecureJwtCookie(jwt)` (cookie `Authorization`, `HttpOnly`, `Secure`, `SameSite=Strict`)
    - y se borra con `createClearJwtCookie()`

Validación de JWT (cuando se necesita):
- `com.vozsegura.service.JwtValidator.validateToken(...)`
- En el Gateway también se valida con `io.jsonwebtoken` dentro del filtro.

### 8.2 Dónde está “toda la seguridad” en el código (mapa rápido)
- Headers de seguridad, CSP, CSRF, etc: `com.vozsegura.config.SecurityConfig`
- Timeout y reglas de sesión: `com.vozsegura.config.SessionTimeoutConfig` + uso de `HttpSession` en controladores
- ZTA (firma/timestamp del Gateway): `com.vozsegura.config.ZeroTrustGatewayFilter`, `com.vozsegura.config.ApiGatewayFilter`, `com.vozsegura.security.GatewayRequestValidator`
- Cifrado y hash: `com.vozsegura.security.*` + `com.vozsegura.service.CryptoService`
- Validación de archivos: `com.vozsegura.security.FileValidationService`
- Rate limiting (base): `com.vozsegura.security.RateLimiter`, `InMemoryRateLimiter`
- Auditoría: `com.vozsegura.service.AuditService` + entidad `AuditEvent`

### 8.3 XSS (Cross-Site Scripting) — mitigaciones
En este proyecto se mitiga sobre todo con:
- **Thymeleaf**: por defecto escapa contenido cuando lo imprime (y no se usa `th:utext` en templates).
- **Headers CSP**: en `SecurityConfig.securityFilterChain(...)` se define `Content-Security-Policy`.
- **Cookies HttpOnly**: el JWT va en cookie `HttpOnly`, así JS no lo puede leer fácil.
- **Sanitización de nombres de archivo**: `ComplaintService.sanitizeFileName(...)` evita caracteres raros.

### 8.4 CSRF (Cross-Site Request Forgery) — mitigaciones
- En `SecurityConfig` está **CSRF habilitado**.
- Las vistas Thymeleaf incluyen el token CSRF (`<input ... th:value="${_csrf.token}"/>`), por ejemplo:
    - `templates/auth/login.html`
    - `templates/admin/*.html`
    - `templates/public/*.html`
- Excepción: se ignora CSRF para `/webhooks/**` porque eso viene desde servicios externos (Didit).

### 8.5 SQL Injection
- El proyecto usa **Spring Data JPA** (repositorios en `com.vozsegura.repo`) y consultas con parámetros.
- No hay construcción de SQL con string concatenado ni `EntityManager` manual.
- Ejemplo seguro: `DerivationRuleRepository` usa `@Query` con parámetros (`:complaintType`, `:severity`).

### 8.6 NoSQL Injection
- En el código no hay base NoSQL (Mongo, etc.), por eso este riesgo no aplica directamente aquí.

### 8.7 Otros puntos de seguridad que sí aparecen
- Anti-replay en OTP (token se consume una vez): `AwsSesOtpClient.verifyOtp(...)`
- Anti-brute-force en OTP: max 3 intentos + rate limit
- Anti-replay en ZTA: timestamp + ventana de tiempo (`GatewayRequestValidator`, `ZeroTrustGatewayFilter`)
- Comparación “sin filtrar timing” (mejor práctica): `GatewayRequestValidator.constantTimeEquals(...)`

> Nota honesta para tu informe: en `DiditService` existe `verifyWebhookSignature(...)`, pero el controller `DiditWebhookController` actualmente no lee/valida el header de firma del webhook. La intención está, pero falta conectarlo en el endpoint (eso se puede mencionar como mejora).


## 9) Recorrido del código (clase por clase)

### 9.1 Core (aplicación principal)

### (sin package / placeholders)
**PiiEncryptionJob** (`src/main/java/com/vozsegura/migration/PiiEncryptionJob.java`)
- Qué es: Placeholder. El archivo está vacío (no implementa nada todavía) y parece reservado para una migración/job futuro de cifrado de PII.

### com.vozsegura
**VozSeguraApplication** (`src/main/java/com/vozsegura/VozSeguraApplication.java`) — @SpringBootApplication
- Qué es: Voz Segura Main Application - Ecuador Whistleblower Platform.
- Métodos clave: main

### com.vozsegura.client
**CivilRegistryClient** (`src/main/java/com/vozsegura/client/CivilRegistryClient.java`)
- Qué es: Interface para cliente del Registro Civil del Ecuador.

**ExternalDerivationClient** (`src/main/java/com/vozsegura/client/ExternalDerivationClient.java`)
- Qué es: Interface para cliente de derivación de casos a entidades externas.

**OtpClient** (`src/main/java/com/vozsegura/client/OtpClient.java`)
- Qué es: Interface para cliente OTP (One-Time Password).

**SecretsManagerClient** (`src/main/java/com/vozsegura/client/SecretsManagerClient.java`)
- Qué es: Interface para cliente de AWS Secrets Manager.

### com.vozsegura.client.aws
**AwsSecretsManagerClientImpl** (`src/main/java/com/vozsegura/client/aws/AwsSecretsManagerClientImpl.java`) — @Component, @Profile
- Qué es: AWS Secrets Manager Client - Recupera secretos de AWS Secrets Manager con caching.
- Métodos clave: init, cleanup, getSecretString, resolveSecretName, isCacheValid, invalidateCache, invalidateAllCache

**AwsSesOtpClient** (`src/main/java/com/vozsegura/client/aws/AwsSesOtpClient.java`) — @Component, @Primary, @Profile
- Qué es: AWS SES OTP Client - Genera y valida códigos OTP vía AWS SES.
- Métodos clave: init, close, sendOtp, enviarEmailSES, generarHtmlEmail, verifyOtp, maskEmail

### com.vozsegura.client.mock
**EnvSecretsManagerClient** (`src/main/java/com/vozsegura/client/mock/EnvSecretsManagerClient.java`) — @Component, @Profile
- Qué es: Environment Secrets Manager Client - Híbrido seguro para desarrollo.
- Métodos clave: init, cleanup, getSecretString, getStaffSecretFromAws

**HttpExternalDerivationClient** (`src/main/java/com/vozsegura/client/mock/HttpExternalDerivationClient.java`) — @Component, @Profile
- Qué es: HTTP External Derivation Client - Envía casos a entidades externas vía HTTP.
- Métodos clave: derivateCase

**MockCivilRegistryClient** (`src/main/java/com/vozsegura/client/mock/MockCivilRegistryClient.java`) — @Component, @Profile
- Qué es: Mock Civil Registry Client - Valida identidad ecuatoriana en memoria.
- Métodos clave: verifyCitizen, esCedulaDePrueba, verifyBiometric, getEmailForCitizen, validarCedulaEcuatoriana, validarCodigoDactilar

**MockOtpClient** (`src/main/java/com/vozsegura/client/mock/MockOtpClient.java`) — @Component, @Profile
- Qué es: Mock OTP Client - Genera y valida códigos OTP en memoria para desarrollo.
- Métodos clave: sendOtp, maskDestination, verifyOtp

### com.vozsegura.config
**ApiGatewayFilter** (`src/main/java/com/vozsegura/config/ApiGatewayFilter.java`) — @Component, @Order
- Qué es: API Gateway Filter - Valida headers de autenticación del Gateway.
- Campos/atributos: gatewayBaseUrl, gatewayRequestValidator, PUBLIC_PATHS
- Métodos clave: doFilter, isPublicPath, isAuthorized, init, destroy

**AwsDatabaseConfig** (`src/main/java/com/vozsegura/config/AwsDatabaseConfig.java`) — @Configuration, @Profile
- Qué es: Configuración de DataSource para perfiles AWS/Producción.
- Campos/atributos: secretsManagerClient, objectMapper, databaseSecretName
- Métodos clave: dataSource, getJsonValue

**CustomLogoutSuccessHandler** (`src/main/java/com/vozsegura/config/CustomLogoutSuccessHandler.java`) — @Component
- Qué es: Handler personalizado para auditar el cierre de sesión.
- Campos/atributos: log, auditService
- Métodos clave: onLogoutSuccess, getClientIp

**FlywayRepairConfig** (`src/main/java/com/vozsegura/config/FlywayRepairConfig.java`) — @Configuration
- Qué es: Configuración de Flyway que repara automáticamente migraciones fallidas.
- Campos/atributos: log
- Métodos clave: flywayMigrationStrategy

**GatewayConfig** (`src/main/java/com/vozsegura/config/GatewayConfig.java`) — @Component
- Qué es: Configuración de URLs del API Gateway.
- Campos/atributos: gatewayBaseUrl
- Métodos clave: getBaseUrl, buildUrl, getLoginUrl, getSessionExpiredUrl, redirectTo, redirectToLogin, redirectToSessionExpired

**GlobalExceptionHandler** (`src/main/java/com/vozsegura/config/GlobalExceptionHandler.java`) — @ControllerAdvice
- Qué es: Manejador global de excepciones.
- Campos/atributos: log, auditService
- Métodos clave: handleNotFound, handleGenericError, auditError, getClientIp

**GlobalModelAttributes** (`src/main/java/com/vozsegura/config/GlobalModelAttributes.java`) — @ControllerAdvice
- Qué es: Global Model Attributes - Inyecta datos comunes en todas las vistas Thymeleaf.
- Campos/atributos: gatewayConfig
- Métodos clave: gatewayUrl, loginUrl

**PgBouncerDataSourceConfig** (`src/main/java/com/vozsegura/config/PgBouncerDataSourceConfig.java`) — @Configuration
- Qué es: Configuración de DataSource optimizada para PgBouncer.
- Campos/atributos: jdbcUrl, username, password, maxPoolSize, minIdle, connectionTimeout, idleTimeout, maxLifetime
- Métodos clave: dataSource, appendPgBouncerParams

**RestClientConfig** (`src/main/java/com/vozsegura/config/RestClientConfig.java`) — @Configuration
- Qué es: Configuración para RestTemplate y ObjectMapper.
- Métodos clave: restTemplate, clientHttpRequestFactory, objectMapper

**SecurityConfig** (`src/main/java/com/vozsegura/config/SecurityConfig.java`) — @Configuration
- Qué es: Configuración de Seguridad - VOZ SEGURA CORE.
- Campos/atributos: logoutSuccessHandler
- Métodos clave: filterChain, passwordEncoder, userDetailsService

**SessionTimeoutConfig** (`src/main/java/com/vozsegura/config/SessionTimeoutConfig.java`) — @Component
- Qué es: Interceptor para configurar tiempos de sesión diferenciados por rol.
- Campos/atributos: log, ADMIN_TIMEOUT, ANALYST_TIMEOUT, CITIZEN_TIMEOUT, DEFAULT_TIMEOUT
- Métodos clave: preHandle, getTimeoutForUserType

**TimeZoneConfig** (`src/main/java/com/vozsegura/config/TimeZoneConfig.java`) — @Configuration
- Qué es: Timezone Configuration for Ecuador (America/Guayaquil UTC-5).
- Campos/atributos: ECUADOR_TIMEZONE
- Métodos clave: init

**WebMvcConfig** (`src/main/java/com/vozsegura/config/WebMvcConfig.java`) — @Configuration
- Qué es: Configuración de recursos estáticos (CSS, JS, imágenes).
- Campos/atributos: sessionValidationInterceptor, sessionTimeoutConfig
- Métodos clave: addInterceptors, addResourceHandlers

**ZeroTrustGatewayFilter** (`src/main/java/com/vozsegura/config/ZeroTrustGatewayFilter.java`) — @Component, @Order
- Qué es: Filtro Zero Trust para validar requests desde API Gateway.
- Campos/atributos: sharedSecret
- Métodos clave: doFilter, generateHmacSignature, isPublicRoute, init, destroy

### com.vozsegura.controller
**AuthController** (`src/main/java/com/vozsegura/controller/AuthController.java`) — @Controller
- Qué es: Controlador MVC para mostrar la página de inicio de sesión.
- Rutas (aprox): @GetMapping /login
- Métodos clave: login

**GlobalErrorController** (`src/main/java/com/vozsegura/controller/GlobalErrorController.java`) — @Controller
- Qué es: Controlador global de errores.
- Rutas (aprox): @RequestMapping /error
- Métodos clave: handleError, extractStatusCode, extractRequestPath, generateRequestId

**HomeController** (`src/main/java/com/vozsegura/controller/HomeController.java`) — @Controller
- Qué es: Controlador para la página de inicio de la aplicación.
- Rutas (aprox): @GetMapping /
- Métodos clave: home

**UnifiedAuthController** (`src/main/java/com/vozsegura/controller/UnifiedAuthController.java`) — @Controller, @RequestMapping
- Qué es: Controlador de autenticación unificada.
- Rutas (aprox): @RequestMapping /auth; @GetMapping /login; @GetMapping /verify-start; @GetMapping /verify-callback; @GetMapping /secret-key; @GetMapping /verify-otp; @GetMapping /logout; @PostMapping /unified-login; @PostMapping /verify-complete; @PostMapping /verify-secret; @PostMapping /verify-otp; @PostMapping /resend-otp
- Métodos clave: showLoginPage, processUnifiedLogin, startDiditVerification, handleDiditCallback, waitForVerification, completeVerification, showSecretKeyPage, verifySecretKey, showOtpPage, verifyOtp, resendOtp, logout, …

### com.vozsegura.controller.admin
**AdminController** (`src/main/java/com/vozsegura/controller/admin/AdminController.java`) — @Controller, @RequestMapping
- Qué es: Controlador del panel administrativo.
- Rutas (aprox): @RequestMapping /admin; @GetMapping , ; @GetMapping /reglas; @GetMapping /logs; @GetMapping /revelacion; @GetMapping /analistas; @PostMapping /reglas/crear; @PostMapping /reglas/{id}/editar; @PostMapping /reglas/{id}/eliminar; @PostMapping /analistas/crear; @PostMapping /analistas/{id}/toggle; @PostMapping /reglas/{id}/activar; @PostMapping /reglas/{id}/desactivar
- Métodos clave: panel, reglas, crearRegla, editarRegla, eliminarRegla, logs, translateEventType, revelacion, isAuthenticated, getUsername, emptyToNull, normalizeJson, …

### com.vozsegura.controller.publicview
**PublicComplaintController** (`src/main/java/com/vozsegura/controller/publicview/PublicComplaintController.java`) — @Controller, @SessionAttributes
- Qué es: Controlador del flujo público de denuncias anónimas.
- Rutas (aprox): @GetMapping /denuncia; @GetMapping /verification/inicio; @GetMapping /verification/start; @GetMapping /verification/callback; @GetMapping /denuncia/opciones; @GetMapping /denuncia/form; @GetMapping /denuncia/confirmacion; @GetMapping /denuncia/editar/{trackingId}; @PostMapping /denuncia/submit; @PostMapping /denuncia/editar/{trackingId}
- Métodos clave: accessForm, showAccessForm, verificationInicio, startVerification, verificationCallback, showOptions, showComplaintForm, submitComplaint, showConfirmation, showAdditionalInfoForm, submitAdditionalInfo

**TermsController** (`src/main/java/com/vozsegura/controller/publicview/TermsController.java`) — @Controller
- Qué es: Terms and Conditions Controller - Muestra términos y condiciones públicos.
- Rutas (aprox): @GetMapping /terms
- Métodos clave: terms

**TrackingController** (`src/main/java/com/vozsegura/controller/publicview/TrackingController.java`) — @Controller, @RequestMapping
- Qué es: Controlador para consulta anónima de seguimiento de denuncias.
- Rutas (aprox): @RequestMapping /seguimiento
- Métodos clave: showTrackingForm, processTracking, isOwner, getClientIp

### com.vozsegura.controller.staff
**StaffCaseController** (`src/main/java/com/vozsegura/controller/staff/StaffCaseController.java`) — @Controller, @RequestMapping
- Qué es: Controlador para gestion de casos por parte del staff.
- Rutas (aprox): @RequestMapping /staff; @GetMapping /casos,/casos-list; @GetMapping /casos/{trackingId}; @GetMapping /evidencias/{id}; @PostMapping /casos/{trackingId}/clasificar; @PostMapping /casos/{trackingId}/estado; @PostMapping /casos/{trackingId}/aprobar-derivar; @PostMapping /casos/{trackingId}/solicitar-info; @PostMapping /casos/{trackingId}/rechazar
- Métodos clave: listCases, viewCase, clasificarCaso, updateEstado, aprobarYDerivar, solicitarMasInfo, rechazarCaso, downloadEvidence, isAuthenticated, getUsername

### com.vozsegura.controller.webhook
**DiditWebhookController** (`src/main/java/com/vozsegura/controller/webhook/DiditWebhookController.java`) — @Controller, @RequestMapping
- Qué es: Recibe callbacks y webhooks de Didit.
- Rutas (aprox): @RequestMapping /webhooks; @GetMapping /didit; @PostMapping /didit
- Métodos clave: handleDiditCallbackGet, readBody, getClientIpAddress

### com.vozsegura.domain.converter
**JsonbStringConverter** (`src/main/java/com/vozsegura/domain/converter/JsonbStringConverter.java`) — @Converter
- Qué es: Converter para campos JSONB en PostgreSQL.
- Métodos clave: convertToDatabaseColumn, convertToEntityAttribute, normalizeToSafeJson

### com.vozsegura.domain.entity
**AuditEvent** (`src/main/java/com/vozsegura/domain/entity/AuditEvent.java`) — @Entity, @Table
- Qué es: Entidad JPA para auditoría de eventos del sistema.
- Campos/atributos: id, eventTime, requestId, correlationId, actorRole, actorStaffId, actorUsername, ipAddress, userAgent, httpMethod, path, statusCode, latencyMs, eventType, outcome, trackingId, entityType, entityId, details

**Complaint** (`src/main/java/com/vozsegura/domain/entity/Complaint.java`) — @Entity, @Table
- Qué es: Entidad JPA que representa una denuncia anónima almacenada en {@code denuncias.denuncia}.
- Campos/atributos: id, trackingId, identityVaultId, status, severity, complaintType, priority, derivedTo, derivedAt, requiresMoreInfo, encryptedText, companyNameEncrypted, companyEmailEncrypted, companyPhoneEncrypted, companyContactEncrypted, companyAddressEncrypted, analystNotesEncrypted, assignedStaffId, createdAt, updatedAt

**DerivationPolicy** (`src/main/java/com/vozsegura/domain/entity/DerivationPolicy.java`) — @Entity, @Table
- Qué es: Politica de derivacion almacenada en reglas_derivacion.politica_derivacion.
- Campos/atributos: id, name, legalFramework, version, effectiveFrom, effectiveTo, active, createdBy, creator, approvedBy, approver, approvedAt, createdAt, updatedAt

**DerivationRule** (`src/main/java/com/vozsegura/domain/entity/DerivationRule.java`) — @Entity, @Table
- Qué es: Regla de derivación automática almacenada en {@code reglas_derivacion.regla_derivacion}.
- Campos/atributos: id, policyId, name, description, active, complaintTypeMatch, severityMatch, conditions, destinationId, destinationEntity, priorityOrder, requiresManualReview, slaHours, normativeReference, createdAt, updatedAt

**DestinationEntity** (`src/main/java/com/vozsegura/domain/entity/DestinationEntity.java`) — @Entity, @Table
- Qué es: Entidad receptora de denuncias derivadas almacenada en {@code reglas_derivacion.entidad_destino}.
- Campos/atributos: id, code, name, description, active, emailEncrypted, phoneEncrypted, addressEncrypted, endpointUrl, createdAt, updatedAt

**DiditVerification** (`src/main/java/com/vozsegura/domain/entity/DiditVerification.java`) — @Entity, @Table
- Qué es: Registro de verificación con Didit almacenado en {@code registro_civil.didit_verification}.
- Campos/atributos: id, identityVaultId, diditSessionId, documentNumberHash, documentNumberEncrypted, verificationStatus, verifiedAt, createdAt, updatedAt, documentNumber

**Evidence** (`src/main/java/com/vozsegura/domain/entity/Evidence.java`) — @Entity, @Table
- Qué es: Evidencia asociada a una denuncia almacenada en {@code evidencias.evidencia}.
- Campos/atributos: id, idDenuncia, complaint, fileNameEncrypted, contentType, sizeBytes, encryptedContent, storageObjectKey, checksum, createdAt, updatedAt

**IdentityVault** (`src/main/java/com/vozsegura/domain/entity/IdentityVault.java`) — @Entity, @Table
- Qué es: Bóveda de identidad almacenada en {@code registro_civil.identity_vault}.
- Campos/atributos: id, documentHash, identityBlobEncrypted, keyVersion, createdAt, updatedAt

**Persona** (`src/main/java/com/vozsegura/domain/entity/Persona.java`) — @Entity, @Table
- Qué es: Identidad verificada almacenada en {@code registro_civil.personas}.
- Campos/atributos: id, identityVaultId, cedulaHash, cedulaEncrypted, primerNombreEncrypted, segundoNombreEncrypted, primerApellidoEncrypted, segundoApellidoEncrypted, nombreCompletoHash, sexo, createdAt, updatedAt, cedula, primerNombre, segundoNombre, primerApellido, segundoApellido

**StaffUser** (`src/main/java/com/vozsegura/domain/entity/StaffUser.java`) — @Entity, @Table
- Qué es: Usuario interno del sistema almacenado en {@code staff.staff_user}.
- Campos/atributos: id, username, passwordHash, role, enabled, emailEncrypted, phoneEncrypted, mfaSecretEncrypted, cedulaHashIdx, createdBy, createdAt, updatedAt, lastLoginAt

### com.vozsegura.domain.enums
**ComplaintType** (`src/main/java/com/vozsegura/domain/enums/ComplaintType.java`)
- Qué es: Tipos de denuncia del sistema.
- Métodos clave: getCode, getLabel, fromCode

**Severity** (`src/main/java/com/vozsegura/domain/enums/Severity.java`)
- Qué es: Severidad de la denuncia.
- Métodos clave: getCode, getLabel, fromCode

### com.vozsegura.dto
**ComplaintStatusDto** (`src/main/java/com/vozsegura/dto/ComplaintStatusDto.java`)
- Qué es: DTO para mostrar el estado de una denuncia al denunciante.
- Métodos clave: translateStatus, translateSeverity, translateComplaintType, getTrackingId, setTrackingId, getStatus, setStatus, getStatusLabel, getSeverity, setSeverity, getSeverityLabel, getCreatedAt, …

### com.vozsegura.dto.forms
**AdditionalInfoForm** (`src/main/java/com/vozsegura/dto/forms/AdditionalInfoForm.java`)
- Qué es: DTO para formulario de información adicional solicitada por analista.
- Campos/atributos: additionalInfo, evidences
- Métodos clave: getAdditionalInfo, setAdditionalInfo, getEvidences, setEvidences

**BiometricOtpForm** (`src/main/java/com/vozsegura/dto/forms/BiometricOtpForm.java`)
- Qué es: DTO para formulario de muestra biométrica (OTP biométrica).
- Campos/atributos: biometricSample
- Métodos clave: getBiometricSample, setBiometricSample

**ComplaintForm** (`src/main/java/com/vozsegura/dto/forms/ComplaintForm.java`)
- Qué es: DTO para el formulario de creación de denuncias públicas.
- Campos/atributos: detail, evidences, companyName, companyAddress, companyContact, companyEmail, companyPhone, additionalInfo, newEvidences
- Métodos clave: getDetail, setDetail, getEvidences, setEvidences, getCompanyName, setCompanyName, getCompanyAddress, setCompanyAddress, getCompanyContact, setCompanyContact, getCompanyEmail, setCompanyEmail, …

**DenunciaAccessForm** (`src/main/java/com/vozsegura/dto/forms/DenunciaAccessForm.java`)
- Qué es: DTO para formulario de acceso inicial a denuncias (entrada pública).
- Campos/atributos: cedula, codigoDactilar, captcha, termsAccepted
- Métodos clave: getCedula, setCedula, getCodigoDactilar, setCodigoDactilar, getCaptcha, setCaptcha, isTermsAccepted, setTermsAccepted

**SecretKeyForm** (`src/main/java/com/vozsegura/dto/forms/SecretKeyForm.java`)
- Qué es: DTO para el formulario de clave secreta (Staff/Admin - Paso 3 del MFA).
- Campos/atributos: secretKey
- Métodos clave: getSecretKey, setSecretKey

**TrackingForm** (`src/main/java/com/vozsegura/dto/forms/TrackingForm.java`)
- Qué es: DTO para formulario de seguimiento anónimo de denuncias.
- Campos/atributos: trackingId
- Métodos clave: getTrackingId, setTrackingId

**UnifiedLoginForm** (`src/main/java/com/vozsegura/dto/forms/UnifiedLoginForm.java`)
- Qué es: DTO para formulario de login unificado (entry point para todos los usuarios).
- Campos/atributos: cedula, codigoDactilar
- Métodos clave: getCedula, setCedula, getCodigoDactilar, setCodigoDactilar

### com.vozsegura.dto.webhook
**DiditWebhookPayload** (`src/main/java/com/vozsegura/dto/webhook/DiditWebhookPayload.java`) — @JsonIgnoreProperties
- Qué es: DTO para procesar el payload del webhook de Didit. Extrae los datos relevantes: nombre completo y número de cédula. /
- Campos/atributos: sessionId, status, workflowId, verificationResult, documentData, vendorData, webhookType, decision, id, livenessPassed, verifiedAt, documentType, documentNumber, personalNumber, firstName, lastName, fullName, dateOfBirth, gender, nationality, expiryDate, issueDate, idVerifications
- Métodos clave: getSessionId, setSessionId, getStatus, setStatus, getWorkflowId, setWorkflowId, getVerificationResult, setVerificationResult, getDocumentData, setDocumentData, getVendorData, setVendorData, …

### com.vozsegura.repo
**AuditEventRepository** (`src/main/java/com/vozsegura/repo/AuditEventRepository.java`) — @Repository
- Qué es: Repositorio para gestionar registros de auditoria del sistema.

**ComplaintRepository** (`src/main/java/com/vozsegura/repo/ComplaintRepository.java`) — @Repository
- Qué es: Repositorio JPA para Complaint.

**DerivationPolicyRepository** (`src/main/java/com/vozsegura/repo/DerivationPolicyRepository.java`) — @Repository
- Qué es: Repositorio JPA para acceder a la base de datos.

**DerivationRuleRepository** (`src/main/java/com/vozsegura/repo/DerivationRuleRepository.java`) — @Repository
- Qué es: Repositorio JPA para acceder a la base de datos.

**DestinationEntityRepository** (`src/main/java/com/vozsegura/repo/DestinationEntityRepository.java`) — @Repository
- Qué es: Spring Data JPA repository para entidad DestinationEntity.

**DiditVerificationRepository** (`src/main/java/com/vozsegura/repo/DiditVerificationRepository.java`)
- Qué es: Repositorio para gestionar registros de verificaciones biometricas via Didit.

**EvidenceRepository** (`src/main/java/com/vozsegura/repo/EvidenceRepository.java`) — @Repository
- Qué es: Spring Data JPA repository para entidad Evidence.

**IdentityVaultRepository** (`src/main/java/com/vozsegura/repo/IdentityVaultRepository.java`)
- Qué es: Repositorio para gestionar la boveda de identidades de ciudadanos de forma anonima.

**PersonaRepository** (`src/main/java/com/vozsegura/repo/PersonaRepository.java`)
- Qué es: Spring Data JPA repository para entidad Persona.

**StaffUserRepository** (`src/main/java/com/vozsegura/repo/StaffUserRepository.java`) — @Repository
- Qué es: Repositorio JPA para StaffUser.

### com.vozsegura.security
**AesGcmEncryptionService** (`src/main/java/com/vozsegura/security/AesGcmEncryptionService.java`) — @Service
- Qué es: Implementación de EncryptionService usando AES-256-GCM (AEAD).
- Métodos clave: loadKey, encryptToBase64, decryptFromBase64, encryptBytes, decryptBytes

**EncryptionService** (`src/main/java/com/vozsegura/security/EncryptionService.java`)
- Qué es: Interface para servicio de encriptación.

**FileValidationService** (`src/main/java/com/vozsegura/security/FileValidationService.java`) — @Service
- Qué es: Servicio de validación de archivos (evidencias).
- Métodos clave: isValidEvidence, isAllowedMimeType, isAllowedFileName, getFileExtension, isAllowedExtension, isValidMagicBytes, matchesMagicBytes

**GatewayRequestValidator** (`src/main/java/com/vozsegura/security/GatewayRequestValidator.java`) — @Component
- Qué es: Validador de autenticidad de peticiones del Gateway.
- Métodos clave: validateRequest, generateSignature, constantTimeEquals, maskCedula

**InMemoryRateLimiter** (`src/main/java/com/vozsegura/security/InMemoryRateLimiter.java`) — @Component
- Qué es: Implementacion en memoria de rate limiter (anti-brute-force, anti-DDoS).
- Métodos clave: tryConsume

**RateLimiter** (`src/main/java/com/vozsegura/security/RateLimiter.java`)
- Qué es: Interface para Rate Limiter (protección anti-brute-force y DDoS).

**SessionValidationInterceptor** (`src/main/java/com/vozsegura/security/SessionValidationInterceptor.java`) — @Component
- Qué es: Interceptor para validar sesión en rutas protegidas.
- Métodos clave: preHandle, postHandle, afterCompletion, isPublicPath, isProtectedPath, isSessionValid

### com.vozsegura.security.converter
**EncryptedStringConverter** (`src/main/java/com/vozsegura/security/converter/EncryptedStringConverter.java`) — @Converter, @Component
- Qué es: JPA AttributeConverter para cifrado automático de strings en BD.
- Métodos clave: setEncryptionService, convertToDatabaseColumn, convertToEntityAttribute

### com.vozsegura.seeder
**DataSeeder** (`src/main/java/com/vozsegura/seeder/DataSeeder.java`) — @Component, @Profile
- Qué es: Seeder de datos iniciales para Voz Segura.
- Métodos clave: run, seedCitizens, seedStaff, seedStaffUser

**DerivationPolicySeeder** (`src/main/java/com/vozsegura/seeder/DerivationPolicySeeder.java`) — @Component, @Profile, @Order
- Qué es: Seeder para crear una política de derivación por defecto si no existe ninguna.
- Métodos clave: run

**DestinationEntityDataSeeder** (`src/main/java/com/vozsegura/seeder/DestinationEntityDataSeeder.java`) — @Component, @Order
- Qué es: Seeder para actualizar datos cifrados de entidades destino.
- Métodos clave: run, seedFGE, seedCGE, seedDPE, seedMDT, seedSUPERCIAS, seedMSP, seedMINEDUC, seedSRI, seedSB, seedUAFE, updateDestination

### com.vozsegura.service
**AuditService** (`src/main/java/com/vozsegura/service/AuditService.java`) — @Service
- Qué es: Servicio de negocio (lógica principal).
- Métodos clave: logEvent, logEventWithSession, logSecurityEvent, logComplaintCreated, logComplaintAccess, logLogout, findAll, baseEvent, generateSecureUserId, hashShort, sanitizeIp, truncate, …

**CaptchaService** (`src/main/java/com/vozsegura/service/CaptchaService.java`) — @Service
- Qué es: Servicio para generar y validar CAPTCHAs de texto.
- Métodos clave: generateCaptcha, validateCaptcha, clearCaptcha

**CitizenVerificationService** (`src/main/java/com/vozsegura/service/CitizenVerificationService.java`) — @Service
- Qué es: Servicio de verificación de ciudadanos anónimos.
- Métodos clave: verifyCitizen, verifyBiometric, sendOtp, verifyOtp, hashCitizenRef

**CloudflareTurnstileService** (`src/main/java/com/vozsegura/service/CloudflareTurnstileService.java`) — @Service
- Qué es: Servicio para validar tokens de Cloudflare Turnstile (CAPTCHA moderno).
- Métodos clave: getSiteKey, verifyTurnstileToken, isSuccess, setSuccess, getChallengeTs, setChallengeTs, getHostname, setHostname, getErrorCodes, setErrorCodes

**ComplaintService** (`src/main/java/com/vozsegura/service/ComplaintService.java`) — @Service
- Qué es: Servicio principal para la gestión de denuncias.
- Métodos clave: createComplaint, processEvidences, isAllowedContentType, encryptBytes, sanitizeFileName, findByTrackingId, findAll, findAllOrderByCreatedAtDesc, findByStatus, decryptComplaintText, updateStatus, classifyComplaint, …

**CryptoService** (`src/main/java/com/vozsegura/service/CryptoService.java`) — @Service
- Qué es: Servicio centralizado de criptografía para el sistema Voz Segura.
- Métodos clave: hashCedula, hashEmail, hashNombreCompleto, encryptPII, decryptPII, generateSHA256Hex, nullToEmpty

**DerivationService** (`src/main/java/com/vozsegura/service/DerivationService.java`) — @Service
- Qué es: Servicio de negocio (lógica principal).
- Métodos clave: findAllRules, findActiveRules, findActivePolicies, findAllPolicies, findById, createRule, updateRule, deleteRule, activateRule, deactivateRule, findDestinationIdForComplaint, findEffectivePolicy, …

**DiditService** (`src/main/java/com/vozsegura/service/DiditService.java`) — @Service
- Qué es: Servicio para integración con Didit v3 - Plataforma de verificación de identidad.
- Métodos clave: createVerificationSession, verifyWebhookSignature, computeHmacSha256, processWebhookPayload, getVerificationBySessionId, getVerificationByDocumentNumber, getSessionDecisionFromDidit, processDiditDecisionResponse, extractDocumentData, buildIdentityJson

**IdentityRevealService** (`src/main/java/com/vozsegura/service/IdentityRevealService.java`) — @Service
- Qué es: Servicio para revelación excepcional y controlada de identidad de denunciante anónimo.
- Métodos clave: requestReveal, approveReveal, getRevealedIdentity, RevealedIdentity

**JwtTokenProvider** (`src/main/java/com/vozsegura/service/JwtTokenProvider.java`) — @Service
- Qué es: Servicio para generar y gestionar JWT (JSON Web Tokens).
- Métodos clave: validateConfiguration, generateToken, generateTokenWithScopes, getExpirationTime

**JwtValidator** (`src/main/java/com/vozsegura/service/JwtValidator.java`) — @Service
- Qué es: Servicio de validación JWT defensivo.
- Métodos clave: validateToken, extractCedula, extractUserType, extractApiKey, isUserType, hasScope

**OtpService** (`src/main/java/com/vozsegura/service/OtpService.java`) — @Service
- Qué es: Servicio para generar y verificar codigos OTP (One-Time Password).
- Métodos clave: sendOtp, verifyOtp

**SystemConfigService** (`src/main/java/com/vozsegura/service/SystemConfigService.java`) — @Service
- Qué es: Servicio para traducir códigos de configuración del sistema a etiquetas legibles.
- Métodos clave: translateStatus, translateSeverity, translateComplaintType, getComplaintTypesAsArray, getPrioritiesAsArray, getStatusesAsArray

**UnifiedAuthService** (`src/main/java/com/vozsegura/service/UnifiedAuthService.java`) — @Service
- Qué es: Servicio de autenticacion unificada para usuarios del sistema.
- Métodos clave: findStaffByUsername, validatePassword, getStaffEmail, hashUserId, logLoginFailed, logLoginSuccess



### 9.2 Gateway (API Gateway)

### com.vozsegura.gateway
**VozSeguraGatewayApplication** (`gateway/src/main/java/com/vozsegura/gateway/VozSeguraGatewayApplication.java`) — @SpringBootApplication
- Qué es: Aplicación principal del API Gateway
- Métodos clave: main, jwtAuthenticationGatewayFilterFactory, routeLocator

### com.vozsegura.gateway.config
**RouteSecurityConfig** (`gateway/src/main/java/com/vozsegura/gateway/config/RouteSecurityConfig.java`) — @Component
- Qué es: Configuración de seguridad por rutas del Gateway.
- Campos/atributos: PUBLIC_ROUTES
- Métodos clave: isPublicRoute, isProtectedRoute, getAllowedRoles

### com.vozsegura.gateway.filter
**ApiKeyValidationFilter** (`gateway/src/main/java/com/vozsegura/gateway/filter/ApiKeyValidationFilter.java`) — @Component
- Qué es: Filtro global para validación de API Keys en el Gateway.
- Métodos clave: filter, requiresApiKeyValidation, isValidApiKey, getRemoteAddress

**AuditLoggingFilter** (`gateway/src/main/java/com/vozsegura/gateway/filter/AuditLoggingFilter.java`) — @Component
- Qué es: Filtro global para auditoría de todas las peticiones a través del API Gateway.
- Métodos clave: filter, getRemoteAddress

**JwtAuthenticationGatewayFilterFactory** (`gateway/src/main/java/com/vozsegura/gateway/filter/JwtAuthenticationGatewayFilterFactory.java`) — @Component
- Qué es: Filtro JWT para validación de tokens en el API Gateway
- Métodos clave: validateConfiguration, apply, generateHmacSignature

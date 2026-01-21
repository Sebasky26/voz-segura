# 🔒 Seguridad de Logs - Gateway (Spring Cloud Gateway)

## ⚠️ PROBLEMA DETECTADO

### Logs Peligrosos en Gateway (Antes de la Corrección)

El Gateway en modo DEBUG exponía:

```log
❌ ANTES (PELIGROSO):
Route matched: auth-service
Mapping [Exchange: GET http://localhost:8080/auth/login?session_expired]
Sorted gatewayFilterFactories: [...detalles de filtros...]
org.springframework.cloud.gateway: DEBUG
org.springframework.security: DEBUG
reactor.netty: DEBUG (expone headers HTTP completos)
```

**Riesgos Específicos del Gateway:**
- ❌ JWT tokens en headers Authorization
- ❌ Cookies con tokens de sesión
- ❌ Headers internos (X-User-Cedula, X-Api-Key)
- ❌ Firmas HMAC en X-Gateway-Signature
- ❌ Rutas completas con parámetros sensibles
- ❌ Request/Response bodies completos
- ❌ IPs de origen y destino

## ✅ SOLUCIÓN IMPLEMENTADA

### 1. Logback Configurado para Gateway

Archivo: `gateway/src/main/resources/logback-spring.xml`

**Características de Seguridad:**
- ✅ Nivel INFO por defecto (NO DEBUG)
- ✅ Spring Cloud Gateway en INFO (NO DEBUG)
- ✅ Reactor Netty en WARN (NO DEBUG)
- ✅ Spring Security en WARN (NO DEBUG)
- ✅ Filtros de routing en WARN
- ✅ Rotación de archivos (10MB, 30 días)

### 2. application.yml Actualizado

```yaml
# ✅ Logging Seguro
logging:
  level:
    root: INFO
    com.vozsegura.gateway: INFO  # NO DEBUG
    org.springframework.cloud.gateway: INFO  # NO DEBUG
    org.springframework.security: WARN  # NO DEBUG
    reactor.netty: WARN  # NO DEBUG
```

### 3. Filtros JWT Sin Logs Sensibles

El `JwtAuthenticationGatewayFilterFactory`:
- ✅ NO loggea tokens JWT
- ✅ NO loggea headers de autenticación
- ✅ NO loggea cédulas extraídas
- ✅ NO loggea firmas HMAC
- ✅ Maneja errores sin exponer detalles

## 📊 Comparativa Gateway

| Componente | Antes (DEBUG) | Ahora (INFO) | Riesgo Eliminado |
|------------|---------------|--------------|------------------|
| JWT Tokens | ✗ En logs | ✓ Nunca loggeados | **CRÍTICO** |
| Headers Auth | ✗ Completos | ✓ Filtrados | **CRÍTICO** |
| Cookies | ✗ Visibles | ✓ No loggeadas | **ALTO** |
| X-User-Cedula | ✗ En headers | ✓ No loggeado | **CRÍTICO** |
| HMAC Signature | ✗ Visible | ✓ No loggeado | **ALTO** |
| Request Bodies | ✗ Completos | ✓ Filtrados | **MEDIO** |
| URLs completas | ✗ Con params | ✓ Solo rutas | **MEDIO** |

## 🔐 Datos Protegidos en Gateway

### NUNCA Loggear:

1. ❌ **JWT tokens** (Authorization header)
2. ❌ **Cookies de sesión** (Authorization cookie)
3. ❌ **Headers internos** (X-User-Cedula, X-Api-Key)
4. ❌ **Firmas HMAC** (X-Gateway-Signature)
5. ❌ **Timestamps de auth** (X-Auth-Time)
6. ❌ **Request/Response bodies**
7. ❌ **Parámetros de query sensibles**
8. ❌ **IPs de clientes** (excepto para auditoría)

### SÍ Loggear:

1. ✅ **Status HTTP** (200, 401, 500)
2. ✅ **Rutas sin parámetros** (/auth/*, /staff/*)
3. ✅ **Métodos HTTP** (GET, POST)
4. ✅ **Errores de gateway** (sin detalles sensibles)
5. ✅ **Métricas de latencia**

## 📝 Ejemplo de Logs Seguros (Gateway)

```log
✅ AHORA (SEGURO):
2026-01-21 02:17:48 [INFO] Netty started on port 8080 (http)
2026-01-21 02:17:48 [INFO] Started VozSeguraGatewayApplication in 2.86 seconds
2026-01-21 02:19:09 [INFO] Route matched: auth-service
```

**Sin:**
- ❌ Tokens JWT
- ❌ Headers completos
- ❌ Firmas HMAC
- ❌ Datos de usuario

## 🔧 Configuración por Ambiente

### Desarrollo (dev)
```yaml
logging.level:
  com.vozsegura.gateway: INFO  # NO DEBUG
  org.springframework.cloud.gateway: INFO
  reactor.netty: WARN
```

### Producción (prod)
```yaml
logging.level:
  root: WARN
  com.vozsegura.gateway: INFO
  org.springframework.cloud.gateway: WARN
  reactor.netty: ERROR
```

## 🚨 Puntos Críticos de Seguridad

### 1. Filtro JWT (`JwtAuthenticationGatewayFilterFactory`)

**Datos Sensibles Manejados:**
- JWT token (header Authorization)
- Cédula extraída (claims.subject)
- User type (claims.userType)
- API key (claims.apiKey)
- Firma HMAC generada

**Protección:**
- ✅ Sin logs en el código
- ✅ Excepciones genéricas (sin detalles)
- ✅ Headers agregados sin logging

### 2. Spring Cloud Gateway

**Datos Sensibles en DEBUG:**
- Request/Response completos
- Headers HTTP (incluye Authorization)
- Cookies (incluye tokens de sesión)
- URLs con query params

**Protección:**
- ✅ Nivel INFO/WARN
- ✅ No loggea detalles de intercambio
- ✅ Solo eventos de routing

### 3. Reactor Netty

**Datos Sensibles en DEBUG:**
- Headers HTTP completos
- Bodies de request/response
- Buffers de memoria

**Protección:**
- ✅ Nivel WARN
- ✅ Solo errores críticos

## 📋 Checklist de Seguridad Gateway

- [x] Logback configurado con niveles seguros
- [x] DEBUG desactivado en todos los componentes
- [x] Spring Cloud Gateway en INFO/WARN
- [x] Reactor Netty en WARN
- [x] Spring Security en WARN
- [x] JwtAuthenticationGatewayFilterFactory sin logs
- [x] Variables de entorno para secretos
- [x] Rotación de logs configurada
- [x] gateway/logs/ en .gitignore

## 🎯 Flujo de Request Seguro

```
Cliente → Gateway (sin logs) → Core (con validación)
         ↓
    JWT validado
    Headers agregados (sin logging)
    HMAC firmado
         ↓
    Forwarding al Core
```

**En cada paso:**
- ✅ NO se loggea el JWT token
- ✅ NO se loggean headers sensibles
- ✅ NO se loggea la firma HMAC
- ✅ Solo eventos de routing

## 🔄 Antes vs Ahora

### Antes (DEBUG):
```log
❌ [DEBUG] Sorted gatewayFilterFactories: [...]
❌ [DEBUG] Request: GET /auth/login
❌ [DEBUG] Headers: {Authorization: Bearer eyJ...}
❌ [DEBUG] Cookies: {Authorization: eyJ...}
❌ [DEBUG] Mutated request with headers: {X-User-Cedula: 1753848637}
```

### Ahora (INFO):
```log
✅ [INFO] Netty started on port 8080
✅ [INFO] Route matched: auth-service
✅ [INFO] Started VozSeguraGatewayApplication
```

---

**Fecha de implementación:** 2026-01-21  
**Versión:** 1.0.0  
**Estado:** ✅ Gateway protegido contra fugas de datos en logs

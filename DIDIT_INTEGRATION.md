# Integración de Didit - Verificación Biométrica

## Descripción General

Se ha implementado la integración de **Didit** como sistema de verificación biométrica, sustituyendo el anterior. Didit proporciona:

- **Escaneo de documentos**: Extrae automáticamente nombre completo y número de cédula
- **Verificación de liveness**: Detecta si es una persona viva (no una foto)
- **Verificación biométrica avanzada**: Face matching con el documento

## Datos Extraídos

Del webhook de Didit se extraen **únicamente estos datos**:

1. **document_number**: Número de cédula del documento
2. **full_name**: Nombre completo (first_name + last_name)
3. **first_name**: Primer nombre
4. **last_name**: Apellido(s)

Estos datos se almacenan en la tabla `didit_verification` en el schema `secure_identities` de Supabase.

## Configuración del Proyecto

### 1. Variables de Entorno (.env)

```bash
# Didit Configuration
DIDIT_APP_ID=50ac3777-1b76-4ae3-bd93-b0de18808b69
DIDIT_API_KEY=69e6NOZMxOdthSDXMsccDKqGjDmoZlTXjye7eL7KbmU
DIDIT_WEBHOOK_URL=https://apprehensibly-electrokinetic-kole.ngrok-free.dev/webhooks/didit
DIDIT_WEBHOOK_SECRET_KEY=UjFC1WZxAEHM50kEKi4razI-mFKl4iCgJGffK3t0rfc
DIDIT_WORKFLOW_ID=7ba10454-93d6-4aa7-ac6a-8eec2d614936
DIDIT_API_URL=https://verification.didit.me
```

### 2. Arquitectura de la Solución

```
┌─────────────────────────────────────────────────────────────┐
│ Frontend (Thymeleaf)                                       │
│ denuncia-biometric.html                                    │
│ - Crea sesión Didit                                        │
│ - Redirige a Didit para verificación                       │
│ - Verifica estado automáticamente cada 3 segundos          │
└─────────────────────────────────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Servidor Voz Segura (Puerto 8082)                          │
│                                                             │
│ GET /denuncia/biometric                                    │
│ └─ PublicComplaintController.showBiometricPage()           │
│    └─ Llama DiditService.createVerificationSession()       │
│       └─ Hace POST a https://verification.didit.me/v2/... │
│          └─ Retorna URL para verificación del usuario      │
│                                                             │
│ GET /denuncia/biometric/status                             │
│ └─ PublicComplaintController.checkVerificationStatus()     │
│    └─ Busca DiditVerification por session_id               │
│                                                             │
│ POST /denuncia/biometric/verify                            │
│ └─ PublicComplaintController.verifyDiditBiometric()        │
│    └─ Vincula verificación con hash del ciudadano          │
│       └─ Redirige a /denuncia/opciones                     │
└─────────────────────────────────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Webhook de Didit                                           │
│                                                             │
│ POST /webhooks/didit                                       │
│ └─ DiditWebhookController.handleDiditWebhook()            │
│    1. Verifica firma HMAC-SHA256 (x-signature header)      │
│    2. Procesa payload con DiditService                     │
│    3. Extrae: document_number, full_name, firstName...     │
│    4. Guarda en tabla didit_verification                   │
│    5. Retorna 200 OK a Didit                               │
└─────────────────────────────────────────────────────────────┘
                           │
                           ↓
┌─────────────────────────────────────────────────────────────┐
│ Supabase PostgreSQL                                         │
│                                                             │
│ schema: secure_identities                                  │
│ table: didit_verification                                  │
│                                                             │
│ - didit_session_id (PK)                                    │
│ - document_number                                          │
│ - full_name                                                │
│ - first_name                                               │
│ - last_name                                                │
│ - verification_status                                      │
│ - citizen_hash (vinculación con denuncia)                  │
│ - webhook_payload (auditoría)                              │
└─────────────────────────────────────────────────────────────┘
```

## Flujo de Uso

### Paso 1: Usuario Accede a Verificación Biométrica

```
GET /denuncia/biometric
```

El controlador:
1. Verifica que el usuario esté autenticado
2. Obtiene el `citizenHash` de la sesión
3. Llama `DiditService.createVerificationSession(citizenHash)`
4. Recibe URL de verificación de Didit
5. Muestra página con botón para abrir Didit

### Paso 2: Usuario Completa Verificación en Didit

El usuario:
1. Hace clic en "Verificar Identidad con Didit"
2. Se abre Didit en nueva ventana
3. Sigue instrucciones para escanear documento
4. Didit envía resultado al webhook

### Paso 3: Webhook Procesa Resultado

```
POST /webhooks/didit
Headers: x-signature: <firma HMAC-SHA256>
Body: {
  "session_id": "...",
  "status": "Completed",
  "document_data": {
    "document_number": "1234567890",
    "first_name": "Juan",
    "last_name": "Pérez",
    "full_name": "Juan Pérez"
  }
}
```

DiditWebhookController:
1. **Valida firma**: `DiditService.verifyWebhookSignature(payload, x-signature)`
2. **Procesa payload**: `DiditService.processWebhookPayload(payload, ipAddress)`
3. **Guarda datos**: Crea `DiditVerification` en BD con:
   - `documentNumber`: "1234567890"
   - `fullName`: "Juan Pérez"
   - `firstName`: "Juan"
   - `lastName`: "Pérez"
   - `verificationStatus`: "Completed"
4. **Retorna**: 200 OK

### Paso 4: Frontend Detecta Verificación

El frontend (cada 3 segundos):
1. Hace GET a `/denuncia/biometric/status`
2. Si `verified == true`, llama `submitVerification()`
3. POST a `/denuncia/biometric/verify`
4. Vincula datos con ciudadano
5. Redirige a `/denuncia/opciones`

## Archivos Creados/Modificados

### Nuevos Archivos

```
src/main/java/com/vozsegura/vozsegura/
├── domain/entity/
│   └── DiditVerification.java          # Entidad JPA para almacenar datos
├── repo/
│   └── DiditVerificationRepository.java # Repositorio JPA
├── service/
│   └── DiditService.java               # Lógica de integración con Didit
├── controller/webhook/
│   └── DiditWebhookController.java     # Receptor del webhook
├── dto/webhook/
│   └── DiditWebhookPayload.java        # DTO para deserializar webhook
└── config/
    └── RestClientConfig.java           # Configuración de RestTemplate

src/main/resources/
├── db/migration/
│   └── V9__create_didit_verification_table.sql
├── templates/public/
│   ├── denuncia-biometric.html        # Plantilla actualizada con Didit
│   └── verification-status.html       # Estado de verificación
└── application-dev.yml                 # Configuración actualizada
```

### Archivos Modificados

```
.env                                      # Variables de entorno de Didit
src/main/java/.../controller/
  publicview/PublicComplaintController.java  # Nuevos endpoints y lógica
src/main/resources/application-dev.yml    # Configuración de Didit
```

## Endpoints Principales

### Crear Sesión Didit
```http
GET /denuncia/biometric
Authorization: Required (autenticado)
Response: HTML con URL de verificación
```

### Verificar Estado
```http
GET /denuncia/biometric/status
Authorization: Required
Response: HTML indicando si la verificación está completa
```

### Confirmar Verificación
```http
POST /denuncia/biometric/verify
Authorization: Required
Response: Redirect a /denuncia/opciones
```

### Webhook de Didit
```http
POST /webhooks/didit
Headers: x-signature: <HMAC-SHA256>
Body: JSON con resultado de verificación
Response: 200 OK
```

## Seguridad

### Verificación de Firma

El webhook incluye un header `x-signature` con firma HMAC-SHA256 del payload:

```java
public boolean verifyWebhookSignature(String payload, String signature) {
    String computed = computeHmacSha256(payload, webhookSecretKey);
    return computed.equals(signature);
}
```

### Almacenamiento en Base de Datos

- Los datos se guardan en schema `secure_identities` (aislado)
- Se almacena el payload completo para auditoría
- Se captura la IP desde la que se envió el webhook
- Todos los datos se guardan como texto plano (no se cifran adicionales)

## Testing del Webhook

Para probar localmente con Didit:

1. **Exponer puerto 8082**: Usar ngrok
   ```bash
   ngrok http 8082
   ```

2. **Actualizar WEBHOOK_URL**: En .env y Didit Console
   ```bash
   DIDIT_WEBHOOK_URL=https://abc123.ngrok-free.dev/webhooks/didit
   ```

3. **Simular webhook** (para desarrollo):
   ```bash
   curl -X POST https://localhost:8082/webhooks/didit \
     -H "Content-Type: application/json" \
     -H "x-signature: <signature>" \
     -d '{
       "session_id": "test-session-123",
       "status": "Completed",
       "document_data": {
         "document_number": "1234567890",
         "first_name": "Juan",
         "last_name": "Pérez",
         "full_name": "Juan Pérez"
       }
     }'
   ```

## Migración de Base de Datos

La migración V9 crea la tabla:

```sql
CREATE TABLE secure_identities.didit_verification (
    id BIGSERIAL PRIMARY KEY,
    didit_session_id VARCHAR(255) NOT NULL UNIQUE,
    document_number VARCHAR(20) NOT NULL UNIQUE,
    full_name VARCHAR(255) NOT NULL,
    first_name VARCHAR(255),
    last_name VARCHAR(255),
    verification_status VARCHAR(50) NOT NULL,
    citizen_hash VARCHAR(128),
    verified_at TIMESTAMP WITH TIME ZONE NOT NULL,
    webhook_ip VARCHAR(45),
    webhook_payload TEXT,
    created_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP
);
```

## Próximos Pasos (Opcional)

1. **Mejorar Frontend**: Agregar transiciones suaves y mejor UX
2. **Almacenamiento de Fotos**: Opcionalmente, guardar foto escaneada
3. **Comparación Facial**: Usar face matching para verificación adicional
4. **Enriquecimiento de Datos**: Guardar más datos del documento (fecha nacimiento, etc.)
5. **Integración con Denuncia**: Usar datos de Didit en formulario de denuncia

## Recursos

- [Documentación Didit](https://docs.didit.me)
- [API Didit v2](https://docs.didit.me/api/v2)
- [Webhooks Didit](https://docs.didit.me/webhooks)
- [Business Console Didit](https://business.didit.me)

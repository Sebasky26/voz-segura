# Configuración de Seguridad de Supabase

## Arquitectura de Datos Separados

La aplicación implementa una arquitectura de seguridad con **esquemas separados** para diferentes tipos de datos:

### 📦 Esquemas de Base de Datos

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

### 🔒 Seguridad Implementada

#### Cifrado en múltiples capas:
- **Cifrado en tránsito**: SSL/TLS obligatorio (`sslmode=require`)
- **Cifrado en reposo**: Supabase cifra todos los datos en disco
- **Cifrado a nivel de aplicación**: 
  - Textos de denuncias cifrados con AES-256-GCM
  - Evidencias cifradas con AES-256-GCM
  - Hashes SHA-256 para identidades

#### Row Level Security (RLS):
- Solo la aplicación (service_role) puede acceder a datos sensibles
- Imposible acceso directo desde consola SQL sin permisos

#### Separación de datos:
- Identidades del registro civil en schema separado
- Evidencias en vault separado
- Logs de auditoría aislados

### 🚀 Configuración Inicial

#### 1. Crear proyecto en Supabase
```bash
# Ve a https://supabase.com
# Crea un nuevo proyecto
# Selecciona región: us-east-1 (o la más cercana)
```

#### 2. Configurar variables de entorno
```bash
# Copia el archivo de ejemplo
cp .env.example .env

# Edita .env con tus credenciales de Supabase
# Encuéntralas en: Project Settings -> Database
```

#### 3. Ejecutar migraciones
Las migraciones de Flyway crearán automáticamente:
- Los esquemas separados
- Las tablas en cada schema
- Los índices de seguridad
- Las políticas RLS

```bash
# Ejecuta la aplicación
mvn spring-boot:run

# Flyway ejecutará todas las migraciones automáticamente
```

#### 4. Verificar en Supabase
Ve a tu proyecto en Supabase:
- **SQL Editor** -> Verifica que existen los schemas: `secure_identities`, `evidence_vault`, `audit_logs`
- **Database** -> **Policies** -> Verifica que RLS está habilitado

### 📋 Cumplimiento de Requisitos

✅ **Bases separadas**: Usando schemas PostgreSQL  
✅ **Datos cifrados**: AES-256-GCM en aplicación + cifrado en reposo de Supabase  
✅ **Solo IDs del registro civil**: `identity_vault` solo guarda hashes SHA-256  
✅ **Logs cifrados**: En schema separado con RLS  
✅ **Evidencias cifradas**: En vault separado con cifrado + RLS

### 🔧 Configuración de Variables de Entorno

#### En desarrollo local (Windows PowerShell):
```powershell
$env:SUPABASE_DB_URL="jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?sslmode=require"
$env:SUPABASE_DB_USERNAME="postgres"
$env:SUPABASE_DB_PASSWORD="tu-password"
$env:VOZSEGURA_DATA_KEY_B64="tu-clave-base64"

mvn spring-boot:run
```

#### En producción (variables de entorno del servidor):
Configura las variables en tu servidor/contenedor:
- `SUPABASE_DB_URL`
- `SUPABASE_DB_USERNAME`
- `SUPABASE_DB_PASSWORD`
- `VOZSEGURA_DATA_KEY_B64`

### ⚠️ Notas Importantes

1. **Nunca commitees el archivo `.env`** con credenciales reales
2. **La clave de cifrado** (`VOZSEGURA_DATA_KEY_B64`) debe ser diferente en cada ambiente
3. **Backups**: Supabase hace backups automáticos, pero configura retención según tus necesidades
4. **Monitoreo**: Revisa regularmente los `audit_logs` para detectar accesos no autorizados
5. **RLS**: Las políticas RLS están configuradas para `service_role`, asegúrate de usar las credenciales correctas

### 🔍 Consultas de Verificación

```sql
-- Ver todos los schemas
SELECT schema_name FROM information_schema.schemata 
WHERE schema_name IN ('secure_identities', 'evidence_vault', 'audit_logs');

-- Verificar RLS habilitado
SELECT schemaname, tablename, rowsecurity 
FROM pg_tables 
WHERE schemaname IN ('secure_identities', 'evidence_vault', 'audit_logs');

-- Ver políticas RLS
SELECT schemaname, tablename, policyname 
FROM pg_policies 
WHERE schemaname IN ('secure_identities', 'evidence_vault', 'audit_logs');
```

# Voz Segura - Plataforma Segura de Denuncias Anonimas

**Version:** 2.0.0  
**Fecha:** Enero 2026  
**Arquitectura:** Zero Trust Architecture (ZTA) + MFA  
**Seguridad:** AWS Secrets Manager + AWS SES + Supabase

---

## Tabla de Contenidos

1. [Descripcion del Proyecto](#1-descripcion-del-proyecto)
2. [Arquitectura de Seguridad](#2-arquitectura-de-seguridad)
3. [Configuracion de AWS](#3-configuracion-de-aws)
   - [3.1 IAM - Usuarios y Permisos](#31-iam---usuarios-y-permisos)
   - [3.2 Secrets Manager - Boveda de Secretos](#32-secrets-manager---boveda-de-secretos)
   - [3.3 SES - Servicio de Email para OTP](#33-ses---servicio-de-email-para-otp)
4. [Credenciales del Equipo](#4-credenciales-del-equipo)
5. [Configuracion del Entorno Local](#5-configuracion-del-entorno-local)
6. [Ejecucion de la Aplicacion](#6-ejecucion-de-la-aplicacion)
7. [Flujo de Autenticacion MFA](#7-flujo-de-autenticacion-mfa)
8. [Usuarios de Prueba](#8-usuarios-de-prueba)
9. [Troubleshooting](#9-troubleshooting)
10. [Comandos Utiles AWS CLI](#10-comandos-utiles-aws-cli)

---

## 1. Descripcion del Proyecto

**Voz Segura** es una plataforma de denuncias anonimas disenada bajo los principios de **Zero Trust Architecture (ZTA)** con autenticacion multifactor (MFA).

### Caracteristicas Principales

- Autenticacion unificada contra Registro Civil del Ecuador
- MFA para Staff/Admin: Clave AWS + Codigo OTP por Email
- Cifrado AES-256-GCM para denuncias y evidencias
- Separacion total entre identidad y denuncias (Identity Vault)
- Auditoria completa de todos los accesos

### Stack Tecnologico

| Componente    | Tecnologia                     |
| ------------- | ------------------------------ |
| Backend       | Java 23 + Spring Boot 3.3.4    |
| Base de Datos | PostgreSQL 17 (Supabase)       |
| Secretos      | AWS Secrets Manager            |
| Email OTP     | AWS Simple Email Service (SES) |
| Cifrado       | AES-256-GCM                    |
| Frontend      | Thymeleaf + CSS                |

---

## 2. Arquitectura de Seguridad

### Diagrama de Componentes

```
                    +------------------+
                    |  Registro Civil  |
                    | (Verificacion)   |
                    +--------+---------+
                             |
                             v
+------------------------------------------------------------------+
|                    PUNTO DE ENTRADA UNICO                        |
|                      /auth/login                                 |
|   +----------------------------------------------------------+   |
|   |  Cedula + Codigo Dactilar + CAPTCHA + Terminos           |   |
|   +----------------------------------------------------------+   |
+---------------------------+--------------------------------------+
                            |
              +-------------+-------------+
              |                           |
              v                           v
    +-----------------+         +-------------------+
    |   DENUNCIANTE   |         |   STAFF / ADMIN   |
    +-----------------+         +-------------------+
              |                           |
              v                           v
    +-----------------+         +-------------------+
    | Verificacion    |         | Factor 1:         |
    | Biometrica      |         | AWS Secret Key    |
    +-----------------+         +-------------------+
              |                           |
              v                           v
    +-----------------+         +-------------------+
    | Formulario      |         | Factor 2:         |
    | Denuncia        |         | OTP via Email     |
    +-----------------+         | (AWS SES)         |
                                +-------------------+
                                          |
                                          v
                                +-------------------+
                                | Panel Staff/Admin |
                                +-------------------+
```

### Principios Zero Trust Implementados

1. **Never Trust, Always Verify** - Cada peticion es verificada
2. **Assume Breach** - El sistema asume que puede estar comprometido
3. **Verify Explicitly** - Autenticacion multifactor obligatoria
4. **Least Privilege Access** - Solo permisos necesarios por rol
5. **Microsegmentation** - Cada recurso protegido individualmente

---

## 3. Configuracion de AWS

### 3.1 IAM - Usuarios y Permisos

#### Que es IAM?

AWS Identity and Access Management (IAM) permite gestionar el acceso a los servicios de AWS de forma segura. En lugar de compartir la cuenta root, creamos usuarios individuales con permisos especificos.

#### Estructura Creada

```
AWS Account (373268394417)
    |
    +-- Grupo: VozSegura_Team
    |       |
    |       +-- Politica: VozSegura_Dev_Policy
    |       |       - secretsmanager:GetSecretValue
    |       |       - secretsmanager:DescribeSecret
    |       |
    |       +-- Politica: AmazonSESFullAccess
    |               - ses:* (envio de emails OTP)
    |
    +-- Usuarios IAM:
            |
            +-- stalin.yungan (Admin)
            +-- sebastian.aisalla
            +-- francis.velastegui
            +-- marlon.vinueza
            +-- jhoel.narvaez
            +-- vozsegura-dev-app (Aplicacion)
```

#### Tipos de Credenciales IAM

Cada usuario tiene DOS tipos de credenciales:

| Tipo                    | Archivo             | Uso                                                          |
| ----------------------- | ------------------- | ------------------------------------------------------------ |
| **Access Keys**         | `*_accessKeys.csv`  | Para codigo/CLI - Contiene Access Key ID y Secret Access Key |
| **Console Credentials** | `*_credentials.csv` | Para login en AWS Console - Usuario y password               |

#### Como usar Access Keys (Programatico)

Las Access Keys permiten que el codigo Java se conecte a AWS:

**Opcion A - Variables de Entorno (Recomendado):**

```bash
# Windows CMD
set AWS_ACCESS_KEY_ID=AKIA...
set AWS_SECRET_ACCESS_KEY=...
set AWS_REGION=us-east-1

# Windows PowerShell
$env:AWS_ACCESS_KEY_ID="AKIA..."
$env:AWS_SECRET_ACCESS_KEY="..."
$env:AWS_REGION="us-east-1"

# Linux/Mac
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
export AWS_REGION=us-east-1
```

**Opcion B - Archivo de Credenciales:**

Crear archivo `~/.aws/credentials` (o `C:\Users\TuUsuario\.aws\credentials` en Windows):

```ini
[default]
aws_access_key_id = AKIAXXXXXXXXXXXXXXXX
aws_secret_access_key = xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
```

Y archivo `~/.aws/config`:

```ini
[default]
region = us-east-1
output = json
```

#### Como usar Console Credentials (Navegador)

1. Ir a: https://373268394417.signin.aws.amazon.com/console
2. Ingresar tu usuario IAM (ej: `stalin.yungan`)
3. Ingresar tu password (del archivo `*_credentials.csv`)
4. Ya puedes navegar la consola de AWS

---

### 3.2 Secrets Manager - Boveda de Secretos

#### Que es Secrets Manager?

AWS Secrets Manager es una "caja fuerte digital" donde almacenamos credenciales sensibles. El codigo Java nunca tiene passwords hardcodeados - los obtiene de AWS al vuelo.

#### Secretos Configurados

| Nombre del Secreto            | Contenido                | Proposito              |
| ----------------------------- | ------------------------ | ---------------------- |
| `dev/VozSegura/Database`      | Credenciales DB Supabase | Conexion a PostgreSQL  |
| `STAFF_SECRET_KEY_1728848274` | Clave secreta Stalin     | MFA Factor 1 - Admin   |
| `STAFF_SECRET_KEY_1726383514` | Clave secreta Sebastian  | MFA Factor 1 - Analyst |
| `STAFF_SECRET_KEY_1754644415` | Clave secreta Francis    | MFA Factor 1 - Analyst |
| `STAFF_SECRET_KEY_1753848637` | Clave secreta Marlon     | MFA Factor 1 - Analyst |

#### Ver Secretos desde AWS Console

1. Login en AWS Console
2. Ir a **Secrets Manager** > **Secrets**
3. Click en el secreto deseado
4. Click en **Retrieve secret value**

#### Ver Secretos desde Terminal (AWS CLI)

```bash
# Listar todos los secretos
aws secretsmanager list-secrets

# Ver valor de un secreto especifico
aws secretsmanager get-secret-value --secret-id "dev/VozSegura/Database"

# Ver clave de un staff
aws secretsmanager get-secret-value --secret-id "STAFF_SECRET_KEY_1728848274"
```

---

### 3.3 SES - Servicio de Email para OTP

#### Que es SES?

AWS Simple Email Service (SES) envia los codigos OTP por email para la autenticacion MFA.

#### Configuracion Actual

- **Region:** us-east-1
- **Modo:** Sandbox (solo emails verificados)
- **Email Remitente:** stalin.yungan@epn.edu.ec

#### Emails Verificados en SES

| Email                         | Estado     | Usuario            |
| ----------------------------- | ---------- | ------------------ |
| stalin.yungan@epn.edu.ec      | Verificado | stalin.yungan      |
| mario.aisalla@epn.edu.ec      | Verificado | sebastian.aisalla  |
| francis.velastegui@epn.edu.ec | Verificado | francis.velastegui |
| marlon.vinueza@epn.edu.ec     | Verificado | marlon.vinueza     |
| roberto.narvaez@epn.edu.ec    | Verificado | jhoel.narvaez      |

#### Como Verificar un Email Nuevo

1. AWS Console > **SES** > **Verified identities**
2. Click **Create identity**
3. Seleccionar **Email address**
4. Ingresar el email
5. Click **Create identity**
6. El usuario recibe email de verificacion y debe hacer click en el enlace

---

## 4. Credenciales del Equipo

> **IMPORTANTE: ELIMINAR ESTA SECCION ANTES DE LA PRESENTACION**

### Access Keys para Desarrollo

| Usuario            | Access Key ID        | Archivo                           |
| ------------------ | -------------------- | --------------------------------- |
| stalin.yungan      | AKIAVN2EHWGYSXYPJVFN | stalin.yungan_accessKeys.csv      |
| sebastian.aisalla  | (ver archivo)        | sebastian.aisalla_accessKeys.csv  |
| francis.velastegui | (ver archivo)        | francis.velastegui_accessKeys.csv |
| marlon.vinueza     | (ver archivo)        | marlon.vinueza_accessKeys.csv     |
| jhoel.narvaez      | (ver archivo)        | jhoel.narvaez_accessKeys.csv      |

### Claves Secretas Staff (AWS Secrets Manager)

| Cedula     | Usuario            | Rol     | Clave Secreta         |
| ---------- | ------------------ | ------- | --------------------- |
| 1728848274 | stalin.yungan      | ADMIN   | VozSegura2026Admin!   |
| 1726383514 | sebastian.aisalla  | ADMIN   | VozSegura2026Admin!   |
| 1754644415 | francis.velastegui | ANALYST | VozSegura2026Analyst! |
| 1753848637 | marlon.vinueza     | ANALYST | VozSegura2026Analyst! |

Jhoel Narvaez -> Usuario final

### Emails para OTP

| Cedula     | Email (recibe OTP)            |
| ---------- | ----------------------------- |
| 1728848274 | stalin.yungan@epn.edu.ec      |
| 1726383514 | mario.aisalla@epn.edu.ec      |
| 1754644415 | francis.velastegui@epn.edu.ec |
| 1753848637 | marlon.vinueza@epn.edu.ec     |

---

## 5. Configuracion del Entorno Local

### Requisitos Previos

- Java 21+ (recomendado Java 23)
- Maven 3.6+
- Git
- AWS CLI (opcional pero recomendado)

### Verificar Instalaciones

```bash
java -version      # Java 21+ requerido
mvn -version       # Maven 3.6+ requerido
aws --version      # AWS CLI (opcional)
```

### Paso 1: Clonar el Repositorio

```bash
git clone https://github.com/tu-repo/voz-segura.git
cd voz-segura
```

### Paso 2: Configurar Credenciales AWS

Usar UNA de estas opciones:

**Opcion A - Variables de Entorno:**

```bash
# Windows PowerShell
$env:AWS_ACCESS_KEY_ID="TU_ACCESS_KEY_ID"
$env:AWS_SECRET_ACCESS_KEY="TU_SECRET_ACCESS_KEY"
$env:AWS_REGION="us-east-1"
```

**Opcion B - Archivo ~/.aws/credentials:**

```ini
[default]
aws_access_key_id = TU_ACCESS_KEY_ID
aws_secret_access_key = TU_SECRET_ACCESS_KEY
```

### Paso 3: Crear archivo .env

Crear archivo `.env` en la raiz del proyecto:

```env
# Base de Datos Supabase
DB_HOST=aws-0-us-west-2.pooler.supabase.com
DB_PORT=6543
DB_NAME=postgres
DB_USERNAME=postgres.tuproyecto
DB_PASSWORD=tu_password_supabase

# AWS SES
AWS_SES_FROM_EMAIL=stalin.yungan@epn.edu.ec
```

> Nota: Solicita las credenciales de Supabase al lider del proyecto.

### Paso 4: Verificar Conexion AWS

```bash
aws sts get-caller-identity
```

Debe mostrar tu usuario IAM:

```json
{
  "UserId": "AIDAXXXXXXXXXX",
  "Account": "373268394417",
  "Arn": "arn:aws:iam::373268394417:user/tu.usuario"
}
```

---

## 6. Ejecucion de la Aplicacion

### Compilar y Ejecutar

```bash
# Compilar
mvn clean compile

# Ejecutar
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

### Verificar que Inicio Correctamente

Buscar en los logs:

```
[AWS SES] Client initialized successfully
[HYBRID] AWS Secrets Manager connected - STAFF secrets secured
Started VozSeguraApplication in XX.XXX seconds
Tomcat started on port 8080
```

### Acceder a la Aplicacion

- URL: http://localhost:8080/auth/login

---

## 7. Flujo de Autenticacion MFA

### Para Staff/Admin

```
1. Ir a http://localhost:8080/auth/login
2. Aceptar terminos y condiciones
3. Ingresar:
   - Cedula (ej: 1728848274)
   - Codigo Dactilar (cualquier valor)
   - CAPTCHA (copiar el codigo mostrado)
4. Click "Iniciar Sesion"
5. Sistema detecta que es Staff -> Redirige a MFA
6. Se envia codigo OTP al email registrado
7. Ingresar:
   - Clave de Verificacion (AWS Secret Key)
   - Codigo OTP (6 digitos del email)
8. Click "Confirmar Acceso"
9. Acceso al panel segun rol
```

### Para Denunciante

```
1. Ir a http://localhost:8080/auth/login
2. Aceptar terminos y condiciones
3. Ingresar:
   - Cedula (cualquier cedula NO registrada como staff)
   - Codigo Dactilar (cualquier valor)
   - CAPTCHA
4. Click "Iniciar Sesion"
5. Sistema detecta que es Denunciante
6. Verificacion biometrica (subir foto)
7. Acceso al formulario de denuncia
```

---

## 8. Usuarios de Prueba

### Staff Registrado

| Cedula     | Nombre             | Rol     | Email OTP                     |
| ---------- | ------------------ | ------- | ----------------------------- |
| 1728848274 | Stalin Yungan      | ADMIN   | stalin.yungan@epn.edu.ec      |
| 1726383514 | Sebastian Aisalla  | ANALYST | mario.aisalla@epn.edu.ec      |
| 1754644415 | Francis Velastegui | ANALYST | francis.velastegui@epn.edu.ec |
| 1753848637 | Marlon Vinueza     | ANALYST | marlon.vinueza@epn.edu.ec     |

### Denunciantes (Cualquier otra cedula)

Cualquier cedula que NO este en la tabla de staff sera tratada como denunciante.
Ejemplo: `9999999999`

---

## 9. Troubleshooting

### Error: "User is not authorized to perform ses:SendEmail"

**Causa:** El usuario IAM no tiene permisos de SES.

**Solucion:**

1. AWS Console > IAM > Users > tu.usuario
2. Add permissions > Attach policies directly
3. Buscar y seleccionar `AmazonSESFullAccess`
4. Save

### Error: "RATE LIMIT: Demasiadas solicitudes"

**Causa:** Se enviaron mas de 3 OTPs en 1 minuto.

**Solucion:** Esperar 1 minuto o reiniciar la aplicacion.

### Error: "Clave secreta incorrecta"

**Causa:** La clave ingresada no coincide con AWS Secrets Manager.

**Verificar:**

```bash
aws secretsmanager get-secret-value --secret-id "STAFF_SECRET_KEY_TU_CEDULA"
```

### Error: "Codigo OTP incorrecto"

**Causas:**

- El codigo expiro (5 minutos de validez)
- Se ingreso incorrectamente
- Se reenvio y el codigo anterior ya no es valido

**Solucion:** Click en "Reenviar codigo" y usar el nuevo.

### Error: "Could not resolve placeholder"

**Causa:** El archivo `.env` no existe o faltan variables.

**Solucion:** Verificar que `.env` exista en la raiz del proyecto con todas las variables.

### La aplicacion no conecta a AWS

**Verificar credenciales:**

```bash
aws sts get-caller-identity
```

Si falla, las credenciales no estan configuradas correctamente.

---

## 10. Comandos Utiles AWS CLI

### Secrets Manager

```bash
# Listar secretos
aws secretsmanager list-secrets

# Ver un secreto
aws secretsmanager get-secret-value --secret-id "nombre-secreto"

# Crear un secreto
aws secretsmanager create-secret --name "nombre" --secret-string "valor"

# Actualizar un secreto
aws secretsmanager update-secret --secret-id "nombre" --secret-string "nuevo-valor"
```

### SES

```bash
# Listar identidades verificadas
aws ses list-identities

# Verificar un email
aws ses verify-email-identity --email-address "email@ejemplo.com"

# Enviar email de prueba
aws ses send-email \
    --from "remitente@ejemplo.com" \
    --destination "ToAddresses=destino@ejemplo.com" \
    --message "Subject={Data=Test},Body={Text={Data=Mensaje de prueba}}"
```

### IAM

```bash
# Ver usuario actual
aws sts get-caller-identity

# Listar usuarios
aws iam list-users

# Ver permisos de un usuario
aws iam list-attached-user-policies --user-name tu.usuario
```

---

## Arquitectura de Archivos Importantes

```
voz-segura/
├── .env                          # Variables locales (NO commitear)
├── .env.example                  # Plantilla de variables
├── pom.xml                       # Dependencias Maven
├── src/
│   ├── main/
│   │   ├── java/.../
│   │   │   ├── client/
│   │   │   │   ├── aws/
│   │   │   │   │   ├── AwsSesOtpClient.java      # Cliente OTP via SES
│   │   │   │   │   └── AwsSecretsManagerClient.java
│   │   │   │   └── mock/
│   │   │   │       └── EnvSecretsManagerClient.java  # Cliente hibrido
│   │   │   ├── config/
│   │   │   │   ├── ApiGatewayFilter.java         # Filtro ZTA
│   │   │   │   └── SecurityConfig.java
│   │   │   ├── controller/
│   │   │   │   └── UnifiedAuthController.java    # Login + MFA
│   │   │   └── service/
│   │   │       └── UnifiedAuthService.java       # Logica autenticacion
│   │   └── resources/
│   │       ├── application.yml
│   │       ├── application-dev.yml
│   │       └── templates/
│   │           └── auth/
│   │               ├── login.html
│   │               └── secret-key.html           # Pantalla MFA
└── README.md
```

---

## Contacto y Soporte

Para dudas sobre la configuracion:

- Lider Tecnico: Stalin Yungan (stalin.yungan@epn.edu.ec)

---

**Voz Segura - Plataforma de Denuncias Confidenciales**  
**Zero Trust Architecture + MFA - Enero 2026**  
**Escuela Politecnica Nacional**

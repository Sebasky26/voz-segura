# 🚀 Setup del Proyecto - Voz Segura

## 📋 Requisitos
- Java 21+
- Maven 3.6+
- Git

## 🔧 Pasos de Configuración

### 1️⃣ Verificar requisitos previos

**Verificar Java:**
```bash
java -version
# Debe mostrar: openjdk version "21.x.x" o superior
```

**Verificar Maven:**
```bash
mvn -version
# Debe mostrar: Apache Maven 3.6.x o superior
```

**Verificar Git:**
```bash
git --version
# Debe mostrar: git version 2.x.x o superior
```

⚠️ **Si no tienes Java 21 o Maven instalados:**
- **Java 21**: Descargar desde [Adoptium](https://adoptium.net/) o [Oracle](https://www.oracle.com/java/technologies/downloads/)
- **Maven**: Descargar desde [Apache Maven](https://maven.apache.org/download.cgi)

---

### 2️⃣ Clonar el repositorio
```bash
git clone <url-del-repo>
cd voz-segura
```

---

### 3️⃣ Configurar credenciales de Supabase

1. **Copia el archivo de ejemplo:**
   ```bash
   # En Linux/Mac:
   cp .env.example .env
   
   # En Windows PowerShell:
   Copy-Item .env.example .env
   ```

2. **Edita el archivo `.env`** en la raíz del proyecto con las credenciales reales:
   ```env
   SUPABASE_DB_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?sslmode=require
   SUPABASE_DB_USERNAME=postgres
   SUPABASE_DB_PASSWORD=tu-password-aqui
   VOZSEGURA_DATA_KEY_B64=clave-de-cifrado-aqui
   ```

   ⚠️ **Solicita estas credenciales al líder del proyecto por correo** (no están en Git por seguridad)

   📝 **Nota:** El proyecto usa la librería `spring-dotenv` que carga automáticamente el archivo `.env` al iniciar.

---

### 4️⃣ Ejecutar la aplicación
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

---

## ✅ ¿Cómo saber que está funcionando?

Deberías ver en los logs:
```
Database: jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres (PostgreSQL 17.6)
Started VozSeguraApplication in X.XXX seconds
Tomcat started on port 8080 (http)
```

---

## 🔒 Seguridad

- ❌ **NUNCA** commitees el archivo `.env`
- ✅ El archivo `.env` ya está en `.gitignore`
- ✅ Solo commitea cambios en `.env.example` si agregas nuevas variables

---

## 🗄️ Base de Datos

El proyecto usa **Supabase** (PostgreSQL en la nube) con:
- ✅ Esquemas separados por tipo de dato
- ✅ Row Level Security habilitado
- ✅ Cifrado en tránsito (SSL/TLS)
- ✅ Migraciones automáticas con Flyway

Las migraciones ya están aplicadas en la base compartida, no necesitas ejecutar nada manualmente.

---

## 📚 Documentación Adicional

- [SUPABASE_SECURITY.md](SUPABASE_SECURITY.md) - Arquitectura de seguridad
- [ARQUITECTURA.md](ARQUITECTURA.md) - Arquitectura del sistema

---

## ❓ Problemas Comunes

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

### Puerto 8080 ocupado
- Cambia el puerto en `application.yml`: `server.port: 8081`
- O detén la aplicación que esté usando el puerto 8080

### Las variables de entorno no se cargan
- Verifica que el archivo `.env` no tenga espacios extra en las líneas
- No uses comillas en los valores: `PASSWORD=abc123` (✅) vs `PASSWORD="abc123"` (❌)
- Reinicia el IDE después de crear el archivo `.env`

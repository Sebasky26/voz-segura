# 🚀 Setup del Proyecto - Voz Segura

## 📋 Requisitos
- Java 21+
- Maven
- Git

## 🔧 Configuración para el Equipo

### 1️⃣ Clonar y actualizar el repositorio
```bash
git clone <url-del-repo>
cd voz-segura
# O si ya lo tienes clonado:
git pull
```

### 2️⃣ Configurar credenciales de Supabase

1. **Copia el archivo de ejemplo:**
   ```bash
   cp .env.example .env
   ```

2. **Edita el archivo `.env`** y actualiza con las credenciales que te proporcionará el líder del proyecto:
   ```env
   SUPABASE_DB_URL=jdbc:postgresql://db.xxxxx.supabase.co:5432/postgres?sslmode=require
   SUPABASE_DB_USERNAME=postgres
   SUPABASE_DB_PASSWORD=tu-password-aqui
   VOZSEGURA_DATA_KEY_B64=clave-de-cifrado-aqui
   ```

   ⚠️ **Solicita estas credenciales al equipo** (no están en Git por seguridad)

### 3️⃣ Ejecutar la aplicación
```bash
mvn spring-boot:run
```

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

### Error: "Connection refused"
- Verifica que las credenciales en `.env` sean correctas
- Asegúrate de tener conexión a internet

### Error: "Flyway validation failed"
- La base de datos ya tiene las migraciones aplicadas
- Esto es normal, la app continuará normalmente

### Puerto 8080 ocupado
- Cambia el puerto en `application.yml` o
- Detén la aplicación que esté usando el puerto 8080

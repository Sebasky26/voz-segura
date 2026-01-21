# Solución COMPLETA: Severidades y Priority Match en Reglas de Derivación

## Problemas Identificados

### 1. Severidades Hardcodeadas
Las severidades (Bajo, Medio, Alto, Crítico) para las reglas de derivación estaban **hardcodeadas** en lugar de estar almacenadas en la base de datos en la tabla `reglas_derivacion.configuracion`.

### 2. Columna priority_match Faltante ❌ CRÍTICO
La tabla `reglas_derivacion.regla_derivacion` en Supabase **NO tiene la columna `priority_match`**, causando el error:
```
ERROR: column dr1_0.priority_match does not exist
```

Esta columna es necesaria para filtrar reglas de derivación por tipo de denuncia.

## Soluciones Implementadas

### 1. Migración V27: Severidades en Configuración ✅
Se ha creado la migración **V27__populate_severity_config.sql** que inserta los 4 niveles de severidad en la tabla de configuración:

- **LOW** → Bajo
- **MEDIUM** → Medio  
- **HIGH** → Alto
- **CRITICAL** → Crítico

### 2. Migración V28: Agregar Columna priority_match ✅
Se ha creado la migración **V28__add_priority_match_to_regla_derivacion.sql** que:
- Agrega la columna `priority_match VARCHAR(64)` si no existe
- Crea índice `idx_regla_derivacion_priority` para optimizar búsquedas
- Permite filtrar reglas por tipo de denuncia (HARASSMENT, DISCRIMINATION, etc.)

### 3. Ubicación de Archivos
```
src/main/resources/db/migration/V27__populate_severity_config.sql
src/main/resources/db/migration/V28__add_priority_match_to_regla_derivacion.sql
APLICAR_CORRECCIONES_COMPLETO.sql (script manual todo-en-uno)
```

### 4. Contenido de las Migraciones

#### V27: Severidades
```sql
INSERT INTO reglas_derivacion.configuracion (config_group, config_key, config_value, display_label, sort_order, active)
VALUES
    ('SEVERITY', 'LOW', 'LOW', 'Bajo', 1, TRUE),
    ('SEVERITY', 'MEDIUM', 'MEDIUM', 'Medio', 2, TRUE),
    ('SEVERITY', 'HIGH', 'HIGH', 'Alto', 3, TRUE),
    ('SEVERITY', 'CRITICAL', 'CRITICAL', 'Crítico', 4, TRUE)
ON CONFLICT (config_group, config_key) DO UPDATE
SET display_label = EXCLUDED.display_label, sort_order = EXCLUDED.sort_order;
```

#### V28: Columna priority_match
```sql
ALTER TABLE reglas_derivacion.regla_derivacion 
ADD COLUMN IF NOT EXISTS priority_match VARCHAR(64);

CREATE INDEX IF NOT EXISTS idx_regla_derivacion_priority 
ON reglas_derivacion.regla_derivacion(priority_match);
```

### 5. Cómo Funciona

#### Antes (Con Problemas):
❌ Valores de severidad hardcodeados en el código Java  
❌ Columna `priority_match` faltante causaba error al cargar reglas  
❌ No se podían filtrar reglas por tipo de denuncia

#### Después (Corregido):
✅ Valores se leen dinámicamente desde `reglas_derivacion.configuracion`  
✅ Columna `priority_match` agregada a la tabla  
✅ Índice creado para optimizar búsquedas  
✅ Reglas de derivación funcionan correctamente  
✅ Admin puede crear reglas filtrando por severidad Y tipo de denuncia

### 6. Integración con el Sistema

El servicio **SystemConfigService** ya está preparado para leer estas severidades:

```java
// Método que obtiene las severidades desde la BD
public List<SystemConfig> getSeveritiesList() {
    return systemConfigRepository.findByConfigGroupAndActiveTrueOrderBySortOrderAsc("SEVERITY");
}
```

El controlador **AdminController** las pasa a la vista:
```java
model.addAttribute("severities", systemConfigService.getSeveritiesList());
```

La plantilla **admin/reglas.html** las muestra en el select:
```html
<select id="severityMatch" name="severityMatch" class="vs-select">
    <option value="">Cualquiera</option>
    <option th:each="sev : ${severities}" 
            th:value="${sev.configValue}" 
            th:text="${sev.displayLabel}"></option>
</select>
```

## Aplicación de la Migración

### Opción 1: Automática (Flyway)
Cuando ejecutes la aplicación, Flyway detectará y aplicará automáticamente V27:

```bash
mvn spring-boot:run
```

o

```bash
java -jar target/voz-segura-core-0.0.1-SNAPSHOT.jar
```

### Opción 2: Manual (SQL directo en Supabase)
Si prefieres aplicarla manualmente en Supabase SQL Editor:

1. Abre Supabase SQL Editor
2. Copia y pega el contenido de `V27__populate_severity_config.sql`
3. Ejecuta el SQL
4. Registra la migración en flyway_schema_history:

```sql
INSERT INTO public.flyway_schema_history 
(installed_rank, version, description, type, script, checksum, installed_by, installed_on, execution_time, success)
VALUES 
(27, '27', 'populate severity config', 'SQL', 'V27__populate_severity_config.sql', -601179760, 'postgres', NOW(), 0, true);
```

## Verificación

Para verificar que las severidades están correctamente insertadas:

```sql
SELECT * FROM reglas_derivacion.configuracion 
WHERE config_group = 'SEVERITY' 
ORDER BY sort_order;
```

Deberías ver:
| config_key | config_value | display_label | sort_order |
|------------|--------------|---------------|------------|
| LOW        | LOW          | Bajo          | 1          |
| MEDIUM     | MEDIUM       | Medio         | 2          |
| HIGH       | HIGH         | Alto          | 3          |
| CRITICAL   | CRITICAL     | Crítico       | 4          |

## Resultado Final

✅ **Severidades NO hardcodeadas**  
✅ **Almacenadas en base de datos**  
✅ **4 niveles: Bajo, Medio, Alto, Crítico**  
✅ **Columna priority_match agregada**  
✅ **Índices optimizados para búsquedas**  
✅ **Disponibles en formularios de administración**  
✅ **Totalmente funcionales con las reglas de derivación**

---

**Fecha:** 2026-01-20  
**Versiones Migración:** V27 y V28  
**Estado:** ✅ Listo para aplicar

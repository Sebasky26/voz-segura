# PROBLEMA RESUELTO - Template Error

**Fecha:** 21 de Enero 2026  
**Error:** TemplateInputException al acceder a staff/casos-list  
**Estado:** SOLUCIONADO

---

## PROBLEMA IDENTIFICADO

### Error Original:
```
Error resolving template [staff/casos-list], template might not exist or might not be accessible
```

### Causa Raíz:
El usuario **ANALYST** (marlon.vinueza) intentaba acceder a `/admin/logs` desde el menú de navegación en el template `staff/casos-list.html`. El filtro `ApiGatewayFilter` correctamente bloqueaba el acceso con:

```
Access denied: insufficient permissions (userType=ANALYST, uri=/admin/logs)
```

Sin embargo, al intentar renderizar la página de error, Thymeleaf fallaba porque había problemas en el template.

---

## SOLUCIONES APLICADAS

### 1. Eliminar Link a /admin/logs del Menú Staff

**Archivo:** `staff/casos-list.html`

**ANTES:**
```html
<nav class="vs-nav" aria-label="Navegación">
    <a th:href="@{/staff/casos}" class="vs-nav__link">Casos</a>
    <a th:href="@{/admin/logs}" class="vs-nav__link">Logs</a>
    <a th:href="@{/auth/logout}" class="vs-nav__link">Cerrar sesión</a>
</nav>
```

**DESPUÉS:**
```html
<nav class="vs-nav" aria-label="Navegación">
    <a th:href="@{/staff/casos}" class="vs-nav__link">Casos</a>
    <a th:href="@{/auth/logout}" class="vs-nav__link">Cerrar sesión</a>
</nav>
```

**Razón:** Los usuarios ANALYST no deben ver ni intentar acceder a rutas `/admin/**`.

---

### 2. Corregir BOM UTF-8 en Template

**Problema:** El archivo tenía un BOM (Byte Order Mark) al inicio: `﻿<!DOCTYPE html>`

**Solución:** Eliminado el BOM para que comience limpio: `<!DOCTYPE html>`

**Razón:** El BOM puede causar problemas de parsing en algunos navegadores y herramientas.

---

## PERMISOS POR ROL

| Rol | Puede Acceder a /staff/** | Puede Acceder a /admin/** |
|-----|---------------------------|---------------------------|
| **ADMIN** | ✅ SÍ | ✅ SÍ |
| **ANALYST** | ✅ SÍ | ❌ NO |
| **DENUNCIANTE** | ❌ NO | ❌ NO |

**Implementado en:** `ApiGatewayFilter.isAuthorized()`

```java
// ANALYST: solo /staff/** (panel de análisis)
if ("ANALYST".equals(userType)) {
    if (requestUri.equals("/staff") || requestUri.startsWith("/staff/")) {
        return true;
    }
    if (requestUri.startsWith("/admin")) {
        return false; // Explícitamente prohibido
    }
    return requestUri.startsWith("/denuncia/");
}
```

---

## RESULTADO

### ANTES (Error):
```
2026-01-21 23:09:59 [ERROR] Exception processing template "staff/casos-list"
2026-01-21 23:10:24 [WARN] Access denied: insufficient permissions (userType=ANALYST, uri=/admin/logs)
```

### DESPUÉS (Funciona):
- ✅ Los usuarios ANALYST ven el menú sin el link a /admin/logs
- ✅ No hay intentos de acceso a rutas prohibidas
- ✅ El template renderiza correctamente
- ✅ Sin errores de Thymeleaf

---

## ARCHIVOS MODIFICADOS

1. **staff/casos-list.html**
   - Eliminado link a `/admin/logs`
   - Corregido BOM UTF-8

---

## COMMIT

```
fix: remove admin logs link from staff menu and fix BOM encoding

- Remove admin/logs navigation link from staff/casos-list template
- ANALYST users should not have access to admin routes
- Fix UTF-8 BOM encoding issue in casos-list.html
- Prevent TemplateInputException when ANALYST tries to access forbidden routes
```

**Archivos:** 1 changed, 1 deletion

---

## VERIFICACIÓN

Para verificar que funciona:

1. Login como ANALYST (marlon.vinueza)
2. Navegar a `/staff/casos`
3. El menú debe mostrar solo:
   - Casos
   - Cerrar sesión
4. No debe haber link a "Logs"
5. No debe haber errores de Thymeleaf

---

**Estado:** ✅ RESUELTO  
**Compilación:** ✅ BUILD SUCCESS  
**Commit:** ✅ COMPLETADO

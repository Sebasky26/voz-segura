# Script para cargar variables del .env y ejecutar Spring Boot localmente
# Uso: .\run-local.ps1

# Cargar todas las variables del archivo .env desde la raíz
Get-Content "$PSScriptRoot/../.env" | ForEach-Object {
    if ($_ -match '^(\s*#|\s*$)') { return } # Saltar comentarios y líneas vacías
    $parts = $_ -split '=', 2
    if ($parts.Length -eq 2) {
        $key = $parts[0].Trim()
        $value = $parts[1].Trim()
        [System.Environment]::SetEnvironmentVariable($key, $value, 'Process')
    }
}

# Ejecutar la app Spring Boot
mvn spring-boot:run

package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;

/**
 * Configuración dinámica del sistema (schema reglas_derivacion.configuracion)
 * 
 * Almacena valores en base de datos en lugar de hardcodearlos:
 * - Tipos de denuncia
 * - Niveles de severidad
 * - Prioridades
 * - Cualquier configuración que deba ser editable en runtime
 * 
 * Estructura:
 * - configGroup: Categoría (COMPLAINT_TYPES, SEVERITIES, PRIORITIES)
 * - configKey: Clave única dentro del grupo
 * - configValue: Valor almacenado
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "config_group", nullable = false, length = 64)
    private String configGroup;

    @Column(name = "config_key", nullable = false, length = 64)
    private String configKey;

    @Column(name = "config_value", nullable = false, length = 255)
    private String configValue;

    @Column(name = "display_label", nullable = false, length = 255)
    private String displayLabel;

    @Column(name = "sort_order")
    private Integer sortOrder = 0;

    @Column(nullable = false)
    private boolean active = true;

    // Getters y Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getConfigGroup() { return configGroup; }
    public void setConfigGroup(String configGroup) { this.configGroup = configGroup; }

    public String getConfigKey() { return configKey; }
    public void setConfigKey(String configKey) { this.configKey = configKey; }

    public String getConfigValue() { return configValue; }
    public void setConfigValue(String configValue) { this.configValue = configValue; }

    public String getDisplayLabel() { return displayLabel; }
    public void setDisplayLabel(String displayLabel) { this.displayLabel = displayLabel; }

    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

package com.vozsegura.vozsegura.service;

import org.springframework.stereotype.Service;

import java.util.logging.Logger;

/**
 * Servicio para obtener configuraciones del sistema.
 * NOTA: Actualmente usa valores por defecto para evitar dependencias de tablas de configuración.
 * Cuando las tablas de configuración estén disponibles, se puede habilitar la lectura desde BD.
 */
@Service
public class SystemConfigService {

    private static final Logger logger = Logger.getLogger(SystemConfigService.class.getName());

    public SystemConfigService() {
        // Constructor vacío - usa valores por defecto
    }

    /**
     * Convierte lista de ComplaintType a array para select (compatibilidad con vistas existentes).
     */
    public String[][] getComplaintTypesAsArray() {
        return new String[][] {
            {"", "Seleccionar..."},
            {"LABOR_RIGHTS", "Derechos Laborales"},
            {"HARASSMENT", "Acoso Laboral"},
            {"DISCRIMINATION", "Discriminación"},
            {"SAFETY", "Seguridad Laboral"},
            {"FRAUD", "Fraude"},
            {"OTHER", "Otro"}
        };
    }

    /**
     * Convierte lista de PriorityLevel a array para select.
     */
    public String[][] getPrioritiesAsArray() {
        return new String[][] {
            {"LOW", "Baja"},
            {"MEDIUM", "Media"},
            {"HIGH", "Alta"},
            {"CRITICAL", "Crítica"}
        };
    }

    /**
     * Convierte lista de EventType a array para select en filtros.
     */
    public String[][] getEventTypesAsArray() {
        return new String[][] {
            {"", "Todos"},
            {"LOGIN_SUCCESS", "Inicio de sesión exitoso"},
            {"LOGIN_FAILED", "Intento de acceso fallido"},
            {"LOGOUT", "Cierre de sesión"},
            {"COMPLAINT_CREATED", "Denuncia creada"},
            {"STATUS_CHANGED", "Estado cambiado"},
            {"COMPLAINT_CLASSIFIED", "Denuncia clasificada"},
            {"MORE_INFO_REQUESTED", "Información solicitada"},
            {"COMPLAINT_REJECTED", "Denuncia rechazada"},
            {"CASE_DERIVED", "Caso derivado"},
            {"EVIDENCE_VIEWED", "Evidencia visualizada"},
            {"RULE_CREATED", "Regla creada"},
            {"RULE_UPDATED", "Regla actualizada"},
            {"RULE_DELETED", "Regla eliminada"}
        };
    }

    /**
     * Convierte lista de ComplaintType a array para reglas (con opción "Cualquiera").
     */
    public String[][] getComplaintTypesForRules() {
        return new String[][] {
            {"", "Cualquiera"},
            {"LABOR_RIGHTS", "Derechos Laborales"},
            {"HARASSMENT", "Acoso Laboral"},
            {"DISCRIMINATION", "Discriminación"},
            {"SAFETY", "Seguridad Laboral"},
            {"FRAUD", "Fraude"},
            {"OTHER", "Otro"}
        };
    }

    /**
     * Convierte lista de PriorityLevel a array para reglas (con opción "Cualquiera").
     */
    public String[][] getPrioritiesForRules() {
        return new String[][] {
            {"", "Cualquiera"},
            {"LOW", "Baja"},
            {"MEDIUM", "Media"},
            {"HIGH", "Alta"},
            {"CRITICAL", "Crítica"}
        };
    }
}

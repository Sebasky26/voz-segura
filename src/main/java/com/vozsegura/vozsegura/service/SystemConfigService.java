package com.vozsegura.vozsegura.service;

import com.vozsegura.vozsegura.domain.entity.SystemConfig;
import com.vozsegura.vozsegura.repo.SystemConfigRepository;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Servicio para obtener configuraciones del sistema desde la base de datos.
 * TODAS las configuraciones se obtienen EXCLUSIVAMENTE de la base de datos.
 * No hay valores hardcodeados por seguridad - Zero Trust Architecture.
 */
@Service
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;

    // Constantes para grupos de configuración
    public static final String GROUP_COMPLAINT_TYPE = "COMPLAINT_TYPE";
    public static final String GROUP_PRIORITY = "PRIORITY";
    public static final String GROUP_EVENT_TYPE = "EVENT_TYPE";
    public static final String GROUP_STATUS = "STATUS";
    public static final String GROUP_SEVERITY = "SEVERITY";

    public SystemConfigService(SystemConfigRepository systemConfigRepository) {
        this.systemConfigRepository = systemConfigRepository;
    }

    /**
     * Verifica que la configuración del sistema esté presente en la base de datos.
     * @return true si la configuración está completa, false si falta algo
     */
    public boolean isConfigurationComplete() {
        try {
            boolean hasComplaintTypes = !systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_COMPLAINT_TYPE).isEmpty();
            boolean hasPriorities = !systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_PRIORITY).isEmpty();
            boolean hasStatuses = !systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_STATUS).isEmpty();
            boolean hasEventTypes = !systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_EVENT_TYPE).isEmpty();
            boolean hasSeverities = !systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_SEVERITY).isEmpty();

            return hasComplaintTypes && hasPriorities && hasStatuses && hasEventTypes && hasSeverities;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtiene los tipos de denuncia desde la base de datos.
     * @return Array de [código, etiqueta] o array vacío si no hay datos
     */
    public String[][] getComplaintTypesAsArray() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_COMPLAINT_TYPE);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size() + 1][2];
            result[0] = new String[]{"", "Seleccionar..."};

            for (int i = 0; i < configs.size(); i++) {
                result[i + 1] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Obtiene las prioridades desde la base de datos.
     * @return Array de [código, etiqueta] o array vacío si no hay datos
     */
    public String[][] getPrioritiesAsArray() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_PRIORITY);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size()][2];
            for (int i = 0; i < configs.size(); i++) {
                result[i] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Obtiene los tipos de evento para filtros de auditoría desde la base de datos.
     * @return Array de [código, etiqueta] o array vacío si no hay datos
     */
    public String[][] getEventTypesAsArray() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_EVENT_TYPE);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size() + 1][2];
            result[0] = new String[]{"", "Todos"};

            for (int i = 0; i < configs.size(); i++) {
                result[i + 1] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Tipos de denuncia para reglas de derivación (con opción "Cualquiera").
     * @return Array de [código, etiqueta] incluyendo opción vacía
     */
    public String[][] getComplaintTypesForRules() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_COMPLAINT_TYPE);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size() + 1][2];
            result[0] = new String[]{"", "Cualquiera"};

            for (int i = 0; i < configs.size(); i++) {
                result[i + 1] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Prioridades para reglas de derivación (con opción "Cualquiera").
     * @return Array de [código, etiqueta] incluyendo opción vacía
     */
    public String[][] getPrioritiesForRules() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_PRIORITY);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size() + 1][2];
            result[0] = new String[]{"", "Cualquiera"};

            for (int i = 0; i < configs.size(); i++) {
                result[i + 1] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Severidades para reglas de derivación (con opción "Cualquiera").
     * @return Array de [código, etiqueta] incluyendo opción vacía
     */
    public String[][] getSeveritiesForRules() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_SEVERITY);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size() + 1][2];
            result[0] = new String[]{"", "Cualquiera"};

            for (int i = 0; i < configs.size(); i++) {
                result[i + 1] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Traduce un código de tipo de denuncia a su etiqueta en español.
     * @param typeCode Código del tipo de denuncia
     * @return Etiqueta traducida o el código si no se encuentra
     */
    public String translateComplaintType(String typeCode) {
        if (typeCode == null || typeCode.isBlank()) {
            return "No clasificado";
        }

        try {
            SystemConfig config = systemConfigRepository
                    .findByConfigGroupAndConfigKeyAndActiveTrue(GROUP_COMPLAINT_TYPE, typeCode);

            if (config != null) {
                return config.getDisplayLabel();
            }

            return typeCode;
        } catch (Exception e) {
            return typeCode;
        }
    }

    /**
     * Traduce un código de prioridad a su etiqueta en español.
     * @param priorityCode Código de la prioridad
     * @return Etiqueta traducida o el código si no se encuentra
     */
    public String translatePriority(String priorityCode) {
        if (priorityCode == null || priorityCode.isBlank()) {
            return "No definida";
        }

        try {
            SystemConfig config = systemConfigRepository
                    .findByConfigGroupAndConfigKeyAndActiveTrue(GROUP_PRIORITY, priorityCode);

            if (config != null) {
                return config.getDisplayLabel();
            }

            return priorityCode;
        } catch (Exception e) {
            return priorityCode;
        }
    }

    /**
     * Obtiene las severidades desde la base de datos.
     * @return Array de [código, etiqueta] o array vacío si no hay datos
     */
    public String[][] getSeveritiesAsArray() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_SEVERITY);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size()][2];
            for (int i = 0; i < configs.size(); i++) {
                result[i] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Obtiene los estados desde la base de datos.
     * @return Array de [código, etiqueta] o array vacío si no hay datos
     */
    public String[][] getStatusesAsArray() {
        try {
            List<SystemConfig> configs = systemConfigRepository
                    .findByConfigGroupAndActiveTrueOrderBySortOrderAsc(GROUP_STATUS);

            if (configs.isEmpty()) {
                return new String[][]{{"", "Error: Configuración no disponible"}};
            }

            String[][] result = new String[configs.size()][2];
            for (int i = 0; i < configs.size(); i++) {
                result[i] = new String[]{configs.get(i).getConfigValue(), configs.get(i).getDisplayLabel()};
            }

            return result;
        } catch (Exception e) {
            return new String[][]{{"", "Error: No se pudo cargar configuración"}};
        }
    }

    /**
     * Traduce un código de estado a su etiqueta en español.
     * @param statusCode Código del estado
     * @return Etiqueta traducida o el código si no se encuentra
     */
    public String translateStatus(String statusCode) {
        if (statusCode == null || statusCode.isBlank()) {
            return "Desconocido";
        }

        try {
            SystemConfig config = systemConfigRepository
                    .findByConfigGroupAndConfigKeyAndActiveTrue(GROUP_STATUS, statusCode);

            if (config != null) {
                return config.getDisplayLabel();
            }

            return statusCode;
        } catch (Exception e) {
            return statusCode;
        }
    }

    /**
     * Traduce un código de severidad a su etiqueta en español.
     * @param severityCode Código de la severidad
     * @return Etiqueta traducida o el código si no se encuentra
     */
    public String translateSeverity(String severityCode) {
        if (severityCode == null || severityCode.isBlank()) {
            return "No definida";
        }

        try {
            SystemConfig config = systemConfigRepository
                    .findByConfigGroupAndConfigKeyAndActiveTrue(GROUP_SEVERITY, severityCode);

            if (config != null) {
                return config.getDisplayLabel();
            }

            return severityCode;
        } catch (Exception e) {
            return severityCode;
        }
    }

    /**
     * Traduce un código de tipo de evento a su etiqueta en español.
     * @param eventTypeCode Código del tipo de evento
     * @return Etiqueta traducida o el código si no se encuentra
     */
    public String translateEventType(String eventTypeCode) {
        if (eventTypeCode == null || eventTypeCode.isBlank()) {
            return "Evento desconocido";
        }

        try {
            SystemConfig config = systemConfigRepository
                    .findByConfigGroupAndConfigKeyAndActiveTrue(GROUP_EVENT_TYPE, eventTypeCode);

            if (config != null) {
                return config.getDisplayLabel();
            }

            return eventTypeCode;
        } catch (Exception e) {
            return eventTypeCode;
        }
    }
}


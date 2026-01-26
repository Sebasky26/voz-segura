package com.vozsegura.service;

import org.springframework.stereotype.Service;

/**
 * Servicio para traducir códigos de configuración del sistema a etiquetas legibles.
 *
 * Propósito:
 * - Traducir estados de denuncias
 * - Traducir severidades
 * - Traducir tipos de denuncia
 * - Proveer listas de valores para formularios
 *
 * @author Voz Segura Team
 * @since 2026-01
 */
@Service
public class SystemConfigService {

    /**
     * Traduce un código de estado a etiqueta legible en español.
     */
    public String translateStatus(String status) {
        if (status == null) return "Desconocido";

        return switch (status.toUpperCase()) {
            case "PENDING" -> "Pendiente";
            case "ASSIGNED" -> "Asignado";
            case "IN_PROGRESS" -> "En progreso";
            case "REQUIRES_REVIEW" -> "Requiere revisión";
            case "REQUIRES_MORE_INFO" -> "Requiere más información";
            case "DERIVED" -> "Derivado";
            case "RESOLVED" -> "Resuelto";
            case "REJECTED" -> "Rechazado";
            case "CLOSED" -> "Cerrado";
            default -> status;
        };
    }

    /**
     * Traduce un código de severidad a etiqueta legible en español.
     */
    public String translateSeverity(String severity) {
        if (severity == null) return "No clasificado";

        return switch (severity.toUpperCase()) {
            case "LOW" -> "Baja";
            case "MEDIUM" -> "Media";
            case "HIGH" -> "Alta";
            case "CRITICAL" -> "Crítica";
            default -> severity;
        };
    }

    /**
     * Traduce un código de tipo de denuncia a etiqueta legible en español.
     */
    public String translateComplaintType(String type) {
        if (type == null) return "No clasificado";

        return switch (type.toUpperCase()) {
            case "LABOR_RIGHTS" -> "Derechos laborales";
            case "HARASSMENT" -> "Acoso";
            case "DISCRIMINATION" -> "Discriminación";
            case "SAFETY" -> "Seguridad";
            case "FRAUD" -> "Fraude";
            case "CORRUPTION" -> "Corrupción";
            case "OTHER" -> "Otro";
            default -> type;
        };
    }

    /**
     * Retorna array de tipos de denuncia para formularios.
     */
    public String[][] getComplaintTypesAsArray() {
        return new String[][]{
                {"", "Seleccione un tipo..."},
                {"LABOR_RIGHTS", "Derechos laborales"},
                {"HARASSMENT", "Acoso"},
                {"DISCRIMINATION", "Discriminación"},
                {"SAFETY", "Seguridad"},
                {"FRAUD", "Fraude"},
                {"CORRUPTION", "Corrupción"},
                {"OTHER", "Otro"}
        };
    }

    /**
     * Retorna array de prioridades para formularios.
     */
    public String[][] getPrioritiesAsArray() {
        return new String[][]{
                {"", "Seleccione una prioridad..."},
                {"LOW", "Baja"},
                {"MEDIUM", "Media"},
                {"HIGH", "Alta"},
                {"CRITICAL", "Crítica"}
        };
    }

    /**
     * Retorna array de estados para formularios.
     */
    public String[][] getStatusesAsArray() {
        return new String[][]{
                {"", "Todos los estados"},
                {"PENDING", "Pendiente"},
                {"ASSIGNED", "Asignado"},
                {"IN_PROGRESS", "En progreso"},
                {"REQUIRES_REVIEW", "Requiere revisión"},
                {"REQUIRES_MORE_INFO", "Requiere más información"},
                {"DERIVED", "Derivado"},
                {"RESOLVED", "Resuelto"},
                {"REJECTED", "Rechazado"},
                {"CLOSED", "Cerrado"}
        };
    }
}

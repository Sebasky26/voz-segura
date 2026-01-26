package com.vozsegura.domain.enums;

/**
 * Severidad de la denuncia.
 *
 * Los valores en BD están en inglés pero se muestran en español al usuario.
 */
public enum Severity {
    BAJA("BAJA", "Baja"),
    MEDIA("MEDIA", "Media"),
    ALTA("ALTA", "Alta"),
    CRITICA("CRITICA", "Crítica");

    private final String code;
    private final String label;

    Severity(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static Severity fromCode(String code) {
        for (Severity severity : values()) {
            if (severity.code.equals(code)) {
                return severity;
            }
        }
        return MEDIA;
    }
}

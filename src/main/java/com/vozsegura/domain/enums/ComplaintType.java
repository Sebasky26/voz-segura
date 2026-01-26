package com.vozsegura.domain.enums;

/**
 * Tipos de denuncia del sistema.
 *
 * Los valores en BD están en inglés pero se muestran en español al usuario.
 */
public enum ComplaintType {
    CORRUPCION("CORRUPCION", "Corrupción"),
    FRAUDE("FRAUDE", "Fraude"),
    ACOSO_LABORAL("ACOSO_LABORAL", "Acoso Laboral"),
    ACOSO_ESCOLAR("ACOSO_ESCOLAR", "Acoso Escolar"),
    MALA_PRAXIS("MALA_PRAXIS", "Mala Praxis Médica"),
    ABUSO_PODER("ABUSO_PODER", "Abuso de Poder"),
    DISCRIMINACION("DISCRIMINACION", "Discriminación"),
    OTRO("OTRO", "Otro");

    private final String code;
    private final String label;

    ComplaintType(String code, String label) {
        this.code = code;
        this.label = label;
    }

    public String getCode() {
        return code;
    }

    public String getLabel() {
        return label;
    }

    public static ComplaintType fromCode(String code) {
        for (ComplaintType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return OTRO;
    }
}

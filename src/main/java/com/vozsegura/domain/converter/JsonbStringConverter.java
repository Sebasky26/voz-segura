package com.vozsegura.domain.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import org.postgresql.util.PGobject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.util.Map;

/**
 * Converter para campos JSONB en PostgreSQL.
 *
 * Convierte String (Java) a PGobject tipo jsonb (PostgreSQL) y viceversa.
 * Seguridad:
 * - Solo guarda JSON válido
 * - Si el String no es JSON válido, usa {}
 * - NUNCA retorna null (evita romper inserts con NOT NULL)
 *
 * Maneja correctamente:
 * - PGobject con tipo jsonb
 * - Strings JSON directos
 * - Maps/Arrays Java (convierte a JSON)
 * - OIDs de PostgreSQL (fallback seguro)
 */
@Converter
public class JsonbStringConverter implements AttributeConverter<String, Object> {

    private static final Logger log = LoggerFactory.getLogger(JsonbStringConverter.class);
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final String EMPTY_JSON = "{}";

    @Override
    public Object convertToDatabaseColumn(String attribute) {
        String safeJson = normalizeToSafeJson(attribute);

        try {
            PGobject jsonObject = new PGobject();
            jsonObject.setType("jsonb");
            jsonObject.setValue(safeJson);
            return jsonObject;
        } catch (SQLException e) {
            log.warn("Could not create PGobject(jsonb), falling back to String: {}", e.getMessage());
            return safeJson;
        } catch (Exception e) {
            log.warn("Unexpected error creating PGobject: {}", e.getMessage());
            return EMPTY_JSON;
        }
    }

    @Override
    public String convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return EMPTY_JSON;
        }

        try {
            // Caso 1: PGobject (tipo jsonb de PostgreSQL)
            if (dbData instanceof PGobject pg) {
                String value = pg.getValue();
                return (value == null || value.isBlank()) ? EMPTY_JSON : value;
            }

            // Caso 2: Ya es un String
            if (dbData instanceof String str) {
                return (str.isBlank()) ? EMPTY_JSON : str;
            }

            // Caso 3: Map o Array Java (convertir a JSON)
            if (dbData instanceof Map || dbData instanceof java.util.List) {
                try {
                    return MAPPER.writeValueAsString(dbData);
                } catch (Exception e) {
                    log.warn("Error converting Map/List to JSON: {}", e.getMessage());
                    return EMPTY_JSON;
                }
            }

            // Caso 4: Number (posiblemente un OID de PostgreSQL - esto es un error)
            if (dbData instanceof Number) {
                log.warn("Received numeric value {} instead of JSONB. This may indicate a schema/mapping issue.", dbData);
                return EMPTY_JSON;
            }

            // Caso 5: Cualquier otro objeto - intentar toString y validar
            String value = dbData.toString();
            if (value == null || value.isBlank()) {
                return EMPTY_JSON;
            }

            // Validar que es JSON válido
            String trimmed = value.trim();
            if (trimmed.startsWith("{") || trimmed.startsWith("[")) {
                try {
                    MAPPER.readTree(trimmed);
                    return trimmed;
                } catch (Exception e) {
                    log.warn("Invalid JSON from toString(): {}", e.getMessage());
                    return EMPTY_JSON;
                }
            }

            return EMPTY_JSON;

        } catch (Exception e) {
            log.error("Error converting database value to entity attribute: {}", e.getMessage());
            return EMPTY_JSON;
        }
    }

    private String normalizeToSafeJson(String input) {
        if (input == null || input.isBlank()) {
            return EMPTY_JSON;
        }

        String trimmed = input.trim();

        // Debe empezar como objeto o arreglo JSON
        if (!trimmed.startsWith("{") && !trimmed.startsWith("[")) {
            return EMPTY_JSON;
        }

        // Validación de JSON
        try {
            MAPPER.readTree(trimmed);
            return trimmed;
        } catch (Exception e) {
            log.warn("Invalid JSON input, returning empty: {}", e.getMessage());
            return EMPTY_JSON;
        }
    }
}

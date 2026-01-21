package com.vozsegura.vozsegura.security;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.io.InputStream;

/**
 * Servicio de validación de archivos (evidencias).
 * 
 * Responsabilidades:
 * - Validar MIME type (declarado por cliente)
 * - Validar "magic bytes" (firma real del archivo)
 * - Validar tamaño máximo
 * - Validar nombre de archivo (caracteres permitidos)
 * 
 * Seguridad:
 * - Previene bypass de validación por MIME spoofing
 * - Detiene archivos maliciosos renombrados
 * - Whitelist estricta: solo formatos especificados
 * 
 * Magic bytes (primeros bytes de archivo):
 * - PDF: %PDF (0x25 0x50 0x44 0x46)
 * - JPG: FF D8 FF
 * - PNG: 89 50 4E 47
 * - GIF: 47 49 46
 * - MP4: ftyp (4D 69 75 73)
 * - DOCX/XLSX: PK (504B) - ZIP
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Slf4j
@Service
public class FileValidationService {

    // Tamaño máximo: 25 MB
    private static final long MAX_FILE_SIZE = 25 * 1024 * 1024;

    // Magic bytes (primeros bytes característicos de cada formato)
    private static final Map<String, byte[][]> MAGIC_BYTES = new HashMap<>();

    static {
        // PDF: %PDF
        MAGIC_BYTES.put("application/pdf", new byte[][] {
            {0x25, 0x50, 0x44, 0x46} // %PDF
        });

        // JPEG: FFD8FF (variantes: FFE0, FFE1, FFE2, etc.)
        MAGIC_BYTES.put("image/jpeg", new byte[][] {
            {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF}
        });

        // PNG: 89504E47
        MAGIC_BYTES.put("image/png", new byte[][] {
            {(byte) 0x89, 0x50, 0x4E, 0x47}
        });

        // GIF eliminado: raramente necesario para evidencias legales
        // Si se requiere, agregar explícitamente con justificación

        // MP4/MOV: ftyp (variable, pero contiene "ftyp")
        MAGIC_BYTES.put("video/mp4", new byte[][] {
            {0x66, 0x74, 0x79, 0x70} // ftyp en bytes 4-7
        });

        // MPEG: 000001B3 o FF FB
        MAGIC_BYTES.put("video/mpeg", new byte[][] {
            {0x00, 0x00, 0x01, (byte) 0xB3},
            {(byte) 0xFF, (byte) 0xFB}
        });

        // DOCX (Office Open XML): PK (ZIP) - bytes 0-1
        // SEGURIDAD: Solo DOCX moderno, NO DOC antiguo (riesgo de macros)
        MAGIC_BYTES.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            new byte[][] {
                {0x50, 0x4B} // PK (ZIP signature)
            });

        // XLSX (Excel Open XML): PK (ZIP)
        MAGIC_BYTES.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            new byte[][] {
                {0x50, 0x4B} // PK
            });

        // NOTA: application/msword (DOC antiguo) ELIMINADO por riesgo de seguridad
        // DOC puede contener macros y exploits. Solo aceptamos DOCX moderno.
    }

    /**
     * Valida un archivo de evidencia.
     * 
     * @param file Archivo a validar
     * @return true si válido, false si es rechazado
     */
    public boolean isValidEvidence(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return false;
        }

        // 1. Validar tamaño
        if (file.getSize() > MAX_FILE_SIZE) {
            log.warn("File rejected: exceeds max size ({}MB)", MAX_FILE_SIZE / (1024 * 1024));
            return false;
        }

        // 2. Validar MIME type
        String contentType = file.getContentType();
        if (!isAllowedMimeType(contentType)) {
            log.warn("File rejected: disallowed MIME type: {}", contentType);
            return false;
        }

        // 3. Validar nombre de archivo
        String fileName = file.getOriginalFilename();
        if (!isAllowedFileName(fileName)) {
            log.warn("File rejected: disallowed filename: {}", fileName);
            return false;
        }

        // 4. Validar magic bytes (firma real del archivo)
        try {
            if (!isValidMagicBytes(file)) {
                log.warn("File rejected: magic bytes don't match MIME type");
                return false;
            }
        } catch (IOException e) {
            log.warn("File rejected: unable to read (possible corruption)");
            return false;
        }

        return true;
    }

    /**
     * Valida que el MIME type esté en whitelist ESTRICTA.
     * SEGURIDAD: NO acepta image/* genérico - solo tipos específicos
     *
     * @param contentType MIME type a validar
     * @return true si está permitido
     */
    private boolean isAllowedMimeType(String contentType) {
        if (contentType == null) {
            return false;
        }

        // Remover charset/parámetros
        String baseType = contentType.split(";")[0].trim().toLowerCase();

        // WHITELIST ESTRICTA: Solo tipos con magic bytes registrados
        // NO aceptamos image/* genérico ni video/* genérico
        return MAGIC_BYTES.containsKey(baseType);
    }

    /**
     * Valida el nombre del archivo (caracteres permitidos, extensión, etc.).
     * 
     * @param fileName Nombre del archivo
     * @return true si es válido
     */
    private boolean isAllowedFileName(String fileName) {
        if (fileName == null || fileName.isEmpty()) {
            return false;
        }

        // No permitir path traversal
        if (fileName.contains("..") || fileName.contains("/") || fileName.contains("\\")) {
            return false;
        }

        // No permitir caracteres control o especiales peligrosos
        if (!fileName.matches("^[\\w\\-. áéíóúñüÁÉÍÓÚÑÜ]+$")) {
            return false;
        }

        // Validar extensión
        String extension = getFileExtension(fileName).toLowerCase();
        return isAllowedExtension(extension);
    }

    /**
     * Obtiene la extensión de un archivo.
     * 
     * @param fileName Nombre del archivo
     * @return Extensión sin punto (ej: "pdf", "jpg")
     */
    private String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot > 0 && lastDot < fileName.length() - 1) {
            return fileName.substring(lastDot + 1);
        }
        return "";
    }

    /**
     * Valida que la extensión esté permitida.
     * SEGURIDAD: Solo extensiones modernas y seguras
     *
     * @param extension Extensión sin punto
     * @return true si está permitida
     */
    private boolean isAllowedExtension(String extension) {
        // WHITELIST: pdf, imágenes (jpg/jpeg/png), videos (mp4/mpeg), Office moderno (docx/xlsx)
        // ELIMINADO: doc (DOC antiguo con riesgo de macros), gif (raramente necesario)
        return extension.matches("^(pdf|jpg|jpeg|png|mp4|mpeg|docx|xlsx)$");
    }

    /**
     * Valida los magic bytes del archivo contra su MIME type.
     * SEGURIDAD: SIEMPRE valida magic bytes - no hay bypass
     *
     * @param file Archivo a validar
     * @return true SOLO si los bytes coinciden con el tipo declarado
     * @throws IOException si hay error al leer el archivo
     */
    private boolean isValidMagicBytes(MultipartFile file) throws IOException {
        String contentType = file.getContentType();
        if (contentType == null) {
            return false;
        }

        String baseType = contentType.split(";")[0].trim().toLowerCase();
        byte[][] expectedMagicBytes = MAGIC_BYTES.get(baseType);

        // Si no hay magic bytes registrados, RECHAZAR
        // Ya no permitimos tipos sin validación de firma
        if (expectedMagicBytes == null) {
            log.warn("File rejected: no magic bytes signature for type: {}", baseType);
            return false;
        }

        // Leer SOLO los primeros bytes necesarios (max 32 bytes)
        byte[] fileBytes = new byte[32];
        try (InputStream is = file.getInputStream()) {
            int bytesRead = is.read(fileBytes);
            if (bytesRead == -1) {
                return false;
            }
        } catch (IOException e) {
            return false;
        }

        // Verificar que alguna secuencia de magic bytes coincida
        for (byte[] magicSequence : expectedMagicBytes) {
            if (matchesMagicBytes(fileBytes, magicSequence)) {
                return true;
            }
        }

        log.warn("File rejected: magic bytes don't match declared type {}", baseType);
        return false;
    }

    /**
     * Verifica si los bytes del archivo coinciden con la secuencia esperada.
     * 
     * @param fileBytes Bytes del archivo
     * @param magicBytes Secuencia esperada
     * @return true si coinciden
     */
    private boolean matchesMagicBytes(byte[] fileBytes, byte[] magicBytes) {
        if (fileBytes.length < magicBytes.length) {
            return false;
        }

        // MP4 es un caso especial: ftyp está en bytes 4-7
        if (magicBytes.length == 4 && 
            magicBytes[0] == 0x66 && magicBytes[1] == 0x74 && 
            magicBytes[2] == 0x79 && magicBytes[3] == 0x70) {
            
            if (fileBytes.length >= 8) {
                return fileBytes[4] == magicBytes[0] &&
                       fileBytes[5] == magicBytes[1] &&
                       fileBytes[6] == magicBytes[2] &&
                       fileBytes[7] == magicBytes[3];
            }
            return false;
        }

        // Coincidencia normal desde el inicio
        for (int i = 0; i < magicBytes.length; i++) {
            if (fileBytes[i] != magicBytes[i]) {
                return false;
            }
        }
        return true;
    }
}

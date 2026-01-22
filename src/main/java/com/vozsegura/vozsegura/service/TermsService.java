package com.vozsegura.vozsegura.service;

// ...existing code...
import org.springframework.stereotype.Service;
// ...existing code...

// ...existing code...
// ...existing code...

/**
 * Servicio para gestión y auditoría de aceptación de términos y condiciones.
 * 
 * Propósito:
 * - Registrar que usuario aceptó términos (compliance legal)
 * - Vinculación anónima: solo guarda token de sesión (NO datos personales)
 * - Auditoría: fecha/hora de aceptación
 * - NUNCA vincular término aceptado con cédula directamente
 * 
 * Flujo de uso:
 * 1. Usuario accede a página de términos (inicio de denuncia)
 * 2. UI genera sessionToken único (UUID)
 * 3. Usuario lee y acepta términos
 * 4. Frontend llama: recordAcceptance() → guarda sessionToken + timestamp
 * 5. sessionToken pasa a siguiente paso (OTP, etc.)
 * 6. En final de denuncia: registrar si tuvo términos aceptados previamente
 * 
 * Nota de seguridad:
 * - Tabla TermsAcceptance tiene SOLO: sessionToken, acceptedAt
 * - NO: cédula, IP, navegador (eso va en AuditLog)
 * - Linking: sessionToken es el puente (anonimidad)
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Service
public class TermsService {

    // Métodos de aceptación de términos eliminados porque la funcionalidad y tabla ya no existen

    // Método hasAccepted eliminado porque ya no hay funcionalidad
}


package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Registro de aceptación de términos y condiciones (schema denuncias.aceptacion_terminos)
 * 
 * Evidencia de que el denunciante aceptó los términos:
 * - Vinculado a persona (id_registro)
 * - Token de sesión único
 * - Timestamp de aceptación
 * - IP desde la que aceptó (para auditoría)
 * 
 * Esencial para compliance y defensa legal
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Entity
@Table(name = "aceptacion_terminos", schema = "denuncias")
public class TermsAcceptance {

    /** ID único de registro de aceptación */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Referencia de registro (puede ser null, no vinculado a cédula) */
    @Column(name = "id_registro")
    private Long idRegistro;

    /** Token único de sesión (UUID 36 caracteres) - vinculación anónima */
    @Column(name = "session_token", nullable = false, unique = true, length = 64)
    private String sessionToken;

    /** Timestamp de aceptación (zona horaria offset UTC) */
    @Column(name = "accepted_at", nullable = false)
    private OffsetDateTime acceptedAt;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Long idRegistro) { this.idRegistro = idRegistro; }

    public String getSessionToken() { return sessionToken; }
    public void setSessionToken(String sessionToken) { this.sessionToken = sessionToken; }

    public OffsetDateTime getAcceptedAt() { return acceptedAt; }
    public void setAcceptedAt(OffsetDateTime acceptedAt) { this.acceptedAt = acceptedAt; }
}

package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Usuario del sistema (staff) almacenado en schema staff.staff_user
 * 
 * Soporta dos roles:
 * - ADMIN: Acceso total al panel, gestión de reglas
 * - ANALYST: Revisión y clasificación de denuncias
 * 
 * Autenticación: cédula + contraseña (hash PBKDF2) + OTP vía email
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */

@Entity
@Table(name = "staff_user", schema = "staff")
public class StaffUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "id_registro", nullable = false, unique = true)
    private Long idRegistro;

    @Column(nullable = false, unique = true, length = 64)
    private String username;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(nullable = false, length = 120)
    private String passwordHash;

    @Column(nullable = false, length = 32)
    private String role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(length = 255)
    private String email;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Long getIdRegistro() { return idRegistro; }
    public void setIdRegistro(Long idRegistro) { this.idRegistro = idRegistro; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getCedula() { return cedula; }
    public void setCedula(String cedula) { this.cedula = cedula; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;
import java.time.OffsetDateTime;

/**
 * Identidad verificada del Registro Civil (schema registro_civil.personas)
 * 
 * Núcleo de confianza del sistema. Solo contiene datos de personas que:
 * 1. Pasaron verificación biométrica Didit
 * 2. Son staff autorizados del sistema
 * 3. Son denunciantes verificados
 * 
 * Campos clave:
 * - cedula: Número de identificación (plain text)
 * - cedulaHash: SHA-256 hash para búsqueda segura sin revelar número real
 * - primerNombre/Apellido: Datos demográficos
 * 
 * NOTA: cedulaHash permite búsquedas sin exponer el número de cédula en queries
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Entity
@Table(name = "personas", schema = "registro_civil")
public class Persona {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long idRegistro;

    @Column(nullable = false, unique = true, length = 20)
    private String cedula;

    @Column(name = "cedula_hash", nullable = false, unique = true, length = 128)
    private String cedulaHash;

    @Column(name = "primer_nombre", length = 255)
    private String primerNombre;

    @Column(name = "segundo_nombre", length = 255)
    private String segundoNombre;

    @Column(name = "primer_apellido", length = 255)
    private String primerApellido;

    @Column(name = "segundo_apellido", length = 255)
    private String segundoApellido;

    @Column(length = 10)
    private String sexo;

    @Column(name = "created_at", nullable = false, updatable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    // Constructores
    public Persona() {
    }

    public Persona(String cedula, String cedulaHash) {
        this.cedula = cedula;
        this.cedulaHash = cedulaHash;
        this.createdAt = OffsetDateTime.now();
        this.updatedAt = OffsetDateTime.now();
    }

    // Getters y Setters

    public Long getIdRegistro() {
        return idRegistro;
    }

    public void setIdRegistro(Long idRegistro) {
        this.idRegistro = idRegistro;
    }

    public String getCedula() {
        return cedula;
    }

    public void setCedula(String cedula) {
        this.cedula = cedula;
    }

    public String getCedulaHash() {
        return cedulaHash;
    }

    public void setCedulaHash(String cedulaHash) {
        this.cedulaHash = cedulaHash;
    }

    public String getPrimerNombre() {
        return primerNombre;
    }

    public void setPrimerNombre(String primerNombre) {
        this.primerNombre = primerNombre;
    }

    public String getSegundoNombre() {
        return segundoNombre;
    }

    public void setSegundoNombre(String segundoNombre) {
        this.segundoNombre = segundoNombre;
    }

    public String getPrimerApellido() {
        return primerApellido;
    }

    public void setPrimerApellido(String primerApellido) {
        this.primerApellido = primerApellido;
    }

    public String getSegundoApellido() {
        return segundoApellido;
    }

    public void setSegundoApellido(String segundoApellido) {
        this.segundoApellido = segundoApellido;
    }

    public String getSexo() {
        return sexo;
    }

    public void setSexo(String sexo) {
        this.sexo = sexo;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public OffsetDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(OffsetDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}

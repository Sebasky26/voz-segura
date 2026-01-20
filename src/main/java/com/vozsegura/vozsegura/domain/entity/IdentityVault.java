package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;

/**
 * Bóveda de identidad (schema secure_identities.identity_vault)
 * 
 * SEGURIDAD CRÍTICA: Solo almacena hashes SHA-256 de cédulas
 * - NUNCA almacena números de cédula en plain text
 * - NUNCA almacena datos personales
 * - One-way hash: imposible recuperar cédula original
 * 
 * Permite:
 * - Verificar que una denuncia proviene de una persona conocida
 * - Auditar sin revelar identidad
 * - Enlazar denuncias al mismo denunciante de forma segura
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_registro")
    private Long id;

    @Column(name = "cedula_hash", nullable = false, unique = true, length = 128)
    private String citizenHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCitizenHash() { return citizenHash; }
    public void setCitizenHash(String citizenHash) { this.citizenHash = citizenHash; }
}

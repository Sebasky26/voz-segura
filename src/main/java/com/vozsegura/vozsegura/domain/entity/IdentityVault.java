package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "personas", schema = "registro_civil")
public class IdentityVault {

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

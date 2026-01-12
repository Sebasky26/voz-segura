package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "identity_vault", schema = "secure_identities")
public class IdentityVault {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "citizen_hash", nullable = false, unique = true, length = 128)
    private String citizenHash;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getCitizenHash() { return citizenHash; }
    public void setCitizenHash(String citizenHash) { this.citizenHash = citizenHash; }
}

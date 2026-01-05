package com.vozsegura.vozsegura.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "derivation_rule")
public class DerivationRule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 64)
    private String name;

    @Column(name = "severity_match", length = 32)
    private String severityMatch;

    @Column(name = "destination", nullable = false, length = 255)
    private String destination;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getSeverityMatch() { return severityMatch; }
    public void setSeverityMatch(String severityMatch) { this.severityMatch = severityMatch; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }
}

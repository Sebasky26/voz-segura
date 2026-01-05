package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.IdentityVault;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IdentityVaultRepository extends JpaRepository<IdentityVault, Long> {

    Optional<IdentityVault> findByCitizenHash(String citizenHash);
}

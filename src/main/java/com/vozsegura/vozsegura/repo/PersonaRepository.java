package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.Persona;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PersonaRepository extends JpaRepository<Persona, Long> {

    Optional<Persona> findByCedula(String cedula);

    Optional<Persona> findByCedulaHash(String cedulaHash);
}

package com.vozsegura.vozsegura.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vozsegura.vozsegura.domain.entity.DestinationEntity;

/**
 * Spring Data JPA repository para entidad DestinationEntity.
 * 
 * Gestiona los destinos (organizaciones) a donde se derivan las denuncias.
 * Ejemplos: Ministerios públicos, entidades regulatorias, etc.
 * 
 * Propósito:
 * - Mantener catálogo de instituciones receptoras de denuncias
 * - Configurar puntos de contacto y canales de entrega
 * - Habilitar/deshabilitar instituciones sin eliminar histórico
 * 
 * Relación con DerivationService:
 * - DerivationService selecciona una DestinationEntity basada en reglas
 * - Las denuncias se envían a la DestinationEntity elegida
 * - Sistema de audit log registra cada derivación
 * 
 * @author Voz Segura Team
 * @since 2026-01
 */
@Repository
public interface DestinationEntityRepository extends JpaRepository<DestinationEntity, Long> {

    /**
     * Obtiene todas las instituciones activas, ordenadas alfabéticamente.
     * Usado para llenar dropdowns en UI de administración.
     * 
     * @return Lista de instituciones activas ordenadas por nombre
     */
    List<DestinationEntity> findByActiveTrueOrderByNameAsc();

    /**
     * Busca instituciones por nombre (búsqueda parcial).
     *
     * @param name Nombre a buscar
     * @return Optional con institución si existe
     */
    Optional<DestinationEntity> findByName(String name);
}

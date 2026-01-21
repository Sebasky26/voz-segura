package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.SystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio para acceder a configuraciones del sistema.
 */
@Repository
public interface SystemConfigRepository extends JpaRepository<SystemConfig, Long> {

    /**
     * Encuentra todas las configuraciones activas de un grupo ordenadas.
     */
    List<SystemConfig> findByConfigGroupAndActiveTrueOrderBySortOrderAsc(String configGroup);

    /**
     * Encuentra una configuración específica por grupo y clave.
     */
    SystemConfig findByConfigGroupAndConfigKeyAndActiveTrue(String configGroup, String configKey);
}

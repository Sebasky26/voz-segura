package com.vozsegura.vozsegura.repo;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.vozsegura.vozsegura.domain.entity.DestinationEntity;

@Repository
public interface DestinationEntityRepository extends JpaRepository<DestinationEntity, Long> {

    List<DestinationEntity> findByActiveTrueOrderByNameAsc();

    Optional<DestinationEntity> findByCode(String code);
}

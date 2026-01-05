package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.DerivationRule;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DerivationRuleRepository extends JpaRepository<DerivationRule, Long> {

    List<DerivationRule> findByActiveTrue();
}

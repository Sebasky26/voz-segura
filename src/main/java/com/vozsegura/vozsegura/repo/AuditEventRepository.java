package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.AuditEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {

    Page<AuditEvent> findAllByOrderByEventTimeDesc(Pageable pageable);

    Page<AuditEvent> findByEventType(String eventType, Pageable pageable);
}

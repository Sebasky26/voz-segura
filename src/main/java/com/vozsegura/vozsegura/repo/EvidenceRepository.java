package com.vozsegura.vozsegura.repo;

import com.vozsegura.vozsegura.domain.entity.Complaint;
import com.vozsegura.vozsegura.domain.entity.Evidence;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvidenceRepository extends JpaRepository<Evidence, Long> {

    List<Evidence> findByComplaint(Complaint complaint);

    int countByComplaint(Complaint complaint);
}

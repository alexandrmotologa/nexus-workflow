package com.engine.nexus.infrastructure.adapter.out.persistence;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface JpaWorkflowInstanceRepository extends JpaRepository<WorkflowInstanceEntity, String> {
    List<WorkflowInstanceEntity> findAllByOrderByCreatedAtDesc(Pageable pageable);
}

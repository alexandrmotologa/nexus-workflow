package com.engine.nexus.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface JpaWorkflowEventRepository extends JpaRepository<WorkflowEventEntity, Long> {
    List<WorkflowEventEntity> findByWorkflowIdOrderBySequenceNumberAsc(String workflowId);
    Optional<WorkflowEventEntity> findTopByWorkflowIdOrderBySequenceNumberDesc(String workflowId);
}

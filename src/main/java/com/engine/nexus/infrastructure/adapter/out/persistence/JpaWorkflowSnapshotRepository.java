package com.engine.nexus.infrastructure.adapter.out.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaWorkflowSnapshotRepository extends JpaRepository<WorkflowSnapshotEntity, Long> {
    Optional<WorkflowSnapshotEntity> findTopByWorkflowIdOrderByLastSequenceNumberDesc(String workflowId);
}

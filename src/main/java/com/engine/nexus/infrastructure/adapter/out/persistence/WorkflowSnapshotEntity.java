package com.engine.nexus.infrastructure.adapter.out.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "nexus_workflow_snapshots")
public class WorkflowSnapshotEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "workflow_id", length = 64, nullable = false)
    private String workflowId;

    @Column(name = "last_sequence_number", nullable = false)
    private Long lastSequenceNumber;

    @Column(name = "snapshot_state", columnDefinition = "TEXT", nullable = false)
    private String snapshotState;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    public WorkflowSnapshotEntity() {}

    public WorkflowSnapshotEntity(String workflowId, Long lastSequenceNumber, String snapshotState, Instant createdAt) {
        this.workflowId = workflowId;
        this.lastSequenceNumber = lastSequenceNumber;
        this.snapshotState = snapshotState;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getWorkflowId() { return workflowId; }
    public void setWorkflowId(String workflowId) { this.workflowId = workflowId; }

    public Long getLastSequenceNumber() { return lastSequenceNumber; }
    public void setLastSequenceNumber(Long lastSequenceNumber) { this.lastSequenceNumber = lastSequenceNumber; }

    public String getSnapshotState() { return snapshotState; }
    public void setSnapshotState(String snapshotState) { this.snapshotState = snapshotState; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}

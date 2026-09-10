package com.engine.nexus.domain.model;

import java.time.Instant;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class WorkflowInstance {
    private final WorkflowId id;
    private final String definitionId;
    private final int definitionVersion;
    private WorkflowStatus status;
    private StepId currentStepId;
    private final Map<String, Object> inputPayload;
    private final Map<String, Object> outputPayload;
    private String errorMessage;
    private final Instant createdAt;
    private Instant updatedAt;
    private Instant completedAt;

    public WorkflowInstance(
            WorkflowId id,
            String definitionId,
            int definitionVersion,
            WorkflowStatus status,
            StepId currentStepId,
            Map<String, Object> inputPayload,
            Map<String, Object> outputPayload,
            String errorMessage,
            Instant createdAt,
            Instant updatedAt,
            Instant completedAt
    ) {
        this.id = Objects.requireNonNull(id, "WorkflowId must not be null");
        this.definitionId = Objects.requireNonNull(definitionId, "definitionId must not be null");
        this.definitionVersion = Math.max(1, definitionVersion);
        this.status = Objects.requireNonNull(status, "status must not be null");
        this.currentStepId = currentStepId;
        this.inputPayload = inputPayload == null ? new HashMap<>() : new HashMap<>(inputPayload);
        this.outputPayload = outputPayload == null ? new HashMap<>() : new HashMap<>(outputPayload);
        this.errorMessage = errorMessage;
        this.createdAt = createdAt != null ? createdAt : Instant.now();
        this.updatedAt = updatedAt != null ? updatedAt : this.createdAt;
        this.completedAt = completedAt;
    }

    public static WorkflowInstance create(WorkflowId id, String definitionId, int definitionVersion, Map<String, Object> input) {
        Instant now = Instant.now();
        return new WorkflowInstance(
                id,
                definitionId,
                definitionVersion,
                WorkflowStatus.PENDING,
                null,
                input,
                new HashMap<>(),
                null,
                now,
                now,
                null
        );
    }

    public void start(StepId initialStep) {
        if (this.status != WorkflowStatus.PENDING) {
            throw new IllegalStateException("Cannot start workflow in status: " + this.status);
        }
        this.status = WorkflowStatus.RUNNING;
        this.currentStepId = initialStep;
        this.updatedAt = Instant.now();
    }

    public void transitionToStep(StepId stepId) {
        if (this.status.isTerminal()) {
            throw new IllegalStateException("Cannot advance step in terminal status: " + this.status);
        }
        this.status = WorkflowStatus.RUNNING;
        this.currentStepId = stepId;
        this.updatedAt = Instant.now();
    }

    public void setWaitingForSignal(StepId stepId) {
        this.status = WorkflowStatus.WAITING_SIGNAL;
        this.currentStepId = stepId;
        this.updatedAt = Instant.now();
    }

    public void setSleeping(StepId stepId) {
        this.status = WorkflowStatus.SLEEPING;
        this.currentStepId = stepId;
        this.updatedAt = Instant.now();
    }

    public void updateOutput(Map<String, Object> stepOutput) {
        if (stepOutput != null) {
            this.outputPayload.putAll(stepOutput);
        }
        this.updatedAt = Instant.now();
    }

    public void complete(Map<String, Object> finalOutput) {
        if (finalOutput != null) {
            this.outputPayload.putAll(finalOutput);
        }
        this.status = WorkflowStatus.COMPLETED;
        this.currentStepId = null;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.completedAt = now;
    }

    public void fail(String message) {
        this.status = WorkflowStatus.FAILED;
        this.errorMessage = message;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.completedAt = now;
    }

    public void startCompensation(StepId failedStep) {
        this.status = WorkflowStatus.COMPENSATING;
        this.currentStepId = failedStep;
        this.updatedAt = Instant.now();
    }

    public void finishCompensation(String reason) {
        this.status = WorkflowStatus.FAILED;
        this.errorMessage = "Compensated after failure: " + reason;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.completedAt = now;
    }

    public void cancel(String reason) {
        this.status = WorkflowStatus.CANCELLED;
        this.errorMessage = "Cancelled: " + reason;
        Instant now = Instant.now();
        this.updatedAt = now;
        this.completedAt = now;
    }

    // Getters
    public WorkflowId getId() {
        return id;
    }

    public String getDefinitionId() {
        return definitionId;
    }

    public int getDefinitionVersion() {
        return definitionVersion;
    }

    public WorkflowStatus getStatus() {
        return status;
    }

    public Optional<StepId> getCurrentStepId() {
        return Optional.ofNullable(currentStepId);
    }

    public Map<String, Object> getInputPayload() {
        return Collections.unmodifiableMap(inputPayload);
    }

    public Map<String, Object> getOutputPayload() {
        return Collections.unmodifiableMap(outputPayload);
    }

    public Optional<String> getErrorMessage() {
        return Optional.ofNullable(errorMessage);
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Optional<Instant> getCompletedAt() {
        return Optional.ofNullable(completedAt);
    }
}

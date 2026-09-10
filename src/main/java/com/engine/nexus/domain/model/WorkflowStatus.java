package com.engine.nexus.domain.model;

public enum WorkflowStatus {
    PENDING,
    RUNNING,
    WAITING_SIGNAL,
    SLEEPING,
    COMPENSATING,
    COMPLETED,
    FAILED,
    CANCELLED;

    public boolean isTerminal() {
        return this == COMPLETED || this == FAILED || this == CANCELLED;
    }
}

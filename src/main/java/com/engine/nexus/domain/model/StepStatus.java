package com.engine.nexus.domain.model;

public enum StepStatus {
    PENDING,
    RUNNING,
    WAITING_SIGNAL,
    SLEEPING,
    COMPLETED,
    FAILED,
    COMPENSATED,
    SKIPPED
}

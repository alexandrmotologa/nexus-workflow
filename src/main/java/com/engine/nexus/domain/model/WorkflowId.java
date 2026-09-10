package com.engine.nexus.domain.model;

import java.util.Objects;
import java.util.UUID;

public record WorkflowId(String value) {
    public WorkflowId {
        Objects.requireNonNull(value, "WorkflowId value must not be null");
        if (value.isBlank()) {
            throw new IllegalArgumentException("WorkflowId value must not be blank");
        }
    }

    public static WorkflowId generate() {
        return new WorkflowId("wf_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
    }

    public static WorkflowId of(String value) {
        return new WorkflowId(value);
    }

    @Override
    public String toString() {
        return value;
    }
}

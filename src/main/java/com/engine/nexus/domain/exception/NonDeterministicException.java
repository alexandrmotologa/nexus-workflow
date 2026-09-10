package com.engine.nexus.domain.exception;

import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;

public class NonDeterministicException extends RuntimeException {
    private final WorkflowId workflowId;
    private final StepId expectedStepId;
    private final StepId actualStepId;

    public NonDeterministicException(WorkflowId workflowId, StepId expectedStepId, StepId actualStepId, String message) {
        super("Non-deterministic workflow change detected for " + workflowId + ". Expected step: " + expectedStepId + ", but found: " + actualStepId + ". Details: " + message);
        this.workflowId = workflowId;
        this.expectedStepId = expectedStepId;
        this.actualStepId = actualStepId;
    }

    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    public StepId getExpectedStepId() {
        return expectedStepId;
    }

    public StepId getActualStepId() {
        return actualStepId;
    }
}

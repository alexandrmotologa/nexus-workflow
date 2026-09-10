package com.engine.nexus.domain.exception;

import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;

public class WorkflowExecutionException extends RuntimeException {
    private final WorkflowId workflowId;
    private final StepId stepId;

    public WorkflowExecutionException(WorkflowId workflowId, StepId stepId, String message, Throwable cause) {
        super("Workflow execution failed at " + workflowId + " on step " + stepId + ": " + message, cause);
        this.workflowId = workflowId;
        this.stepId = stepId;
    }

    public WorkflowExecutionException(WorkflowId workflowId, StepId stepId, String message) {
        this(workflowId, stepId, message, null);
    }

    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    public StepId getStepId() {
        return stepId;
    }
}

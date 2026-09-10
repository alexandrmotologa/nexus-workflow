package com.engine.nexus.domain.exception;

import com.engine.nexus.domain.model.WorkflowId;

public class WorkflowNotFoundException extends RuntimeException {
    private final WorkflowId workflowId;

    public WorkflowNotFoundException(WorkflowId workflowId) {
        super("Workflow instance not found: " + workflowId);
        this.workflowId = workflowId;
    }

    public WorkflowId getWorkflowId() {
        return workflowId;
    }
}

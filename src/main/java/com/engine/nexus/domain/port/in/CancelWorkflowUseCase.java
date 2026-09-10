package com.engine.nexus.domain.port.in;

import com.engine.nexus.domain.model.WorkflowId;

public interface CancelWorkflowUseCase {
    void cancelWorkflow(WorkflowId workflowId, String reason);
}

package com.engine.nexus.domain.exception;

import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.WorkflowId;

public class SignalTimeoutException extends RuntimeException {
    private final WorkflowId workflowId;
    private final SignalName signalName;

    public SignalTimeoutException(WorkflowId workflowId, SignalName signalName) {
        super("Timed out waiting for signal " + signalName + " on workflow " + workflowId);
        this.workflowId = workflowId;
        this.signalName = signalName;
    }

    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    public SignalName getSignalName() {
        return signalName;
    }
}

package com.engine.nexus.domain.port.in;

import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.WorkflowId;

import java.util.Map;

public interface SignalWorkflowUseCase {
    void sendSignal(WorkflowId workflowId, SignalName signalName, Map<String, Object> payload);
}

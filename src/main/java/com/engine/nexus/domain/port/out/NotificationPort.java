package com.engine.nexus.domain.port.out;

import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowStatus;

import java.util.Map;

public interface NotificationPort {
    void notifyStatusChange(WorkflowId workflowId, String definitionId, WorkflowStatus status, Map<String, Object> output, String error);
}

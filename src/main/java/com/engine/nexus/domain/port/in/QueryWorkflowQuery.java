package com.engine.nexus.domain.port.in;

import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;

import java.util.List;

public interface QueryWorkflowQuery {
    WorkflowInstance getWorkflowInstance(WorkflowId workflowId);
    List<NexusDomainEvent> getWorkflowHistory(WorkflowId workflowId);
    List<WorkflowInstance> listWorkflows(int limit, int offset);
}

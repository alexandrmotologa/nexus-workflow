package com.engine.nexus.domain.port.out;

import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;

import java.util.List;
import java.util.Optional;

public interface WorkflowEventStorePort {
    void appendEvent(NexusDomainEvent event);
    List<NexusDomainEvent> getEventsForWorkflow(WorkflowId workflowId);
    long getNextSequenceNumber(WorkflowId workflowId);
    void saveInstance(WorkflowInstance instance);
    Optional<WorkflowInstance> findInstance(WorkflowId workflowId);
    List<WorkflowInstance> findAllInstances(int limit, int offset);
}

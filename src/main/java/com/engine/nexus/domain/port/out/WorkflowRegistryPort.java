package com.engine.nexus.domain.port.out;

import com.engine.nexus.domain.model.WorkflowDefinition;

import java.util.List;
import java.util.Optional;

public interface WorkflowRegistryPort {
    void register(WorkflowDefinition definition);
    Optional<WorkflowDefinition> getDefinition(String definitionId);
    List<WorkflowDefinition> getAllDefinitions();
}

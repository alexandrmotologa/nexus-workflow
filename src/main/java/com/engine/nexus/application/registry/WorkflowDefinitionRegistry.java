package com.engine.nexus.application.registry;

import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.port.out.WorkflowRegistryPort;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class WorkflowDefinitionRegistry implements WorkflowRegistryPort {
    private final Map<String, WorkflowDefinition> definitions = new ConcurrentHashMap<>();

    @Override
    public void register(WorkflowDefinition definition) {
        Objects.requireNonNull(definition, "definition must not be null");
        definitions.put(definition.id(), definition);
    }

    @Override
    public Optional<WorkflowDefinition> getDefinition(String definitionId) {
        if (definitionId == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(definitions.get(definitionId));
    }

    @Override
    public List<WorkflowDefinition> getAllDefinitions() {
        return new ArrayList<>(definitions.values());
    }
}

package com.engine.nexus.application.dsl;

import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class WorkflowContext {
    private final WorkflowId workflowId;
    private final StepId stepId;
    private final Map<String, Object> input;
    private final Map<String, Object> state;

    public WorkflowContext(WorkflowId workflowId, StepId stepId, Map<String, Object> input, Map<String, Object> state) {
        this.workflowId = Objects.requireNonNull(workflowId, "workflowId must not be null");
        this.stepId = stepId;
        this.input = input == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(input));
        this.state = state == null ? Collections.emptyMap() : Collections.unmodifiableMap(new HashMap<>(state));
    }

    public WorkflowId getWorkflowId() {
        return workflowId;
    }

    public Optional<StepId> getStepId() {
        return Optional.ofNullable(stepId);
    }

    public Map<String, Object> getInput() {
        return input;
    }

    public Map<String, Object> getState() {
        return state;
    }

    @SuppressWarnings("unchecked")
    public <T> Optional<T> get(String key, Class<T> type) {
        Object val = state.getOrDefault(key, input.get(key));
        if (val == null) {
            return Optional.empty();
        }
        if (type.isInstance(val)) {
            return Optional.of((T) val);
        }
        return Optional.empty();
    }
}

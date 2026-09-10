package com.engine.nexus.application.registry;

import com.engine.nexus.application.dsl.ActivityFunction;
import com.engine.nexus.application.dsl.CompensationFunction;
import com.engine.nexus.application.dsl.WorkflowContext;
import com.engine.nexus.domain.port.out.ActivityDispatcherPort;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class ActivityRegistry implements ActivityDispatcherPort {
    private final Map<String, ActivityFunction> activities = new ConcurrentHashMap<>();
    private final Map<String, CompensationFunction> compensations = new ConcurrentHashMap<>();

    public void registerActivity(String name, ActivityFunction function) {
        Objects.requireNonNull(name, "Activity name must not be null");
        Objects.requireNonNull(function, "ActivityFunction must not be null");
        activities.put(name, function);
    }

    public void registerCompensation(String name, CompensationFunction function) {
        Objects.requireNonNull(name, "Compensation name must not be null");
        Objects.requireNonNull(function, "CompensationFunction must not be null");
        compensations.put(name, function);
    }

    public Optional<ActivityFunction> findActivity(String name) {
        return Optional.ofNullable(activities.get(name));
    }

    public Optional<CompensationFunction> findCompensation(String name) {
        return Optional.ofNullable(compensations.get(name));
    }

    @Override
    public Map<String, Object> dispatch(String activityName, Map<String, Object> inputs) {
        ActivityFunction function = activities.get(activityName);
        if (function == null) {
            throw new IllegalArgumentException("No activity registered with name: " + activityName);
        }
        try {
            WorkflowContext ctx = new WorkflowContext(
                    (com.engine.nexus.domain.model.WorkflowId) inputs.get("workflowId"),
                    (com.engine.nexus.domain.model.StepId) inputs.get("stepId"),
                    inputs,
                    inputs
            );
            return function.execute(ctx);
        } catch (Exception e) {
            throw new RuntimeException("Error executing activity " + activityName + ": " + e.getMessage(), e);
        }
    }
}

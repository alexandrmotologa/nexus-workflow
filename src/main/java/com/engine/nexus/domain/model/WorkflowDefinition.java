package com.engine.nexus.domain.model;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

public record WorkflowDefinition(
        String id,
        int version,
        List<StepDefinition> steps,
        String failureHandlerActivity
) {
    public WorkflowDefinition {
        Objects.requireNonNull(id, "Workflow definition id must not be null");
        if (id.isBlank()) {
            throw new IllegalArgumentException("Workflow definition id must not be blank");
        }
        if (version < 1) {
            throw new IllegalArgumentException("Workflow version must be at least 1");
        }
        steps = steps == null ? Collections.emptyList() : List.copyOf(steps);
        validateStepUniqueness(steps);
    }

    private static void validateStepUniqueness(List<StepDefinition> steps) {
        Set<StepId> seen = new HashSet<>();
        for (StepDefinition step : steps) {
            if (!seen.add(step.stepId())) {
                throw new IllegalArgumentException("Duplicate stepId detected in workflow definition: " + step.stepId());
            }
            if (step.type() == StepType.PARALLEL && !step.parallelBranches().isEmpty()) {
                for (StepDefinition branch : step.parallelBranches()) {
                    if (!seen.add(branch.stepId())) {
                        throw new IllegalArgumentException("Duplicate stepId in parallel branch: " + branch.stepId());
                    }
                }
            }
        }
    }

    public Optional<StepDefinition> findStep(StepId stepId) {
        for (StepDefinition step : steps) {
            if (step.stepId().equals(stepId)) {
                return Optional.of(step);
            }
            for (StepDefinition branch : step.parallelBranches()) {
                if (branch.stepId().equals(stepId)) {
                    return Optional.of(branch);
                }
            }
        }
        return Optional.empty();
    }

    public Optional<String> getFailureHandler() {
        return Optional.ofNullable(failureHandlerActivity);
    }
}

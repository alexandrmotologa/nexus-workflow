package com.engine.nexus.domain.model;

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record StepDefinition(
        StepId stepId,
        StepType type,
        String activityName,
        RetryPolicy retryPolicy,
        Duration timeout,
        SignalName expectedSignal,
        String compensationActivityName,
        List<StepDefinition> parallelBranches,
        int maxConcurrency,
        StepPredicate condition,
        StepDefinition thenBranch,
        StepDefinition otherwiseBranch,
        String childWorkflowDefinitionId,
        ChildInputMapper childInputMapper
) {
    public StepDefinition {
        Objects.requireNonNull(stepId, "stepId must not be null");
        Objects.requireNonNull(type, "type must not be null");
        parallelBranches = parallelBranches == null ? Collections.emptyList() : List.copyOf(parallelBranches);
    }

    public static StepDefinition activity(StepId stepId, String activityName, RetryPolicy retryPolicy, String compensationActivityName) {
        return new StepDefinition(
                stepId,
                StepType.ACTIVITY,
                activityName,
                retryPolicy != null ? retryPolicy : RetryPolicy.defaultPolicy(),
                Duration.ofMinutes(5),
                null,
                compensationActivityName,
                Collections.emptyList(),
                1,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static StepDefinition signal(StepId stepId, SignalName signalName, Duration timeout) {
        return new StepDefinition(
                stepId,
                StepType.SIGNAL,
                null,
                RetryPolicy.none(),
                timeout != null ? timeout : Duration.ofDays(7),
                signalName,
                null,
                Collections.emptyList(),
                1,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static StepDefinition sleep(StepId stepId, Duration duration) {
        return new StepDefinition(
                stepId,
                StepType.SLEEP,
                null,
                RetryPolicy.none(),
                duration,
                null,
                null,
                Collections.emptyList(),
                1,
                null,
                null,
                null,
                null,
                null
        );
    }

    public static StepDefinition parallel(StepId stepId, List<StepDefinition> branches, int maxConcurrency) {
        return new StepDefinition(
                stepId,
                StepType.PARALLEL,
                null,
                RetryPolicy.none(),
                Duration.ofHours(1),
                null,
                null,
                branches,
                Math.max(1, maxConcurrency),
                null,
                null,
                null,
                null,
                null
        );
    }

    public static StepDefinition conditional(StepId stepId, StepPredicate condition, StepDefinition thenBranch, StepDefinition otherwiseBranch) {
        return new StepDefinition(
                stepId,
                StepType.CONDITIONAL,
                null,
                RetryPolicy.none(),
                Duration.ofMinutes(10),
                null,
                null,
                Collections.emptyList(),
                1,
                condition,
                thenBranch,
                otherwiseBranch,
                null,
                null
        );
    }

    public static StepDefinition child(StepId stepId, String childWorkflowDefinitionId, ChildInputMapper childInputMapper) {
        return new StepDefinition(
                stepId,
                StepType.CHILD_WORKFLOW,
                null,
                RetryPolicy.none(),
                Duration.ofHours(24),
                null,
                null,
                Collections.emptyList(),
                1,
                null,
                null,
                null,
                childWorkflowDefinitionId,
                childInputMapper
        );
    }

    public Optional<String> getCompensationActivity() {
        return Optional.ofNullable(compensationActivityName);
    }
}

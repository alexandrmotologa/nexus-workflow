package com.engine.nexus.application.dsl;

import com.engine.nexus.domain.model.RetryPolicy;
import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.StepDefinition;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowDefinition;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Workflow {
    private Workflow() {}

    public static Builder define(String workflowId) {
        return new Builder(workflowId);
    }

    public static class Builder {
        private final String id;
        private int version = 1;
        private final List<StepDefinition> steps = new ArrayList<>();
        private String failureHandlerActivity;

        public Builder(String id) {
            this.id = Objects.requireNonNull(id, "Workflow id must not be null");
        }

        public Builder version(int version) {
            this.version = version;
            return this;
        }

        public Builder step(String stepId, String activityName) {
            return step(stepId, activityName, RetryPolicy.defaultPolicy(), null);
        }

        public Builder step(String stepId, String activityName, RetryPolicy retryPolicy) {
            return step(stepId, activityName, retryPolicy, null);
        }

        public Builder step(String stepId, String activityName, RetryPolicy retryPolicy, String compensationActivity) {
            steps.add(StepDefinition.activity(StepId.of(stepId), activityName, retryPolicy, compensationActivity));
            return this;
        }

        public Builder step(StepDefinition stepDefinition) {
            steps.add(Objects.requireNonNull(stepDefinition, "stepDefinition must not be null"));
            return this;
        }

        public Builder waitForSignal(String signalName, Duration timeout) {
            return waitForSignal("wait-signal-" + signalName, signalName, timeout);
        }

        public Builder waitForSignal(String stepId, String signalName, Duration timeout) {
            steps.add(StepDefinition.signal(StepId.of(stepId), SignalName.of(signalName), timeout));
            return this;
        }

        public Builder sleep(String stepId, Duration duration) {
            steps.add(StepDefinition.sleep(StepId.of(stepId), duration));
            return this;
        }

        public Builder parallel(String stepId, List<StepDefinition> branches, int maxConcurrency) {
            steps.add(StepDefinition.parallel(StepId.of(stepId), branches, maxConcurrency));
            return this;
        }

        public Builder onFailure(String failureActivity) {
            this.failureHandlerActivity = failureActivity;
            return this;
        }

        public WorkflowDefinition build() {
            return new WorkflowDefinition(id, version, List.copyOf(steps), failureHandlerActivity);
        }
    }
}

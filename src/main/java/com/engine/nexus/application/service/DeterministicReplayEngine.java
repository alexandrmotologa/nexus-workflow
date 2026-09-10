package com.engine.nexus.application.service;

import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.exception.NonDeterministicException;
import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.StepDefinition;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.StepType;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.model.WorkflowStatus;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class DeterministicReplayEngine {

    public record ReplayState(
            WorkflowId workflowId,
            WorkflowStatus status,
            StepId currentStepId,
            Map<String, Object> input,
            Map<String, Object> state,
            Map<StepId, Map<String, Object>> completedStepOutputs,
            List<StepId> completedStepOrder,
            Map<SignalName, Map<String, Object>> receivedSignals,
            long lastSequenceNumber,
            String failureReason
    ) {
        public boolean isStepCompleted(StepId stepId) {
            return completedStepOutputs.containsKey(stepId);
        }

        public Map<String, Object> getStepOutput(StepId stepId) {
            return completedStepOutputs.getOrDefault(stepId, Collections.emptyMap());
        }

        public boolean hasSignal(SignalName signalName) {
            return receivedSignals.containsKey(signalName);
        }

        public Map<String, Object> getSignalPayload(SignalName signalName) {
            return receivedSignals.getOrDefault(signalName, Collections.emptyMap());
        }
    }

    public ReplayState replay(WorkflowId workflowId, List<NexusDomainEvent> events) {
        if (events == null || events.isEmpty()) {
            return new ReplayState(
                    workflowId,
                    WorkflowStatus.PENDING,
                    null,
                    new HashMap<>(),
                    new HashMap<>(),
                    new HashMap<>(),
                    new ArrayList<>(),
                    new HashMap<>(),
                    0,
                    null
            );
        }

        WorkflowStatus status = WorkflowStatus.PENDING;
        StepId currentStepId = null;
        Map<String, Object> input = new HashMap<>();
        Map<String, Object> state = new HashMap<>();
        Map<StepId, Map<String, Object>> completedStepOutputs = new HashMap<>();
        List<StepId> completedStepOrder = new ArrayList<>();
        Map<SignalName, Map<String, Object>> receivedSignals = new HashMap<>();
        long lastSeq = 0;
        String failureReason = null;

        for (NexusDomainEvent event : events) {
            lastSeq = Math.max(lastSeq, event.sequenceNumber());

            switch (event) {
                case NexusDomainEvent.WorkflowStartedEvent e -> {
                    status = WorkflowStatus.RUNNING;
                    if (e.inputPayload() != null) {
                        input.putAll(e.inputPayload());
                        state.putAll(e.inputPayload());
                    }
                }
                case NexusDomainEvent.StepStartedEvent e -> {
                    currentStepId = e.stepId();
                    status = WorkflowStatus.RUNNING;
                }
                case NexusDomainEvent.StepCompletedEvent e -> {
                    completedStepOutputs.put(e.stepId(), e.stepOutput() != null ? e.stepOutput() : Collections.emptyMap());
                    completedStepOrder.add(e.stepId());
                    if (e.stepOutput() != null) {
                        state.putAll(e.stepOutput());
                    }
                }
                case NexusDomainEvent.StepFailedEvent e -> {
                    failureReason = e.errorMessage();
                }
                case NexusDomainEvent.SignalWaitingEvent e -> {
                    currentStepId = e.stepId();
                    status = WorkflowStatus.WAITING_SIGNAL;
                }
                case NexusDomainEvent.SignalReceivedEvent e -> {
                    receivedSignals.put(e.signalName(), e.signalPayload() != null ? e.signalPayload() : Collections.emptyMap());
                    if (e.signalPayload() != null) {
                        state.putAll(e.signalPayload());
                    }
                }
                case NexusDomainEvent.SleepScheduledEvent e -> {
                    currentStepId = e.stepId();
                    status = WorkflowStatus.SLEEPING;
                }
                case NexusDomainEvent.WorkflowCompletedEvent e -> {
                    status = WorkflowStatus.COMPLETED;
                    currentStepId = null;
                    if (e.finalOutput() != null) {
                        state.putAll(e.finalOutput());
                    }
                }
                case NexusDomainEvent.WorkflowFailedEvent e -> {
                    status = WorkflowStatus.FAILED;
                    failureReason = e.reason();
                }
                case NexusDomainEvent.WorkflowCompensatedEvent e -> {
                    status = WorkflowStatus.FAILED;
                    failureReason = "Compensated: " + e.failureReason();
                }
                case NexusDomainEvent.StepCompensatedEvent ignored -> {}
            }
        }

        return new ReplayState(
                workflowId,
                status,
                currentStepId,
                input,
                state,
                completedStepOutputs,
                completedStepOrder,
                receivedSignals,
                lastSeq,
                failureReason
        );
    }

    public void validateDeterminism(WorkflowDefinition definition, ReplayState replayState) {
        List<StepId> recordedOrder = replayState.completedStepOrder();
        List<StepDefinition> steps = definition.steps();

        int recordedIdx = 0;
        for (StepDefinition stepDef : steps) {
            if (recordedIdx >= recordedOrder.size()) {
                break;
            }

            StepId recordedStepId = recordedOrder.get(recordedIdx);

            if (stepDef.type() == StepType.PARALLEL) {
                // For parallel steps, verify that recorded step is one of the branch steps or the parallel step
                boolean matchesBranch = stepDef.parallelBranches().stream()
                        .anyMatch(b -> b.stepId().equals(recordedStepId));
                if (!matchesBranch && !stepDef.stepId().equals(recordedStepId)) {
                    throw new NonDeterministicException(
                            replayState.workflowId(),
                            stepDef.stepId(),
                            recordedStepId,
                            "Parallel step branching mismatch in definition"
                    );
                }
                recordedIdx++;
            } else {
                if (!stepDef.stepId().equals(recordedStepId)) {
                    throw new NonDeterministicException(
                            replayState.workflowId(),
                            stepDef.stepId(),
                            recordedStepId,
                            "Workflow definition step sequence has diverged from recorded execution history"
                    );
                }
                recordedIdx++;
            }
        }
    }
}

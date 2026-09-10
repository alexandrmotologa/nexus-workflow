package com.engine.nexus.application.service;

import com.engine.nexus.application.dsl.CompensationFunction;
import com.engine.nexus.application.dsl.WorkflowContext;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.StepDefinition;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.port.out.WorkflowEventStorePort;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class SagaCompensationCoordinator {
    private final ActivityRegistry activityRegistry;
    private final WorkflowEventStorePort eventStore;

    public SagaCompensationCoordinator(ActivityRegistry activityRegistry, WorkflowEventStorePort eventStore) {
        this.activityRegistry = activityRegistry;
        this.eventStore = eventStore;
    }

    public void rollback(
            WorkflowDefinition definition,
            WorkflowId workflowId,
            List<StepId> completedSteps,
            Map<String, Object> finalState,
            String failureReason,
            long currentSeq
    ) {
        long seq = currentSeq;

        // Walk backwards through completed steps
        List<StepId> reversedSteps = new java.util.ArrayList<>(completedSteps);
        Collections.reverse(reversedSteps);

        for (StepId stepId : reversedSteps) {
            Optional<StepDefinition> stepOpt = definition.findStep(stepId);
            if (stepOpt.isPresent() && stepOpt.get().getCompensationActivity().isPresent()) {
                String compActivity = stepOpt.get().getCompensationActivity().get();
                Optional<CompensationFunction> compFn = activityRegistry.findCompensation(compActivity);

                seq++;
                if (compFn.isPresent()) {
                    try {
                        WorkflowContext ctx = new WorkflowContext(workflowId, stepId, finalState, finalState);
                        compFn.get().compensate(ctx);
                        eventStore.appendEvent(new NexusDomainEvent.StepCompensatedEvent(
                                workflowId,
                                seq,
                                Instant.now(),
                                stepId,
                                compActivity,
                                "SUCCESS"
                        ));
                    } catch (Exception e) {
                        eventStore.appendEvent(new NexusDomainEvent.StepCompensatedEvent(
                                workflowId,
                                seq,
                                Instant.now(),
                                stepId,
                                compActivity,
                                "ERROR: " + e.getMessage()
                        ));
                    }
                } else {
                    eventStore.appendEvent(new NexusDomainEvent.StepCompensatedEvent(
                            workflowId,
                            seq,
                            Instant.now(),
                            stepId,
                            compActivity,
                            "SKIPPED_UNREGISTERED"
                    ));
                }
            }
        }

        // Check for global failure handler on definition
        if (definition.getFailureHandler().isPresent()) {
            String globalComp = definition.getFailureHandler().get();
            Optional<CompensationFunction> compFn = activityRegistry.findCompensation(globalComp);
            if (compFn.isPresent()) {
                try {
                    WorkflowContext ctx = new WorkflowContext(workflowId, null, finalState, finalState);
                    compFn.get().compensate(ctx);
                } catch (Exception ignored) {
                }
            }
        }

        seq++;
        eventStore.appendEvent(new NexusDomainEvent.WorkflowCompensatedEvent(
                workflowId,
                seq,
                Instant.now(),
                failureReason
        ));
    }
}

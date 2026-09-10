package com.engine.nexus;

import com.engine.nexus.application.dsl.Workflow;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.application.registry.WorkflowDefinitionRegistry;
import com.engine.nexus.application.service.DeterministicReplayEngine;
import com.engine.nexus.application.service.SagaCompensationCoordinator;
import com.engine.nexus.application.service.WorkflowEngineImpl;
import com.engine.nexus.domain.model.StepDefinition;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.model.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkflowConditionalTest {

    @Test
    @DisplayName("Should execute then-branch when condition evaluates to true")
    void shouldExecuteThenBranchWhenConditionIsTrue() {
        DeterministicReplayTest.InMemoryEventStore eventStore = new DeterministicReplayTest.InMemoryEventStore();
        ActivityRegistry activityRegistry = new ActivityRegistry();
        WorkflowDefinitionRegistry workflowRegistry = new WorkflowDefinitionRegistry();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        SagaCompensationCoordinator compensationCoordinator = new SagaCompensationCoordinator(activityRegistry, eventStore);
        DeterministicReplayTest.NoOpTimerPort timerPort = new DeterministicReplayTest.NoOpTimerPort();

        activityRegistry.registerActivity("CalculateScore", ctx -> Map.of("riskScore", 0.95));
        activityRegistry.registerActivity("ManualReview", ctx -> Map.of("actionTaken", "MANUAL_REVIEW"));
        activityRegistry.registerActivity("AutoApprove", ctx -> Map.of("actionTaken", "AUTO_APPROVE"));

        WorkflowDefinition def = Workflow.define("fraud-check-flow")
                .step("calc-score", "CalculateScore")
                .choose("eval-risk",
                        state -> ((Number) state.getOrDefault("riskScore", 0.0)).doubleValue() > 0.8,
                        StepDefinition.activity(StepId.of("manual-review"), "ManualReview", null, null),
                        StepDefinition.activity(StepId.of("auto-approve"), "AutoApprove", null, null)
                )
                .build();
        workflowRegistry.register(def);

        WorkflowEngineImpl engine = new WorkflowEngineImpl(
                workflowRegistry,
                activityRegistry,
                eventStore,
                replayEngine,
                compensationCoordinator,
                timerPort
        );

        WorkflowId wfId = WorkflowId.of("wf_cond_true_01");
        engine.startWorkflow(wfId, def.id(), Map.of());

        // Wait brief moment for virtual thread to run
        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        WorkflowInstance instance = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(instance);
        assertEquals(WorkflowStatus.COMPLETED, instance.getStatus());
        assertEquals("MANUAL_REVIEW", instance.getOutputPayload().get("actionTaken"));
    }

    @Test
    @DisplayName("Should execute otherwise-branch when condition evaluates to false")
    void shouldExecuteOtherwiseBranchWhenConditionIsFalse() {
        DeterministicReplayTest.InMemoryEventStore eventStore = new DeterministicReplayTest.InMemoryEventStore();
        ActivityRegistry activityRegistry = new ActivityRegistry();
        WorkflowDefinitionRegistry workflowRegistry = new WorkflowDefinitionRegistry();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        SagaCompensationCoordinator compensationCoordinator = new SagaCompensationCoordinator(activityRegistry, eventStore);
        DeterministicReplayTest.NoOpTimerPort timerPort = new DeterministicReplayTest.NoOpTimerPort();

        activityRegistry.registerActivity("CalculateScore", ctx -> Map.of("riskScore", 0.35));
        activityRegistry.registerActivity("ManualReview", ctx -> Map.of("actionTaken", "MANUAL_REVIEW"));
        activityRegistry.registerActivity("AutoApprove", ctx -> Map.of("actionTaken", "AUTO_APPROVE"));

        WorkflowDefinition def = Workflow.define("fraud-check-low-risk")
                .step("calc-score", "CalculateScore")
                .choose("eval-risk",
                        state -> ((Number) state.getOrDefault("riskScore", 0.0)).doubleValue() > 0.8,
                        StepDefinition.activity(StepId.of("manual-review"), "ManualReview", null, null),
                        StepDefinition.activity(StepId.of("auto-approve"), "AutoApprove", null, null)
                )
                .build();
        workflowRegistry.register(def);

        WorkflowEngineImpl engine = new WorkflowEngineImpl(
                workflowRegistry,
                activityRegistry,
                eventStore,
                replayEngine,
                compensationCoordinator,
                timerPort
        );

        WorkflowId wfId = WorkflowId.of("wf_cond_false_01");
        engine.startWorkflow(wfId, def.id(), Map.of());

        try { Thread.sleep(200); } catch (InterruptedException ignored) {}

        WorkflowInstance instance = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(instance);
        assertEquals(WorkflowStatus.COMPLETED, instance.getStatus());
        assertEquals("AUTO_APPROVE", instance.getOutputPayload().get("actionTaken"));
    }
}

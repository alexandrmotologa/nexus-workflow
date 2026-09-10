package com.engine.nexus;

import com.engine.nexus.application.dsl.Workflow;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.application.registry.WorkflowDefinitionRegistry;
import com.engine.nexus.application.service.DeterministicReplayEngine;
import com.engine.nexus.application.service.SagaCompensationCoordinator;
import com.engine.nexus.application.service.WorkflowEngineImpl;
import com.engine.nexus.domain.model.RetryPolicy;
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

class OperatorInterventionTest {

    @Test
    @DisplayName("Should allow operator to skip a problematic step and finish workflow")
    void shouldAllowOperatorToSkipStep() {
        DeterministicReplayTest.InMemoryEventStore eventStore = new DeterministicReplayTest.InMemoryEventStore();
        ActivityRegistry activityRegistry = new ActivityRegistry();
        WorkflowDefinitionRegistry workflowRegistry = new WorkflowDefinitionRegistry();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        SagaCompensationCoordinator compensationCoordinator = new SagaCompensationCoordinator(activityRegistry, eventStore);
        DeterministicReplayTest.NoOpTimerPort timerPort = new DeterministicReplayTest.NoOpTimerPort();

        activityRegistry.registerActivity("StepA", ctx -> Map.of("stepA", "done"));
        activityRegistry.registerActivity("StepB", ctx -> { throw new RuntimeException("External service down"); });
        activityRegistry.registerActivity("StepC", ctx -> Map.of("stepC", "done"));

        WorkflowDefinition def = Workflow.define("operator-skip-flow")
                .step("step-a", "StepA", RetryPolicy.none())
                .step("step-b", "StepB", RetryPolicy.none())
                .step("step-c", "StepC", RetryPolicy.none())
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

        WorkflowId wfId = WorkflowId.of("wf_op_skip_01");
        engine.startWorkflow(wfId, def.id(), Map.of());

        // Wait until step-b fails
        awaitCondition(() -> eventStore.findInstance(wfId)
                .filter(i -> i.getStatus() == WorkflowStatus.FAILED).isPresent(), 1000);

        // Step B failed. Now operator skips step-b:
        engine.skipStep(wfId, StepId.of("step-b"), "Known issue, skipped manually");

        // Wait until workflow completes
        awaitCondition(() -> eventStore.findInstance(wfId)
                .filter(i -> i.getStatus() == WorkflowStatus.COMPLETED).isPresent(), 1000);

        WorkflowInstance finalInstance = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(finalInstance);
        assertEquals(WorkflowStatus.COMPLETED, finalInstance.getStatus());
        assertEquals("done", finalInstance.getOutputPayload().get("stepA"));
        assertEquals("done", finalInstance.getOutputPayload().get("stepC"));
    }

    @Test
    @DisplayName("Should allow operator to override step output with custom payload")
    void shouldAllowOperatorToOverrideStep() {
        DeterministicReplayTest.InMemoryEventStore eventStore = new DeterministicReplayTest.InMemoryEventStore();
        ActivityRegistry activityRegistry = new ActivityRegistry();
        WorkflowDefinitionRegistry workflowRegistry = new WorkflowDefinitionRegistry();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        SagaCompensationCoordinator compensationCoordinator = new SagaCompensationCoordinator(activityRegistry, eventStore);
        DeterministicReplayTest.NoOpTimerPort timerPort = new DeterministicReplayTest.NoOpTimerPort();

        activityRegistry.registerActivity("Step1", ctx -> Map.of("step1", "val1"));
        activityRegistry.registerActivity("Step2", ctx -> { throw new RuntimeException("Third-party API failure"); });
        activityRegistry.registerActivity("Step3", ctx -> Map.of("processedOutput", ctx.get("manualOverrideKey", String.class).orElse("none")));

        WorkflowDefinition def = Workflow.define("operator-override-flow")
                .step("step-1", "Step1", RetryPolicy.none())
                .step("step-2", "Step2", RetryPolicy.none())
                .step("step-3", "Step3", RetryPolicy.none())
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

        WorkflowId wfId = WorkflowId.of("wf_op_override_01");
        engine.startWorkflow(wfId, def.id(), Map.of());

        // Wait until step-2 fails
        awaitCondition(() -> eventStore.findInstance(wfId)
                .filter(i -> i.getStatus() == WorkflowStatus.FAILED).isPresent(), 1000);

        // Step 2 failed. Operator injects custom output for step-2:
        engine.overrideStep(wfId, StepId.of("step-2"), Map.of("manualOverrideKey", "injected_by_ops"));

        // Wait until workflow completes
        awaitCondition(() -> eventStore.findInstance(wfId)
                .filter(i -> i.getStatus() == WorkflowStatus.COMPLETED).isPresent(), 1000);

        WorkflowInstance finalInstance = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(finalInstance);
        assertEquals(WorkflowStatus.COMPLETED, finalInstance.getStatus());
        assertEquals("injected_by_ops", finalInstance.getOutputPayload().get("processedOutput"));
    }

    private void awaitCondition(java.util.function.BooleanSupplier condition, long timeoutMs) {
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < timeoutMs) {
            if (condition.getAsBoolean()) {
                return;
            }
            try {
                Thread.sleep(20);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
        }
    }
}

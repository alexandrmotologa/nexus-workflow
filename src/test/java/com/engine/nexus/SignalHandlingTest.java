package com.engine.nexus;

import com.engine.nexus.application.dsl.Workflow;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.application.registry.WorkflowDefinitionRegistry;
import com.engine.nexus.application.service.DeterministicReplayEngine;
import com.engine.nexus.application.service.SagaCompensationCoordinator;
import com.engine.nexus.application.service.WorkflowEngineImpl;
import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.model.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SignalHandlingTest {

    @Test
    @DisplayName("Should pause on signal step and resume to completion when signal is delivered")
    void shouldPauseAndResumeOnSignal() {
        DeterministicReplayTest.InMemoryEventStore eventStore = new DeterministicReplayTest.InMemoryEventStore();
        ActivityRegistry activityRegistry = new ActivityRegistry();
        WorkflowDefinitionRegistry workflowRegistry = new WorkflowDefinitionRegistry();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        SagaCompensationCoordinator compensationCoordinator = new SagaCompensationCoordinator(activityRegistry, eventStore);
        DeterministicReplayTest.NoOpTimerPort timerPort = new DeterministicReplayTest.NoOpTimerPort();

        activityRegistry.registerActivity("InitStep", ctx -> Map.of("initDone", true));
        activityRegistry.registerActivity("FinalStep", ctx -> Map.of("finalDone", true));

        WorkflowDefinition signalFlow = Workflow.define("signal-flow")
                .step("step-init", "InitStep")
                .waitForSignal("wait-approval", "USER_APPROVAL", Duration.ofHours(24))
                .step("step-final", "FinalStep")
                .build();
        workflowRegistry.register(signalFlow);

        WorkflowEngineImpl engine = new WorkflowEngineImpl(
                workflowRegistry,
                activityRegistry,
                eventStore,
                replayEngine,
                compensationCoordinator,
                timerPort
        );

        WorkflowId wfId = WorkflowId.of("wf_signal_test_101");
        eventStore.saveInstance(WorkflowInstance.create(wfId, signalFlow.id(), 1, Map.of()));
        eventStore.appendEvent(new NexusDomainEvent.WorkflowStartedEvent(wfId, 1L, java.time.Instant.now(), signalFlow.id(), 1, Map.of()));

        // Run initial execution
        engine.executeWorkflowRun(wfId);

        WorkflowInstance intermediateState = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(intermediateState);
        assertEquals(WorkflowStatus.WAITING_SIGNAL, intermediateState.getStatus());
        assertEquals("wait-approval", intermediateState.getCurrentStepId().get().value());

        // Now inject external signal
        engine.sendSignal(wfId, SignalName.of("USER_APPROVAL"), Map.of("approvedBy", "ChiefComplianceOfficer"));

        // Allow virtual thread execution to complete
        try {
            Thread.sleep(200);
        } catch (InterruptedException ignored) {}

        WorkflowInstance finalState = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(finalState);
        assertEquals(WorkflowStatus.COMPLETED, finalState.getStatus());
        assertEquals("ChiefComplianceOfficer", finalState.getOutputPayload().get("approvedBy"));
        assertEquals(true, finalState.getOutputPayload().get("finalDone"));

        List<NexusDomainEvent> events = eventStore.getEventsForWorkflow(wfId);
        assertTrue(events.stream().anyMatch(e -> e instanceof NexusDomainEvent.SignalWaitingEvent));
        assertTrue(events.stream().anyMatch(e -> e instanceof NexusDomainEvent.SignalReceivedEvent));
        assertTrue(events.stream().anyMatch(e -> e instanceof NexusDomainEvent.WorkflowCompletedEvent));
    }
}

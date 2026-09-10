package com.engine.nexus;

import com.engine.nexus.application.dsl.Workflow;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.application.registry.WorkflowDefinitionRegistry;
import com.engine.nexus.application.service.DeterministicReplayEngine;
import com.engine.nexus.application.service.SagaCompensationCoordinator;
import com.engine.nexus.application.service.WorkflowEngineImpl;
import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.RetryPolicy;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.model.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SagaCompensationTest {

    @Test
    @DisplayName("Should execute compensating activities in reverse order when a downstream step fails")
    void shouldExecuteCompensationsInReverseOrder() {
        DeterministicReplayTest.InMemoryEventStore eventStore = new DeterministicReplayTest.InMemoryEventStore();
        ActivityRegistry activityRegistry = new ActivityRegistry();
        WorkflowDefinitionRegistry workflowRegistry = new WorkflowDefinitionRegistry();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        SagaCompensationCoordinator compensationCoordinator = new SagaCompensationCoordinator(activityRegistry, eventStore);
        DeterministicReplayTest.NoOpTimerPort timerPort = new DeterministicReplayTest.NoOpTimerPort();

        List<String> rollbackJournal = new ArrayList<>();

        activityRegistry.registerActivity("ReserveFunds", ctx -> Map.of("fundsReserved", 150.0));
        activityRegistry.registerCompensation("RefundFunds", ctx -> rollbackJournal.add("REFUND_FUNDS"));

        activityRegistry.registerActivity("ReserveSeat", ctx -> Map.of("seatNumber", "14B"));
        activityRegistry.registerCompensation("CancelSeat", ctx -> rollbackJournal.add("CANCEL_SEAT"));

        activityRegistry.registerActivity("IssueTicket", ctx -> {
            throw new RuntimeException("Airline ticketing service unreachable");
        });

        WorkflowDefinition saga = Workflow.define("flight-reservation-saga")
                .step("step-funds", "ReserveFunds", RetryPolicy.none(), "RefundFunds")
                .step("step-seat", "ReserveSeat", RetryPolicy.none(), "CancelSeat")
                .step("step-ticket", "IssueTicket", RetryPolicy.none(), null)
                .build();
        workflowRegistry.register(saga);

        WorkflowEngineImpl engine = new WorkflowEngineImpl(
                workflowRegistry,
                activityRegistry,
                eventStore,
                replayEngine,
                compensationCoordinator,
                timerPort
        );

        WorkflowId wfId = WorkflowId.of("wf_saga_fail_test");
        eventStore.saveInstance(WorkflowInstance.create(wfId, saga.id(), 1, Map.of()));
        eventStore.appendEvent(new NexusDomainEvent.WorkflowStartedEvent(wfId, 1L, java.time.Instant.now(), saga.id(), 1, Map.of()));

        engine.executeWorkflowRun(wfId);

        WorkflowInstance finalState = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(finalState);
        assertEquals(WorkflowStatus.FAILED, finalState.getStatus());
        assertTrue(finalState.getErrorMessage().orElse("").contains("Compensated after failure"));

        // Verify reverse compensation order: CancelSeat MUST run before RefundFunds!
        assertEquals(2, rollbackJournal.size());
        assertEquals("CANCEL_SEAT", rollbackJournal.get(0), "Seat reservation must be compensated before funds refund");
        assertEquals("REFUND_FUNDS", rollbackJournal.get(1));

        // Verify events in event store
        List<NexusDomainEvent> events = eventStore.getEventsForWorkflow(wfId);
        boolean hasCompensatedEvent = events.stream().anyMatch(e -> e instanceof NexusDomainEvent.WorkflowCompensatedEvent);
        assertTrue(hasCompensatedEvent, "Event store must contain WorkflowCompensatedEvent");
    }
}

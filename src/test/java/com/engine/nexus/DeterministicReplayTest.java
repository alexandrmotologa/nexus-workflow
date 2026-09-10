package com.engine.nexus;

import com.engine.nexus.application.dsl.Workflow;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.application.registry.WorkflowDefinitionRegistry;
import com.engine.nexus.application.service.DeterministicReplayEngine;
import com.engine.nexus.application.service.SagaCompensationCoordinator;
import com.engine.nexus.application.service.WorkflowEngineImpl;
import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.exception.NonDeterministicException;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.model.WorkflowStatus;
import com.engine.nexus.domain.port.out.TimerPort;
import com.engine.nexus.domain.port.out.WorkflowEventStorePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DeterministicReplayTest {

    static class InMemoryEventStore implements WorkflowEventStorePort {
        private final Map<WorkflowId, List<NexusDomainEvent>> eventStore = new ConcurrentHashMap<>();
        private final Map<WorkflowId, WorkflowInstance> instanceStore = new ConcurrentHashMap<>();

        @Override
        public synchronized void appendEvent(NexusDomainEvent event) {
            eventStore.computeIfAbsent(event.workflowId(), k -> new ArrayList<>()).add(event);
        }

        @Override
        public synchronized List<NexusDomainEvent> getEventsForWorkflow(WorkflowId workflowId) {
            return new ArrayList<>(eventStore.getOrDefault(workflowId, Collections.emptyList()));
        }

        @Override
        public synchronized long getNextSequenceNumber(WorkflowId workflowId) {
            List<NexusDomainEvent> events = eventStore.get(workflowId);
            return events == null || events.isEmpty() ? 1L : events.getLast().sequenceNumber() + 1;
        }

        @Override
        public synchronized void saveInstance(WorkflowInstance instance) {
            instanceStore.put(instance.getId(), instance);
        }

        @Override
        public synchronized Optional<WorkflowInstance> findInstance(WorkflowId workflowId) {
            return Optional.ofNullable(instanceStore.get(workflowId));
        }

        @Override
        public synchronized List<WorkflowInstance> findAllInstances(int limit, int offset) {
            return new ArrayList<>(instanceStore.values());
        }

        private final Map<WorkflowId, Map<String, Object>> snapshots = new ConcurrentHashMap<>();
        private final Map<WorkflowId, Long> snapshotSeqs = new ConcurrentHashMap<>();
        private final Map<String, WorkflowId> idempotencyKeys = new ConcurrentHashMap<>();

        @Override
        public synchronized void saveSnapshot(WorkflowId workflowId, long sequenceNumber, Map<String, Object> state) {
            snapshots.put(workflowId, state);
            snapshotSeqs.put(workflowId, sequenceNumber);
        }

        @Override
        public synchronized Optional<Map<String, Object>> getLatestSnapshot(WorkflowId workflowId) {
            return Optional.ofNullable(snapshots.get(workflowId));
        }

        @Override
        public synchronized long getLatestSnapshotSequenceNumber(WorkflowId workflowId) {
            return snapshotSeqs.getOrDefault(workflowId, 0L);
        }

        @Override
        public synchronized boolean tryAcquireIdempotencyKey(String key, WorkflowId workflowId) {
            return idempotencyKeys.putIfAbsent(key, workflowId) == null;
        }

        @Override
        public synchronized Optional<WorkflowId> findWorkflowByIdempotencyKey(String key) {
            return Optional.ofNullable(idempotencyKeys.get(key));
        }
    }

    static class NoOpTimerPort implements TimerPort {
        @Override
        public void scheduleWakeup(WorkflowId workflowId, StepId stepId, Duration delay) {}
    }

    @Test
    @DisplayName("Should replay completed steps from event store without repeating side effects after crash")
    void shouldReplayWithoutRepeatingSideEffects() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        ActivityRegistry activityRegistry = new ActivityRegistry();
        WorkflowDefinitionRegistry workflowRegistry = new WorkflowDefinitionRegistry();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        SagaCompensationCoordinator compensationCoordinator = new SagaCompensationCoordinator(activityRegistry, eventStore);
        TimerPort timerPort = new NoOpTimerPort();

        AtomicInteger step1Executions = new AtomicInteger(0);
        AtomicInteger step2Executions = new AtomicInteger(0);
        AtomicInteger step3Executions = new AtomicInteger(0);

        activityRegistry.registerActivity("Step1Activity", ctx -> {
            step1Executions.incrementAndGet();
            return Map.of("result1", "ok-step1");
        });

        activityRegistry.registerActivity("Step2Activity", ctx -> {
            step2Executions.incrementAndGet();
            return Map.of("result2", "ok-step2");
        });

        activityRegistry.registerActivity("Step3Activity", ctx -> {
            step3Executions.incrementAndGet();
            return Map.of("result3", "ok-step3");
        });

        WorkflowDefinition definition = Workflow.define("crash-resilience-flow")
                .step("step-1", "Step1Activity")
                .step("step-2", "Step2Activity")
                .step("step-3", "Step3Activity")
                .build();
        workflowRegistry.register(definition);

        // Pre-populate event store as if Step 1 and Step 2 had completed before worker node crashed
        WorkflowId wfId = WorkflowId.of("wf_crash_test_001");
        eventStore.saveInstance(WorkflowInstance.create(wfId, definition.id(), 1, Map.of("initKey", "initVal")));

        eventStore.appendEvent(new NexusDomainEvent.WorkflowStartedEvent(wfId, 1L, Instant.now(), definition.id(), 1, Map.of("initKey", "initVal")));
        eventStore.appendEvent(new NexusDomainEvent.StepStartedEvent(wfId, 2L, Instant.now(), StepId.of("step-1"), "ACTIVITY", Map.of()));
        eventStore.appendEvent(new NexusDomainEvent.StepCompletedEvent(wfId, 3L, Instant.now(), StepId.of("step-1"), Map.of("result1", "ok-step1")));
        eventStore.appendEvent(new NexusDomainEvent.StepStartedEvent(wfId, 4L, Instant.now(), StepId.of("step-2"), "ACTIVITY", Map.of()));
        eventStore.appendEvent(new NexusDomainEvent.StepCompletedEvent(wfId, 5L, Instant.now(), StepId.of("step-2"), Map.of("result2", "ok-step2")));

        // Start a fresh new WorkflowEngine instance (simulating worker crash & reboot)
        WorkflowEngineImpl recoveryEngine = new WorkflowEngineImpl(
                workflowRegistry,
                activityRegistry,
                eventStore,
                replayEngine,
                compensationCoordinator,
                timerPort
        );

        // Execute recovery run
        recoveryEngine.executeWorkflowRun(wfId);

        WorkflowInstance finalState = eventStore.findInstance(wfId).orElse(null);
        assertNotNull(finalState);
        assertEquals(WorkflowStatus.COMPLETED, finalState.getStatus());

        // CRITICAL ASSERTION: Step 1 and Step 2 must NEVER be re-executed (count = 0)
        assertEquals(0, step1Executions.get(), "Step 1 activity was re-executed instead of using cached replay output!");
        assertEquals(0, step2Executions.get(), "Step 2 activity was re-executed instead of using cached replay output!");

        // Step 3 was not previously executed, so it ran exactly once
        assertEquals(1, step3Executions.get(), "Step 3 should have executed exactly once during resume");

        // Assert all outputs are present in final state
        assertEquals("ok-step1", finalState.getOutputPayload().get("result1"));
        assertEquals("ok-step2", finalState.getOutputPayload().get("result2"));
        assertEquals("ok-step3", finalState.getOutputPayload().get("result3"));
    }

    @Test
    @DisplayName("Should detect non-deterministic change in workflow definition and throw NonDeterministicException")
    void shouldThrowNonDeterministicExceptionWhenStepOrderChanges() {
        InMemoryEventStore eventStore = new InMemoryEventStore();
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();

        WorkflowId wfId = WorkflowId.of("wf_nondet_test");
        List<NexusDomainEvent> events = List.of(
                new NexusDomainEvent.WorkflowStartedEvent(wfId, 1L, Instant.now(), "flow", 1, Map.of()),
                new NexusDomainEvent.StepCompletedEvent(wfId, 2L, Instant.now(), StepId.of("step-A"), Map.of()),
                new NexusDomainEvent.StepCompletedEvent(wfId, 3L, Instant.now(), StepId.of("step-B"), Map.of())
        );

        DeterministicReplayEngine.ReplayState replayState = replayEngine.replay(wfId, events);

        // A modified workflow definition where step-C was placed before step-B
        WorkflowDefinition modifiedDefinition = Workflow.define("flow")
                .step("step-A", "ActA")
                .step("step-C", "ActC") // Divergence!
                .step("step-B", "ActB")
                .build();

        assertThrows(NonDeterministicException.class, () -> {
            replayEngine.validateDeterminism(modifiedDefinition, replayState);
        });
    }
}

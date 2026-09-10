package com.engine.nexus.application.service;

import com.engine.nexus.application.dsl.ActivityFunction;
import com.engine.nexus.application.dsl.WorkflowContext;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.exception.WorkflowExecutionException;
import com.engine.nexus.domain.exception.WorkflowNotFoundException;
import com.engine.nexus.domain.model.RetryPolicy;
import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.StepDefinition;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.StepType;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.model.WorkflowStatus;
import com.engine.nexus.domain.port.in.CancelWorkflowUseCase;
import com.engine.nexus.domain.port.in.QueryWorkflowQuery;
import com.engine.nexus.domain.port.in.SignalWorkflowUseCase;
import com.engine.nexus.domain.port.in.StartWorkflowUseCase;
import com.engine.nexus.domain.port.out.TimerPort;
import com.engine.nexus.domain.port.out.WorkflowEventStorePort;
import com.engine.nexus.domain.port.out.WorkflowRegistryPort;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;

public class WorkflowEngineImpl implements
        StartWorkflowUseCase,
        SignalWorkflowUseCase,
        QueryWorkflowQuery,
        CancelWorkflowUseCase {

    private final WorkflowRegistryPort workflowRegistry;
    private final ActivityRegistry activityRegistry;
    private final WorkflowEventStorePort eventStore;
    private final DeterministicReplayEngine replayEngine;
    private final SagaCompensationCoordinator compensationCoordinator;
    private final TimerPort timerPort;
    private final ExecutorService virtualExecutor;
    private final Map<WorkflowId, Object> instanceLocks = new ConcurrentHashMap<>();
    private final Random random = new Random();

    public WorkflowEngineImpl(
            WorkflowRegistryPort workflowRegistry,
            ActivityRegistry activityRegistry,
            WorkflowEventStorePort eventStore,
            DeterministicReplayEngine replayEngine,
            SagaCompensationCoordinator compensationCoordinator,
            TimerPort timerPort
    ) {
        this.workflowRegistry = workflowRegistry;
        this.activityRegistry = activityRegistry;
        this.eventStore = eventStore;
        this.replayEngine = replayEngine;
        this.compensationCoordinator = compensationCoordinator;
        this.timerPort = timerPort;
        this.virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public WorkflowInstance startWorkflow(String definitionId, Map<String, Object> input) {
        return startWorkflow(WorkflowId.generate(), definitionId, input);
    }

    @Override
    public WorkflowInstance startWorkflow(WorkflowId customId, String definitionId, Map<String, Object> input) {
        WorkflowDefinition definition = workflowRegistry.getDefinition(definitionId)
                .orElseThrow(() -> new IllegalArgumentException("Unknown workflow definition: " + definitionId));

        WorkflowInstance instance = WorkflowInstance.create(customId, definition.id(), definition.version(), input);
        eventStore.saveInstance(instance);

        NexusDomainEvent.WorkflowStartedEvent startEvent = new NexusDomainEvent.WorkflowStartedEvent(
                customId,
                1L,
                Instant.now(),
                definition.id(),
                definition.version(),
                input
        );
        eventStore.appendEvent(startEvent);

        virtualExecutor.submit(() -> executeWorkflowRun(customId));
        return instance;
    }

    @Override
    public void sendSignal(WorkflowId workflowId, SignalName signalName, Map<String, Object> payload) {
        WorkflowInstance instance = getWorkflowInstance(workflowId);
        if (instance.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot deliver signal to terminated workflow: " + workflowId);
        }

        long nextSeq = eventStore.getNextSequenceNumber(workflowId);
        NexusDomainEvent.SignalReceivedEvent signalEvent = new NexusDomainEvent.SignalReceivedEvent(
                workflowId,
                nextSeq,
                Instant.now(),
                signalName,
                payload != null ? payload : Collections.emptyMap()
        );
        eventStore.appendEvent(signalEvent);

        virtualExecutor.submit(() -> executeWorkflowRun(workflowId));
    }

    @Override
    public WorkflowInstance getWorkflowInstance(WorkflowId workflowId) {
        return eventStore.findInstance(workflowId)
                .orElseThrow(() -> new WorkflowNotFoundException(workflowId));
    }

    @Override
    public List<NexusDomainEvent> getWorkflowHistory(WorkflowId workflowId) {
        return eventStore.getEventsForWorkflow(workflowId);
    }

    @Override
    public List<WorkflowInstance> listWorkflows(int limit, int offset) {
        return eventStore.findAllInstances(limit, offset);
    }

    @Override
    public void cancelWorkflow(WorkflowId workflowId, String reason) {
        WorkflowInstance instance = getWorkflowInstance(workflowId);
        if (instance.getStatus().isTerminal()) {
            return;
        }

        long nextSeq = eventStore.getNextSequenceNumber(workflowId);
        eventStore.appendEvent(new NexusDomainEvent.WorkflowFailedEvent(
                workflowId,
                nextSeq,
                Instant.now(),
                "CANCELLED: " + reason
        ));

        instance.cancel(reason);
        eventStore.saveInstance(instance);
    }

    public void executeWorkflowRun(WorkflowId workflowId) {
        Object lock = instanceLocks.computeIfAbsent(workflowId, k -> new Object());
        synchronized (lock) {
            try {
                WorkflowInstance instance = getWorkflowInstance(workflowId);
                if (instance.getStatus().isTerminal()) {
                    return;
                }

                WorkflowDefinition definition = workflowRegistry.getDefinition(instance.getDefinitionId())
                        .orElseThrow(() -> new IllegalStateException("Definition missing for workflow: " + instance.getDefinitionId()));

                List<NexusDomainEvent> events = eventStore.getEventsForWorkflow(workflowId);
                DeterministicReplayEngine.ReplayState replayState = replayEngine.replay(workflowId, events);
                replayEngine.validateDeterminism(definition, replayState);

                long currentSeq = replayState.lastSequenceNumber();
                Map<String, Object> runState = new HashMap<>(replayState.state());
                List<StepId> completedSteps = new ArrayList<>(replayState.completedStepOrder());

                for (StepDefinition stepDef : definition.steps()) {
                    // Check if already completed in past replay
                    if (replayState.isStepCompleted(stepDef.stepId())) {
                        runState.putAll(replayState.getStepOutput(stepDef.stepId()));
                        continue;
                    }

                    // Process according to StepType
                    switch (stepDef.type()) {
                        case ACTIVITY -> {
                            currentSeq++;
                            eventStore.appendEvent(new NexusDomainEvent.StepStartedEvent(
                                    workflowId,
                                    currentSeq,
                                    Instant.now(),
                                    stepDef.stepId(),
                                    stepDef.type().name(),
                                    runState
                            ));
                            instance.transitionToStep(stepDef.stepId());
                            eventStore.saveInstance(instance);

                            Map<String, Object> output = executeWithRetry(workflowId, stepDef, runState);
                            if (output != null) {
                                currentSeq++;
                                eventStore.appendEvent(new NexusDomainEvent.StepCompletedEvent(
                                        workflowId,
                                        currentSeq,
                                        Instant.now(),
                                        stepDef.stepId(),
                                        output
                                ));
                                runState.putAll(output);
                                completedSteps.add(stepDef.stepId());
                                instance.updateOutput(output);
                                eventStore.saveInstance(instance);
                            } else {
                                // Step failed permanently
                                handleStepFailure(definition, instance, stepDef, completedSteps, runState, currentSeq, "Max retries exceeded");
                                return;
                            }
                        }

                        case PARALLEL -> {
                            currentSeq++;
                            eventStore.appendEvent(new NexusDomainEvent.StepStartedEvent(
                                    workflowId,
                                    currentSeq,
                                    Instant.now(),
                                    stepDef.stepId(),
                                    stepDef.type().name(),
                                    runState
                            ));
                            instance.transitionToStep(stepDef.stepId());
                            eventStore.saveInstance(instance);

                            Map<String, Object> parallelOutputs = executeParallelBranches(workflowId, stepDef, runState, replayState);
                            if (parallelOutputs != null) {
                                currentSeq++;
                                eventStore.appendEvent(new NexusDomainEvent.StepCompletedEvent(
                                        workflowId,
                                        currentSeq,
                                        Instant.now(),
                                        stepDef.stepId(),
                                        parallelOutputs
                                ));
                                runState.putAll(parallelOutputs);
                                completedSteps.add(stepDef.stepId());
                                instance.updateOutput(parallelOutputs);
                                eventStore.saveInstance(instance);
                            } else {
                                handleStepFailure(definition, instance, stepDef, completedSteps, runState, currentSeq, "Parallel branch failed");
                                return;
                            }
                        }

                        case SIGNAL -> {
                            SignalName signalName = stepDef.expectedSignal();
                            if (replayState.hasSignal(signalName)) {
                                Map<String, Object> signalPayload = replayState.getSignalPayload(signalName);
                                currentSeq++;
                                eventStore.appendEvent(new NexusDomainEvent.StepCompletedEvent(
                                        workflowId,
                                        currentSeq,
                                        Instant.now(),
                                        stepDef.stepId(),
                                        signalPayload
                                ));
                                runState.putAll(signalPayload);
                                completedSteps.add(stepDef.stepId());
                                instance.updateOutput(signalPayload);
                                eventStore.saveInstance(instance);
                            } else {
                                currentSeq++;
                                eventStore.appendEvent(new NexusDomainEvent.SignalWaitingEvent(
                                        workflowId,
                                        currentSeq,
                                        Instant.now(),
                                        stepDef.stepId(),
                                        signalName,
                                        stepDef.timeout()
                                ));
                                instance.setWaitingForSignal(stepDef.stepId());
                                eventStore.saveInstance(instance);
                                return; // Stop until signal arrives
                            }
                        }

                        case SLEEP -> {
                            currentSeq++;
                            eventStore.appendEvent(new NexusDomainEvent.SleepScheduledEvent(
                                    workflowId,
                                    currentSeq,
                                    Instant.now(),
                                    stepDef.stepId(),
                                    stepDef.timeout()
                            ));
                            instance.setSleeping(stepDef.stepId());
                            eventStore.saveInstance(instance);

                            timerPort.scheduleWakeup(workflowId, stepDef.stepId(), stepDef.timeout());
                            return; // Stop until timer wakes up
                        }

                        default -> {}
                    }
                }

                // All steps completed successfully!
                currentSeq++;
                eventStore.appendEvent(new NexusDomainEvent.WorkflowCompletedEvent(
                        workflowId,
                        currentSeq,
                        Instant.now(),
                        runState
                ));
                instance.complete(runState);
                eventStore.saveInstance(instance);

            } catch (Exception e) {
                WorkflowInstance instance = eventStore.findInstance(workflowId).orElse(null);
                if (instance != null && !instance.getStatus().isTerminal()) {
                    long currentSeq = eventStore.getNextSequenceNumber(workflowId);
                    eventStore.appendEvent(new NexusDomainEvent.WorkflowFailedEvent(
                            workflowId,
                            currentSeq,
                            Instant.now(),
                            e.getMessage()
                    ));
                    instance.fail(e.getMessage());
                    eventStore.saveInstance(instance);
                }
            } finally {
                instanceLocks.remove(workflowId);
            }
        }
    }

    private Map<String, Object> executeWithRetry(WorkflowId workflowId, StepDefinition stepDef, Map<String, Object> currentState) {
        String activityName = stepDef.activityName();
        Optional<ActivityFunction> activityOpt = activityRegistry.findActivity(activityName);
        if (activityOpt.isEmpty()) {
            throw new WorkflowExecutionException(workflowId, stepDef.stepId(), "Activity not registered: " + activityName);
        }

        ActivityFunction activity = activityOpt.get();
        RetryPolicy policy = stepDef.retryPolicy();
        int attempt = 0;
        Duration delay = policy.initialInterval();

        while (attempt < policy.maxAttempts()) {
            attempt++;
            try {
                WorkflowContext ctx = new WorkflowContext(workflowId, stepDef.stepId(), currentState, currentState);
                Map<String, Object> result = activity.execute(ctx);
                return result != null ? result : Collections.emptyMap();
            } catch (Exception ex) {
                long currentSeq = eventStore.getNextSequenceNumber(workflowId);
                eventStore.appendEvent(new NexusDomainEvent.StepFailedEvent(
                        workflowId,
                        currentSeq,
                        Instant.now(),
                        stepDef.stepId(),
                        ex.getMessage(),
                        attempt
                ));

                if (attempt >= policy.maxAttempts()) {
                    return null;
                }

                try {
                    // Exponential backoff + jitter calculation
                    long delayMillis = delay.toMillis();
                    if (policy.jitterFactor() > 0.0) {
                        double jitterRange = delayMillis * policy.jitterFactor();
                        double jitterOffset = (random.nextDouble() * 2.0 - 1.0) * jitterRange;
                        delayMillis = Math.max(10, (long) (delayMillis + jitterOffset));
                    }
                    Thread.sleep(delayMillis);
                    delay = Duration.ofMillis((long) Math.min(policy.maxInterval().toMillis(), delay.toMillis() * policy.backoffMultiplier()));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return null;
                }
            }
        }
        return null;
    }

    private Map<String, Object> executeParallelBranches(
            WorkflowId workflowId,
            StepDefinition parallelStep,
            Map<String, Object> currentState,
            DeterministicReplayEngine.ReplayState replayState
    ) {
        List<StepDefinition> branches = parallelStep.parallelBranches();
        if (branches.isEmpty()) {
            return Collections.emptyMap();
        }

        Semaphore semaphore = new Semaphore(parallelStep.maxConcurrency());
        Map<String, Object> combinedOutputs = new ConcurrentHashMap<>();
        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (StepDefinition branch : branches) {
            // If already replayed
            if (replayState.isStepCompleted(branch.stepId())) {
                combinedOutputs.putAll(replayState.getStepOutput(branch.stepId()));
                continue;
            }

            CompletableFuture<Void> future = CompletableFuture.runAsync(() -> {
                try {
                    semaphore.acquire();
                    Map<String, Object> branchOut = executeWithRetry(workflowId, branch, currentState);
                    if (branchOut == null) {
                        throw new RuntimeException("Parallel branch failed: " + branch.stepId());
                    }
                    combinedOutputs.putAll(branchOut);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Parallel branch interrupted", e);
                } finally {
                    semaphore.release();
                }
            }, virtualExecutor);
            futures.add(future);
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
            return combinedOutputs;
        } catch (Exception ex) {
            return null;
        }
    }

    private void handleStepFailure(
            WorkflowDefinition definition,
            WorkflowInstance instance,
            StepDefinition failedStep,
            List<StepId> completedSteps,
            Map<String, Object> state,
            long currentSeq,
            String reason
    ) {
        instance.startCompensation(failedStep.stepId());
        eventStore.saveInstance(instance);

        compensationCoordinator.rollback(
                definition,
                instance.getId(),
                completedSteps,
                state,
                reason,
                currentSeq
        );

        instance.finishCompensation(reason);
        eventStore.saveInstance(instance);
    }
}

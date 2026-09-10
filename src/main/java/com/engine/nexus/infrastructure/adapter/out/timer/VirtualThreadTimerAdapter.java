package com.engine.nexus.infrastructure.adapter.out.timer;

import com.engine.nexus.application.service.WorkflowEngineImpl;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.port.out.TimerPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Component
public class VirtualThreadTimerAdapter implements TimerPort {
    private static final Logger log = LoggerFactory.getLogger(VirtualThreadTimerAdapter.class);

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(Thread.ofVirtual().factory());
    private WorkflowEngineImpl workflowEngine;

    public VirtualThreadTimerAdapter() {}

    @Lazy
    public void setWorkflowEngine(WorkflowEngineImpl workflowEngine) {
        this.workflowEngine = workflowEngine;
    }

    @Override
    public void scheduleWakeup(WorkflowId workflowId, StepId stepId, Duration delay) {
        log.info("Scheduling virtual-thread timer wakeup for workflow: {} on step: {} in {}ms",
                workflowId, stepId, delay.toMillis());

        scheduler.schedule(() -> {
            try {
                log.info("Timer expired for workflow: {}, resuming execution", workflowId);
                if (workflowEngine != null) {
                    workflowEngine.executeWorkflowRun(workflowId);
                }
            } catch (Exception e) {
                log.error("Error resuming workflow {} after timer wakeup: {}", workflowId, e.getMessage(), e);
            }
        }, Math.max(1, delay.toMillis()), TimeUnit.MILLISECONDS);
    }
}

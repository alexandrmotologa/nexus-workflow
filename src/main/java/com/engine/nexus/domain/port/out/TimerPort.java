package com.engine.nexus.domain.port.out;

import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;

import java.time.Duration;

public interface TimerPort {
    void scheduleWakeup(WorkflowId workflowId, StepId stepId, Duration delay);
}

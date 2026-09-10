package com.engine.nexus.domain.port.in;

import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;

import java.util.Map;

public interface OperatorInterventionUseCase {
    void retryStep(WorkflowId workflowId, StepId stepId);
    void skipStep(WorkflowId workflowId, StepId stepId, String reason);
    void overrideStep(WorkflowId workflowId, StepId stepId, Map<String, Object> customOutput);
}

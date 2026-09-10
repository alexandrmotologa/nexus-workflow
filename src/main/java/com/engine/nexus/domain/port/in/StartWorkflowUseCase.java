package com.engine.nexus.domain.port.in;

import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;

import java.util.Map;

public interface StartWorkflowUseCase {
    WorkflowInstance startWorkflow(String definitionId, Map<String, Object> input);
    WorkflowInstance startWorkflow(WorkflowId customId, String definitionId, Map<String, Object> input);
}

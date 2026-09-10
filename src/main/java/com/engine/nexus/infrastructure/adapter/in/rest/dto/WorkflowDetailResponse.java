package com.engine.nexus.infrastructure.adapter.in.rest.dto;

import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowInstance;

import java.time.Instant;
import java.util.Map;

public record WorkflowDetailResponse(
        String id,
        String definitionId,
        int definitionVersion,
        String status,
        String currentStepId,
        Map<String, Object> input,
        Map<String, Object> output,
        String errorMessage,
        Instant createdAt,
        Instant updatedAt,
        Instant completedAt
) {
    public static WorkflowDetailResponse from(WorkflowInstance instance) {
        return new WorkflowDetailResponse(
                instance.getId().value(),
                instance.getDefinitionId(),
                instance.getDefinitionVersion(),
                instance.getStatus().name(),
                instance.getCurrentStepId().map(StepId::value).orElse(null),
                instance.getInputPayload(),
                instance.getOutputPayload(),
                instance.getErrorMessage().orElse(null),
                instance.getCreatedAt(),
                instance.getUpdatedAt(),
                instance.getCompletedAt().orElse(null)
        );
    }
}

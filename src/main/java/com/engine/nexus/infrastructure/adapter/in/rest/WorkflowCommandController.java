package com.engine.nexus.infrastructure.adapter.in.rest;

import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.port.in.CancelWorkflowUseCase;
import com.engine.nexus.domain.port.in.SignalWorkflowUseCase;
import com.engine.nexus.domain.port.in.StartWorkflowUseCase;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.SignalRequest;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.StartWorkflowRequest;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.WorkflowDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow Commands", description = "Endpoints to start, signal, and cancel workflow executions")
public class WorkflowCommandController {

    private final StartWorkflowUseCase startWorkflowUseCase;
    private final SignalWorkflowUseCase signalWorkflowUseCase;
    private final CancelWorkflowUseCase cancelWorkflowUseCase;

    public WorkflowCommandController(
            StartWorkflowUseCase startWorkflowUseCase,
            SignalWorkflowUseCase signalWorkflowUseCase,
            CancelWorkflowUseCase cancelWorkflowUseCase
    ) {
        this.startWorkflowUseCase = startWorkflowUseCase;
        this.signalWorkflowUseCase = signalWorkflowUseCase;
        this.cancelWorkflowUseCase = cancelWorkflowUseCase;
    }

    @PostMapping("/{definitionId}/start")
    @Operation(summary = "Start a new workflow instance")
    public ResponseEntity<WorkflowDetailResponse> startWorkflow(
            @PathVariable String definitionId,
            @RequestBody(required = false) StartWorkflowRequest request
    ) {
        Map<String, Object> input = request != null && request.input() != null ? request.input() : Collections.emptyMap();
        WorkflowInstance instance;
        if (request != null && request.customId() != null && !request.customId().isBlank()) {
            instance = startWorkflowUseCase.startWorkflow(WorkflowId.of(request.customId()), definitionId, input);
        } else {
            instance = startWorkflowUseCase.startWorkflow(definitionId, input);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(WorkflowDetailResponse.from(instance));
    }

    @PostMapping("/{workflowId}/signals/{signalName}")
    @Operation(summary = "Deliver an external signal to a waiting workflow")
    public ResponseEntity<Map<String, String>> sendSignal(
            @PathVariable String workflowId,
            @PathVariable String signalName,
            @RequestBody(required = false) SignalRequest request
    ) {
        Map<String, Object> payload = request != null && request.payload() != null ? request.payload() : Collections.emptyMap();
        signalWorkflowUseCase.sendSignal(WorkflowId.of(workflowId), SignalName.of(signalName), payload);
        return ResponseEntity.ok(Map.of("message", "Signal delivered successfully", "workflowId", workflowId, "signalName", signalName));
    }

    @PostMapping("/{workflowId}/cancel")
    @Operation(summary = "Cancel an in-flight workflow")
    public ResponseEntity<Map<String, String>> cancelWorkflow(
            @PathVariable String workflowId,
            @RequestParam(defaultValue = "User requested cancellation") String reason
    ) {
        cancelWorkflowUseCase.cancelWorkflow(WorkflowId.of(workflowId), reason);
        return ResponseEntity.ok(Map.of("message", "Workflow cancellation initiated", "workflowId", workflowId));
    }
}

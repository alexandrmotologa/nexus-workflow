package com.engine.nexus.infrastructure.adapter.in.rest;

import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.port.in.CancelWorkflowUseCase;
import com.engine.nexus.domain.port.in.OperatorInterventionUseCase;
import com.engine.nexus.domain.port.in.SignalWorkflowUseCase;
import com.engine.nexus.domain.port.in.StartWorkflowUseCase;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.SignalRequest;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.StartWorkflowRequest;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.WorkflowDetailResponse;
import com.engine.nexus.infrastructure.adapter.out.notification.WebhookNotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Collections;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow Commands", description = "Endpoints to start, signal, cancel, and intervene in workflow executions")
public class WorkflowCommandController {

    private final StartWorkflowUseCase startWorkflowUseCase;
    private final SignalWorkflowUseCase signalWorkflowUseCase;
    private final CancelWorkflowUseCase cancelWorkflowUseCase;
    private final OperatorInterventionUseCase operatorInterventionUseCase;
    private final WebhookNotificationService webhookNotificationService;

    public WorkflowCommandController(
            StartWorkflowUseCase startWorkflowUseCase,
            SignalWorkflowUseCase signalWorkflowUseCase,
            CancelWorkflowUseCase cancelWorkflowUseCase,
            OperatorInterventionUseCase operatorInterventionUseCase,
            WebhookNotificationService webhookNotificationService
    ) {
        this.startWorkflowUseCase = startWorkflowUseCase;
        this.signalWorkflowUseCase = signalWorkflowUseCase;
        this.cancelWorkflowUseCase = cancelWorkflowUseCase;
        this.operatorInterventionUseCase = operatorInterventionUseCase;
        this.webhookNotificationService = webhookNotificationService;
    }

    @PostMapping("/{definitionId}/start")
    @Operation(summary = "Start a new workflow instance with optional idempotency key")
    public ResponseEntity<WorkflowDetailResponse> startWorkflow(
            @PathVariable String definitionId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @RequestBody(required = false) StartWorkflowRequest request
    ) {
        Map<String, Object> input = request != null && request.input() != null ? request.input() : Collections.emptyMap();
        WorkflowId customId = (request != null && request.customId() != null && !request.customId().isBlank())
                ? WorkflowId.of(request.customId())
                : WorkflowId.generate();

        WorkflowInstance instance = startWorkflowUseCase.startWorkflow(customId, definitionId, input, idempotencyKey);
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

    @PostMapping("/{workflowId}/steps/{stepId}/retry")
    @Operation(summary = "Operator intervention: retry a failed step")
    public ResponseEntity<Map<String, String>> retryStep(
            @PathVariable String workflowId,
            @PathVariable String stepId
    ) {
        operatorInterventionUseCase.retryStep(WorkflowId.of(workflowId), StepId.of(stepId));
        return ResponseEntity.ok(Map.of("message", "Step retry initiated", "workflowId", workflowId, "stepId", stepId));
    }

    @PostMapping("/{workflowId}/steps/{stepId}/skip")
    @Operation(summary = "Operator intervention: skip a step and continue")
    public ResponseEntity<Map<String, String>> skipStep(
            @PathVariable String workflowId,
            @PathVariable String stepId,
            @RequestParam(defaultValue = "Operator skipped") String reason
    ) {
        operatorInterventionUseCase.skipStep(WorkflowId.of(workflowId), StepId.of(stepId), reason);
        return ResponseEntity.ok(Map.of("message", "Step skipped", "workflowId", workflowId, "stepId", stepId));
    }

    @PostMapping("/{workflowId}/steps/{stepId}/override")
    @Operation(summary = "Operator intervention: override step output with custom payload")
    public ResponseEntity<Map<String, String>> overrideStep(
            @PathVariable String workflowId,
            @PathVariable String stepId,
            @RequestBody Map<String, Object> customOutput
    ) {
        operatorInterventionUseCase.overrideStep(WorkflowId.of(workflowId), StepId.of(stepId), customOutput);
        return ResponseEntity.ok(Map.of("message", "Step output overridden", "workflowId", workflowId, "stepId", stepId));
    }

    @PostMapping("/webhooks")
    @Operation(summary = "Register a webhook endpoint for workflow events")
    public ResponseEntity<Map<String, String>> registerWebhook(@RequestBody Map<String, String> body) {
        String url = body.get("url");
        if (url == null || url.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Webhook URL must not be blank"));
        }
        webhookNotificationService.registerWebhook(url);
        return ResponseEntity.ok(Map.of("message", "Webhook registered", "url", url));
    }
}

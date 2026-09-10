package com.engine.nexus.infrastructure.adapter.in.rest;

import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.WorkflowDefinition;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.port.in.QueryWorkflowQuery;
import com.engine.nexus.domain.port.out.WorkflowRegistryPort;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.EventResponse;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.WorkflowDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow Queries", description = "Endpoints to inspect status, history, definitions, and instances")
public class WorkflowQueryController {

    private final QueryWorkflowQuery queryWorkflowQuery;
    private final WorkflowRegistryPort workflowRegistryPort;

    public WorkflowQueryController(
            QueryWorkflowQuery queryWorkflowQuery,
            WorkflowRegistryPort workflowRegistryPort
    ) {
        this.queryWorkflowQuery = queryWorkflowQuery;
        this.workflowRegistryPort = workflowRegistryPort;
    }

    @GetMapping
    @Operation(summary = "List all workflow instances")
    public ResponseEntity<List<WorkflowDetailResponse>> listWorkflows(
            @RequestParam(defaultValue = "50") int limit,
            @RequestParam(defaultValue = "0") int offset
    ) {
        List<WorkflowInstance> instances = queryWorkflowQuery.listWorkflows(limit, offset);
        List<WorkflowDetailResponse> responses = instances.stream()
                .map(WorkflowDetailResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/{workflowId}/status")
    @Operation(summary = "Get current status and state of a workflow")
    public ResponseEntity<WorkflowDetailResponse> getWorkflowStatus(@PathVariable String workflowId) {
        WorkflowInstance instance = queryWorkflowQuery.getWorkflowInstance(WorkflowId.of(workflowId));
        return ResponseEntity.ok(WorkflowDetailResponse.from(instance));
    }

    @GetMapping("/{workflowId}/history")
    @Operation(summary = "Get immutable event sourcing history for a workflow")
    public ResponseEntity<List<EventResponse>> getWorkflowHistory(@PathVariable String workflowId) {
        List<NexusDomainEvent> events = queryWorkflowQuery.getWorkflowHistory(WorkflowId.of(workflowId));
        List<EventResponse> responses = events.stream()
                .map(EventResponse::from)
                .toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/definitions")
    @Operation(summary = "List all registered workflow definitions")
    public ResponseEntity<List<WorkflowDefinition>> listDefinitions() {
        return ResponseEntity.ok(workflowRegistryPort.getAllDefinitions());
    }
}

package com.engine.nexus.infrastructure.adapter.in.rest;

import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.port.in.QueryWorkflowQuery;
import com.engine.nexus.infrastructure.adapter.in.rest.dto.WorkflowDetailResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RestController
@RequestMapping("/api/v1/workflows")
@Tag(name = "Workflow Real-Time Streams", description = "Server-Sent Events endpoint for live DAG visualization")
public class WorkflowLiveSseController {
    private static final Logger log = LoggerFactory.getLogger(WorkflowLiveSseController.class);

    private final QueryWorkflowQuery queryWorkflowQuery;
    private final ScheduledExecutorService sseScheduler = Executors.newScheduledThreadPool(2, Thread.ofVirtual().factory());

    public WorkflowLiveSseController(QueryWorkflowQuery queryWorkflowQuery) {
        this.queryWorkflowQuery = queryWorkflowQuery;
    }

    @GetMapping(path = "/{workflowId}/live", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Stream live workflow execution status and events via SSE")
    public SseEmitter streamWorkflowLive(@PathVariable String workflowId) {
        SseEmitter emitter = new SseEmitter(180_000L); // 3 minutes timeout
        WorkflowId wfId = WorkflowId.of(workflowId);

        var task = sseScheduler.scheduleAtFixedRate(() -> {
            try {
                WorkflowInstance instance = queryWorkflowQuery.getWorkflowInstance(wfId);
                List<NexusDomainEvent> history = queryWorkflowQuery.getWorkflowHistory(wfId);

                Map<String, Object> data = Map.of(
                        "instance", WorkflowDetailResponse.from(instance),
                        "eventCount", history.size(),
                        "latestEvent", history.isEmpty() ? "NONE" : history.getLast().eventType(),
                        "timestamp", System.currentTimeMillis()
                );

                emitter.send(SseEmitter.event()
                        .name("workflow-update")
                        .data(data));

                if (instance.getStatus().isTerminal()) {
                    emitter.complete();
                }
            } catch (IOException e) {
                emitter.complete();
            } catch (Exception e) {
                log.debug("Live stream disconnected or finished for {}: {}", workflowId, e.getMessage());
                emitter.complete();
            }
        }, 0, 1, TimeUnit.SECONDS);

        emitter.onCompletion(() -> task.cancel(true));
        emitter.onTimeout(() -> task.cancel(true));
        emitter.onError(t -> task.cancel(true));

        return emitter;
    }
}

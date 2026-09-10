package com.engine.nexus.infrastructure.adapter.out.persistence;

import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowInstance;
import com.engine.nexus.domain.model.WorkflowStatus;
import com.engine.nexus.domain.port.out.WorkflowEventStorePort;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Component
public class JpaWorkflowEventStoreAdapter implements WorkflowEventStorePort {

    private final JpaWorkflowInstanceRepository instanceRepository;
    private final JpaWorkflowEventRepository eventRepository;
    private final ObjectMapper objectMapper;

    public JpaWorkflowEventStoreAdapter(
            JpaWorkflowInstanceRepository instanceRepository,
            JpaWorkflowEventRepository eventRepository,
            ObjectMapper objectMapper
    ) {
        this.instanceRepository = instanceRepository;
        this.eventRepository = eventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public void appendEvent(NexusDomainEvent event) {
        try {
            Map<String, Object> payloadMap = extractEventPayload(event);
            String json = objectMapper.writeValueAsString(payloadMap);
            WorkflowEventEntity entity = new WorkflowEventEntity(
                    event.workflowId().value(),
                    event.sequenceNumber(),
                    event.eventType(),
                    json,
                    event.timestamp()
            );
            eventRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to append event: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<NexusDomainEvent> getEventsForWorkflow(WorkflowId workflowId) {
        List<WorkflowEventEntity> entities = eventRepository.findByWorkflowIdOrderBySequenceNumberAsc(workflowId.value());
        List<NexusDomainEvent> events = new ArrayList<>(entities.size());
        for (WorkflowEventEntity entity : entities) {
            events.add(mapToDomainEvent(entity));
        }
        return events;
    }

    @Override
    @Transactional(readOnly = true)
    public long getNextSequenceNumber(WorkflowId workflowId) {
        return eventRepository.findTopByWorkflowIdOrderBySequenceNumberDesc(workflowId.value())
                .map(e -> e.getSequenceNumber() + 1)
                .orElse(1L);
    }

    @Override
    @Transactional
    public void saveInstance(WorkflowInstance instance) {
        try {
            String inputJson = instance.getInputPayload().isEmpty() ? "{}" : objectMapper.writeValueAsString(instance.getInputPayload());
            String outputJson = instance.getOutputPayload().isEmpty() ? "{}" : objectMapper.writeValueAsString(instance.getOutputPayload());

            WorkflowInstanceEntity entity = new WorkflowInstanceEntity(
                    instance.getId().value(),
                    instance.getDefinitionId(),
                    instance.getStatus().name(),
                    instance.getCurrentStepId().map(StepId::value).orElse(null),
                    inputJson,
                    outputJson,
                    instance.getErrorMessage().orElse(null),
                    instance.getCreatedAt(),
                    instance.getUpdatedAt(),
                    instance.getCompletedAt().orElse(null)
            );
            instanceRepository.save(entity);
        } catch (Exception e) {
            throw new RuntimeException("Failed to save workflow instance: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<WorkflowInstance> findInstance(WorkflowId workflowId) {
        return instanceRepository.findById(workflowId.value()).map(this::mapToWorkflowInstance);
    }

    @Override
    @Transactional(readOnly = true)
    public List<WorkflowInstance> findAllInstances(int limit, int offset) {
        int page = offset / Math.max(1, limit);
        return instanceRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, limit))
                .stream()
                .map(this::mapToWorkflowInstance)
                .toList();
    }

    private WorkflowInstance mapToWorkflowInstance(WorkflowInstanceEntity entity) {
        Map<String, Object> inputMap = parseJsonMap(entity.getInputPayload());
        Map<String, Object> outputMap = parseJsonMap(entity.getOutputPayload());

        return new WorkflowInstance(
                WorkflowId.of(entity.getId()),
                entity.getDefinitionId(),
                1,
                WorkflowStatus.valueOf(entity.getStatus()),
                entity.getCurrentStepId() != null ? StepId.of(entity.getCurrentStepId()) : null,
                inputMap,
                outputMap,
                entity.getErrorMessage(),
                entity.getCreatedAt(),
                entity.getUpdatedAt(),
                entity.getCompletedAt()
        );
    }

    private Map<String, Object> parseJsonMap(String json) {
        if (json == null || json.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }

    private Map<String, Object> extractEventPayload(NexusDomainEvent event) {
        Map<String, Object> data = new HashMap<>();
        switch (event) {
            case NexusDomainEvent.WorkflowStartedEvent e -> {
                data.put("definitionId", e.definitionId());
                data.put("definitionVersion", e.definitionVersion());
                data.put("inputPayload", e.inputPayload());
            }
            case NexusDomainEvent.StepStartedEvent e -> {
                data.put("stepId", e.stepId().value());
                data.put("stepType", e.stepType());
                data.put("stepInput", e.stepInput());
            }
            case NexusDomainEvent.StepCompletedEvent e -> {
                data.put("stepId", e.stepId().value());
                data.put("stepOutput", e.stepOutput());
            }
            case NexusDomainEvent.StepFailedEvent e -> {
                data.put("stepId", e.stepId().value());
                data.put("errorMessage", e.errorMessage());
                data.put("attemptCount", e.attemptCount());
            }
            case NexusDomainEvent.StepCompensatedEvent e -> {
                data.put("stepId", e.stepId().value());
                data.put("compensationActivity", e.compensationActivity());
                data.put("resultMessage", e.resultMessage());
            }
            case NexusDomainEvent.SignalWaitingEvent e -> {
                data.put("stepId", e.stepId().value());
                data.put("signalName", e.signalName().value());
                data.put("timeoutMillis", e.timeout().toMillis());
            }
            case NexusDomainEvent.SignalReceivedEvent e -> {
                data.put("signalName", e.signalName().value());
                data.put("signalPayload", e.signalPayload());
            }
            case NexusDomainEvent.SleepScheduledEvent e -> {
                data.put("stepId", e.stepId().value());
                data.put("durationMillis", e.duration().toMillis());
            }
            case NexusDomainEvent.WorkflowCompletedEvent e -> {
                data.put("finalOutput", e.finalOutput());
            }
            case NexusDomainEvent.WorkflowFailedEvent e -> {
                data.put("reason", e.reason());
            }
            case NexusDomainEvent.WorkflowCompensatedEvent e -> {
                data.put("failureReason", e.failureReason());
            }
        }
        return data;
    }

    @SuppressWarnings("unchecked")
    private NexusDomainEvent mapToDomainEvent(WorkflowEventEntity entity) {
        WorkflowId wfId = WorkflowId.of(entity.getWorkflowId());
        long seq = entity.getSequenceNumber();
        Instant time = entity.getCreatedAt();
        Map<String, Object> data = parseJsonMap(entity.getEventPayload());

        return switch (entity.getEventType()) {
            case "WORKFLOW_STARTED" -> new NexusDomainEvent.WorkflowStartedEvent(
                    wfId, seq, time,
                    (String) data.get("definitionId"),
                    ((Number) data.getOrDefault("definitionVersion", 1)).intValue(),
                    (Map<String, Object>) data.getOrDefault("inputPayload", Collections.emptyMap())
            );
            case "STEP_STARTED" -> new NexusDomainEvent.StepStartedEvent(
                    wfId, seq, time,
                    StepId.of((String) data.get("stepId")),
                    (String) data.get("stepType"),
                    (Map<String, Object>) data.getOrDefault("stepInput", Collections.emptyMap())
            );
            case "STEP_COMPLETED" -> new NexusDomainEvent.StepCompletedEvent(
                    wfId, seq, time,
                    StepId.of((String) data.get("stepId")),
                    (Map<String, Object>) data.getOrDefault("stepOutput", Collections.emptyMap())
            );
            case "STEP_FAILED" -> new NexusDomainEvent.StepFailedEvent(
                    wfId, seq, time,
                    StepId.of((String) data.get("stepId")),
                    (String) data.get("errorMessage"),
                    ((Number) data.getOrDefault("attemptCount", 1)).intValue()
            );
            case "STEP_COMPENSATED" -> new NexusDomainEvent.StepCompensatedEvent(
                    wfId, seq, time,
                    StepId.of((String) data.get("stepId")),
                    (String) data.get("compensationActivity"),
                    (String) data.get("resultMessage")
            );
            case "SIGNAL_WAITING" -> new NexusDomainEvent.SignalWaitingEvent(
                    wfId, seq, time,
                    StepId.of((String) data.get("stepId")),
                    SignalName.of((String) data.get("signalName")),
                    Duration.ofMillis(((Number) data.getOrDefault("timeoutMillis", 0)).longValue())
            );
            case "SIGNAL_RECEIVED" -> new NexusDomainEvent.SignalReceivedEvent(
                    wfId, seq, time,
                    SignalName.of((String) data.get("signalName")),
                    (Map<String, Object>) data.getOrDefault("signalPayload", Collections.emptyMap())
            );
            case "SLEEP_SCHEDULED" -> new NexusDomainEvent.SleepScheduledEvent(
                    wfId, seq, time,
                    StepId.of((String) data.get("stepId")),
                    Duration.ofMillis(((Number) data.getOrDefault("durationMillis", 0)).longValue())
            );
            case "WORKFLOW_COMPLETED" -> new NexusDomainEvent.WorkflowCompletedEvent(
                    wfId, seq, time,
                    (Map<String, Object>) data.getOrDefault("finalOutput", Collections.emptyMap())
            );
            case "WORKFLOW_FAILED" -> new NexusDomainEvent.WorkflowFailedEvent(
                    wfId, seq, time,
                    (String) data.get("reason")
            );
            case "WORKFLOW_COMPENSATED" -> new NexusDomainEvent.WorkflowCompensatedEvent(
                    wfId, seq, time,
                    (String) data.get("failureReason")
            );
            default -> throw new IllegalArgumentException("Unknown event type: " + entity.getEventType());
        };
    }
}

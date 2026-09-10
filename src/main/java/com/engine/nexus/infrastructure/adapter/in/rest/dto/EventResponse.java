package com.engine.nexus.infrastructure.adapter.in.rest.dto;

import com.engine.nexus.domain.event.NexusDomainEvent;

import java.time.Instant;

public record EventResponse(
        String workflowId,
        long sequenceNumber,
        String eventType,
        Instant timestamp,
        Object details
) {
    public static EventResponse from(NexusDomainEvent event) {
        return new EventResponse(
                event.workflowId().value(),
                event.sequenceNumber(),
                event.eventType(),
                event.timestamp(),
                event
        );
    }
}

package com.engine.nexus.domain.event;

import com.engine.nexus.domain.model.SignalName;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

public sealed interface NexusDomainEvent permits
        NexusDomainEvent.WorkflowStartedEvent,
        NexusDomainEvent.StepStartedEvent,
        NexusDomainEvent.StepCompletedEvent,
        NexusDomainEvent.StepFailedEvent,
        NexusDomainEvent.StepCompensatedEvent,
        NexusDomainEvent.StepSkippedEvent,
        NexusDomainEvent.StepOverriddenEvent,
        NexusDomainEvent.SignalWaitingEvent,
        NexusDomainEvent.SignalReceivedEvent,
        NexusDomainEvent.SleepScheduledEvent,
        NexusDomainEvent.ChildWorkflowStartedEvent,
        NexusDomainEvent.ChildWorkflowCompletedEvent,
        NexusDomainEvent.ChildWorkflowFailedEvent,
        NexusDomainEvent.WorkflowCompletedEvent,
        NexusDomainEvent.WorkflowFailedEvent,
        NexusDomainEvent.WorkflowCompensatedEvent {

    WorkflowId workflowId();
    long sequenceNumber();
    Instant timestamp();
    String eventType();

    record WorkflowStartedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            String definitionId,
            int definitionVersion,
            Map<String, Object> inputPayload
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "WORKFLOW_STARTED";
        }
    }

    record StepStartedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            String stepType,
            Map<String, Object> stepInput
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "STEP_STARTED";
        }
    }

    record StepCompletedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            Map<String, Object> stepOutput
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "STEP_COMPLETED";
        }
    }

    record StepFailedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            String errorMessage,
            int attemptCount
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "STEP_FAILED";
        }
    }

    record StepCompensatedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            String compensationActivity,
            String resultMessage
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "STEP_COMPENSATED";
        }
    }

    record StepSkippedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            String reason
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "STEP_SKIPPED";
        }
    }

    record StepOverriddenEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            Map<String, Object> customOutput
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "STEP_OVERRIDDEN";
        }
    }

    record SignalWaitingEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            SignalName signalName,
            Duration timeout
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "SIGNAL_WAITING";
        }
    }

    record SignalReceivedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            SignalName signalName,
            Map<String, Object> signalPayload
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "SIGNAL_RECEIVED";
        }
    }

    record SleepScheduledEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            Duration duration
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "SLEEP_SCHEDULED";
        }
    }

    record ChildWorkflowStartedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            WorkflowId childWorkflowId,
            String childDefinitionId
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "CHILD_WORKFLOW_STARTED";
        }
    }

    record ChildWorkflowCompletedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            WorkflowId childWorkflowId,
            Map<String, Object> childOutput
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "CHILD_WORKFLOW_COMPLETED";
        }
    }

    record ChildWorkflowFailedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            StepId stepId,
            WorkflowId childWorkflowId,
            String errorReason
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "CHILD_WORKFLOW_FAILED";
        }
    }

    record WorkflowCompletedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            Map<String, Object> finalOutput
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "WORKFLOW_COMPLETED";
        }
    }

    record WorkflowFailedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            String reason
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "WORKFLOW_FAILED";
        }
    }

    record WorkflowCompensatedEvent(
            WorkflowId workflowId,
            long sequenceNumber,
            Instant timestamp,
            String failureReason
    ) implements NexusDomainEvent {
        @Override
        public String eventType() {
            return "WORKFLOW_COMPENSATED";
        }
    }
}

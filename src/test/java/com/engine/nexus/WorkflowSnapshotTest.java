package com.engine.nexus;

import com.engine.nexus.application.service.DeterministicReplayEngine;
import com.engine.nexus.domain.event.NexusDomainEvent;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowStatus;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class WorkflowSnapshotTest {

    @Test
    @DisplayName("Should replay state from snapshot without reprocessing older events")
    void shouldReplayFromSnapshot() {
        DeterministicReplayEngine replayEngine = new DeterministicReplayEngine();
        WorkflowId wfId = WorkflowId.of("wf_snapshot_test_42");

        // Seeded snapshot state at sequence 10
        Map<String, Object> snapshotState = Map.of(
                "accountId", "acc_999",
                "kycStatus", "VERIFIED",
                "balance", 5000.0
        );
        long snapshotSeq = 10L;

        // Events that occurred AFTER snapshot (sequence 11 and 12)
        List<NexusDomainEvent> events = List.of(
                new NexusDomainEvent.StepStartedEvent(wfId, 11L, Instant.now(), StepId.of("invest-step"), "ACTIVITY", Map.of()),
                new NexusDomainEvent.StepCompletedEvent(wfId, 12L, Instant.now(), StepId.of("invest-step"), Map.of("portfolioId", "port_123")),
                new NexusDomainEvent.WorkflowCompletedEvent(wfId, 13L, Instant.now(), Map.of("finalStatus", "SUCCESS"))
        );

        DeterministicReplayEngine.ReplayState state = replayEngine.replay(wfId, snapshotState, snapshotSeq, events);

        assertNotNull(state);
        assertEquals(WorkflowStatus.COMPLETED, state.status());
        // State should contain both snapshot keys and post-snapshot event keys
        assertEquals("acc_999", state.state().get("accountId"));
        assertEquals("VERIFIED", state.state().get("kycStatus"));
        assertEquals("port_123", state.state().get("portfolioId"));
        assertEquals(13L, state.lastSequenceNumber());
    }
}

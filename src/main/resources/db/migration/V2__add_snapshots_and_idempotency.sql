-- NexusWorkflow Database Schema
-- Phase 2: Snapshots & Idempotency Keys

CREATE TABLE IF NOT EXISTS nexus_workflow_snapshots (
    id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES nexus_workflow_instances(id) ON DELETE CASCADE,
    last_sequence_number BIGINT NOT NULL,
    snapshot_state TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_workflow_snapshots ON nexus_workflow_snapshots(workflow_id, last_sequence_number DESC);

CREATE TABLE IF NOT EXISTS nexus_idempotency_keys (
    idempotency_key VARCHAR(128) PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES nexus_workflow_instances(id) ON DELETE CASCADE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_idempotency_wf ON nexus_idempotency_keys(workflow_id);

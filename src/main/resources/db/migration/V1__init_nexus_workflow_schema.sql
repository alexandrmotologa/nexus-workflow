-- NexusWorkflow Database Schema
-- Level 2: Event Sourcing & Durable Execution Storage

CREATE TABLE IF NOT EXISTS nexus_workflow_instances (
    id VARCHAR(64) PRIMARY KEY,
    definition_id VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    current_step_id VARCHAR(128),
    input_payload TEXT,
    output_payload TEXT,
    error_message TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP WITH TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_workflow_status ON nexus_workflow_instances(status);
CREATE INDEX IF NOT EXISTS idx_workflow_definition ON nexus_workflow_instances(definition_id);
CREATE INDEX IF NOT EXISTS idx_workflow_created ON nexus_workflow_instances(created_at DESC);

CREATE TABLE IF NOT EXISTS nexus_workflow_events (
    id BIGSERIAL PRIMARY KEY,
    workflow_id VARCHAR(64) NOT NULL REFERENCES nexus_workflow_instances(id) ON DELETE CASCADE,
    sequence_number BIGINT NOT NULL,
    event_type VARCHAR(64) NOT NULL,
    event_payload TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_workflow_sequence UNIQUE (workflow_id, sequence_number)
);

CREATE INDEX IF NOT EXISTS idx_workflow_events_lookup ON nexus_workflow_events(workflow_id, sequence_number ASC);

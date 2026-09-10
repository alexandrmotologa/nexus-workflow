# Deterministic Replay and Recovery

NexusWorkflow uses event sourcing to recover workflow execution after process restarts or hardware failures.

```
Client               Nexus Engine                  Event Store           External Service
  |                        |                            |                       |
  |--- Start Workflow ---->|                            |                       |
  |                        |--- Append Started Event -->|                       |
  |                        |--- Execute Step 1 -------->|---------------------->|
  |                        |                            |                       | (Network call)
  |                        |<-- Output Received --------|<----------------------|
  |                        |--- Append Completed Event->|                       |
  |                        |                            |                       |
  |                        |       [Worker Crashes]     |                       |
  |                        |                            |                       |
  |                        |== Standby Worker Boots === |                       |
  |                        |--- Read Workflow Events -->|                       |
  |                        |<-- Event Stream Returned --|                       |
  |                        |                            |                       |
  |                        | (Replay Step 1 from cache; |                       |
  |                        |  skips external call)      |                       |
  |                        |                            |                       |
  |                        |--- Execute Step 2 -------->|---------------------->|
  |                        |                            |                       | (Network call)
  |                        |--- Append Completed Event->|                       |
  |                        |--- Append Finished Event ->|                       |
  |<-- Workflow Finished --|                            |                       |
```

## How Replay Operates

1. **State Reconstruction**:
   When an in-flight workflow resumes, `DeterministicReplayEngine` loads all recorded events for that `workflow_id` ordered by `sequence_number`.
   By folding each event in order, the engine builds a snapshot of:
   - Initial workflow inputs.
   - Outputs for each completed activity.
   - Any external signals received during previous executions.
   - The sequence of completed step identifiers.

2. **Activity Interception**:
   During workflow execution, before invoking an activity function, the engine checks whether that step identifier already exists in the replay snapshot.
   - If present: the engine returns the recorded output immediately. It does not call the activity function again. This prevents duplicate external side effects like duplicate credit card charges or duplicate emails.
   - If not present: the engine invokes the activity function, records a `STEP_STARTED` event, and on success appends a `STEP_COMPLETED` event containing the output.

3. **Determinism Verification**:
   The engine checks that the sequence of step definitions declared in Java code matches the sequence recorded in the event store. If a developer changes the workflow code such that steps run in a different order or an unrecorded step precedes an already completed step, the engine throws `NonDeterministicException`.

## NonDeterministicException Handling

When `NonDeterministicException` is thrown:
- The workflow execution halts immediately to prevent corrupting state.
- The instance status remains inspectable with the error details logged.
- The workflow definition versioning mechanism allows older in-flight instances to complete using their original definition while new instances run the updated code.

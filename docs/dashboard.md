# Live SVG DAG Visualizer and Operator Dashboard

NexusWorkflow includes an embedded web dashboard served at `http://localhost:8080/dashboard`.

## Features

1. **Dynamic SVG DAG Rendering**:
   Generates a visual node-link diagram for the active workflow definition directly in scalable vector graphics (SVG). Step rectangles reflect live status with distinct styling:
   - Deep Blue with glow: Currently running.
   - Emerald Green: Completed successfully.
   - Amber with pulse: Waiting for external signal approval.
   - Crimson: Step failed or compensated.
   - Slate: Pending execution.

2. **Server-Sent Events (SSE) Live Feed**:
   The web page connects to `/api/v1/workflows/{workflowId}/live`. Updates flow over an open HTTP connection every second, updating node colors, step payloads, and sequence counters without page reloads.

3. **Signal Injection Console**:
   Workflows waiting for human-in-the-loop decisions (such as document verification or budget approval) display an "Inject Signal" button in the toolbar. Clicking this opens a modal where operators can input JSON payloads and submit signals directly to the running instance.

4. **Execution History and Audit Log**:
   The right-hand panel displays both the current step outputs and an immutable timeline of all events recorded by the PostgreSQL event store.

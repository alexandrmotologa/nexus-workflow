# Live SVG DAG Visualizer and Operator Dashboard

NexusWorkflow provides a built-in web dashboard at `http://localhost:8080/dashboard`. It provides full observability into distributed workflow state, latency timelines, and operator intervention controls without external frontend dependencies or build steps.

---

## 1. Dynamic SVG DAG Visualizer

The visualizer generates a responsive vector node-link diagram for the active workflow definition:

![NexusWorkflow Real-Time DAG Visualizer](images/nexus-dashboard-dag.png)

### Visual Node States

- **Deep Blue with Glow**: Step is currently executing on a virtual thread worker.
- **Emerald Green**: Step completed successfully and output payload is committed to the event store.
- **Amber with Pulse**: Workflow is paused at a signal step, waiting for external human or API approval (for example, `wait-kyc`).
- **Crimson**: Step failed, triggered retries, or initiated backward saga compensation.
- **Slate Gray**: Step is queued or pending execution.

### Interactive Controls

- **Pan & Zoom Canvas**: Click and drag or scroll to inspect large DAGs.
- **Direct Node Selection**: Clicking any step opens the operator intervention drawer.
- **Mermaid Diagram Export**: Click the "Export Mermaid" button in the toolbar to copy the entire DAG definition as a Mermaid chart for markdown documentation or design reviews.
- **Search & Filter**: Search workflow executions by instance ID, workflow definition name, or status (`COMPLETED`, `WAITING_SIGNAL`, `RUNNING`, `FAILED`).

---

## 2. Latency Waterfall (Gantt View)

Switch to the **Waterfall (Gantt)** tab on the right inspector panel to view execution duration per step:

![Gantt Waterfall Latency Timeline](images/nexus-dashboard-waterfall.png)

- Each completed activity displays its execution duration in milliseconds.
- Relative horizontal progress bars highlight latency hot spots across both serial steps and concurrent parallel branches.
- Helps teams identify slow third-party API dependencies or slow database queries.

---

## 3. Operator Intervention Console

When unexpected exceptions or human verification bottlenecks occur, operators can intervene directly from the dashboard:

![Operator Step Intervention Console](images/nexus-dashboard-intervention.png)

### Intervention Actions

1. **Retry Step**:
   Triggers immediate re-execution of a failed activity without restarting the entire workflow.

2. **Skip Step**:
   Bypasses an optional or problematic step, registers an audit reason (e.g., `ApprovedManuallyByCompliance`), and advances the DAG to subsequent steps.

3. **Override Step Output**:
   Allows the operator to supply a custom JSON payload for a step's output. Downstream steps receive this payload as their input state, unblocking the execution path safely.

All operator actions are logged to the immutable event audit history table (`nexus_workflow_events`), preserving compliance trace records.

---

## 4. Real-Time Updates via Server-Sent Events (SSE)

The visualizer stays synchronized with the backend via Server-Sent Events (`GET /api/v1/workflows/{workflowId}/live`). As virtual threads finish activities or signals arrive, the browser receives status updates and repaints node states automatically without requiring manual page refreshes.

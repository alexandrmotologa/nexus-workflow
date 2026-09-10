# NexusWorkflow Architecture

NexusWorkflow implements an event-sourced, durable execution engine in Java 21 LTS with Spring Boot 3.3.3. The system executes complex workflows by recording every state transition as an immutable event in PostgreSQL.

## Core Design Principles

1. **Hexagonal Architecture (Ports and Adapters)**:
   The domain layer has no dependencies on Spring, Hibernate, Jackson, or external libraries. All business rules, state transitions, and step definitions reside in `com.engine.nexus.domain`. Adapters in `com.engine.nexus.infrastructure` implement inbound REST APIs and outbound database persistence.

2. **Durable Execution**:
   Each workflow execution is represented by a stream of append-only events. If an application instance restarts or worker threads crash mid-flight, a standby worker reloads the event stream, skips already completed activities without repeating side effects, and continues execution from the point of interruption.

3. **Virtual Threads for Concurrency**:
   NexusWorkflow uses Java 21 virtual threads (`Executors.newVirtualThreadPerTaskExecutor()`) to manage concurrent step dispatching, long-running timers, and parallel branch execution without consuming OS-level platform threads.

```
com.engine.nexus
├── domain
│   ├── model            (WorkflowInstance, WorkflowDefinition, StepDefinition, RetryPolicy)
│   ├── event            (Sealed NexusDomainEvent hierarchy)
│   ├── exception        (NonDeterministicException, WorkflowExecutionException)
│   └── port
│       ├── in           (StartWorkflowUseCase, SignalWorkflowUseCase, QueryWorkflowQuery)
│       └── out          (WorkflowEventStorePort, ActivityDispatcherPort, TimerPort)
├── application
│   ├── dsl              (Workflow fluent builder, WorkflowContext)
│   ├── registry         (WorkflowDefinitionRegistry, ActivityRegistry)
│   └── service          (WorkflowEngineImpl, DeterministicReplayEngine, SagaCompensationCoordinator)
└── infrastructure
    ├── adapter
    │   ├── in.rest      (WorkflowCommandController, WorkflowQueryController, WorkflowLiveSseController)
    │   ├── out.persistence (PostgreSQL JPA & Flyway event store adapter)
    │   └── out.timer    (VirtualThreadTimerAdapter, ChronosEngineTimerAdapter)
    ├── config           (NexusEngineConfig, OpenApiConfig)
    └── dashboard        (Interactive SVG DAG user interface)
```

## State Machine and Lifecycle

Workflows progress through defined lifecycle states:
- `PENDING`: Instance created and saved to the event store, awaiting worker pickup.
- `RUNNING`: Worker actively evaluating steps or executing activities.
- `WAITING_SIGNAL`: Paused at a signal step until an external event or approval arrives.
- `SLEEPING`: Paused on a scheduled duration, awaiting timer expiration.
- `COMPENSATING`: A downstream step failed after all configured retries; executing compensations in reverse order.
- `COMPLETED`: All steps evaluated and completed successfully.
- `FAILED`: Execution terminated with error or compensation completed.
- `CANCELLED`: Execution explicitly aborted by user request.

## Storage Model

Two relational tables manage execution state:
- `nexus_workflow_instances`: Tracks current status, definition identifier, step pointer, timestamps, and serialized input/output payloads.
- `nexus_workflow_events`: Append-only event store keyed by `(workflow_id, sequence_number)` with a unique constraint. Sequence numbers increment monotonically with every state transition.

package com.engine.nexus.infrastructure.config;

import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.application.registry.WorkflowDefinitionRegistry;
import com.engine.nexus.application.service.DeterministicReplayEngine;
import com.engine.nexus.application.service.SagaCompensationCoordinator;
import com.engine.nexus.application.service.WorkflowEngineImpl;
import com.engine.nexus.domain.port.out.TimerPort;
import com.engine.nexus.domain.port.out.WorkflowEventStorePort;
import com.engine.nexus.infrastructure.adapter.out.timer.VirtualThreadTimerAdapter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NexusEngineConfig {

    @Bean
    public WorkflowDefinitionRegistry workflowDefinitionRegistry() {
        return new WorkflowDefinitionRegistry();
    }

    @Bean
    public ActivityRegistry activityRegistry() {
        return new ActivityRegistry();
    }

    @Bean
    public DeterministicReplayEngine deterministicReplayEngine() {
        return new DeterministicReplayEngine();
    }

    @Bean
    public SagaCompensationCoordinator sagaCompensationCoordinator(
            ActivityRegistry activityRegistry,
            WorkflowEventStorePort eventStore
    ) {
        return new SagaCompensationCoordinator(activityRegistry, eventStore);
    }

    @Bean
    public WorkflowEngineImpl workflowEngine(
            WorkflowDefinitionRegistry workflowRegistry,
            ActivityRegistry activityRegistry,
            WorkflowEventStorePort eventStore,
            DeterministicReplayEngine replayEngine,
            SagaCompensationCoordinator compensationCoordinator,
            TimerPort timerPort,
            com.engine.nexus.infrastructure.adapter.out.notification.WebhookNotificationService webhookNotificationService
    ) {
        WorkflowEngineImpl engine = new WorkflowEngineImpl(
                workflowRegistry,
                activityRegistry,
                eventStore,
                replayEngine,
                compensationCoordinator,
                timerPort,
                webhookNotificationService
        );

        if (timerPort instanceof VirtualThreadTimerAdapter vtta) {
            vtta.setWorkflowEngine(engine);
        }

        return engine;
    }
}

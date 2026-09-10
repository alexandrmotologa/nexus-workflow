package com.engine.nexus;

import com.engine.nexus.application.dsl.Workflow;
import com.engine.nexus.domain.model.RetryPolicy;
import com.engine.nexus.domain.model.StepDefinition;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.StepType;
import com.engine.nexus.domain.model.WorkflowDefinition;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowDslTest {

    @Test
    @DisplayName("Should build valid workflow definition using fluent DSL")
    void shouldBuildValidWorkflowDefinition() {
        WorkflowDefinition workflow = Workflow.define("order-fulfillment")
                .version(2)
                .step("validate-order", "ValidateOrderActivity")
                .step("charge-payment", "ChargePaymentActivity", RetryPolicy.builder().maxAttempts(5).build(), "RefundPaymentActivity")
                .waitForSignal("wait-customer-confirmation", "CUSTOMER_CONFIRMED", Duration.ofHours(48))
                .sleep("wait-cooling-off", Duration.ofMinutes(15))
                .parallel("dispatch-items", List.of(
                        StepDefinition.activity(StepId.of("pack-item-a"), "PackItemActivity", null, null),
                        StepDefinition.activity(StepId.of("pack-item-b"), "PackItemActivity", null, null)
                ), 2)
                .step("send-receipt", "SendReceiptActivity")
                .onFailure("GlobalRollbackActivity")
                .build();

        assertNotNull(workflow);
        assertEquals("order-fulfillment", workflow.id());
        assertEquals(2, workflow.version());
        assertEquals(6, workflow.steps().size());
        assertEquals(StepType.ACTIVITY, workflow.steps().get(0).type());
        assertEquals(StepType.SIGNAL, workflow.steps().get(2).type());
        assertEquals(StepType.SLEEP, workflow.steps().get(3).type());
        assertEquals(StepType.PARALLEL, workflow.steps().get(4).type());
        assertTrue(workflow.getFailureHandler().isPresent());
        assertEquals("GlobalRollbackActivity", workflow.getFailureHandler().get());
    }

    @Test
    @DisplayName("Should reject workflow definition with duplicate step IDs")
    void shouldRejectDuplicateStepIds() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> {
            Workflow.define("invalid-workflow")
                    .step("step-alpha", "SomeActivity")
                    .step("step-alpha", "AnotherActivity")
                    .build();
        });

        assertTrue(ex.getMessage().contains("Duplicate stepId detected"));
    }

    @Test
    @DisplayName("Should reject blank workflow ID")
    void shouldRejectBlankWorkflowId() {
        assertThrows(IllegalArgumentException.class, () -> {
            Workflow.define("   ").build();
        });
    }
}

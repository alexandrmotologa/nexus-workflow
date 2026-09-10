package com.engine.nexus.sample;

import com.engine.nexus.application.dsl.Workflow;
import com.engine.nexus.application.registry.ActivityRegistry;
import com.engine.nexus.application.registry.WorkflowDefinitionRegistry;
import com.engine.nexus.domain.model.RetryPolicy;
import com.engine.nexus.domain.model.StepDefinition;
import com.engine.nexus.domain.model.StepId;
import com.engine.nexus.domain.model.WorkflowDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class SampleWorkflowInitializer implements CommandLineRunner {
    private static final Logger log = LoggerFactory.getLogger(SampleWorkflowInitializer.class);

    private final WorkflowDefinitionRegistry workflowRegistry;
    private final ActivityRegistry activityRegistry;

    public SampleWorkflowInitializer(WorkflowDefinitionRegistry workflowRegistry, ActivityRegistry activityRegistry) {
        this.workflowRegistry = workflowRegistry;
        this.activityRegistry = activityRegistry;
    }

    @Override
    public void run(String... args) {
        registerActivities();
        registerUserOnboardingWorkflow();
        registerTripBookingSagaWorkflow();
        log.info("NexusWorkflow sample definitions and activities registered successfully");
    }

    private void registerActivities() {
        // User Onboarding Activities
        activityRegistry.registerActivity("CreateAccountActivity", ctx -> {
            String userId = ctx.get("userId", String.class).orElse("usr_default");
            String accountId = "acc_" + UUID.randomUUID().toString().substring(0, 8);
            log.info("Executing CreateAccountActivity for userId: {} -> generated accountId: {}", userId, accountId);
            return Map.of("accountId", accountId, "accountStatus", "ACTIVE");
        });

        activityRegistry.registerCompensation("DeleteAccountActivity", ctx -> {
            log.warn("COMPENSATING: Deleting account for workflow: {}", ctx.getWorkflowId());
        });

        activityRegistry.registerActivity("ProvisionStorageActivity", ctx -> {
            String accountId = ctx.get("accountId", String.class).orElse("acc_unknown");
            String bucket = "s3://nexus-bucket-" + accountId;
            log.info("Executing ProvisionStorageActivity for accountId: {} -> bucket: {}", accountId, bucket);
            return Map.of("storageBucket", bucket, "storageAllocatedGb", 50);
        });

        activityRegistry.registerCompensation("ReleaseStorageActivity", ctx -> {
            log.warn("COMPENSATING: Releasing storage bucket for workflow: {}", ctx.getWorkflowId());
        });

        activityRegistry.registerActivity("SendWelcomeEmailActivity", ctx -> {
            String userId = ctx.get("userId", String.class).orElse("user");
            log.info("Executing SendWelcomeEmailActivity to user: {}", userId);
            return Map.of("emailSent", true, "emailTimestamp", System.currentTimeMillis());
        });

        // Trip Booking Saga Activities
        activityRegistry.registerActivity("BookFlightActivity", ctx -> {
            String destination = ctx.get("destination", String.class).orElse("Rome");
            String ticketNumber = "FL-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase();
            log.info("Executing BookFlightActivity to destination: {} -> ticket: {}", destination, ticketNumber);
            return Map.of("ticketNumber", ticketNumber, "flightPrice", 350.0);
        });

        activityRegistry.registerCompensation("CancelFlightActivity", ctx -> {
            log.warn("COMPENSATING: Cancelling flight ticket for workflow: {}", ctx.getWorkflowId());
        });

        activityRegistry.registerActivity("BookHotelActivity", ctx -> {
            String roomNumber = "ROOM-" + (100 + (int)(Math.random() * 800));
            log.info("Executing BookHotelActivity -> room: {}", roomNumber);
            return Map.of("roomNumber", roomNumber, "hotelPrice", 220.0);
        });

        activityRegistry.registerCompensation("CancelHotelActivity", ctx -> {
            log.warn("COMPENSATING: Cancelling hotel reservation for workflow: {}", ctx.getWorkflowId());
        });

        activityRegistry.registerActivity("RentCarActivity", ctx -> {
            String licensePlate = "CAR-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
            log.info("Executing RentCarActivity -> car plate: {}", licensePlate);
            return Map.of("carPlate", licensePlate, "carRentalPrice", 95.0);
        });

        activityRegistry.registerCompensation("CancelCarActivity", ctx -> {
            log.warn("COMPENSATING: Cancelling car rental reservation for workflow: {}", ctx.getWorkflowId());
        });

        activityRegistry.registerCompensation("GlobalCleanupActivity", ctx -> {
            log.warn("GLOBAL COMPENSATION EXECUTED for workflow: {}", ctx.getWorkflowId());
        });
    }

    private void registerUserOnboardingWorkflow() {
        WorkflowDefinition workflow = Workflow.define("user-onboarding")
                .version(1)
                .step("create-account", "CreateAccountActivity", RetryPolicy.builder().maxAttempts(3).build(), "DeleteAccountActivity")
                .step("provision-storage", "ProvisionStorageActivity", RetryPolicy.builder().maxAttempts(2).build(), "ReleaseStorageActivity")
                .waitForSignal("wait-kyc", "ID_VERIFIED", Duration.ofHours(24))
                .step("send-welcome", "SendWelcomeEmailActivity")
                .onFailure("GlobalCleanupActivity")
                .build();

        workflowRegistry.register(workflow);
    }

    private void registerTripBookingSagaWorkflow() {
        StepDefinition bookFlight = StepDefinition.activity(
                StepId.of("book-flight"),
                "BookFlightActivity",
                RetryPolicy.builder().maxAttempts(2).build(),
                "CancelFlightActivity"
        );

        StepDefinition bookHotel = StepDefinition.activity(
                StepId.of("book-hotel"),
                "BookHotelActivity",
                RetryPolicy.builder().maxAttempts(2).build(),
                "CancelHotelActivity"
        );

        WorkflowDefinition saga = Workflow.define("trip-booking-saga")
                .version(1)
                .parallel("reserve-travel", List.of(bookFlight, bookHotel), 2)
                .step("rent-car", "RentCarActivity", RetryPolicy.defaultPolicy(), "CancelCarActivity")
                .onFailure("GlobalCleanupActivity")
                .build();

        workflowRegistry.register(saga);
    }
}

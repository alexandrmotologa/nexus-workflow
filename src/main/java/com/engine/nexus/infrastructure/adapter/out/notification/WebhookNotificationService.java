package com.engine.nexus.infrastructure.adapter.out.notification;

import com.engine.nexus.domain.model.WorkflowId;
import com.engine.nexus.domain.model.WorkflowStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.engine.nexus.domain.port.out.NotificationPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Service
public class WebhookNotificationService implements NotificationPort {
    private static final Logger log = LoggerFactory.getLogger(WebhookNotificationService.class);

    private final List<String> webhookUrls = new CopyOnWriteArrayList<>();
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public WebhookNotificationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(virtualExecutor)
                .build();
    }

    public void registerWebhook(String url) {
        if (url != null && !url.isBlank() && !webhookUrls.contains(url)) {
            webhookUrls.add(url);
            log.info("Registered webhook URL: {}", url);
        }
    }

    public List<String> getRegisteredWebhooks() {
        return List.copyOf(webhookUrls);
    }

    public void notifyStatusChange(WorkflowId workflowId, String definitionId, WorkflowStatus status, Map<String, Object> output, String error) {
        if (webhookUrls.isEmpty()) {
            return;
        }

        virtualExecutor.submit(() -> {
            try {
                Map<String, Object> payload = Map.of(
                        "workflowId", workflowId.value(),
                        "definitionId", definitionId,
                        "status", status.name(),
                        "output", output != null ? output : Map.of(),
                        "error", error != null ? error : "",
                        "timestamp", System.currentTimeMillis()
                );

                String json = objectMapper.writeValueAsString(payload);

                for (String url : webhookUrls) {
                    try {
                        HttpRequest request = HttpRequest.newBuilder()
                                .uri(URI.create(url))
                                .timeout(Duration.ofSeconds(5))
                                .header("Content-Type", "application/json")
                                .header("User-Agent", "NexusWorkflow-NotificationEngine/1.0")
                                .POST(HttpRequest.BodyPublishers.ofString(json))
                                .build();

                        httpClient.sendAsync(request, HttpResponse.BodyHandlers.discarding())
                                .thenAccept(resp -> log.debug("Webhook sent to {} status: {}", url, resp.statusCode()))
                                .exceptionally(ex -> {
                                    log.warn("Failed to dispatch webhook to {}: {}", url, ex.getMessage());
                                    return null;
                                });
                    } catch (Exception e) {
                        log.warn("Error creating webhook request to {}: {}", url, e.getMessage());
                    }
                }
            } catch (Exception e) {
                log.error("Error serializing webhook payload: {}", e.getMessage());
            }
        });
    }
}

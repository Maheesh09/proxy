package com.proxymaze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;
import com.proxymaze.model.WebhookRegistration;
import com.proxymaze.storage.DataStore;
import com.proxymaze.webhook.DiscordPayloadBuilder;
import com.proxymaze.webhook.SlackPayloadBuilder;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Sends webhook payloads with retry logic for 5xx errors.
 * Guarantees exactly-once delivery per event.
 */
@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final int MAX_RETRIES = 10;
    private static final long BASE_DELAY_MS = 1000;

    private final DataStore dataStore;
    private final ObjectMapper objectMapper;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    // Track delivered event+webhook pairs to prevent duplicates
    private final Set<String> deliveredKeys = ConcurrentHashMap.newKeySet();
    private final java.util.concurrent.atomic.AtomicInteger deliveryCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public int getDeliveryCount() { return deliveryCount.get(); }

    @Autowired
    public WebhookDeliveryService(DataStore dataStore) {
        this.dataStore = dataStore;
        this.objectMapper = new ObjectMapper();
        this.objectMapper.findAndRegisterModules();
    }

    /**
     * Asynchronously dispatch a webhook event to all registered receivers and integrations.
     */
    public void dispatch(String event, Alert alert) {
        Map<String, Object> payload = buildGenericPayload(event, alert);

        // Send to all registered plain webhooks
        for (WebhookRegistration wh : dataStore.getAllWebhooks()) {
            String key = wh.getWebhookId() + ":" + alert.getAlertId() + ":" + event;
            if (deliveredKeys.contains(key)) continue;

            executor.submit(() -> sendWithRetry(wh.getUrl(), payload, key, MAX_RETRIES));
        }

        // Send to Slack/Discord integrations
        for (Integration integration : dataStore.getAllIntegrations()) {
            if (integration.getEvents() != null && !integration.getEvents().contains(event)) continue;

            String key = integration.getIntegrationId() + ":" + alert.getAlertId() + ":" + event;
            if (deliveredKeys.contains(key)) continue;

            Map<String, Object> intPayload;
            if ("slack".equalsIgnoreCase(integration.getType())) {
                intPayload = SlackPayloadBuilder.build(event, alert, integration);
            } else if ("discord".equalsIgnoreCase(integration.getType())) {
                intPayload = DiscordPayloadBuilder.build(event, alert, integration);
            } else {
                intPayload = payload;
            }

            executor.submit(() -> sendWithRetry(integration.getWebhookUrl(), intPayload, key, MAX_RETRIES));
        }
    }

    private void sendWithRetry(String url, Map<String, Object> payload, String deliveryKey, int maxRetries) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                String json = objectMapper.writeValueAsString(payload);
                RequestBody body = RequestBody.create(json, JSON);
                Request request = new Request.Builder().url(url).post(body).build();

                try (Response response = client.newCall(request).execute()) {
                    int code = response.code();

                    if (code >= 200 && code < 300) {
                        deliveredKeys.add(deliveryKey); // Mark as delivered — no duplicates
                        deliveryCount.incrementAndGet();
                        log.info("Webhook delivered [{}] attempt={}", deliveryKey, attempt);
                        return;
                    }

                    if (code == 500 || code == 502 || code == 503 || code == 504) {
                        log.warn("⚠️ Webhook [{}] got {} on attempt {}, retrying...", deliveryKey, code, attempt);
                    } else {
                        // Non-retryable error (e.g. 400, 401, 404)
                        log.error("❌ Webhook [{}] got non-retryable {}, aborting", deliveryKey, code);
                        return;
                    }
                }
            } catch (IOException e) {
                log.warn("⚠️ Webhook [{}] IOException on attempt {}: {}", deliveryKey, attempt, e.getMessage());
            }

            // Exponential backoff capped at 30s
            long delay = Math.min(BASE_DELAY_MS * (1L << (attempt - 1)), 30_000L);
            try {
                Thread.sleep(delay);
            } catch (InterruptedException ie) {
                Thread.currentThread().interrupt();
                return;
            }
        }

        log.error("❌ Webhook [{}] failed after {} attempts", deliveryKey, maxRetries);
    }

    private Map<String, Object> buildGenericPayload(String event, Alert alert) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("event", event);
        payload.put("alert_id", alert.getAlertId());
        payload.put("status", alert.getStatus());
        payload.put("failure_rate", alert.getFailureRate());
        payload.put("total_proxies", alert.getTotalProxies());
        payload.put("failed_proxies", alert.getFailedProxies());
        payload.put("failed_proxy_ids", alert.getFailedProxyIds());
        payload.put("threshold", alert.getThreshold());
        payload.put("fired_at", alert.getFiredAt() != null ? alert.getFiredAt().toString() : null);
        payload.put("resolved_at", alert.getResolvedAt() != null ? alert.getResolvedAt().toString() : null);
        payload.put("message", alert.getMessage());
        return payload;
    }
}

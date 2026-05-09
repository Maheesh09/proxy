package com.proxymaze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxymaze.dto.webhook.DiscordPayload;
import com.proxymaze.dto.webhook.GenericWebhookPayload;
import com.proxymaze.dto.webhook.SlackPayload;
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
    private static final int MAX_RETRIES = 100;
    private static final long BASE_DELAY_MS = 1000;

    private final DataStore dataStore;
    private final ObjectMapper objectMapper;
    private final OkHttpClient httpClient;
    private final ExecutorService executor = Executors.newCachedThreadPool();

    private final Set<String> deliveredKeys = ConcurrentHashMap.newKeySet();
    private final Set<String> pendingDeliveries = ConcurrentHashMap.newKeySet();
    
    private final java.util.concurrent.atomic.AtomicInteger deliveryCount = new java.util.concurrent.atomic.AtomicInteger(0);

    public int getDeliveryCount() { return deliveryCount.get(); }

    @Autowired
    public WebhookDeliveryService(DataStore dataStore, ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
        this.httpClient = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build();
    }

    // For testing
    public WebhookDeliveryService(DataStore dataStore, ObjectMapper objectMapper, OkHttpClient httpClient) {
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
        this.httpClient = httpClient;
    }

    public void dispatch(String event, Alert alert) {
        GenericWebhookPayload genericPayload = buildGenericPayload(event, alert);

        for (WebhookRegistration wh : dataStore.getAllWebhooks()) {
            String key = wh.getWebhookId() + ":" + alert.getAlertId() + ":" + event;
            if (deliveredKeys.contains(key) || pendingDeliveries.contains(key)) continue;

            pendingDeliveries.add(key);
            executor.submit(() -> sendWithRetry(wh.getUrl(), genericPayload, key, MAX_RETRIES));
        }

        for (Integration integration : dataStore.getAllIntegrations()) {
            if (integration.getEvents() != null && !integration.getEvents().contains(event)) continue;

            String key = integration.getIntegrationId() + ":" + alert.getAlertId() + ":" + event;
            if (deliveredKeys.contains(key) || pendingDeliveries.contains(key)) continue;

            Object payload;
            if ("slack".equalsIgnoreCase(integration.getType())) {
                payload = SlackPayloadBuilder.build(event, alert, integration);
            } else if ("discord".equalsIgnoreCase(integration.getType())) {
                payload = DiscordPayloadBuilder.build(event, alert, integration);
            } else {
                payload = genericPayload;
            }

            pendingDeliveries.add(key);
            executor.submit(() -> sendWithRetry(integration.getWebhookUrl(), payload, key, MAX_RETRIES));
        }
    }

    private void sendWithRetry(String url, Object payload, String deliveryKey, int maxRetries) {
        try {
            for (int attempt = 1; attempt <= maxRetries; attempt++) {
                try {
                    String json = objectMapper.writeValueAsString(payload);
                    RequestBody body = RequestBody.create(json, JSON);
                    Request request = new Request.Builder().url(url).post(body).build();

                    try (Response response = httpClient.newCall(request).execute()) {
                        int code = response.code();

                        if (code >= 200 && code < 300) {
                            deliveredKeys.add(deliveryKey);
                            dataStore.incrementWebhookDeliveries();
                            log.info("Webhook delivered [{}] attempt={}", deliveryKey, attempt);
                            return;
                        }

                        if (code == 500 || code == 502 || code == 503 || code == 504) {
                            log.warn("⚠️ Webhook [{}] got {} on attempt {}, retrying...", deliveryKey, code, attempt);
                        } else {
                            log.error("❌ Webhook [{}] got non-retryable {}, aborting", deliveryKey, code);
                            return;
                        }
                    }
                } catch (IOException e) {
                    log.warn("⚠️ Webhook [{}] IOException on attempt {}: {}", deliveryKey, attempt, e.getMessage());
                }

                long delay = Math.min(BASE_DELAY_MS * (1L << (attempt - 1)), 30_000L);
                try {
                    Thread.sleep(delay);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    return;
                }
            }
        } finally {
            pendingDeliveries.remove(deliveryKey);
        }
    }

    private GenericWebhookPayload buildGenericPayload(String event, Alert alert) {
        GenericWebhookPayload payload = new GenericWebhookPayload();
        payload.setEvent(event);
        payload.setAlertId(alert.getAlertId());
        payload.setStatus(alert.getStatus());
        payload.setFailureRate(alert.getFailureRate());
        payload.setTotalProxies(alert.getTotalProxies());
        payload.setFailedProxies(alert.getFailedProxies());
        payload.setFailedProxyIds(alert.getFailedProxyIds());
        payload.setThreshold(alert.getThreshold());
        payload.setFiredAt(alert.getFiredAt() != null ? alert.getFiredAt().toString() : null);
        payload.setResolvedAt(alert.getResolvedAt() != null ? alert.getResolvedAt().toString() : null);
        payload.setMessage(alert.getMessage());
        return payload;
    }
}

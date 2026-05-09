package com.proxymaze.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.proxymaze.dto.WebhookFiredPayload;
import com.proxymaze.dto.WebhookResolvedPayload;
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

import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.*;

/**
 * Delivers webhook events exactly once with:
 *  - Synchronous first attempt (avoids Cloud Run CPU suspension)
 *  - Manual redirect following (preserves POST method on 3xx)
 *  - Retry loop for transient 5xx failures (per Criterion 24)
 *  - Exactly-once delivery tracking (per Criterion 25)
 */
@Service
public class WebhookDeliveryService {

    private static final Logger log = LoggerFactory.getLogger(WebhookDeliveryService.class);
    private static final MediaType JSON = MediaType.get("application/json");

    // Retry configuration: 10 attempts, 5s apart = up to 50s of retries
    private static final int MAX_ATTEMPTS = 10;
    private static final long RETRY_DELAY_MS = 5_000;

    private final DataStore dataStore;
    private final ObjectMapper objectMapper;
    private final OkHttpClient client;

    // Tracks keys that have been *successfully* delivered — never retry those
    private final Set<String> deliveredKeys = ConcurrentHashMap.newKeySet();

    // Tracks keys currently being dispatched — prevents duplicate dispatches for the same event
    private final Set<String> inFlightKeys = ConcurrentHashMap.newKeySet();

    private final ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    public WebhookDeliveryService(DataStore dataStore, ObjectMapper objectMapper) {
        this.dataStore = dataStore;
        this.objectMapper = objectMapper;
        this.client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(15, TimeUnit.SECONDS)
                .followRedirects(false)   // Manual redirect to preserve POST method
                .followSslRedirects(false)
                .build();
    }

    /**
     * Dispatches an alert event to all registered webhooks and integrations.
     * First attempt is synchronous; failures are retried in background.
     */
    public void dispatch(String event, Alert alert) {
        // Snapshot generic payload once
        Object genericPayload = buildPayload(event, alert);

        for (WebhookRegistration wh : dataStore.getAllWebhooks()) {
            String key = wh.getWebhookId() + ":" + alert.getAlertId() + ":" + event;
            dispatchOne(wh.getUrl(), genericPayload, key);
        }

        for (Integration integration : dataStore.getAllIntegrations()) {
            dispatchToIntegration(event, alert, integration);
        }
    }

    /**
     * Dispatches a single event to a single integration.
     * Called by dispatch() and also directly from IntegrationController
     * when an integration is registered while an alert is already active.
     */
    public void dispatchToIntegration(String event, Alert alert, Integration integration) {
        if (!integration.getEvents().contains(event)) return;

        Map<String, Object> intPayload;
        if ("slack".equalsIgnoreCase(integration.getType())) {
            intPayload = SlackPayloadBuilder.build(event, alert, integration);
        } else if ("discord".equalsIgnoreCase(integration.getType())) {
            intPayload = DiscordPayloadBuilder.build(event, alert, integration);
        } else {
            return;
        }

        String key = integration.getIntegrationId() + ":" + alert.getAlertId() + ":" + event;
        dispatchOne(integration.getWebhookUrl(), intPayload, key);
    }

    /**
     * Tries to deliver one event to one endpoint.
     * Synchronous first, background retries on transient failure.
     */
    private void dispatchOne(String url, Object payload, String key) {
        // Already delivered? Skip.
        if (deliveredKeys.contains(key)) return;

        // Already in-flight for this event? Skip (prevents duplicate dispatches).
        if (!inFlightKeys.add(key)) {
            log.debug("Webhook already in-flight, skipping [{}]", key);
            return;
        }

        // Try synchronously first (keeps CPU active on Cloud Run)
        boolean success = doPost(url, payload, key);

        if (!success) {
            // Retry in background for transient failures (Criterion 24)
            executor.submit(() -> {
                try {
                    for (int attempt = 2; attempt <= MAX_ATTEMPTS; attempt++) {
                        if (deliveredKeys.contains(key)) return;
                        Thread.sleep(RETRY_DELAY_MS);
                        if (deliveredKeys.contains(key)) return;
                        if (doPost(url, payload, key)) return;
                    }
                    log.error("Webhook exhausted all retries [{}]", key);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    inFlightKeys.remove(key);
                }
            });
        } else {
            inFlightKeys.remove(key);
        }
    }

    /**
     * Makes a single POST attempt, following 3xx redirects while preserving POST.
     * Returns true if delivered successfully (2xx).
     */
    private boolean doPost(String url, Object payload, String key) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            String currentUrl = url;

            // Manual redirect loop (max 5 hops) — preserves POST on 301/302
            for (int hop = 0; hop < 5; hop++) {
                if (deliveredKeys.contains(key)) return true; // delivered by another path

                RequestBody body = RequestBody.create(JSON, json);
                Request request = new Request.Builder()
                        .url(currentUrl)
                        .post(body)
                        .header("Content-Type", "application/json")
                        .build();

                try (Response response = client.newCall(request).execute()) {
                    int code = response.code();

                    // Success
                    if (response.isSuccessful()) {
                        deliveredKeys.add(key);
                        dataStore.incrementWebhookDeliveries();
                        log.info("Webhook delivered [{}] -> {} ({})", key, currentUrl, code);
                        return true;
                    }

                    // Redirect: follow with POST (don't let OkHttp switch to GET)
                    if (code == 301 || code == 302 || code == 303 || code == 307 || code == 308) {
                        String location = response.header("Location");
                        if (location != null && !location.isBlank()) {
                            log.info("Webhook redirect [{}]: {} -> {}", key, code, location);
                            currentUrl = location;
                            continue; // retry the loop with new URL
                        }
                    }

                    // Transient server errors — caller will retry
                    if (code == 429 || code == 500 || code == 502 || code == 503 || code == 504) {
                        log.warn("Webhook transient failure [{}]: HTTP {} (will retry)", key, code);
                        return false;
                    }

                    // Non-retryable (4xx etc.)
                    log.error("Webhook non-retryable failure [{}]: HTTP {}", key, code);
                    return false;
                }
            }

            log.warn("Webhook redirect loop exceeded [{}]", key);
            return false;

        } catch (Exception e) {
            log.warn("Webhook error [{}]: {}", key, e.getMessage());
            return false;
        }
    }

    private Object buildPayload(String event, Alert alert) {
        if ("alert.fired".equals(event)) {
            WebhookFiredPayload p = new WebhookFiredPayload();
            p.alertId = alert.getAlertId();
            p.firedAt = alert.getFiredAt().truncatedTo(ChronoUnit.SECONDS).toString();
            p.failureRate = Math.round(alert.getFailureRate() * 100.0) / 100.0;
            p.totalProxies = alert.getTotalProxies();
            p.failedProxies = alert.getFailedProxies();
            List<String> sortedIds = new ArrayList<>(
                    alert.getFailedProxyIds() != null ? alert.getFailedProxyIds() : List.of());
            Collections.sort(sortedIds);
            p.failedProxyIds = sortedIds;
            p.threshold = alert.getThreshold();
            p.message = alert.getMessage();
            return p;
        } else {
            WebhookResolvedPayload p = new WebhookResolvedPayload();
            p.alertId = alert.getAlertId();
            p.resolvedAt = alert.getResolvedAt().truncatedTo(ChronoUnit.SECONDS).toString();
            return p;
        }
    }
}
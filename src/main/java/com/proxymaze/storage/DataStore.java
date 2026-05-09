package com.proxymaze.storage;

import com.proxymaze.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Central thread-safe in-memory data store.
 * All services share this single source of truth.
 */
@Component
public class DataStore {

    // Proxy pool — keyed by proxy ID
    private final ConcurrentHashMap<String, ProxyEntry> proxies = new ConcurrentHashMap<>();

    // Alert history — newest last
    private final CopyOnWriteArrayList<Alert> alerts = new CopyOnWriteArrayList<>();

    // Registered webhook receivers
    private final ConcurrentHashMap<String, WebhookRegistration> webhooks = new ConcurrentHashMap<>();

    // Slack/Discord integrations
    private final ConcurrentHashMap<String, Integration> integrations = new ConcurrentHashMap<>();

    // Runtime config (mutable, volatile reference for safe publish)
    private volatile RuntimeConfiguration config = new RuntimeConfiguration();

    // Metrics counters
    private final java.util.concurrent.atomic.AtomicLong totalChecks = new java.util.concurrent.atomic.AtomicLong(0);
    private final java.util.concurrent.atomic.AtomicLong webhookDeliveries = new java.util.concurrent.atomic.AtomicLong(0);

    // ─── Proxies ─────────────────────────────────────────────────────────────

    public void addProxy(ProxyEntry proxy) {
        proxies.put(proxy.getId(), proxy);
    }

    public Optional<ProxyEntry> getProxy(String id) {
        return Optional.ofNullable(proxies.get(id));
    }

    public Collection<ProxyEntry> getAllProxies() {
        return proxies.values();
    }

    public void clearProxies() {
        proxies.clear();
    }

    public boolean removeProxy(String id) {
        return proxies.remove(id) != null;
    }

    public int proxyCount() {
        return proxies.size();
    }

    // ─── Alerts ──────────────────────────────────────────────────────────────

    public void addAlert(Alert alert) {
        alerts.add(alert);
    }

    public List<Alert> getAllAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    /** Returns the single ACTIVE alert if one exists, otherwise empty. */
    public Optional<Alert> getActiveAlert() {
        return alerts.stream()
                .filter(a -> "active".equals(a.getStatus()))
                .findFirst();
    }

    public void updateAlert(Alert updated) {
        // In-place update: find by ID and replace the object
        for (int i = 0; i < alerts.size(); i++) {
            if (alerts.get(i).getAlertId().equals(updated.getAlertId())) {
                alerts.set(i, updated);
                return;
            }
        }
    }

    // ─── Webhooks ─────────────────────────────────────────────────────────────

    public void addWebhook(WebhookRegistration wh) {
        webhooks.put(wh.getWebhookId(), wh);
    }

    public Collection<WebhookRegistration> getAllWebhooks() {
        return webhooks.values();
    }

    public boolean removeWebhook(String id) {
        return webhooks.remove(id) != null;
    }

    // ─── Integrations ─────────────────────────────────────────────────────────

    public void addIntegration(Integration integration) {
        integrations.put(integration.getIntegrationId(), integration);
    }

    public Collection<Integration> getAllIntegrations() {
        return integrations.values();
    }

    public boolean removeIntegration(String id) {
        return integrations.remove(id) != null;
    }

    // ─── Config ───────────────────────────────────────────────────────────────

    public RuntimeConfiguration getConfig() {
        return config;
    }

    public void setConfig(RuntimeConfiguration config) {
        this.config = config;
    }

    public long getTotalChecks() { return totalChecks.get(); }
    public void incrementChecks() { totalChecks.incrementAndGet(); }

    public long getWebhookDeliveries() { return webhookDeliveries.get(); }
    public void incrementWebhookDeliveries() { webhookDeliveries.incrementAndGet(); }
}

package com.proxymaze.storage;

import com.proxymaze.model.*;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;


@Component
public class DataStore {

    private final ConcurrentHashMap<String, ProxyEntry> proxies = new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<String> proxyOrder = new CopyOnWriteArrayList<>();

    private final CopyOnWriteArrayList<Alert> alerts = new CopyOnWriteArrayList<>();

    private final ConcurrentHashMap<String, WebhookRegistration> webhooks = new ConcurrentHashMap<>();

    private final ConcurrentHashMap<String, Integration> integrations = new ConcurrentHashMap<>();

    private volatile RuntimeConfiguration config = new RuntimeConfiguration();

    private final AtomicLong totalChecks = new AtomicLong(0);
    private final AtomicLong webhookDeliveries = new AtomicLong(0);


    public synchronized void addProxy(ProxyEntry proxy) {
        if (!proxies.containsKey(proxy.getId())) {
            proxyOrder.add(proxy.getId());
        }
        proxies.put(proxy.getId(), proxy);
    }

    public Optional<ProxyEntry> getProxy(String id) {
        return Optional.ofNullable(proxies.get(id));
    }

    public List<ProxyEntry> getAllProxies() {
        List<ProxyEntry> ordered = new ArrayList<>();
        for (String id : proxyOrder) {
            ProxyEntry p = proxies.get(id);
            if (p != null) ordered.add(p);
        }
        return ordered;
    }

    public synchronized void clearProxies() {
        proxies.clear();
        proxyOrder.clear();
    }

    public synchronized boolean removeProxy(String id) {
        proxyOrder.remove(id);
        return proxies.remove(id) != null;
    }

    public int proxyCount() {
        return proxies.size();
    }


    public void addAlert(Alert alert) {
        alerts.add(alert);
    }

    public List<Alert> getAllAlerts() {
        return Collections.unmodifiableList(alerts);
    }

    public Optional<Alert> getActiveAlert() {
        return alerts.stream()
                .filter(a -> "active".equals(a.getStatus()))
                .findFirst();
    }

    public void updateAlert(Alert updated) {
        for (int i = 0; i < alerts.size(); i++) {
            if (alerts.get(i).getAlertId().equals(updated.getAlertId())) {
                alerts.set(i, updated);
                return;
            }
        }
    }


    public void addWebhook(WebhookRegistration wh) {
        webhooks.put(wh.getWebhookId(), wh);
    }

    public Collection<WebhookRegistration> getAllWebhooks() {
        return webhooks.values();
    }

    public boolean removeWebhook(String id) {
        return webhooks.remove(id) != null;
    }


    public void addIntegration(Integration integration) {
        integrations.put(integration.getIntegrationId(), integration);
    }

    public Collection<Integration> getAllIntegrations() {
        return integrations.values();
    }

    public boolean removeIntegration(String id) {
        return integrations.remove(id) != null;
    }


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
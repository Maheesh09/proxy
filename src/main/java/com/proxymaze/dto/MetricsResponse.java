package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricsResponse {

    @JsonProperty("total_checks")
    private int totalChecks;

    @JsonProperty("current_pool_size")
    private int currentPoolSize;

    @JsonProperty("active_alerts")
    private int activeAlerts;

    @JsonProperty("total_alerts")
    private int totalAlerts;

    @JsonProperty("webhook_deliveries")
    private int webhookDeliveries;

    public int getTotalChecks() { return totalChecks; }
    public void setTotalChecks(int totalChecks) { this.totalChecks = totalChecks; }

    public int getCurrentPoolSize() { return currentPoolSize; }
    public void setCurrentPoolSize(int currentPoolSize) { this.currentPoolSize = currentPoolSize; }

    public int getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(int activeAlerts) { this.activeAlerts = activeAlerts; }

    public int getTotalAlerts() { return totalAlerts; }
    public void setTotalAlerts(int totalAlerts) { this.totalAlerts = totalAlerts; }

    public int getWebhookDeliveries() { return webhookDeliveries; }
    public void setWebhookDeliveries(int webhookDeliveries) { this.webhookDeliveries = webhookDeliveries; }
}
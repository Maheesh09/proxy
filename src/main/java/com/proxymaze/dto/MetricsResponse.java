package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class MetricsResponse {

    @JsonProperty("total_proxies")
    private int totalProxies;

    @JsonProperty("up_proxies")
    private int upProxies;

    @JsonProperty("down_proxies")
    private int downProxies;

    @JsonProperty("pending_proxies")
    private int pendingProxies;

    @JsonProperty("failure_rate")
    private double failureRate;

    @JsonProperty("active_alerts")
    private int activeAlerts;

    @JsonProperty("total_alerts")
    private int totalAlerts;

    @JsonProperty("total_checks")
    private int totalChecks;

    @JsonProperty("webhook_deliveries")
    private int webhookDeliveries;

    @JsonProperty("check_interval_seconds")
    private int checkIntervalSeconds;

    @JsonProperty("failure_threshold")
    private double failureThreshold;

    // Getters & Setters
    public int getTotalProxies() { return totalProxies; }
    public void setTotalProxies(int totalProxies) { this.totalProxies = totalProxies; }

    public int getUpProxies() { return upProxies; }
    public void setUpProxies(int upProxies) { this.upProxies = upProxies; }

    public int getDownProxies() { return downProxies; }
    public void setDownProxies(int downProxies) { this.downProxies = downProxies; }

    public int getPendingProxies() { return pendingProxies; }
    public void setPendingProxies(int pendingProxies) { this.pendingProxies = pendingProxies; }

    public double getFailureRate() { return failureRate; }
    public void setFailureRate(double failureRate) { this.failureRate = failureRate; }

    public int getActiveAlerts() { return activeAlerts; }
    public void setActiveAlerts(int activeAlerts) { this.activeAlerts = activeAlerts; }

    public int getTotalAlerts() { return totalAlerts; }
    public void setTotalAlerts(int totalAlerts) { this.totalAlerts = totalAlerts; }

    public int getTotalChecks() { return totalChecks; }
    public void setTotalChecks(int totalChecks) { this.totalChecks = totalChecks; }

    public int getWebhookDeliveries() { return webhookDeliveries; }
    public void setWebhookDeliveries(int webhookDeliveries) { this.webhookDeliveries = webhookDeliveries; }

    public int getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(int checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }

    public double getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(double failureThreshold) { this.failureThreshold = failureThreshold; }
}

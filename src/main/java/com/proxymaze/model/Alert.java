package com.proxymaze.model;

import java.time.Instant;
import java.util.List;

public class Alert {
    private String alertId;
    private String status; // ACTIVE, RESOLVED
    private double failureRate;
    private int totalProxies;
    private int failedProxies;
    private List<String> failedProxyIds;
    private double threshold;
    private Instant firedAt;
    private Instant resolvedAt;
    private String message;

    public Alert() {}

    // Getters & Setters
    public String getAlertId() { return alertId; }
    public void setAlertId(String alertId) { this.alertId = alertId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public double getFailureRate() { return failureRate; }
    public void setFailureRate(double failureRate) { this.failureRate = failureRate; }

    public int getTotalProxies() { return totalProxies; }
    public void setTotalProxies(int totalProxies) { this.totalProxies = totalProxies; }

    public int getFailedProxies() { return failedProxies; }
    public void setFailedProxies(int failedProxies) { this.failedProxies = failedProxies; }

    public List<String> getFailedProxyIds() { return failedProxyIds; }
    public void setFailedProxyIds(List<String> failedProxyIds) { this.failedProxyIds = failedProxyIds; }

    public double getThreshold() { return threshold; }
    public void setThreshold(double threshold) { this.threshold = threshold; }

    public Instant getFiredAt() { return firedAt; }
    public void setFiredAt(Instant firedAt) { this.firedAt = firedAt; }

    public Instant getResolvedAt() { return resolvedAt; }
    public void setResolvedAt(Instant resolvedAt) { this.resolvedAt = resolvedAt; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}

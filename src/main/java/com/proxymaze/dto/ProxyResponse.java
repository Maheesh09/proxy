package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;

public class ProxyResponse {

    @JsonProperty("id")
    private String id;

    @JsonProperty("url")
    private String url;

    @JsonProperty("status")
    private String status;

    @JsonProperty("last_checked_at")
    private Instant lastCheckedAt;

    @JsonProperty("consecutive_failures")
    private int consecutiveFailures;

    @JsonProperty("total_checks")
    private int totalChecks;

    @JsonProperty("successful_checks")
    private int successfulChecks;

    @JsonProperty("uptime_percentage")
    private double uptimePercentage;

    @JsonProperty("created_at")
    private Instant createdAt;

    @JsonProperty("history")
    private java.util.List<com.proxymaze.model.CheckRecord> history;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public int getConsecutiveFailures() { return consecutiveFailures; }
    public void setConsecutiveFailures(int consecutiveFailures) { this.consecutiveFailures = consecutiveFailures; }

    public int getTotalChecks() { return totalChecks; }
    public void setTotalChecks(int totalChecks) { this.totalChecks = totalChecks; }

    public int getSuccessfulChecks() { return successfulChecks; }
    public void setSuccessfulChecks(int successfulChecks) { this.successfulChecks = successfulChecks; }

    public double getUptimePercentage() { return uptimePercentage; }
    public void setUptimePercentage(double uptimePercentage) { this.uptimePercentage = uptimePercentage; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public java.util.List<com.proxymaze.model.CheckRecord> getHistory() { return history; }
    public void setHistory(java.util.List<com.proxymaze.model.CheckRecord> history) { this.history = history; }
}
package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/**
 * Snapshot DTO for fired alerts.
 */
public class WebhookFiredPayload {
    public String event = "alert.fired";
    @JsonProperty("alert_id")
    public String alertId;
    @JsonProperty("fired_at")
    public String firedAt;
    @JsonProperty("failure_rate")
    public double failureRate;
    @JsonProperty("total_proxies")
    public int totalProxies;
    @JsonProperty("failed_proxies")
    public int failedProxies;
    @JsonProperty("failed_proxy_ids")
    public List<String> failedProxyIds;
    public double threshold;
    public String message;
}
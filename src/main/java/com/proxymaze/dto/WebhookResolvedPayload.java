package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Snapshot DTO for resolved alerts.
 */
public class WebhookResolvedPayload {
    public String event = "alert.resolved";
    @JsonProperty("alert_id")
    public String alertId;
    @JsonProperty("resolved_at")
    public String resolvedAt;
}
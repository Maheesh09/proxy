package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public class IntegrationRequest {

    @JsonProperty("type")
    private String type;

    @JsonProperty("webhook_url")
    private String webhookUrl;

    @JsonProperty("username")
    private String username;

    @JsonProperty("events")
    private List<String> events;

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
}

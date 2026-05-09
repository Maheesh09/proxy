package com.proxymaze.model;

import java.util.List;

public class Integration {
    private String integrationId;
    private String type; 
    private String webhookUrl;
    private String username;
    private List<String> events;

    public Integration() {}

    public String getIntegrationId() { return integrationId; }
    public void setIntegrationId(String integrationId) { this.integrationId = integrationId; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getWebhookUrl() { return webhookUrl; }
    public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public List<String> getEvents() { return events; }
    public void setEvents(List<String> events) { this.events = events; }
}
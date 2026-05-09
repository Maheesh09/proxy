package com.proxymaze.model;

public class WebhookRegistration {
    private String webhookId;
    private String url;

    public WebhookRegistration() {}

    public WebhookRegistration(String webhookId, String url) {
        this.webhookId = webhookId;
        this.url = url;
    }

    public String getWebhookId() { return webhookId; }
    public void setWebhookId(String webhookId) { this.webhookId = webhookId; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
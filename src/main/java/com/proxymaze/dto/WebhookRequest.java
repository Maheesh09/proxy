package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookRequest {

    @JsonProperty("url")
    private String url;

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
}
package com.proxymaze.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class ConfigRequest {

    @JsonProperty("check_interval_seconds")
    private Integer checkIntervalSeconds;

    @JsonProperty("request_timeout_ms")
    private Integer requestTimeoutMs;

    @JsonProperty("failure_threshold")
    private Double failureThreshold;

    public Integer getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(Integer checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }

    public Integer getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(Integer requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }

    public Double getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(Double failureThreshold) { this.failureThreshold = failureThreshold; }
}
package com.proxymaze.model;

public class RuntimeConfiguration {
    @com.fasterxml.jackson.annotation.JsonProperty("check_interval_seconds")
    private int checkIntervalSeconds;
    @com.fasterxml.jackson.annotation.JsonProperty("request_timeout_ms")
    private int requestTimeoutMs;
    @com.fasterxml.jackson.annotation.JsonProperty("failure_threshold")
    private double failureThreshold;

    public RuntimeConfiguration() {
        this.checkIntervalSeconds = 10;
        this.requestTimeoutMs = 5000;
        this.failureThreshold = 0.20;
    }

    public int getCheckIntervalSeconds() { return checkIntervalSeconds; }
    public void setCheckIntervalSeconds(int checkIntervalSeconds) { this.checkIntervalSeconds = checkIntervalSeconds; }

    public int getRequestTimeoutMs() { return requestTimeoutMs; }
    public void setRequestTimeoutMs(int requestTimeoutMs) { this.requestTimeoutMs = requestTimeoutMs; }

    public double getFailureThreshold() { return failureThreshold; }
    public void setFailureThreshold(double failureThreshold) { this.failureThreshold = failureThreshold; }
}

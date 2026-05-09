package com.proxymaze.model;

public class RuntimeConfiguration {
    private int checkIntervalSeconds;
    private int requestTimeoutMs;
    private double failureThreshold;

    public RuntimeConfiguration() {
        this.checkIntervalSeconds = 30;
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

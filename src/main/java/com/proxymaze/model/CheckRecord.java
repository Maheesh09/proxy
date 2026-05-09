package com.proxymaze.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.Instant;


public class CheckRecord {
    @JsonProperty("checked_at")
    private Instant checkedAt;
    private String status;

    @JsonIgnore
    private Long responseTimeMs;

    public CheckRecord() {}

    public CheckRecord(Instant checkedAt, String status, Long responseTimeMs) {
        this.checkedAt = checkedAt;
        this.status = status;
        this.responseTimeMs = responseTimeMs;
    }

    public Instant getCheckedAt() { return checkedAt; }
    public void setCheckedAt(Instant checkedAt) { this.checkedAt = checkedAt; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getResponseTimeMs() { return responseTimeMs; }
    public void setResponseTimeMs(Long responseTimeMs) { this.responseTimeMs = responseTimeMs; }
}
package com.proxymaze.model;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ProxyEntry {
    private String id;
    private String url;
    private volatile ProxyStatus status;
    private volatile Instant lastCheckedAt;
    private final AtomicInteger consecutiveFailures = new AtomicInteger(0);
    private final AtomicInteger totalChecks = new AtomicInteger(0);
    private final AtomicInteger successfulChecks = new AtomicInteger(0);
    private final CopyOnWriteArrayList<CheckRecord> history = new CopyOnWriteArrayList<>();
    private Instant createdAt;

    private static final int MAX_HISTORY = 100;

    public ProxyEntry() {}

    public ProxyEntry(String id, String url) {
        this.id = id;
        this.url = url;
        this.status = ProxyStatus.PENDING;
        this.createdAt = Instant.now();
    }

    public void recordCheck(boolean success, Long responseTimeMs) {
        totalChecks.incrementAndGet();
        Instant now = Instant.now();
        lastCheckedAt = now;

        if (success) {
            successfulChecks.incrementAndGet();
            consecutiveFailures.set(0);
            status = ProxyStatus.UP;
        } else {
            consecutiveFailures.incrementAndGet();
            status = ProxyStatus.DOWN;
        }

        CheckRecord record = new CheckRecord(now, status.name().toLowerCase(), responseTimeMs);
        history.add(record);

        while (history.size() > MAX_HISTORY) {
            history.remove(0);
        }
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public ProxyStatus getStatus() { return status; }
    public void setStatus(ProxyStatus status) { this.status = status; }

    public Instant getLastCheckedAt() { return lastCheckedAt; }
    public void setLastCheckedAt(Instant lastCheckedAt) { this.lastCheckedAt = lastCheckedAt; }

    public int getConsecutiveFailures() { return consecutiveFailures.get(); }
    public int getTotalChecks() { return totalChecks.get(); }
    public int getSuccessfulChecks() { return successfulChecks.get(); }

    public List<CheckRecord> getHistory() { return history; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
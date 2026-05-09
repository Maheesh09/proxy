package com.proxymaze.scheduler;

import com.proxymaze.service.ProxyMonitoringService;
import com.proxymaze.storage.DataStore;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

/**
 * Dynamic background scheduler.
 * Runs a monitoring cycle every check_interval_seconds.
 * Can be restarted at runtime when config changes.
 */
@Component
public class MonitoringScheduler {

    private static final Logger log = LoggerFactory.getLogger(MonitoringScheduler.class);

    private final ProxyMonitoringService monitoringService;
    private final DataStore dataStore;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "proxy-monitor");
        t.setDaemon(false); // IMPORTANT: keep alive on Cloud Run with CPU always allocated
        return t;
    });

    private ScheduledFuture<?> currentTask;

    @Autowired
    public MonitoringScheduler(ProxyMonitoringService monitoringService, DataStore dataStore) {
        this.monitoringService = monitoringService;
        this.dataStore = dataStore;
    }

    @PostConstruct
    public void start() {
        restartWithInterval(dataStore.getConfig().getCheckIntervalSeconds());
        log.info("✅ MonitoringScheduler started with interval={}s",
                dataStore.getConfig().getCheckIntervalSeconds());
    }

    /**
     * Restarts the scheduler with a new interval.
     * Called when POST /config changes check_interval_seconds.
     */
    public synchronized void restartWithInterval(int intervalSeconds) {
        if (currentTask != null && !currentTask.isCancelled()) {
            currentTask.cancel(false); // let current cycle finish
        }

        currentTask = scheduler.scheduleWithFixedDelay(
                this::runCycle,
                0,                  // initial delay
                intervalSeconds,
                TimeUnit.SECONDS
        );

        log.info("🔄 MonitoringScheduler rescheduled: interval={}s", intervalSeconds);
    }

    private void runCycle() {
        try {
            monitoringService.runCycle();
        } catch (Exception e) {
            log.error("💥 Monitoring cycle failed: {}", e.getMessage(), e);
        }
    }

    @PreDestroy
    public void stop() {
        scheduler.shutdownNow();
        log.info("MonitoringScheduler stopped");
    }
}

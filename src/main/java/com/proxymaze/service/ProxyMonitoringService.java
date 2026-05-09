package com.proxymaze.service;

import com.proxymaze.model.ProxyEntry;
import com.proxymaze.storage.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.ArrayList;
import java.util.List;

/**
 * Checks all proxies in parallel and updates DataStore.
 * Called by MonitoringScheduler every X seconds.
 */
@Service
public class ProxyMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(ProxyMonitoringService.class);

    private final DataStore dataStore;
    private final ProxyProbeService probeService;
    private final AlertService alertService;

    // Thread pool for parallel proxy checks
    private final ExecutorService probePool = Executors.newCachedThreadPool();

    @Autowired
    public ProxyMonitoringService(DataStore dataStore, ProxyProbeService probeService, AlertService alertService) {
        this.dataStore = dataStore;
        this.probeService = probeService;
        this.alertService = alertService;
    }

    /**
     * Runs one full monitoring cycle:
     * 1. Check all proxies in parallel
     * 2. Update their state in DataStore
     * 3. Trigger AlertService.evaluate()
     */
    public void runCycle() {
        Collection<ProxyEntry> proxies = dataStore.getAllProxies();

        if (proxies.isEmpty()) {
            return;
        }

        int timeoutMs = dataStore.getConfig().getRequestTimeoutMs();
        log.debug("Starting monitoring cycle for {} proxies (timeout={}ms)", proxies.size(), timeoutMs);

        List<Future<?>> futures = new ArrayList<>();

        for (ProxyEntry proxy : proxies) {
            futures.add(probePool.submit(() -> {
                try {
                    ProxyProbeService.ProbeResult result = probeService.probe(proxy, timeoutMs);
                    proxy.recordCheck(result.success(), result.responseTimeMs());
                    log.debug("Proxy [{}] {} → {}", proxy.getId(), proxy.getUrl(), proxy.getStatus());
                } catch (Exception e) {
                    log.error("Unexpected error probing proxy {}: {}", proxy.getUrl(), e.getMessage());
                    proxy.recordCheck(false, -1L);
                }
            }));
        }

        // Wait for all probes to complete
        for (Future<?> f : futures) {
            try {
                f.get();
            } catch (Exception e) {
                log.error("Error waiting for probe: {}", e.getMessage());
            }
        }

        // Now evaluate alert state based on fresh results
        alertService.evaluate();
        log.debug("Monitoring cycle complete");
    }
}

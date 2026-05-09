package com.proxymaze.service;

import com.proxymaze.model.ProxyEntry;
import com.proxymaze.storage.DataStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


@Service
public class ProxyMonitoringService {

    private static final Logger log = LoggerFactory.getLogger(ProxyMonitoringService.class);

    private final DataStore dataStore;
    private final ProxyProbeService probeService;
    private final AlertService alertService;
    private final ExecutorService probeExecutor = Executors.newVirtualThreadPerTaskExecutor();

    @Autowired
    public ProxyMonitoringService(DataStore dataStore, ProxyProbeService probeService, AlertService alertService) {
        this.dataStore = dataStore;
        this.probeService = probeService;
        this.alertService = alertService;
    }

    public synchronized void runCycle() {
        List<ProxyEntry> proxies = dataStore.getAllProxies();
        if (proxies.isEmpty()) return;

        int timeoutMs = dataStore.getConfig().getRequestTimeoutMs();
        log.debug("Starting monitoring cycle for {} proxies...", proxies.size());

        List<CompletableFuture<Void>> futures = new ArrayList<>();
        for (ProxyEntry proxy : proxies) {
            futures.add(CompletableFuture.runAsync(() -> {
                ProxyProbeService.ProbeResult result = probeService.probe(proxy, timeoutMs);
                dataStore.incrementChecks();
                proxy.recordCheck(result.success(), result.responseTimeMs());
            }, probeExecutor));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        alertService.evaluate();
        log.debug("Monitoring cycle complete.");
    }
}
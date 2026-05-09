package com.proxymaze.service;

import com.proxymaze.model.Alert;
import com.proxymaze.model.ProxyEntry;
import com.proxymaze.model.ProxyStatus;
import com.proxymaze.storage.DataStore;
import com.proxymaze.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Monitors the proxy pool state and manages the lifecycle of alerts.
 */
@Service
public class AlertService {

    private static final Logger log = LoggerFactory.getLogger(AlertService.class);

    private final DataStore dataStore;
    private final WebhookDeliveryService webhookDeliveryService;

    @Autowired
    public AlertService(DataStore dataStore, WebhookDeliveryService webhookDeliveryService) {
        this.dataStore = dataStore;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    /**
     * Evaluates the proxy pool state.
     * Fires/Resolves alerts based on the failure threshold (default 0.20).
     */
    public synchronized void evaluate() {
        Collection<ProxyEntry> proxies = dataStore.getAllProxies();
        if (proxies.isEmpty()) return;

        int totalCount = proxies.size();
        List<String> downIds = proxies.stream()
                .filter(p -> p.getStatus() == ProxyStatus.DOWN)
                .map(ProxyEntry::getId)
                .collect(Collectors.toList());
        
        int downCount = downIds.size();
        double failureRate = (double) downCount / totalCount;
        double threshold = dataStore.getConfig().getFailureThreshold();

        Optional<Alert> activeAlertOpt = dataStore.getActiveAlert();

        if (failureRate >= threshold) {
            if (activeAlertOpt.isEmpty()) {
                // CHAPTER 09: New Alert with "alert-" prefix
                Alert newAlert = new Alert();
                newAlert.setAlertId(IdGenerator.generateShortId("alert"));
                newAlert.setStatus("active");
                newAlert.setFailureRate(Math.round(failureRate * 100.0) / 100.0);
                newAlert.setTotalProxies(totalCount);
                newAlert.setFailedProxies(downCount);
                // Consistency: Use a copy of the ID list
                newAlert.setFailedProxyIds(new ArrayList<>(downIds));
                newAlert.setThreshold(threshold);
                newAlert.setFiredAt(Instant.now());
                newAlert.setMessage("Proxy pool failure rate exceeded threshold");
                
                dataStore.addAlert(newAlert);
                webhookDeliveryService.dispatch("alert.fired", newAlert);
                log.warn("ALERT FIRED: {} (rate: {})", newAlert.getAlertId(), failureRate);
            } else {
                // Persistent Breach: Update the single active alert
                Alert active = activeAlertOpt.get();
                active.setFailureRate(Math.round(failureRate * 100.0) / 100.0);
                active.setFailedProxies(downCount);
                active.setFailedProxyIds(new ArrayList<>(downIds));
                dataStore.updateAlert(active);
                // No fired webhook for persistent breach
            }
        } else if (activeAlertOpt.isPresent()) {
            // Recovery: Resolve the active alert
            Alert active = activeAlertOpt.get();
            active.setStatus("resolved");
            active.setResolvedAt(Instant.now());
            active.setFailureRate(Math.round(failureRate * 100.0) / 100.0);
            active.setFailedProxies(downCount);
            active.setFailedProxyIds(new ArrayList<>(downIds));
            
            dataStore.updateAlert(active);
            webhookDeliveryService.dispatch("alert.resolved", active);
            log.info("ALERT RESOLVED: {}", active.getAlertId());
        }
    }
}
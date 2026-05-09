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
import java.util.Collection;
import java.util.List;
import java.util.Optional;

/**
 * Alert lifecycle manager:
 *  - Only one ACTIVE alert at a time
 *  - ACTIVE → RESOLVED when failure_rate drops below threshold
 *  - New breach after resolution = brand-new alert_id
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

    public synchronized void evaluate() {
        Collection<ProxyEntry> all = dataStore.getAllProxies();

        // Only consider proxies that have been checked (not PENDING)
        List<ProxyEntry> checked = all.stream()
                .filter(p -> p.getStatus() != ProxyStatus.PENDING)
                .toList();

        if (checked.isEmpty()) return;

        long downCount = all.stream().filter(p -> p.getStatus() == ProxyStatus.DOWN).count();
        int total = all.size();
        if (total == 0) return;
        
        double failureRate = (double) downCount / total;
        double threshold = dataStore.getConfig().getFailureThreshold();

        List<String> failedIds = all.stream()
                .filter(p -> p.getStatus() == ProxyStatus.DOWN)
                .map(ProxyEntry::getId)
                .toList();

        Optional<Alert> activeAlert = dataStore.getActiveAlert();

        if (failureRate >= threshold) {
            if (activeAlert.isEmpty()) {
                // Fire a new alert
                Alert alert = new Alert();
                alert.setAlertId(IdGenerator.generateId());
                alert.setStatus("active");
                alert.setFailureRate(failureRate);
                alert.setTotalProxies(total);
                alert.setFailedProxies((int) downCount);
                alert.setFailedProxyIds(failedIds);
                alert.setThreshold(threshold);
                alert.setFiredAt(Instant.now());
                alert.setMessage(String.format(
                        "Failure rate %.1f%% exceeds threshold %.1f%%",
                        failureRate * 100, threshold * 100));

                dataStore.addAlert(alert);
                log.warn("ALERT FIRED: {} - failure rate {}%", alert.getAlertId(),
                        String.format("%.1f", failureRate * 100));
                webhookDeliveryService.dispatch("alert.fired", alert);
            } else {
                // Update existing active alert — same alert_id
                Alert existing = activeAlert.get();
                existing.setFailureRate(failureRate);
                existing.setTotalProxies(total);
                existing.setFailedProxies((int) downCount);
                existing.setFailedProxyIds(failedIds);
                existing.setMessage(String.format(
                        "Failure rate %.1f%% exceeds threshold %.1f%%",
                        failureRate * 100, threshold * 100));
                dataStore.updateAlert(existing);
            }
        } else {
            if (activeAlert.isPresent()) {
                Alert existing = activeAlert.get();
                existing.setStatus("resolved");
                existing.setResolvedAt(Instant.now());
                existing.setFailureRate(failureRate);
                existing.setTotalProxies(total);
                existing.setFailedProxies((int) downCount);
                existing.setFailedProxyIds(failedIds);
                existing.setMessage(String.format(
                        "Failure rate %.1f%% recovered below threshold %.1f%%",
                        failureRate * 100, threshold * 100));
                dataStore.updateAlert(existing);
                log.info("ALERT RESOLVED: {}", existing.getAlertId());
                webhookDeliveryService.dispatch("alert.resolved", existing);
            }
        }
    }
}

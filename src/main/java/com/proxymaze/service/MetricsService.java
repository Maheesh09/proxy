package com.proxymaze.service;

import com.proxymaze.dto.MetricsResponse;
import com.proxymaze.model.Alert;
import com.proxymaze.model.ProxyEntry;
import com.proxymaze.model.ProxyStatus;
import com.proxymaze.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class MetricsService {

    private final DataStore dataStore;
    private final WebhookDeliveryService webhookDeliveryService;

    @Autowired
    public MetricsService(DataStore dataStore, WebhookDeliveryService webhookDeliveryService) {
        this.dataStore = dataStore;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    public MetricsResponse getMetrics() {
        Collection<ProxyEntry> proxies = dataStore.getAllProxies();
        List<Alert> alerts = dataStore.getAllAlerts();

        long total   = proxies.size();
        long up      = proxies.stream().filter(p -> p.getStatus() == ProxyStatus.UP).count();
        long down    = proxies.stream().filter(p -> p.getStatus() == ProxyStatus.DOWN).count();
        long pending = proxies.stream().filter(p -> p.getStatus() == ProxyStatus.PENDING).count();
        long checked = up + down;
        double failureRate = checked == 0 ? 0.0 : (double) down / checked;

        long activeAlerts = alerts.stream().filter(a -> "active".equals(a.getStatus())).count();
        long totalChecks  = proxies.stream().mapToLong(ProxyEntry::getTotalChecks).sum();

        MetricsResponse res = new MetricsResponse();
        res.setTotalProxies((int) total);
        res.setUpProxies((int) up);
        res.setDownProxies((int) down);
        res.setPendingProxies((int) pending);
        res.setFailureRate(failureRate);
        res.setActiveAlerts((int) activeAlerts);
        res.setTotalAlerts(alerts.size());
        res.setTotalChecks((int) totalChecks);
        res.setWebhookDeliveries(webhookDeliveryService.getDeliveryCount());
        res.setCheckIntervalSeconds(dataStore.getConfig().getCheckIntervalSeconds());
        res.setFailureThreshold(dataStore.getConfig().getFailureThreshold());
        return res;
    }
}

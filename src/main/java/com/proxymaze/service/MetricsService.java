package com.proxymaze.service;

import com.proxymaze.dto.MetricsResponse;
import com.proxymaze.model.Alert;
import com.proxymaze.model.ProxyEntry;
import com.proxymaze.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

@Service
public class MetricsService {

    private final DataStore dataStore;

    @Autowired
    public MetricsService(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    public MetricsResponse getMetrics() {
        Collection<ProxyEntry> proxies = dataStore.getAllProxies();
        List<Alert> alerts = dataStore.getAllAlerts();

        long activeAlerts = alerts.stream().filter(a -> "active".equals(a.getStatus())).count();

        MetricsResponse res = new MetricsResponse();
        res.setTotalChecks((int) dataStore.getTotalChecks());
        res.setCurrentPoolSize(proxies.size());
        res.setActiveAlerts((int) activeAlerts);
        res.setTotalAlerts(alerts.size());
        res.setWebhookDeliveries((int) dataStore.getWebhookDeliveries());
        return res;
    }
}
package com.proxymaze.service;

import com.proxymaze.model.Alert;
import com.proxymaze.model.ProxyEntry;
import com.proxymaze.model.RuntimeConfiguration;
import com.proxymaze.storage.DataStore;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;

public class PersistentBreachTest {

    @Test
    public void testPersistentBreachDoesNotDuplicateAlerts() {
        DataStore dataStore = mock(DataStore.class);
        WebhookDeliveryService webhookService = mock(WebhookDeliveryService.class);
        
        RuntimeConfiguration config = new RuntimeConfiguration();
        config.setFailureThreshold(0.5);
        when(dataStore.getConfig()).thenReturn(config);

        ProxyEntry proxy = new ProxyEntry("p1", "http://px1");
        proxy.recordCheck(false, 100L); // 100% failure rate
        
        when(dataStore.getAllProxies()).thenReturn(List.of(proxy));
        
        // First cycle: no active alert
        when(dataStore.getActiveAlert()).thenReturn(Optional.empty());
        
        AlertService alertService = new AlertService(dataStore, webhookService);
        alertService.evaluate();
        
        // Should fire one alert
        verify(dataStore, times(1)).addAlert(any(Alert.class));
        verify(webhookService, times(1)).dispatch(eq("alert.fired"), any(Alert.class));
        
        // Second cycle: alert is now active
        Alert activeAlert = new Alert();
        activeAlert.setAlertId("a1");
        activeAlert.setStatus("active");
        when(dataStore.getActiveAlert()).thenReturn(Optional.of(activeAlert));
        
        alertService.evaluate();
        
        // Should NOT fire another alert, just update
        verify(dataStore, times(1)).addAlert(any(Alert.class)); // total 1
        verify(dataStore, times(1)).updateAlert(any(Alert.class)); // one update
        verify(webhookService, times(1)).dispatch(eq("alert.fired"), any(Alert.class)); // still total 1
    }
}

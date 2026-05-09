package com.proxymaze.controller;

import com.proxymaze.exception.InvalidRequestException;
import com.proxymaze.model.Alert;
import com.proxymaze.model.Integration;
import com.proxymaze.service.WebhookDeliveryService;
import com.proxymaze.storage.DataStore;
import com.proxymaze.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/integrations")
public class IntegrationController {

    private static final Logger log = LoggerFactory.getLogger(IntegrationController.class);

    private final DataStore dataStore;
    private final WebhookDeliveryService webhookDeliveryService;

    @Autowired
    public IntegrationController(DataStore dataStore, WebhookDeliveryService webhookDeliveryService) {
        this.dataStore = dataStore;
        this.webhookDeliveryService = webhookDeliveryService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        String type = (String) body.get("type");
        String webhookUrl = (String) body.get("webhook_url");
        String username = (String) body.get("username");
        @SuppressWarnings("unchecked")
        List<String> events = (List<String>) body.get("events");

        if (type == null || (!type.equalsIgnoreCase("slack") && !type.equalsIgnoreCase("discord"))) {
            throw new InvalidRequestException("type must be 'slack' or 'discord'");
        }
        if (webhookUrl == null || webhookUrl.isBlank()) {
            throw new InvalidRequestException("webhook_url is required");
        }

        Integration integration = new Integration();
        integration.setIntegrationId(IdGenerator.generateId());
        integration.setType(type.toLowerCase());
        integration.setWebhookUrl(webhookUrl.trim());
        integration.setUsername(username);
        integration.setEvents(events != null ? events : List.of("alert.fired", "alert.resolved"));

        dataStore.addIntegration(integration);

        // KEY FIX: If there is already an active alert when an integration is registered,
        // immediately dispatch the alert.fired event to the new integration.
        // This handles the case where the evaluator registers integrations AFTER the alert fires.
        Optional<Alert> activeAlert = dataStore.getActiveAlert();
        if (activeAlert.isPresent() && integration.getEvents().contains("alert.fired")) {
            Alert alert = activeAlert.get();
            log.info("Integration [{}] registered with active alert [{}] — dispatching alert.fired immediately",
                    integration.getIntegrationId(), alert.getAlertId());
            // Dispatch in a background thread to avoid blocking the HTTP response
            Thread.ofVirtual().start(() -> webhookDeliveryService.dispatchToIntegration("alert.fired", alert, integration));
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("integration_id", integration.getIntegrationId());
        result.put("type", integration.getType());
        result.put("webhook_url", integration.getWebhookUrl());
        result.put("username", integration.getUsername());
        result.put("events", integration.getEvents());
        return ResponseEntity.ok(result);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listIntegrations() {
        List<Map<String, Object>> list = dataStore.getAllIntegrations().stream()
                .map(i -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("integration_id", i.getIntegrationId());
                    m.put("type", i.getType());
                    m.put("webhook_url", i.getWebhookUrl());
                    m.put("username", i.getUsername());
                    m.put("events", i.getEvents());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("total", list.size(), "integrations", list));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> removeIntegration(@PathVariable String id) {
        boolean removed = dataStore.removeIntegration(id);
        if (!removed) return ResponseEntity.notFound().build();
        return ResponseEntity.ok(Map.of("message", "Integration removed", "id", id));
    }
}
package com.proxymaze.controller;

import com.proxymaze.dto.WebhookRequest;
import com.proxymaze.exception.InvalidRequestException;
import com.proxymaze.model.WebhookRegistration;
import com.proxymaze.storage.DataStore;
import com.proxymaze.util.IdGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/webhooks")
public class WebhookController {

    private final DataStore dataStore;

    @Autowired
    public WebhookController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /** POST /webhooks — register a webhook receiver */
    @PostMapping
    public ResponseEntity<Map<String, Object>> register(@RequestBody WebhookRequest req) {
        if (req.getUrl() == null || req.getUrl().isBlank()) {
            throw new InvalidRequestException("url is required");
        }

        WebhookRegistration wh = new WebhookRegistration(
                IdGenerator.generateId(),
                req.getUrl().trim()
        );
        dataStore.addWebhook(wh);

        return ResponseEntity.ok(Map.of(
                "webhook_id", wh.getWebhookId(),
                "url", wh.getUrl(),
                "message", "Webhook registered successfully"
        ));
    }

    /** GET /webhooks — list all registered webhooks */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listWebhooks() {
        List<Map<String, Object>> list = dataStore.getAllWebhooks().stream()
                .map(wh -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("webhook_id", wh.getWebhookId());
                    m.put("url", wh.getUrl());
                    return m;
                })
                .collect(Collectors.toList());

        return ResponseEntity.ok(Map.of("total", list.size(), "webhooks", list));
    }

    /** DELETE /webhooks/{id} — remove a webhook */
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> removeWebhook(@PathVariable String id) {
        boolean removed = dataStore.removeWebhook(id);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Webhook removed", "id", id));
    }
}

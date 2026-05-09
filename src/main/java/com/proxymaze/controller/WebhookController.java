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

    @PostMapping
    public ResponseEntity<Map<String, Object>> register(@RequestBody Map<String, Object> body) {
        String url = (String) body.get("url");
        if (url == null || url.isBlank()) {
            throw new InvalidRequestException("url is required");
        }

        WebhookRegistration wh = new WebhookRegistration(
                IdGenerator.generateId(),
                url.trim()
        );
        dataStore.addWebhook(wh);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("webhook_id", wh.getWebhookId());
        response.put("url", wh.getUrl());
        return ResponseEntity.status(201).body(response);
    }

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

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> removeWebhook(@PathVariable String id) {
        boolean removed = dataStore.removeWebhook(id);
        if (!removed) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("message", "Webhook removed", "id", id));
    }
}
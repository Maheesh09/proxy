package com.proxymaze.controller;

import com.proxymaze.dto.AlertResponse;
import com.proxymaze.model.Alert;
import com.proxymaze.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Handles alert archive endpoints.
 * Strictly adheres to Chapter 09 return format (raw JSON array).
 */
@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final DataStore dataStore;

    @Autowired
    public AlertController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /**
     * GET /alerts — Returns ALL alerts as a raw JSON array.
     * Newest alerts first (descending order).
     */
    @GetMapping
    public ResponseEntity<List<AlertResponse>> listAlerts() {
        List<Alert> all = new ArrayList<>(dataStore.getAllAlerts());
        // Reverse to show newest first
        Collections.reverse(all); 

        List<AlertResponse> list = all.stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
        
        return ResponseEntity.ok(list);
    }

    @GetMapping("/active")
    public ResponseEntity<?> getActiveAlert() {
        return dataStore.getActiveAlert()
                .map(a -> ResponseEntity.ok((Object) toResponse(a)))
                .orElse(ResponseEntity.notFound().build());
    }

    private AlertResponse toResponse(Alert a) {
        AlertResponse r = new AlertResponse();
        r.setAlertId(a.getAlertId());
        r.setStatus(a.getStatus());
        r.setFailureRate(a.getFailureRate());
        r.setTotalProxies(a.getTotalProxies());
        r.setFailedProxies(a.getFailedProxies());
        r.setFailedProxyIds(a.getFailedProxyIds() != null ? a.getFailedProxyIds() : List.of());
        r.setThreshold(a.getThreshold());
        r.setFiredAt(a.getFiredAt());
        r.setResolvedAt(a.getResolvedAt());
        r.setMessage(a.getMessage());
        return r;
    }
}
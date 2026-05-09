package com.proxymaze.controller;

import com.proxymaze.dto.AlertResponse;
import com.proxymaze.model.Alert;
import com.proxymaze.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/alerts")
public class AlertController {

    private final DataStore dataStore;

    @Autowired
    public AlertController(DataStore dataStore) {
        this.dataStore = dataStore;
    }

    /** GET /alerts — newest first */
    @GetMapping
    public ResponseEntity<Map<String, Object>> listAlerts() {
        List<Alert> all = new ArrayList<>(dataStore.getAllAlerts());
        Collections.reverse(all); // newest first

        List<AlertResponse> list = all.stream().map(this::toResponse).collect(Collectors.toList());
        long activeCount = list.stream().filter(a -> "active".equals(a.getStatus())).count();

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", list.size());
        result.put("active", (int) activeCount);
        result.put("alerts", list);
        return ResponseEntity.ok(result);
    }

    /** GET /alerts/active */
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

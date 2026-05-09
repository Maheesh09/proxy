package com.proxymaze.controller;

import com.proxymaze.dto.ProxyResponse;
import com.proxymaze.exception.InvalidRequestException;
import com.proxymaze.exception.ProxyNotFoundException;
import com.proxymaze.model.CheckRecord;
import com.proxymaze.model.ProxyEntry;
import com.proxymaze.model.ProxyStatus;
import com.proxymaze.service.ProxyMonitoringService;
import com.proxymaze.storage.DataStore;
import com.proxymaze.util.ProxyIdExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/proxies")
public class ProxyController {

    private final DataStore dataStore;
    private final ProxyMonitoringService monitoringService;

    @Autowired
    public ProxyController(DataStore dataStore, ProxyMonitoringService monitoringService) {
        this.dataStore = dataStore;
        this.monitoringService = monitoringService;
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> addProxies(@RequestBody Map<String, Object> body) {
        Object proxiesRaw = body.get("proxies");
        if (proxiesRaw == null) throw new InvalidRequestException("proxies is required");

        @SuppressWarnings("unchecked")
        List<String> urls = (List<String>) proxiesRaw;
        if (urls.isEmpty()) throw new InvalidRequestException("proxies list cannot be empty");

        boolean replace = false;
        Object replaceRaw = body.get("replace");
        if (replaceRaw != null) {
            replace = Boolean.parseBoolean(String.valueOf(replaceRaw));
        }

        if (replace) {
            dataStore.clearProxies();
        }

        List<ProxyResponse> added = new ArrayList<>();
        for (String url : urls) {
            if (url == null || url.isBlank()) continue;
            String id = ProxyIdExtractor.extractId(url.trim());
            dataStore.getProxy(id).ifPresentOrElse(
                existing -> {
                    existing.setUrl(url.trim()); 
                    added.add(toResponse(existing));
                },
                () -> {
                    ProxyEntry entry = new ProxyEntry(id, url.trim());
                    dataStore.addProxy(entry);
                    added.add(toResponse(entry));
                }
            );
        }

        Thread.ofVirtual().start(() -> {
            try { 
                monitoringService.runCycle(); 
            } catch (Exception ignored) {}
        });

        List<Map<String, Object>> responseProxies = added.stream().map(p -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", p.getId());
            m.put("url", p.getUrl());
            m.put("status", p.getStatus());
            return m;
        }).collect(Collectors.toList());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", added.size());
        result.put("proxies", responseProxies); 
        return ResponseEntity.status(201).body(result);
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> listProxies() {
        Collection<ProxyEntry> all = dataStore.getAllProxies();
        List<ProxyResponse> list = all.stream().map(this::toResponse).collect(Collectors.toList());

        long up   = all.stream().filter(p -> p.getStatus() == ProxyStatus.UP).count();
        long down = all.stream().filter(p -> p.getStatus() == ProxyStatus.DOWN).count();
        int totalCount = all.size();
        double failureRate = totalCount == 0 ? 0.0 : (double) down / totalCount;

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", totalCount);
        result.put("up", (int) up);
        result.put("down", (int) down);
        result.put("failure_rate", Math.round(failureRate * 100.0) / 100.0);
        result.put("proxies", list);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProxyResponse> getProxy(@PathVariable String id) {
        ProxyEntry entry = dataStore.getProxy(id)
                .orElseThrow(() -> new ProxyNotFoundException(id));
        return ResponseEntity.ok(toResponse(entry));
    }

    @GetMapping("/{id}/history")
    public ResponseEntity<List<CheckRecord>> getHistory(@PathVariable String id) {
        ProxyEntry entry = dataStore.getProxy(id)
                .orElseThrow(() -> new ProxyNotFoundException(id));
        return ResponseEntity.ok(entry.getHistory());
    }

    @DeleteMapping
    public ResponseEntity<Void> clearProxies() {
        dataStore.clearProxies();
        return ResponseEntity.noContent().build(); 
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> removeProxy(@PathVariable String id) {
        boolean removed = dataStore.removeProxy(id);
        if (!removed) throw new ProxyNotFoundException(id);
        return ResponseEntity.ok(Map.of("message", "Proxy removed", "id", id));
    }

    private ProxyResponse toResponse(ProxyEntry e) {
        int total = e.getTotalChecks();
        double uptime = total == 0 ? 0.0 : ((double) e.getSuccessfulChecks() / total) * 100.0;

        ProxyResponse r = new ProxyResponse();
        java.math.BigDecimal bd = new java.math.BigDecimal(uptime).setScale(1, java.math.RoundingMode.HALF_UP);
        r.setUptimePercentage(bd.doubleValue());
        r.setId(e.getId());
        r.setUrl(e.getUrl());
        r.setStatus(e.getStatus().name().toLowerCase()); 
        r.setLastCheckedAt(e.getLastCheckedAt()); 
        r.setConsecutiveFailures(e.getConsecutiveFailures());
        r.setTotalChecks(e.getTotalChecks());
        r.setSuccessfulChecks(e.getSuccessfulChecks());
        r.setCreatedAt(e.getCreatedAt());
        r.setHistory(e.getHistory());
        return r;
    }
}
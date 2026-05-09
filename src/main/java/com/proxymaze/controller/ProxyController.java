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

    /**
     * POST /proxies — add URLs to monitoring pool.
     * Returns 201 with field "accepted".
     * Supports "replace": true to clear pool first, false/absent to append.
     * Proxy ID = last URL path segment (e.g. px-001 from .../px-001).
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> addProxies(@RequestBody Map<String, Object> body) {
        // Extract proxies (list of URLs)
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

        // Trigger immediate check in background
        Thread.ofVirtual().start(() -> {
            try { 
                monitoringService.runCycle(); 
            } catch (Exception ignored) {}
        });

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("accepted", added.size());
        result.put("proxies", added); 
        return ResponseEntity.status(201).body(result);
    }

    /** GET /proxies — list all proxies with summary stats */
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
        result.put("failure_rate", failureRate);
        result.put("proxies", list);
        return ResponseEntity.ok(result);
    }

    /** GET /proxies/{id} */
    @GetMapping("/{id}")
    public ResponseEntity<ProxyResponse> getProxy(@PathVariable String id) {
        ProxyEntry entry = dataStore.getProxy(id)
                .orElseThrow(() -> new ProxyNotFoundException(id));
        return ResponseEntity.ok(toResponse(entry));
    }

    /** GET /proxies/{id}/history — returns plain JSON array of check records */
    @GetMapping("/{id}/history")
    public ResponseEntity<List<CheckRecord>> getHistory(@PathVariable String id) {
        ProxyEntry entry = dataStore.getProxy(id)
                .orElseThrow(() -> new ProxyNotFoundException(id));
        return ResponseEntity.ok(entry.getHistory());
    }

    /** DELETE /proxies — clear all proxies; return 204 No Content */
    @DeleteMapping
    public ResponseEntity<Void> clearProxies() {
        dataStore.clearProxies();
        return ResponseEntity.noContent().build(); // 204
    }

    /** DELETE /proxies/{id} */
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
        r.setId(e.getId());
        r.setUrl(e.getUrl());
        r.setStatus(e.getStatus().name().toLowerCase()); // "up"/"down"/"pending"
        r.setLastCheckedAt(e.getLastCheckedAt()); // Reverts to Instant (ISO 8601 string)
        r.setConsecutiveFailures(e.getConsecutiveFailures());
        r.setTotalChecks(e.getTotalChecks());
        r.setSuccessfulChecks(e.getSuccessfulChecks());
        r.setUptimePercentage(uptime);
        r.setCreatedAt(e.getCreatedAt());
        r.setHistory(e.getHistory());
        return r;
    }
}

package com.proxymaze.controller;

import com.proxymaze.dto.ConfigRequest;
import com.proxymaze.model.RuntimeConfiguration;
import com.proxymaze.scheduler.MonitoringScheduler;
import com.proxymaze.storage.DataStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/config")
public class ConfigController {

    private final DataStore dataStore;
    private final MonitoringScheduler scheduler;

    @Autowired
    public ConfigController(DataStore dataStore, MonitoringScheduler scheduler) {
        this.dataStore = dataStore;
        this.scheduler = scheduler;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> getConfig() {
        RuntimeConfiguration cfg = dataStore.getConfig();
        return ResponseEntity.ok(toMap(cfg));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> updateConfig(@RequestBody ConfigRequest req) {
        RuntimeConfiguration cfg = dataStore.getConfig();

        boolean intervalChanged = false;

        if (req.getCheckIntervalSeconds() != null && req.getCheckIntervalSeconds() > 0) {
            if (cfg.getCheckIntervalSeconds() != req.getCheckIntervalSeconds()) {
                intervalChanged = true;
            }
            cfg.setCheckIntervalSeconds(req.getCheckIntervalSeconds());
        }
        if (req.getRequestTimeoutMs() != null && req.getRequestTimeoutMs() > 0) {
            cfg.setRequestTimeoutMs(req.getRequestTimeoutMs());
        }
        if (req.getFailureThreshold() != null && req.getFailureThreshold() > 0 && req.getFailureThreshold() <= 1.0) {
            cfg.setFailureThreshold(req.getFailureThreshold());
        }

        dataStore.setConfig(cfg);

        // Dynamically restart scheduler if interval changed
        if (intervalChanged) {
            scheduler.restartWithInterval(cfg.getCheckIntervalSeconds());
        }

        return ResponseEntity.ok(toMap(cfg));
    }

    private Map<String, Object> toMap(RuntimeConfiguration cfg) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("check_interval_seconds", cfg.getCheckIntervalSeconds());
        m.put("request_timeout_ms", cfg.getRequestTimeoutMs());
        m.put("failure_threshold", cfg.getFailureThreshold());
        return m;
    }
}

package com.proxymaze.controller;

import com.proxymaze.dto.MetricsResponse;
import com.proxymaze.service.MetricsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/metrics")
public class MetricsController {

    private final MetricsService metricsService;

    @Autowired
    public MetricsController(MetricsService metricsService) {
        this.metricsService = metricsService;
    }

    @GetMapping
    public ResponseEntity<MetricsResponse> getMetrics() {
        return ResponseEntity.ok(metricsService.getMetrics());
    }
}
package com.sentinelflow.analytics.controller;

import com.sentinelflow.analytics.service.MetricsAggregator;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final MetricsAggregator aggregator;

    public AnalyticsController(MetricsAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @GetMapping("/summary")
    public ResponseEntity<?> summary() {
        return ResponseEntity.ok(aggregator.getSummary());
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "analytics-service"
        ));
    }
}

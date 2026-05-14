package com.sentinelflow.analytics.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class MetricsAggregator {

    private static final Logger log = LoggerFactory.getLogger(MetricsAggregator.class);

    private final ObjectMapper objectMapper;

    // In-memory metrics (in prod: TimescaleDB hypertables)
    private final Map<String, LongAdder> eventCountByTenant = new ConcurrentHashMap<>();
    private final Map<String, LongAdder> fraudCountByTenant = new ConcurrentHashMap<>();
    private final AtomicLong totalEvents  = new AtomicLong(0);
    private final AtomicLong totalFraud   = new AtomicLong(0);

    public MetricsAggregator(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void processPaymentEvent(String eventJson) {
        try {
            JsonNode event = objectMapper.readTree(eventJson);
            String tenantId = event.has("tenantId") ?
                              event.get("tenantId").asText() : "unknown";

            eventCountByTenant.computeIfAbsent(tenantId, k -> new LongAdder()).increment();
            totalEvents.incrementAndGet();

            log.info("Analytics: processed payment event — total: {}", totalEvents.get());
        } catch (Exception e) {
            log.error("Failed to process payment event: {}", e.getMessage());
        }
    }

    public void processFraudAlert(String alertJson) {
        try {
            JsonNode alert = objectMapper.readTree(alertJson);
            String tenantId = alert.has("tenantId") ?
                              alert.get("tenantId").asText() : "unknown";

            fraudCountByTenant.computeIfAbsent(tenantId, k -> new LongAdder()).increment();
            totalFraud.incrementAndGet();

            log.warn("Analytics: fraud alert recorded — total fraud: {}", totalFraud.get());
        } catch (Exception e) {
            log.error("Failed to process fraud alert: {}", e.getMessage());
        }
    }

    public Map<String, Object> getSummary() {
        double fraudRate = totalEvents.get() > 0 ?
                (double) totalFraud.get() / totalEvents.get() * 100 : 0.0;
        return Map.of(
            "totalEvents",       totalEvents.get(),
            "totalFraudAlerts",  totalFraud.get(),
            "fraudRatePct",      String.format("%.2f%%", fraudRate),
            "eventsByTenant",    eventCountByTenant,
            "fraudByTenant",     fraudCountByTenant
        );
    }
}

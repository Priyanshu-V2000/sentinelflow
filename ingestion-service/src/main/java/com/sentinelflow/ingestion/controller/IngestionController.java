package com.sentinelflow.ingestion.controller;

import com.sentinelflow.ingestion.model.PaymentEvent;
import com.sentinelflow.ingestion.service.IngestionService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/payments")
public class IngestionController {

    private static final Logger log = LoggerFactory.getLogger(IngestionController.class);

    private final IngestionService ingestionService;

    public IngestionController(IngestionService ingestionService) {
        this.ingestionService = ingestionService;
    }

    @PostMapping
    public ResponseEntity<?> ingest(
            @Valid @RequestBody PaymentEvent event,
            @RequestHeader(value = "X-Tenant-Id",
                           defaultValue = "00000000-0000-0000-0000-000000000001") String tenantId) {
        try {
            PaymentEvent saved = ingestionService.ingest(event, tenantId);
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of(
                "event_id",    saved.getId().toString(),
                "accepted_at", Instant.now().toString(),
                "status",      "QUEUED_FOR_PROCESSING"
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(Map.of("error", "DUPLICATE_TRANSACTION",
                                 "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Ingestion failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "INGESTION_FAILED",
                                 "message", e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of("status", "UP", "service", "ingestion-service"));
    }
}

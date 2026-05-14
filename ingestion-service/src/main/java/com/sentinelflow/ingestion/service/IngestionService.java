package com.sentinelflow.ingestion.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelflow.ingestion.model.PaymentEvent;
import com.sentinelflow.ingestion.repository.PaymentEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class IngestionService {

    private static final Logger log = LoggerFactory.getLogger(IngestionService.class);
    private static final String TOPIC = "payment-events";

    private final PaymentEventRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public IngestionService(PaymentEventRepository repository,
                            KafkaTemplate<String, String> kafkaTemplate,
                            ObjectMapper objectMapper) {
        this.repository    = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper  = objectMapper;
    }

    @Transactional
    public PaymentEvent ingest(PaymentEvent event, String tenantId) throws Exception {

        // Set tenant from JWT header (passed by gateway)
        event.setTenantId(UUID.fromString(tenantId));

        // Idempotency check — reject duplicate transaction IDs
        if (repository.existsByTenantIdAndTransactionId(
                event.getTenantId(), event.getTransactionId())) {
            throw new IllegalArgumentException(
                "Duplicate transaction ID: " + event.getTransactionId());
        }

        // Save to PostgreSQL
        PaymentEvent saved = repository.save(event);
        log.info("Saved payment event: {} for tenant: {}",
                 saved.getTransactionId(), tenantId);

        // Publish to Kafka (fire and forget for now)
        String payload = objectMapper.writeValueAsString(saved);
        kafkaTemplate.send(TOPIC, saved.getTenantId().toString(), payload)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish to Kafka: {}", ex.getMessage());
                    } else {
                        log.info("Published to Kafka topic: {}", TOPIC);
                    }
                });

        return saved;
    }
}

package com.sentinelflow.fraud.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sentinelflow.fraud.model.FraudDecision;
import com.sentinelflow.fraud.repository.FraudDecisionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Map;
import java.util.UUID;

@Service
public class FraudScoringService {

    private static final Logger log = LoggerFactory.getLogger(FraudScoringService.class);
    private static final String FRAUD_ALERTS_TOPIC = "fraud-alerts";
    private static final String MODEL_VERSION = "xgboost-v1.0.0";

    @Value("${fraud.score-threshold:0.75}")
    private double threshold;

    private final FraudDecisionRepository repository;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public FraudScoringService(FraudDecisionRepository repository,
                               KafkaTemplate<String, String> kafkaTemplate,
                               ObjectMapper objectMapper) {
        this.repository    = repository;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper  = objectMapper;
    }

    public void score(String eventJson) {
        try {
            JsonNode event = objectMapper.readTree(eventJson);

            UUID paymentEventId = UUID.fromString(event.get("id").asText());
            UUID tenantId       = UUID.fromString(event.get("tenantId").asText());
            double amount       = event.get("amount").asDouble();
            String countryCode  = event.has("countryCode") ?
                                  event.get("countryCode").asText() : "IN";
            String merchantCat  = event.has("merchantCat") ?
                                  event.get("merchantCat").asText() : "0000";

            // ── ML Feature Engineering (stub — connects to Azure ML in prod) ──
            double score = computeFraudScore(amount, countryCode, merchantCat);

            String decision = score >= threshold ? "FRAUD" : "LEGITIMATE";

            log.info("Scored transaction {} — score: {:.4f}, decision: {}",
                     paymentEventId, score, decision);

            // Save audit record
            FraudDecision fd = new FraudDecision();
            fd.setTenantId(tenantId);
            fd.setPaymentEventId(paymentEventId);
            fd.setFraudScore(BigDecimal.valueOf(score).setScale(4, RoundingMode.HALF_UP));
            fd.setScoreThreshold(BigDecimal.valueOf(threshold).setScale(4, RoundingMode.HALF_UP));
            fd.setDecision(decision);
            fd.setModelVersion(MODEL_VERSION);
            repository.save(fd);

            // Publish fraud alert if detected
            if ("FRAUD".equals(decision)) {
                String alert = objectMapper.writeValueAsString(Map.of(
                    "alertId",         UUID.randomUUID().toString(),
                    "tenantId",        tenantId.toString(),
                    "paymentEventId",  paymentEventId.toString(),
                    "fraudScore",      score,
                    "modelVersion",    MODEL_VERSION,
                    "decision",        decision
                ));
                kafkaTemplate.send(FRAUD_ALERTS_TOPIC, tenantId.toString(), alert);
                log.warn("FRAUD ALERT published for transaction: {}", paymentEventId);
            }

        } catch (Exception e) {
            log.error("Fraud scoring failed: {}", e.getMessage(), e);
        }
    }

    /**
     * ML scoring stub — simulates XGBoost model.
     * In production this calls Azure ML endpoint via HTTP.
     *
     * Risk factors:
     *   - High amount → higher risk
     *   - High-risk merchant categories → higher risk
     *   - High-risk countries → higher risk
     */
    private double computeFraudScore(double amount,
                                     String countryCode,
                                     String merchantCat) {
        double score = 0.0;

        // Amount risk (normalised 0-1, amounts over 100k are very suspicious)
        score += Math.min(amount / 200000.0, 0.4);

        // Merchant category risk
        score += switch (merchantCat) {
            case "6051", "6052" -> 0.35; // Crypto, money transfer — very high risk
            case "7995"         -> 0.30; // Gambling
            case "5912"         -> 0.20; // Drug stores
            case "5999"         -> 0.10; // Misc retail — moderate
            default             -> 0.05;
        };

        // Country risk
        score += switch (countryCode) {
            case "NG", "RO", "UA" -> 0.25; // High-risk countries
            case "RU", "VN"       -> 0.20;
            case "IN", "US", "GB" -> 0.05; // Low risk
            default               -> 0.10;
        };

        // Cap at 1.0
        return Math.min(score, 1.0);
    }
}

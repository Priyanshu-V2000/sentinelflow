package com.sentinelflow.insight.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class AiExplanationService {

    private static final Logger log = LoggerFactory.getLogger(AiExplanationService.class);

    private final ObjectMapper objectMapper;

    // Store recent fraud alerts in memory (in prod: stored in DB + pgvector)
    private final Map<String, JsonNode> recentAlerts = new ConcurrentHashMap<>();

    public AiExplanationService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public void storeFraudAlert(String alertJson) {
        try {
            JsonNode alert = objectMapper.readTree(alertJson);
            String alertId = alert.get("alertId").asText();
            recentAlerts.put(alertId, alert);
            log.info("Stored fraud alert: {}", alertId);
        } catch (Exception e) {
            log.error("Failed to store fraud alert: {}", e.getMessage());
        }
    }

    /**
     * Generates a fraud explanation via SSE streaming.
     * In production: calls Azure OpenAI GPT-4o with RAG context.
     * Here: generates a realistic rule-based explanation.
     */
    public void streamExplanation(String fraudDecisionId, SseEmitter emitter) {
        Thread.ofVirtual().start(() -> {
            try {
                // Find matching alert
                JsonNode alert = recentAlerts.values().stream()
                        .filter(a -> a.has("paymentEventId") &&
                                     a.get("paymentEventId").asText().equals(fraudDecisionId))
                        .findFirst()
                        .orElse(null);

                double score = alert != null ?
                        alert.get("fraudScore").asDouble() : 0.95;

                // Simulate GPT-4o streaming — token by token
                List<String> tokens = buildExplanationTokens(score, fraudDecisionId);

                for (String token : tokens) {
                    emitter.send(SseEmitter.event()
                            .name("token")
                            .data(token));
                    Thread.sleep(80); // 80ms between tokens = realistic streaming
                }

                emitter.send(SseEmitter.event().name("done").data("[DONE]"));
                emitter.complete();

            } catch (Exception e) {
                log.error("SSE streaming failed: {}", e.getMessage());
                emitter.completeWithError(e);
            }
        });
    }

    private List<String> buildExplanationTokens(double score, String decisionId) {
        String explanation = String.format(
            "## Fraud Analysis Report\n\n" +
            "**Decision ID:** %s\n" +
            "**Fraud Score:** %.4f (threshold: 0.75)\n\n" +
            "### Why this transaction was flagged:\n\n" +
            "This transaction exhibits **three high-risk signals** that our XGBoost model " +
            "identified as strongly correlated with fraudulent activity:\n\n" +
            "1. **Merchant Category Risk** — The merchant category code (6051) corresponds " +
            "to cryptocurrency exchanges, which have a historically elevated fraud rate " +
            "of 34%% in our training dataset.\n\n" +
            "2. **Geographic Risk** — The transaction originated from a high-risk country. " +
            "Our model assigns a 0.25 risk contribution for this geography based on " +
            "historical fraud patterns.\n\n" +
            "3. **Transaction Amount** — The amount of ₹95,000 is significantly above " +
            "the median transaction value for this merchant category " +
            "(median: ₹12,400). Amount z-score: +3.2σ.\n\n" +
            "### Recommendation:\n\n" +
            "**Block and investigate.** Similar transactions in our historical database " +
            "were confirmed as fraud in 87%% of cases with equivalent risk profiles.\n\n" +
            "### Similar Past Cases:\n\n" +
            "- Case #A1B2: Score 0.98 → Confirmed fraud (crypto + NG + high amount)\n" +
            "- Case #C3D4: Score 0.91 → Confirmed fraud (similar merchant + geography)\n" +
            "- Case #E5F6: Score 0.88 → False positive (legitimate business transfer)\n",
            decisionId, score
        );

        // Split into word tokens for streaming effect
        List<String> tokens = new ArrayList<>();
        for (String word : explanation.split("(?<=\\s)|(?=\\s)")) {
            tokens.add(word);
        }
        return tokens;
    }

    public Map<String, JsonNode> getRecentAlerts() {
        return Collections.unmodifiableMap(recentAlerts);
    }
}

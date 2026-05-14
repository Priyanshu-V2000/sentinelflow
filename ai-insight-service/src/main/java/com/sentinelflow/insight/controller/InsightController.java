package com.sentinelflow.insight.controller;

import com.sentinelflow.insight.service.AiExplanationService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/insights")
public class InsightController {

    private final AiExplanationService aiService;

    public InsightController(AiExplanationService aiService) {
        this.aiService = aiService;
    }

    /**
     * SSE endpoint — streams GPT-4o explanation token by token
     * Open in browser: http://localhost:8084/api/v1/insights/{id}/explain
     */
    @GetMapping(value = "/{fraudDecisionId}/explain",
                produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter explain(@PathVariable String fraudDecisionId) {
        SseEmitter emitter = new SseEmitter(60_000L); // 60s timeout
        aiService.streamExplanation(fraudDecisionId, emitter);
        return emitter;
    }

    @GetMapping("/alerts")
    public ResponseEntity<?> getAlerts() {
        return ResponseEntity.ok(Map.of(
            "count", aiService.getRecentAlerts().size(),
            "alerts", aiService.getRecentAlerts().keySet()
        ));
    }

    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "UP",
            "service", "ai-insight-service"
        ));
    }
}

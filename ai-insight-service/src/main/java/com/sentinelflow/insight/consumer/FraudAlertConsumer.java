package com.sentinelflow.insight.consumer;

import com.sentinelflow.insight.service.AiExplanationService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class FraudAlertConsumer {

    private static final Logger log = LoggerFactory.getLogger(FraudAlertConsumer.class);
    private final AiExplanationService aiService;

    public FraudAlertConsumer(AiExplanationService aiService) {
        this.aiService = aiService;
    }

    @KafkaListener(topics = "fraud-alerts", groupId = "ai-insight-service")
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Received fraud alert from Kafka");
        aiService.storeFraudAlert(record.value());
    }
}

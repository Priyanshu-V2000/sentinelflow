package com.sentinelflow.fraud.consumer;

import com.sentinelflow.fraud.service.FraudScoringService;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final FraudScoringService fraudScoringService;

    public PaymentEventConsumer(FraudScoringService fraudScoringService) {
        this.fraudScoringService = fraudScoringService;
    }

    @KafkaListener(topics = "payment-events",
                   groupId = "fraud-detection-service")
    public void consume(ConsumerRecord<String, String> record) {
        log.info("Received payment event from Kafka — offset: {}, partition: {}",
                 record.offset(), record.partition());
        fraudScoringService.score(record.value());
    }
}

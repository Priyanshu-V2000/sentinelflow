package com.sentinelflow.analytics.consumer;

import com.sentinelflow.analytics.service.MetricsAggregator;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class AnalyticsConsumer {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsConsumer.class);
    private final MetricsAggregator aggregator;

    public AnalyticsConsumer(MetricsAggregator aggregator) {
        this.aggregator = aggregator;
    }

    @KafkaListener(topics = "payment-events", groupId = "analytics-service")
    public void consumePayments(ConsumerRecord<String, String> record) {
        aggregator.processPaymentEvent(record.value());
    }

    @KafkaListener(topics = "fraud-alerts", groupId = "analytics-service")
    public void consumeFraudAlerts(ConsumerRecord<String, String> record) {
        aggregator.processFraudAlert(record.value());
    }
}

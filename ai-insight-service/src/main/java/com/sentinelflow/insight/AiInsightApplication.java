package com.sentinelflow.insight;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class AiInsightApplication {

    private static final Logger log = LoggerFactory.getLogger(AiInsightApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AiInsightApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("SentinelFlow AI Insight Service started on port 8084");
    }
}

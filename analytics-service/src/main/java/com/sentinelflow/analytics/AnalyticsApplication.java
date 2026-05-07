package com.sentinelflow.analytics;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class AnalyticsApplication {

    private static final Logger log = LoggerFactory.getLogger(AnalyticsApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("SentinelFlow Analytics Service started on port 8082");
    }
}

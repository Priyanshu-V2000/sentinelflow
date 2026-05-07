package com.sentinelflow.ingestion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class IngestionApplication {

    private static final Logger log = LoggerFactory.getLogger(IngestionApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(IngestionApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("SentinelFlow Ingestion Service started on port 8081");
    }
}

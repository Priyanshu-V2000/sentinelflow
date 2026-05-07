package com.sentinelflow.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;

@SpringBootApplication
public class UserTenantApplication {

    private static final Logger log = LoggerFactory.getLogger(UserTenantApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(UserTenantApplication.class, args);
    }

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("SentinelFlow User Tenant Service started on port 8085");
    }
}

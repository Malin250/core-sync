package com.example.coresyncservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Core Sync Service — Spring Boot entry point.
 *
 * {@code @EnableScheduling} activates the scheduled task that purges
 * expired password-reset tokens from the database nightly.
 */
@SpringBootApplication
@EnableScheduling
public class CoreSyncServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CoreSyncServiceApplication.class, args);
    }
}

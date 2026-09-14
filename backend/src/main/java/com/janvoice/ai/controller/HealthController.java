package com.janvoice.ai.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Simple health-check endpoint for Railway.
 * Returns 200 OK when the service is running.
 * Also accessible via Spring Boot Actuator at /actuator/health.
 */
@RestController
public class HealthController {

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }
}

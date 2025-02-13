package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.HealthStatus;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {
    private boolean isHealthy = true; // Default to healthy

    @GetMapping
    public ResponseEntity<?> checkHealth() {
        if (isHealthy) {
            return ResponseEntity.ok(HealthStatus.builder().status("UP").build());
        } else {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(HealthStatus.builder().status("DOWN").build());
        }
    }

    @PostMapping("/toggle/{status}")
    @ResponseStatus(HttpStatus.OK)
    public String toggleHealth(@PathVariable boolean status) {
        isHealthy = status;
        return "Health status set to: " + (status ? "Healthy" : "Unhealthy");
    }
}

package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.dto.HealthStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health")
@RequiredArgsConstructor
public class HealthController {

    @Value("${aws.region}")
    private String region;

    @GetMapping
    public ResponseEntity<?> checkHealth() {
        return ResponseEntity.ok(HealthStatus.builder().status("UP").region(region).build());
    }

}

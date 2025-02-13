package org.example.controller;

import lombok.RequiredArgsConstructor;
import org.example.util.monitoring.CustomHealthIndicator;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/health-toggle")
@RequiredArgsConstructor
public class HealthController {
    private final CustomHealthIndicator customHealthIndicator;

    @PostMapping("/down")
    public String setDown() {
        customHealthIndicator.setHealthStatus(false);
        return "Health set to DOWN";
    }

    @PostMapping("/up")
    public String setUp() {
        customHealthIndicator.setHealthStatus(true);
        return "Health set to UP";
    }
}

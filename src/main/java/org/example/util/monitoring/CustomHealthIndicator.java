package org.example.util.monitoring;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Service;

@Service
public class CustomHealthIndicator implements HealthIndicator {
    private boolean isHealthy = true;

    @Override
    public Health health() {
        return isHealthy ? Health.up().build() : Health.down().withDetail("Error", "Manually set to DOWN").build();
    }

    public void setHealthStatus(boolean status) {
        this.isHealthy = status;
    }
}

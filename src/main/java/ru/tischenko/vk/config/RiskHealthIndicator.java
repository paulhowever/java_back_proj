package ru.tischenko.vk.config;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;
import ru.tischenko.vk.repository.Repositories.TaskRepository;

@Component
public class RiskHealthIndicator implements HealthIndicator {
    private final TaskRepository taskRepository;

    public RiskHealthIndicator(TaskRepository taskRepository) {
        this.taskRepository = taskRepository;
    }

    @Override
    public Health health() {
        return Health.up().withDetail("totalTasks", taskRepository.count()).build();
    }
}

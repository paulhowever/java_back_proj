package ru.tischenko.vk.service;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

@Service
public class DomainEventService {
    private final ApplicationEventPublisher publisher;

    public DomainEventService(ApplicationEventPublisher publisher) {
        this.publisher = publisher;
    }

    public void publishTaskStarted(Long taskId) { publisher.publishEvent(new TaskStartedEvent(taskId)); }
    public void publishRiskDetected(String text) { publisher.publishEvent(new RiskDetectedEvent(text)); }
    public void publishRebalanceDone(Long sprintId) { publisher.publishEvent(new RebalanceDoneEvent(sprintId)); }
    public void publishNotificationCreated(Long notificationId) { publisher.publishEvent(new NotificationCreatedEvent(notificationId)); }

    public record TaskStartedEvent(Long taskId) {}
    public record RiskDetectedEvent(String text) {}
    public record RebalanceDoneEvent(Long sprintId) {}
    public record NotificationCreatedEvent(Long notificationId) {}
}

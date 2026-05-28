package ru.tischenko.vk.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import ru.tischenko.vk.service.ops.RiskNotificationService;

@Component
public class ScheduledJobs {
    private final RiskNotificationService riskNotificationService;

    public ScheduledJobs(RiskNotificationService riskNotificationService) {
        this.riskNotificationService = riskNotificationService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void sendRiskNotifications() {
        riskNotificationService.scheduleRiskNotifications();
    }
}

package ru.tischenko.vk.service;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class ScheduledJobs {
    private final CoreService coreService;

    public ScheduledJobs(CoreService coreService) {
        this.coreService = coreService;
    }

    @Scheduled(cron = "0 */10 * * * *")
    public void sendRiskNotifications() {
        coreService.scheduleRiskNotifications();
    }
}

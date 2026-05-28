package ru.tischenko.vk.service.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.domain.Enums.DeliveryStatus;
import ru.tischenko.vk.domain.Enums.NotificationType;
import ru.tischenko.vk.domain.NotificationEntity;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.repository.NotificationRepository;
import ru.tischenko.vk.repository.TaskRepository;
import ru.tischenko.vk.service.DomainEventService;

import java.time.Instant;
import java.util.List;

@Service
public class RiskNotificationService {
    private static final Logger log = LoggerFactory.getLogger(RiskNotificationService.class);

    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final DomainEventService eventService;

    public RiskNotificationService(TaskRepository taskRepository,
                                   NotificationRepository notificationRepository,
                                   DomainEventService eventService) {
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
        this.eventService = eventService;
    }

    @Transactional
    public void scheduleRiskNotifications() {
        List<TaskEntity> overdue = taskRepository.findOverdueTasks(Instant.now());
        int created = 0;
        for (TaskEntity task : overdue) {
            if (task.getAssignee() == null) {
                continue;
            }
            if (notificationRepository.existsByUserIdAndTextAndType(
                    task.getAssignee().getId(),
                    "Task is overdue: " + task.getTitle(),
                    NotificationType.RISK)) {
                continue;
            }
            NotificationEntity n = new NotificationEntity();
            n.setUser(task.getAssignee());
            n.setType(NotificationType.RISK);
            n.setText("Task is overdue: " + task.getTitle());
            n.setDeliveryStatus(DeliveryStatus.PENDING);
            notificationRepository.save(n);
            eventService.publishNotificationCreated(n.getId());
            created++;
        }
        if (created > 0) {
            log.info("scheduleRiskNotifications createdNotifications={}", created);
        }
    }
}

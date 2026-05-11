package ru.tischenko.vk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import ru.tischenko.vk.domain.Enums.DeliveryStatus;
import ru.tischenko.vk.repository.Repositories.NotificationRepository;

@Component
public class NotificationHandlers {
    private static final Logger log = LoggerFactory.getLogger(NotificationHandlers.class);
    private final JavaMailSender mailSender;
    private final NotificationRepository notificationRepository;

    public NotificationHandlers(JavaMailSender mailSender, NotificationRepository notificationRepository) {
        this.mailSender = mailSender;
        this.notificationRepository = notificationRepository;
    }

    @EventListener
    public void onRisk(DomainEventService.RiskDetectedEvent event) {
        log.warn("Risk detected: {}", event.text());
    }

    @Async
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onNotificationCreated(DomainEventService.NotificationCreatedEvent event) {
        notificationRepository.findById(event.notificationId()).ifPresent(n -> {
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo(n.getUser().getEmail());
                msg.setSubject("TeamCoord notification");
                msg.setText(n.getText());
                mailSender.send(msg);
                n.setDeliveryStatus(DeliveryStatus.SENT);
            } catch (Exception e) {
                log.error("Failed to send email for notification {}", event.notificationId(), e);
                n.setDeliveryStatus(DeliveryStatus.FAILED);
            }
        });
    }
}

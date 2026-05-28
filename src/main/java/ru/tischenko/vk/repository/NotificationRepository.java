package ru.tischenko.vk.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tischenko.vk.domain.NotificationEntity;
import ru.tischenko.vk.domain.Enums.NotificationType;

@Repository
public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
    boolean existsByUserIdAndTextAndType(Long userId, String text, NotificationType type);
}

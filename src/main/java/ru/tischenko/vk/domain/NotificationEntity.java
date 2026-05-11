package ru.tischenko.vk.domain;

import jakarta.persistence.*;
import ru.tischenko.vk.domain.Enums.DeliveryStatus;
import ru.tischenko.vk.domain.Enums.NotificationType;

import java.time.Instant;

@Entity
@Table(name = "notifications")
public class NotificationEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private NotificationType type;

    @Column(nullable = false)
    private String text;

    @Enumerated(EnumType.STRING)
    @Column(name = "delivery_status", nullable = false)
    private DeliveryStatus deliveryStatus;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

    public Long getId() { return id; }
    public UserEntity getUser() { return user; }
    public void setUser(UserEntity user) { this.user = user; }
    public NotificationType getType() { return type; }
    public void setType(NotificationType type) { this.type = type; }
    public String getText() { return text; }
    public void setText(String text) { this.text = text; }
    public DeliveryStatus getDeliveryStatus() { return deliveryStatus; }
    public void setDeliveryStatus(DeliveryStatus deliveryStatus) { this.deliveryStatus = deliveryStatus; }
}

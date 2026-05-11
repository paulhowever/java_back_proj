package ru.tischenko.vk.domain;

public final class Enums {
    private Enums() {
    }

    public enum Role {
        USER, ADMIN
    }

    public enum UserLevel {
        JUNIOR, MIDDLE, LEAD
    }

    public enum ProjectStatus {
        PLANNED, ACTIVE, DONE
    }

    public enum SprintStatus {
        PLANNED, ACTIVE, DONE
    }

    public enum TaskStatus {
        TODO, IN_PROGRESS, DONE
    }

    public enum TaskPriority {
        LOW, MEDIUM, HIGH, CRITICAL
    }

    public enum SubTeamDirection {
        BACKEND, FRONTEND, DESIGN
    }

    public enum NotificationType {
        INFO, WARNING, RISK
    }

    public enum DeliveryStatus {
        PENDING, SENT, FAILED
    }
}

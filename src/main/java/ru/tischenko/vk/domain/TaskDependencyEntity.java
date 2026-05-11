package ru.tischenko.vk.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "task_dependencies",
        uniqueConstraints = @UniqueConstraint(columnNames = {"blocker_task_id", "blocked_task_id"}))
public class TaskDependencyEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocker_task_id", nullable = false)
    private TaskEntity blockerTask;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "blocked_task_id", nullable = false)
    private TaskEntity blockedTask;

    public Long getId() { return id; }
    public TaskEntity getBlockerTask() { return blockerTask; }
    public void setBlockerTask(TaskEntity blockerTask) { this.blockerTask = blockerTask; }
    public TaskEntity getBlockedTask() { return blockedTask; }
    public void setBlockedTask(TaskEntity blockedTask) { this.blockedTask = blockedTask; }
}

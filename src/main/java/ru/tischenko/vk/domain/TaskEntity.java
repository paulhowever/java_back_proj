package ru.tischenko.vk.domain;

import jakarta.persistence.*;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;

import java.time.Instant;

@Entity
@Table(name = "tasks")
public class TaskEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sprint_id", nullable = false)
    private SprintEntity sprint;

    @Column(nullable = false)
    private String title;

    @Column
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskPriority priority;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assignee_id")
    private UserEntity assignee;

    @Column
    private Instant deadline;

    @Column(name = "estimated_hours", nullable = false)
    private Integer estimatedHours = 1;

    @Version
    private Long version;

    public Long getId() { return id; }
    public SprintEntity getSprint() { return sprint; }
    public void setSprint(SprintEntity sprint) { this.sprint = sprint; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public TaskStatus getStatus() { return status; }
    public void setStatus(TaskStatus status) { this.status = status; }
    public TaskPriority getPriority() { return priority; }
    public void setPriority(TaskPriority priority) { this.priority = priority; }
    public UserEntity getAssignee() { return assignee; }
    public void setAssignee(UserEntity assignee) { this.assignee = assignee; }
    public Instant getDeadline() { return deadline; }
    public void setDeadline(Instant deadline) { this.deadline = deadline; }
    public Integer getEstimatedHours() { return estimatedHours; }
    public void setEstimatedHours(Integer estimatedHours) { this.estimatedHours = estimatedHours; }
    public Long getVersion() { return version; }
}

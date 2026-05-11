package ru.tischenko.vk.domain;

import jakarta.persistence.*;
import ru.tischenko.vk.domain.Enums.SprintStatus;

import java.time.LocalDate;

@Entity
@Table(name = "sprints")
public class SprintEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false)
    private String name;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SprintStatus status;

    @Version
    private Long version;

    public Long getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public void setProject(ProjectEntity project) { this.project = project; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public SprintStatus getStatus() { return status; }
    public void setStatus(SprintStatus status) { this.status = status; }
}

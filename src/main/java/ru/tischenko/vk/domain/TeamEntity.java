package ru.tischenko.vk.domain;

import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "teams", uniqueConstraints = @UniqueConstraint(columnNames = {"project_id", "name"}))
public class TeamEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", nullable = false)
    private ProjectEntity project;

    @Column(nullable = false)
    private String name;

    @ManyToMany
    @JoinTable(name = "team_members",
            joinColumns = @JoinColumn(name = "team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<UserEntity> members = new HashSet<>();

    @Version
    private Long version;

    public Long getId() { return id; }
    public ProjectEntity getProject() { return project; }
    public void setProject(ProjectEntity project) { this.project = project; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<UserEntity> getMembers() { return members; }
}

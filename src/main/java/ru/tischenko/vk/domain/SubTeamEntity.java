package ru.tischenko.vk.domain;

import jakarta.persistence.*;
import ru.tischenko.vk.domain.Enums.SubTeamDirection;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "sub_teams", uniqueConstraints = @UniqueConstraint(columnNames = {"team_id", "name"}))
public class SubTeamEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "team_id", nullable = false)
    private TeamEntity team;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SubTeamDirection direction;

    @Column(nullable = false)
    private String name;

    @ManyToMany
    @JoinTable(name = "sub_team_members",
            joinColumns = @JoinColumn(name = "sub_team_id"),
            inverseJoinColumns = @JoinColumn(name = "user_id"))
    private Set<UserEntity> members = new HashSet<>();

    @Version
    private Long version;

    public Long getId() { return id; }
    public TeamEntity getTeam() { return team; }
    public void setTeam(TeamEntity team) { this.team = team; }
    public SubTeamDirection getDirection() { return direction; }
    public void setDirection(SubTeamDirection direction) { this.direction = direction; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Set<UserEntity> getMembers() { return members; }
}

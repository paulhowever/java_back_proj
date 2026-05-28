package ru.tischenko.vk.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.TeamRequest;
import ru.tischenko.vk.domain.TeamEntity;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.repository.TeamRepository;
import ru.tischenko.vk.repository.UserRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TeamService {
    private final TeamRepository teamRepository;
    private final UserRepository userRepository;
    private final ProjectService projectService;

    public TeamService(TeamRepository teamRepository, UserRepository userRepository, ProjectService projectService) {
        this.teamRepository = teamRepository;
        this.userRepository = userRepository;
        this.projectService = projectService;
    }

    @Transactional
    public TeamEntity createTeam(TeamRequest req) {
        TeamEntity team = new TeamEntity();
        team.setProject(projectService.getProject(req.projectId()));
        team.setName(req.name());
        try {
            return teamRepository.save(team);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("Team name already exists in project");
        }
    }

    public TeamEntity getTeam(Long id) {
        return teamRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Team not found"));
    }

    @Transactional(readOnly = true)
    public TeamEntity getTeamWithMembers(Long id) {
        TeamEntity team = getTeam(id);
        team.getMembers().size();
        return team;
    }

    @Transactional
    public void deleteTeam(Long id) {
        teamRepository.delete(getTeam(id));
    }

    public Page<TeamEntity> listTeams(Long projectId, Pageable pageable) {
        return projectId == null ? teamRepository.findAll(pageable) : teamRepository.findByProjectId(projectId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<TeamEntity> listTeamsWithMembers(Long projectId, Pageable pageable) {
        Page<TeamEntity> page = listTeams(projectId, pageable);
        page.forEach(t -> t.getMembers().size());
        return page;
    }

    @Transactional
    public TeamEntity setTeamMembers(Long teamId, List<Long> userIds) {
        TeamEntity team = getTeam(teamId);
        Set<Long> uniqueIds = new HashSet<>(userIds);
        if (uniqueIds.size() != userIds.size()) {
            throw new Exceptions.BusinessRuleException("Duplicate user ids in request");
        }
        List<UserEntity> users = userRepository.findAllById(uniqueIds);
        if (users.size() != uniqueIds.size()) {
            throw new Exceptions.NotFoundException("One or more users not found");
        }
        team.getMembers().clear();
        team.getMembers().addAll(users);
        return team;
    }
}

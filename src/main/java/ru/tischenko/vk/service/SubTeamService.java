package ru.tischenko.vk.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.SubTeamRequest;
import ru.tischenko.vk.domain.SubTeamEntity;
import ru.tischenko.vk.domain.TeamEntity;
import ru.tischenko.vk.repository.SubTeamRepository;

@Service
public class SubTeamService {
    private final SubTeamRepository subTeamRepository;
    private final TeamService teamService;

    public SubTeamService(SubTeamRepository subTeamRepository, TeamService teamService) {
        this.subTeamRepository = subTeamRepository;
        this.teamService = teamService;
    }

    @Transactional
    public SubTeamEntity createSubTeam(SubTeamRequest req) {
        TeamEntity team = teamService.getTeam(req.teamId());
        SubTeamEntity subTeam = new SubTeamEntity();
        subTeam.setTeam(team);
        subTeam.setName(req.name());
        subTeam.setDirection(req.direction());
        try {
            return subTeamRepository.save(subTeam);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("SubTeam name already exists in team");
        }
    }

    public SubTeamEntity getSubTeam(Long id) {
        return subTeamRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("SubTeam not found"));
    }

    @Transactional(readOnly = true)
    public SubTeamEntity getSubTeamWithMembers(Long id) {
        SubTeamEntity sub = getSubTeam(id);
        sub.getMembers().size();
        return sub;
    }

    @Transactional
    public void deleteSubTeam(Long id) {
        subTeamRepository.delete(getSubTeam(id));
    }

    public Page<SubTeamEntity> listSubTeams(Long teamId, Pageable pageable) {
        return teamId == null ? subTeamRepository.findAll(pageable) : subTeamRepository.findByTeamId(teamId, pageable);
    }

    @Transactional(readOnly = true)
    public Page<SubTeamEntity> listSubTeamsWithMembers(Long teamId, Pageable pageable) {
        Page<SubTeamEntity> page = listSubTeams(teamId, pageable);
        page.forEach(s -> s.getMembers().size());
        return page;
    }
}

package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.TeamMembersRequest;
import ru.tischenko.vk.api.dto.Dtos.TeamRequest;
import ru.tischenko.vk.api.dto.Dtos.TeamResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.TeamMapper;
import ru.tischenko.vk.service.TeamService;

@RestController
@RequestMapping("/api/v1/teams")
public class TeamController {
    private final TeamService teamService;
    private final TeamMapper teamMapper;

    public TeamController(TeamService teamService, TeamMapper teamMapper) {
        this.teamService = teamService;
        this.teamMapper = teamMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create team")
    public TeamResponse createTeam(@RequestBody @Valid TeamRequest req) {
        return teamMapper.toResponse(teamService.createTeam(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TeamResponse getTeam(@PathVariable Long id) {
        return teamMapper.toResponse(teamService.getTeamWithMembers(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable Long id) {
        teamService.deleteTeam(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<TeamResponse> listTeams(@RequestParam(required = false) Long projectId, Pageable pageable) {
        return teamService.listTeamsWithMembers(projectId, pageable).map(teamMapper::toResponse);
    }

    @PutMapping("/{id}/members")
    @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse setTeamMembers(@PathVariable Long id, @RequestBody @Valid TeamMembersRequest req) {
        return teamMapper.toResponse(teamService.setTeamMembers(id, req.userIds()));
    }
}

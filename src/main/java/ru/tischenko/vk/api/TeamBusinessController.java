package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.AnalyzeTeamResponse;
import ru.tischenko.vk.api.dto.Dtos.AssignSubTeamRequest;
import ru.tischenko.vk.api.dto.Dtos.AssignSubTeamResponse;
import ru.tischenko.vk.service.ops.TeamOperationsService;

@RestController
@RequestMapping("/api/v1/business")
public class TeamBusinessController {
    private final TeamOperationsService teamOperations;

    public TeamBusinessController(TeamOperationsService teamOperations) {
        this.teamOperations = teamOperations;
    }

    @PostMapping("/subteam/assign")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign users to sub-team (warns if no lead)")
    public AssignSubTeamResponse assignSubTeam(@RequestBody @Valid AssignSubTeamRequest req) {
        var result = teamOperations.assignUsersToSubTeam(req);
        return new AssignSubTeamResponse(result.hasLead(), result.warningNotificationId());
    }

    @GetMapping("/team/{teamId}/analysis")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public AnalyzeTeamResponse analyzeTeam(@PathVariable Long teamId) {
        return teamOperations.analyzeTeam(teamId);
    }
}

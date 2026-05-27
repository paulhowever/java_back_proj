package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.SubTeamRequest;
import ru.tischenko.vk.api.dto.Dtos.SubTeamResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.SubTeamMapper;
import ru.tischenko.vk.service.SubTeamService;

@RestController
@RequestMapping("/api/v1/subteams")
public class SubTeamController {
    private final SubTeamService subTeamService;
    private final SubTeamMapper subTeamMapper;

    public SubTeamController(SubTeamService subTeamService, SubTeamMapper subTeamMapper) {
        this.subTeamService = subTeamService;
        this.subTeamMapper = subTeamMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create sub-team")
    public SubTeamResponse createSubTeam(@RequestBody @Valid SubTeamRequest req) {
        return subTeamMapper.toResponse(subTeamService.createSubTeam(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public SubTeamResponse getSubTeam(@PathVariable Long id) {
        return subTeamMapper.toResponse(subTeamService.getSubTeamWithMembers(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubTeam(@PathVariable Long id) {
        subTeamService.deleteSubTeam(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<SubTeamResponse> listSubTeams(@RequestParam(required = false) Long teamId, Pageable pageable) {
        return subTeamService.listSubTeamsWithMembers(teamId, pageable).map(subTeamMapper::toResponse);
    }
}

package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.SprintRequest;
import ru.tischenko.vk.api.dto.Dtos.SprintResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.SprintMapper;
import ru.tischenko.vk.service.SprintService;

@RestController
@RequestMapping("/api/v1/sprints")
public class SprintController {
    private final SprintService sprintService;
    private final SprintMapper sprintMapper;

    public SprintController(SprintService sprintService, SprintMapper sprintMapper) {
        this.sprintService = sprintService;
        this.sprintMapper = sprintMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create sprint (duration must be 1..3 days)")
    @ApiResponse(responseCode = "201", description = "Sprint created")
    @ApiResponse(responseCode = "409", description = "Sprint duration violation")
    public SprintResponse createSprint(@RequestBody @Valid SprintRequest req) {
        return sprintMapper.toResponse(sprintService.createSprint(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public SprintResponse getSprint(@PathVariable Long id) {
        return sprintMapper.toResponse(sprintService.getSprint(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public SprintResponse updateSprint(@PathVariable Long id, @RequestBody @Valid SprintRequest req) {
        return sprintMapper.toResponse(sprintService.updateSprint(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSprint(@PathVariable Long id) {
        sprintService.deleteSprint(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<SprintResponse> listSprints(@RequestParam(required = false) Long projectId, Pageable pageable) {
        return sprintService.listSprints(projectId, pageable).map(sprintMapper::toResponse);
    }
}

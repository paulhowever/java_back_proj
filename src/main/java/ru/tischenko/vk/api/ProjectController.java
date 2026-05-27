package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.ProjectRequest;
import ru.tischenko.vk.api.dto.Dtos.ProjectResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.ProjectMapper;
import ru.tischenko.vk.domain.Enums.ProjectStatus;
import ru.tischenko.vk.service.ProjectService;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/projects")
public class ProjectController {
    private final ProjectService projectService;
    private final ProjectMapper projectMapper;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create project")
    @ApiResponse(responseCode = "201", description = "Project created")
    public ProjectResponse createProject(@RequestBody @Valid ProjectRequest req) {
        return projectMapper.toResponse(projectService.createProject(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ProjectResponse getProject(@PathVariable Long id) {
        return projectMapper.toResponse(projectService.getProject(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse updateProject(@PathVariable Long id, @RequestBody @Valid ProjectRequest req) {
        return projectMapper.toResponse(projectService.updateProject(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) {
        projectService.deleteProject(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "List projects with filters (status, startDate range, name)")
    public Page<ProjectResponse> listProjects(@RequestParam(required = false) ProjectStatus status,
                                              @RequestParam(required = false) LocalDate startDateFrom,
                                              @RequestParam(required = false) LocalDate startDateTo,
                                              @RequestParam(required = false) String nameContains,
                                              Pageable pageable) {
        return projectService.listProjects(status, startDateFrom, startDateTo, nameContains, pageable).map(projectMapper::toResponse);
    }
}

package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.TaskDependencyRequest;
import ru.tischenko.vk.api.dto.Dtos.TaskDependencyResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.TaskDependencyMapper;
import ru.tischenko.vk.service.TaskDependencyService;

@RestController
@RequestMapping("/api/v1/task-dependencies")
public class TaskDependencyController {
    private final TaskDependencyService taskDependencyService;
    private final TaskDependencyMapper taskDependencyMapper;

    public TaskDependencyController(TaskDependencyService taskDependencyService, TaskDependencyMapper taskDependencyMapper) {
        this.taskDependencyService = taskDependencyService;
        this.taskDependencyMapper = taskDependencyMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create task dependency")
    public TaskDependencyResponse createDependency(@RequestBody @Valid TaskDependencyRequest req) {
        return taskDependencyMapper.toResponse(taskDependencyService.createTaskDependency(req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDependency(@PathVariable Long id) {
        taskDependencyService.deleteTaskDependency(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<TaskDependencyResponse> listDependencies(Pageable pageable) {
        return taskDependencyService.listTaskDependencies(pageable).map(taskDependencyMapper::toResponse);
    }
}

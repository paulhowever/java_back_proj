package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.TaskRequest;
import ru.tischenko.vk.api.dto.Dtos.TaskResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.TaskMapper;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.service.TaskService;

@RestController
@RequestMapping("/api/v1/tasks")
public class TaskController {
    private final TaskService taskService;
    private final TaskMapper taskMapper;

    public TaskController(TaskService taskService, TaskMapper taskMapper) {
        this.taskService = taskService;
        this.taskMapper = taskMapper;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create task")
    @ApiResponse(responseCode = "201", description = "Task created")
    public TaskResponse createTask(@RequestBody @Valid TaskRequest req) {
        return taskMapper.toResponse(taskService.createTask(req));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TaskResponse getTask(@PathVariable Long id) {
        return taskMapper.toResponse(taskService.getTask(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TaskResponse updateTask(@PathVariable Long id, @RequestBody @Valid TaskRequest req) {
        return taskMapper.toResponse(taskService.updateTask(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<TaskResponse> listTasks(@RequestParam(required = false) TaskStatus status,
                                        @RequestParam(required = false) Long assigneeId,
                                        @RequestParam(required = false) TaskPriority priority,
                                        Pageable pageable) {
        return taskService.listTasks(status, assigneeId, priority, pageable).map(taskMapper::toResponse);
    }
}

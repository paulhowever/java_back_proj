package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.CriticalTaskResponse;
import ru.tischenko.vk.api.dto.Dtos.StartTaskRequest;
import ru.tischenko.vk.api.dto.Dtos.TaskResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.TaskMapper;
import ru.tischenko.vk.service.ops.TaskOperationsService;

@RestController
@RequestMapping("/api/v1/business")
public class TaskBusinessController {
    private final TaskOperationsService taskOperations;
    private final TaskMapper taskMapper;

    public TaskBusinessController(TaskOperationsService taskOperations, TaskMapper taskMapper) {
        this.taskOperations = taskOperations;
        this.taskMapper = taskMapper;
    }

    @PostMapping("/task/start")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Start task with business checks")
    @ApiResponse(responseCode = "200", description = "Task moved to IN_PROGRESS")
    @ApiResponse(responseCode = "409", description = "Business rule violation")
    public TaskResponse startTask(@RequestBody @Valid StartTaskRequest req) {
        return taskMapper.toResponse(taskOperations.startTask(req));
    }

    @GetMapping("/sprint/{sprintId}/critical-tasks")
    @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CriticalTaskResponse criticalTasks(@PathVariable Long sprintId) {
        return taskOperations.findCriticalTasks(sprintId);
    }
}

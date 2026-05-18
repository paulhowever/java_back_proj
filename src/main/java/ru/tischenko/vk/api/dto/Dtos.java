package ru.tischenko.vk.api.dto;

import jakarta.validation.constraints.*;
import ru.tischenko.vk.domain.Enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class Dtos {
    private Dtos() {
    }

    public record UserRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6) String password,
            @NotNull Role role,
            @NotNull UserLevel level
    ) {}

    public record UserResponse(Long id, String email, Role role, UserLevel level) {}

    public record ProjectRequest(
            @NotBlank String name,
            @NotNull LocalDate startDate,
            @NotNull LocalDate endDate,
            @NotNull ProjectStatus status
    ) {}

    public record ProjectResponse(Long id, String name, LocalDate startDate, LocalDate endDate, ProjectStatus status) {}

    public record TaskRequest(
            @NotNull Long sprintId,
            @NotBlank String title,
            String description,
            @NotNull TaskStatus status,
            @NotNull TaskPriority priority,
            Long assigneeId,
            Instant deadline,
            @Min(1) Integer estimatedHours
    ) {}

    public record TaskResponse(Long id, Long sprintId, String title, TaskStatus status, TaskPriority priority, Long assigneeId, Long version) {}

    public record StartTaskRequest(@NotNull Long taskId) {}
    public record AssignSubTeamRequest(@NotNull Long subTeamId, @NotEmpty List<Long> userIds) {}
    public record AssignSubTeamResponse(boolean hasLead, Long warningNotificationId) {}
    public record AnalyzeTeamResponse(boolean hasLead, long juniors, long middles, long leads, String recommendation) {}
    public record CriticalTaskResponse(List<Long> criticalTaskIds) {}
    public record CompleteSprintRequest(@NotNull Long sprintId) {}
    public record BulkRebalanceRequest(@NotNull Long sprintId) {}
    public record IdempotentResultResponse(String operation, String idempotencyKey, boolean replayed, Object payload) {}

    public record AuthRequest(@NotBlank String email, @NotBlank String password) {}
    public record AuthResponse(String token) {}
    public record RegisterRequest(
            @Email @NotBlank String email,
            @NotBlank @Size(min = 6) String password,
            @NotNull UserLevel level
    ) {}

    public record TeamRequest(@NotNull Long projectId, @NotBlank String name) {}
    public record TeamResponse(Long id, Long projectId, String name, List<Long> memberIds) {}

    public record SubTeamRequest(
            @NotNull Long teamId,
            @NotBlank String name,
            @NotNull SubTeamDirection direction
    ) {}
    public record SubTeamResponse(Long id, Long teamId, String name, SubTeamDirection direction, List<Long> memberIds) {}

    public record SprintRequest(
            @NotNull Long projectId,
            @NotBlank String name,
            @NotNull java.time.LocalDate startDate,
            @NotNull java.time.LocalDate endDate,
            @NotNull SprintStatus status
    ) {}
    public record SprintResponse(Long id, Long projectId, String name, java.time.LocalDate startDate, java.time.LocalDate endDate, SprintStatus status) {}

    public record TaskDependencyRequest(@NotNull Long blockerTaskId, @NotNull Long blockedTaskId) {}
    public record TaskDependencyResponse(Long id, Long blockerTaskId, Long blockedTaskId) {}

    public record TeamMembersRequest(@NotEmpty List<Long> userIds) {}

    public record EscalateCriticalRequest(@NotNull Long sprintId) {}
    public record CriticalEscalationResponse(List<Long> escalatedTaskIds) {}
}

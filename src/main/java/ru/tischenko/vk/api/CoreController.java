package ru.tischenko.vk.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import ru.tischenko.vk.api.dto.Dtos.*;
import ru.tischenko.vk.api.mapper.ApiMappers.ProjectMapper;
import ru.tischenko.vk.api.mapper.ApiMappers.TaskMapper;
import ru.tischenko.vk.api.mapper.ApiMappers.UserMapper;
import ru.tischenko.vk.domain.Enums.ProjectStatus;
import ru.tischenko.vk.domain.Enums.Role;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.Enums.UserLevel;
import ru.tischenko.vk.domain.SubTeamEntity;
import ru.tischenko.vk.domain.TaskDependencyEntity;
import ru.tischenko.vk.domain.TeamEntity;
import ru.tischenko.vk.service.IdempotentOperationExecutor;
import ru.tischenko.vk.service.ProjectService;
import ru.tischenko.vk.service.SprintService;
import ru.tischenko.vk.service.SubTeamService;
import ru.tischenko.vk.service.TaskDependencyService;
import ru.tischenko.vk.service.TaskService;
import ru.tischenko.vk.service.TeamService;
import ru.tischenko.vk.service.UserService;
import ru.tischenko.vk.service.ops.SprintOperationsService;
import ru.tischenko.vk.service.ops.TaskOperationsService;
import ru.tischenko.vk.service.ops.TeamOperationsService;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class CoreController {
    private final UserService userService;
    private final ProjectService projectService;
    private final SprintService sprintService;
    private final TaskService taskService;
    private final TeamService teamService;
    private final SubTeamService subTeamService;
    private final TaskDependencyService taskDependencyService;
    private final TeamOperationsService teamOperations;
    private final TaskOperationsService taskOperations;
    private final SprintOperationsService sprintOperations;
    private final UserMapper userMapper;
    private final ProjectMapper projectMapper;
    private final TaskMapper taskMapper;
    private final IdempotentOperationExecutor idempotentOperationExecutor;

    public CoreController(UserService userService,
                          ProjectService projectService,
                          SprintService sprintService,
                          TaskService taskService,
                          TeamService teamService,
                          SubTeamService subTeamService,
                          TaskDependencyService taskDependencyService,
                          TeamOperationsService teamOperations,
                          TaskOperationsService taskOperations,
                          SprintOperationsService sprintOperations,
                          UserMapper userMapper,
                          ProjectMapper projectMapper,
                          TaskMapper taskMapper,
                          IdempotentOperationExecutor idempotentOperationExecutor) {
        this.userService = userService;
        this.projectService = projectService;
        this.sprintService = sprintService;
        this.taskService = taskService;
        this.teamService = teamService;
        this.subTeamService = subTeamService;
        this.taskDependencyService = taskDependencyService;
        this.teamOperations = teamOperations;
        this.taskOperations = taskOperations;
        this.sprintOperations = sprintOperations;
        this.userMapper = userMapper;
        this.projectMapper = projectMapper;
        this.taskMapper = taskMapper;
        this.idempotentOperationExecutor = idempotentOperationExecutor;
    }

    // ===== USER =====

    @PostMapping("/users")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create user")
    @ApiResponse(responseCode = "201", description = "User created")
    @ApiResponse(responseCode = "409", description = "User conflict")
    public UserResponse createUser(@RequestBody @Valid UserRequest req) { return userMapper.toResponse(userService.createUser(req)); }

    @GetMapping("/users/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public UserResponse getUser(@PathVariable Long id) { return userMapper.toResponse(userService.getUser(id)); }

    @PutMapping("/users/{id}") @PreAuthorize("hasRole('ADMIN')")
    public UserResponse updateUser(@PathVariable Long id, @RequestBody @Valid UserRequest req) { return userMapper.toResponse(userService.updateUser(id, req)); }

    @DeleteMapping("/users/{id}") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteUser(@PathVariable Long id) { userService.deleteUser(id); }

    @GetMapping("/users") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "List users with filters (role, level, enabled)")
    public Page<UserResponse> listUsers(@RequestParam(required = false) Role role,
                                        @RequestParam(required = false) UserLevel level,
                                        @RequestParam(required = false) Boolean enabled,
                                        Pageable pageable) { return userService.listUsers(role, level, enabled, pageable).map(userMapper::toResponse); }

    // ===== PROJECT =====

    @PostMapping("/projects") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create project")
    @ApiResponse(responseCode = "201", description = "Project created")
    public ProjectResponse createProject(@RequestBody @Valid ProjectRequest req) { return projectMapper.toResponse(projectService.createProject(req)); }

    @GetMapping("/projects/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public ProjectResponse getProject(@PathVariable Long id) { return projectMapper.toResponse(projectService.getProject(id)); }

    @PutMapping("/projects/{id}") @PreAuthorize("hasRole('ADMIN')")
    public ProjectResponse updateProject(@PathVariable Long id, @RequestBody @Valid ProjectRequest req) { return projectMapper.toResponse(projectService.updateProject(id, req)); }

    @DeleteMapping("/projects/{id}") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProject(@PathVariable Long id) { projectService.deleteProject(id); }

    @GetMapping("/projects") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "List projects with filters (status, startDate range, name)")
    public Page<ProjectResponse> listProjects(@RequestParam(required = false) ProjectStatus status,
                                              @RequestParam(required = false) LocalDate startDateFrom,
                                              @RequestParam(required = false) LocalDate startDateTo,
                                              @RequestParam(required = false) String nameContains,
                                              Pageable pageable) { return projectService.listProjects(status, startDateFrom, startDateTo, nameContains, pageable).map(projectMapper::toResponse); }

    // ===== TASK =====

    @PostMapping("/tasks") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create task")
    @ApiResponse(responseCode = "201", description = "Task created")
    public TaskResponse createTask(@RequestBody @Valid TaskRequest req) { return taskMapper.toResponse(taskService.createTask(req)); }

    @GetMapping("/tasks/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TaskResponse getTask(@PathVariable Long id) { return taskMapper.toResponse(taskService.getTask(id)); }

    @PutMapping("/tasks/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TaskResponse updateTask(@PathVariable Long id, @RequestBody @Valid TaskRequest req) { return taskMapper.toResponse(taskService.updateTask(id, req)); }

    @DeleteMapping("/tasks/{id}") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTask(@PathVariable Long id) { taskService.deleteTask(id); }

    @GetMapping("/tasks") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<TaskResponse> listTasks(@RequestParam(required = false) TaskStatus status,
                                        @RequestParam(required = false) Long assigneeId,
                                        @RequestParam(required = false) TaskPriority priority,
                                        Pageable pageable) { return taskService.listTasks(status, assigneeId, priority, pageable).map(taskMapper::toResponse); }

    // ===== TEAM CRUD =====

    @PostMapping("/teams") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create team")
    public TeamResponse createTeam(@RequestBody @Valid TeamRequest req) { return toTeamResponse(teamService.createTeam(req)); }

    @GetMapping("/teams/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public TeamResponse getTeam(@PathVariable Long id) { return toTeamResponse(teamService.getTeamWithMembers(id)); }

    @DeleteMapping("/teams/{id}") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteTeam(@PathVariable Long id) { teamService.deleteTeam(id); }

    @GetMapping("/teams") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<TeamResponse> listTeams(@RequestParam(required = false) Long projectId, Pageable pageable) {
        return teamService.listTeamsWithMembers(projectId, pageable).map(this::toTeamResponse);
    }

    @PutMapping("/teams/{id}/members") @PreAuthorize("hasRole('ADMIN')")
    public TeamResponse setTeamMembers(@PathVariable Long id, @RequestBody @Valid TeamMembersRequest req) {
        return toTeamResponse(teamService.setTeamMembers(id, req.userIds()));
    }

    // ===== SUBTEAM CRUD =====

    @PostMapping("/subteams") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create sub-team")
    public SubTeamResponse createSubTeam(@RequestBody @Valid SubTeamRequest req) { return toSubTeamResponse(subTeamService.createSubTeam(req)); }

    @GetMapping("/subteams/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public SubTeamResponse getSubTeam(@PathVariable Long id) { return toSubTeamResponse(subTeamService.getSubTeamWithMembers(id)); }

    @DeleteMapping("/subteams/{id}") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSubTeam(@PathVariable Long id) { subTeamService.deleteSubTeam(id); }

    @GetMapping("/subteams") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<SubTeamResponse> listSubTeams(@RequestParam(required = false) Long teamId, Pageable pageable) {
        return subTeamService.listSubTeamsWithMembers(teamId, pageable).map(this::toSubTeamResponse);
    }

    // ===== SPRINT CRUD =====

    @PostMapping("/sprints") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create sprint (duration must be 1..3 days)")
    @ApiResponse(responseCode = "201", description = "Sprint created")
    @ApiResponse(responseCode = "409", description = "Sprint duration violation")
    public SprintResponse createSprint(@RequestBody @Valid SprintRequest req) { return toSprintResponse(sprintService.createSprint(req)); }

    @GetMapping("/sprints/{id}") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public SprintResponse getSprint(@PathVariable Long id) { return toSprintResponse(sprintService.getSprint(id)); }

    @PutMapping("/sprints/{id}") @PreAuthorize("hasRole('ADMIN')")
    public SprintResponse updateSprint(@PathVariable Long id, @RequestBody @Valid SprintRequest req) { return toSprintResponse(sprintService.updateSprint(id, req)); }

    @DeleteMapping("/sprints/{id}") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteSprint(@PathVariable Long id) { sprintService.deleteSprint(id); }

    @GetMapping("/sprints") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<SprintResponse> listSprints(@RequestParam(required = false) Long projectId, Pageable pageable) {
        return sprintService.listSprints(projectId, pageable).map(this::toSprintResponse);
    }

    // ===== TASK DEPENDENCY CRUD =====

    @PostMapping("/task-dependencies") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create task dependency")
    public TaskDependencyResponse createDependency(@RequestBody @Valid TaskDependencyRequest req) {
        return toDependencyResponse(taskDependencyService.createTaskDependency(req));
    }

    @DeleteMapping("/task-dependencies/{id}") @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteDependency(@PathVariable Long id) { taskDependencyService.deleteTaskDependency(id); }

    @GetMapping("/task-dependencies") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public Page<TaskDependencyResponse> listDependencies(Pageable pageable) {
        return taskDependencyService.listTaskDependencies(pageable).map(this::toDependencyResponse);
    }

    // ===== BUSINESS OPS =====

    @PostMapping("/business/subteam/assign") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Assign users to sub-team (warns if no lead)")
    public AssignSubTeamResponse assignSubTeam(@RequestBody @Valid AssignSubTeamRequest req) {
        var result = teamOperations.assignUsersToSubTeam(req);
        return new AssignSubTeamResponse(result.hasLead(), result.warningNotificationId());
    }

    @GetMapping("/business/team/{teamId}/analysis") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public AnalyzeTeamResponse analyzeTeam(@PathVariable Long teamId) { return teamOperations.analyzeTeam(teamId); }

    @PostMapping("/business/task/start") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    @Operation(summary = "Start task with business checks")
    @ApiResponse(responseCode = "200", description = "Task moved to IN_PROGRESS")
    @ApiResponse(responseCode = "409", description = "Business rule violation")
    public TaskResponse startTask(@RequestBody @Valid StartTaskRequest req) { return taskMapper.toResponse(taskOperations.startTask(req)); }

    @GetMapping("/business/sprint/{sprintId}/critical-tasks") @PreAuthorize("hasAnyRole('USER','ADMIN')")
    public CriticalTaskResponse criticalTasks(@PathVariable Long sprintId) { return taskOperations.findCriticalTasks(sprintId); }

    @PostMapping("/business/sprint/escalate-critical") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Auto-escalate priority for tasks that block N+ others in sprint")
    @ApiResponse(responseCode = "200", description = "Returns ids of escalated tasks")
    public CriticalEscalationResponse escalateCritical(@RequestBody @Valid EscalateCriticalRequest req) {
        return taskOperations.escalateCriticalTasks(req.sprintId());
    }

    @PostMapping("/business/sprint/complete") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Complete sprint: marks DONE, emits notifications for unfinished tasks")
    public CompleteSprintResponse completeSprint(@RequestBody @Valid CompleteSprintRequest req) { return sprintOperations.completeSprint(req); }

    @PostMapping("/business/sprint/bulk-rebalance") @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Bulk rebalance sprint tasks across project team members (idempotent via Idempotency-Key header)")
    @ApiResponse(responseCode = "200", description = "Rebalance completed or replayed")
    @ApiResponse(responseCode = "409", description = "Operation already in progress or business rule violation")
    public IdempotentResultResponse bulkRebalance(@RequestBody @Valid BulkRebalanceRequest req,
                                                  @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey) {
        return idempotentOperationExecutor.execute(idempotencyKey, "bulkRebalance", () -> sprintOperations.bulkRebalance(req));
    }

    // ===== mapping helpers =====

    private TeamResponse toTeamResponse(TeamEntity team) {
        List<Long> memberIds = new ArrayList<>();
        if (team.getMembers() != null) team.getMembers().forEach(m -> memberIds.add(m.getId()));
        return new TeamResponse(team.getId(), team.getProject() == null ? null : team.getProject().getId(), team.getName(), memberIds);
    }

    private SubTeamResponse toSubTeamResponse(SubTeamEntity sub) {
        List<Long> memberIds = new ArrayList<>();
        if (sub.getMembers() != null) sub.getMembers().forEach(m -> memberIds.add(m.getId()));
        return new SubTeamResponse(sub.getId(), sub.getTeam() == null ? null : sub.getTeam().getId(), sub.getName(), sub.getDirection(), memberIds);
    }

    private SprintResponse toSprintResponse(ru.tischenko.vk.domain.SprintEntity s) {
        return new SprintResponse(s.getId(), s.getProject() == null ? null : s.getProject().getId(), s.getName(), s.getStartDate(), s.getEndDate(), s.getStatus());
    }

    private TaskDependencyResponse toDependencyResponse(TaskDependencyEntity d) {
        return new TaskDependencyResponse(d.getId(),
                d.getBlockerTask() == null ? null : d.getBlockerTask().getId(),
                d.getBlockedTask() == null ? null : d.getBlockedTask().getId());
    }
}

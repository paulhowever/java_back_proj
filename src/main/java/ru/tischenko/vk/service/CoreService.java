package ru.tischenko.vk.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.*;
import ru.tischenko.vk.domain.*;
import ru.tischenko.vk.domain.Enums.*;
import ru.tischenko.vk.repository.Repositories.*;
import ru.tischenko.vk.service.policy.TaskStartPolicy;
import ru.tischenko.vk.service.strategy.RebalanceStrategy;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CoreService {
    private static final Logger log = LoggerFactory.getLogger(CoreService.class);

    private final UserRepository userRepository;
    private final ProjectRepository projectRepository;
    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TeamRepository teamRepository;
    private final SubTeamRepository subTeamRepository;
    private final NotificationRepository notificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final DomainEventService eventService;
    private final List<TaskStartPolicy> taskStartPolicies;
    private final RebalanceStrategy rebalanceStrategy;

    @Value("${app.business.critical-blocker-threshold:2}")
    private int criticalBlockerThreshold;

    public CoreService(UserRepository userRepository, ProjectRepository projectRepository, SprintRepository sprintRepository, TaskRepository taskRepository, TaskDependencyRepository taskDependencyRepository, TeamRepository teamRepository, SubTeamRepository subTeamRepository, NotificationRepository notificationRepository, PasswordEncoder passwordEncoder, DomainEventService eventService, List<TaskStartPolicy> taskStartPolicies, RebalanceStrategy rebalanceStrategy) {
        this.userRepository = userRepository;
        this.projectRepository = projectRepository;
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.teamRepository = teamRepository;
        this.subTeamRepository = subTeamRepository;
        this.notificationRepository = notificationRepository;
        this.passwordEncoder = passwordEncoder;
        this.eventService = eventService;
        this.taskStartPolicies = taskStartPolicies;
        this.rebalanceStrategy = rebalanceStrategy;
    }

    // ===== USER =====

    @Transactional
    public UserEntity createUser(UserRequest req) {
        UserEntity e = new UserEntity();
        e.setEmail(req.email());
        e.setPasswordHash(passwordEncoder.encode(req.password()));
        e.setRole(req.role());
        e.setLevel(req.level());
        try {
            return userRepository.save(e);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("Email already exists");
        }
    }

    public UserEntity getUser(Long id) { return userRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("User not found")); }
    @Transactional public UserEntity updateUser(Long id, UserRequest req) {
        UserEntity u = getUser(id);
        u.setEmail(req.email());
        u.setRole(req.role());
        u.setLevel(req.level());
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        try {
            return userRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("Email already exists");
        }
    }
    @Transactional public void deleteUser(Long id) { userRepository.delete(getUser(id)); }
    public Page<UserEntity> listUsers(Role role, UserLevel level, Boolean enabled, Pageable pageable) {
        return userRepository.search(role, level, enabled, pageable);
    }

    // ===== PROJECT =====

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectEntity createProject(ProjectRequest req) {
        validateProjectDuration(req.startDate(), req.endDate());
        ProjectEntity e = new ProjectEntity();
        e.setName(req.name());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        e.setStatus(req.status());
        return projectRepository.save(e);
    }

    @Cacheable(value = "projects", key = "#id")
    public ProjectEntity getProject(Long id) { return projectRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Project not found")); }
    @Transactional @CacheEvict(value = "projects", key = "#id")
    public ProjectEntity updateProject(Long id, ProjectRequest req) { validateProjectDuration(req.startDate(), req.endDate()); ProjectEntity p = getProject(id); p.setName(req.name()); p.setStartDate(req.startDate()); p.setEndDate(req.endDate()); p.setStatus(req.status()); return projectRepository.save(p); }
    @Transactional @CacheEvict(value = "projects", key = "#id")
    public void deleteProject(Long id) { projectRepository.delete(getProject(id)); }
    public Page<ProjectEntity> listProjects(ProjectStatus status, LocalDate startDateFrom, LocalDate startDateTo, String nameContains, Pageable pageable) {
        String normalisedName = (nameContains == null || nameContains.isBlank()) ? "" : nameContains.trim();
        return projectRepository.search(status, startDateFrom, startDateTo, normalisedName, pageable);
    }

    // ===== TASK =====

    @Transactional
    public TaskEntity createTask(TaskRequest req) {
        TaskEntity e = new TaskEntity();
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setStatus(req.status());
        e.setPriority(req.priority());
        e.setSprint(sprintRepository.findById(req.sprintId()).orElseThrow(() -> new Exceptions.NotFoundException("Sprint not found")));
        if (req.assigneeId() != null) {
            e.setAssignee(getUser(req.assigneeId()));
        }
        e.setDeadline(req.deadline());
        e.setEstimatedHours(req.estimatedHours() == null ? 1 : req.estimatedHours());
        return taskRepository.save(e);
    }

    public TaskEntity getTask(Long id) { return taskRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Task not found")); }
    @Transactional public TaskEntity updateTask(Long id, TaskRequest req) {
        TaskEntity t = getTask(id);
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setStatus(req.status());
        t.setPriority(req.priority());
        t.setDeadline(req.deadline());
        if (req.estimatedHours() != null) {
            t.setEstimatedHours(req.estimatedHours());
        }
        if (req.assigneeId() != null) { t.setAssignee(getUser(req.assigneeId())); }
        return taskRepository.save(t);
    }
    @Transactional public void deleteTask(Long id) { taskRepository.delete(getTask(id)); }
    public Page<TaskEntity> listTasks(TaskStatus status, Long assigneeId, TaskPriority priority, Pageable pageable) { return taskRepository.search(status, assigneeId, priority, pageable); }

    // ===== TEAM CRUD =====

    @Transactional(readOnly = true)
    public TeamEntity getTeamWithMembers(Long id) {
        TeamEntity team = getTeam(id);
        team.getMembers().size();
        return team;
    }

    @Transactional(readOnly = true)
    public Page<TeamEntity> listTeamsWithMembers(Long projectId, Pageable pageable) {
        Page<TeamEntity> page = listTeams(projectId, pageable);
        page.forEach(t -> t.getMembers().size());
        return page;
    }

    @Transactional(readOnly = true)
    public SubTeamEntity getSubTeamWithMembers(Long id) {
        SubTeamEntity sub = getSubTeam(id);
        sub.getMembers().size();
        return sub;
    }

    @Transactional(readOnly = true)
    public Page<SubTeamEntity> listSubTeamsWithMembers(Long teamId, Pageable pageable) {
        Page<SubTeamEntity> page = listSubTeams(teamId, pageable);
        page.forEach(s -> s.getMembers().size());
        return page;
    }

    @Transactional
    public TeamEntity createTeam(TeamRequest req) {
        TeamEntity team = new TeamEntity();
        team.setProject(getProject(req.projectId()));
        team.setName(req.name());
        try {
            return teamRepository.save(team);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("Team name already exists in project");
        }
    }

    public TeamEntity getTeam(Long id) { return teamRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Team not found")); }

    @Transactional
    public void deleteTeam(Long id) { teamRepository.delete(getTeam(id)); }

    public Page<TeamEntity> listTeams(Long projectId, Pageable pageable) {
        return projectId == null ? teamRepository.findAll(pageable) : teamRepository.findByProjectId(projectId, pageable);
    }

    @Transactional
    public TeamEntity setTeamMembers(Long teamId, List<Long> userIds) {
        TeamEntity team = getTeam(teamId);
        List<UserEntity> users = userRepository.findAllById(userIds);
        if (users.size() != new HashSet<>(userIds).size()) {
            throw new Exceptions.NotFoundException("One or more users not found");
        }
        team.getMembers().clear();
        team.getMembers().addAll(users);
        return team;
    }

    // ===== SUBTEAM CRUD =====

    @Transactional
    public SubTeamEntity createSubTeam(SubTeamRequest req) {
        TeamEntity team = getTeam(req.teamId());
        SubTeamEntity subTeam = new SubTeamEntity();
        subTeam.setTeam(team);
        subTeam.setName(req.name());
        subTeam.setDirection(req.direction());
        try {
            return subTeamRepository.save(subTeam);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("SubTeam name already exists in team");
        }
    }

    public SubTeamEntity getSubTeam(Long id) { return subTeamRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("SubTeam not found")); }

    @Transactional
    public void deleteSubTeam(Long id) { subTeamRepository.delete(getSubTeam(id)); }

    public Page<SubTeamEntity> listSubTeams(Long teamId, Pageable pageable) {
        return teamId == null ? subTeamRepository.findAll(pageable) : subTeamRepository.findByTeamId(teamId, pageable);
    }

    // ===== SPRINT CRUD =====

    @Transactional
    public SprintEntity createSprint(SprintRequest req) {
        validateSprintDuration(req.startDate(), req.endDate());
        SprintEntity s = new SprintEntity();
        s.setProject(getProject(req.projectId()));
        s.setName(req.name());
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        s.setStatus(req.status());
        return sprintRepository.save(s);
    }

    public SprintEntity getSprint(Long id) { return sprintRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Sprint not found")); }

    @Transactional
    public SprintEntity updateSprint(Long id, SprintRequest req) {
        validateSprintDuration(req.startDate(), req.endDate());
        SprintEntity s = getSprint(id);
        s.setName(req.name());
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        s.setStatus(req.status());
        return sprintRepository.save(s);
    }

    @Transactional
    public void deleteSprint(Long id) { sprintRepository.delete(getSprint(id)); }

    public Page<SprintEntity> listSprints(Long projectId, Pageable pageable) {
        return projectId == null ? sprintRepository.findAll(pageable) : sprintRepository.findByProjectId(projectId, pageable);
    }

    // ===== TASK DEPENDENCY CRUD =====

    @Transactional
    public TaskDependencyEntity createTaskDependency(TaskDependencyRequest req) {
        if (req.blockerTaskId().equals(req.blockedTaskId())) {
            throw new Exceptions.BusinessRuleException("Task cannot depend on itself");
        }
        TaskEntity blocker = getTask(req.blockerTaskId());
        TaskEntity blocked = getTask(req.blockedTaskId());
        TaskDependencyEntity dep = new TaskDependencyEntity();
        dep.setBlockerTask(blocker);
        dep.setBlockedTask(blocked);
        try {
            return taskDependencyRepository.save(dep);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("Dependency already exists");
        }
    }

    @Transactional
    public void deleteTaskDependency(Long id) {
        TaskDependencyEntity dep = taskDependencyRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Dependency not found"));
        taskDependencyRepository.delete(dep);
    }

    public Page<TaskDependencyEntity> listTaskDependencies(Pageable pageable) {
        return taskDependencyRepository.findAll(pageable);
    }

    // ===== BUSINESS OPS =====

    @Transactional
    public AssignSubTeamResultLog assignUsersToSubTeam(AssignSubTeamRequest req) {
        SubTeamEntity subTeam = subTeamRepository.findById(req.subTeamId()).orElseThrow(() -> new Exceptions.NotFoundException("SubTeam not found"));
        List<UserEntity> users = userRepository.findAllById(req.userIds());
        if (users.size() != new HashSet<>(req.userIds()).size()) {
            throw new Exceptions.NotFoundException("One or more users not found");
        }
        subTeam.getMembers().clear();
        subTeam.getMembers().addAll(users);
        boolean hasLead = users.stream().anyMatch(u -> u.getLevel() == UserLevel.LEAD);
        Long warningNotificationId = null;
        if (!hasLead && !users.isEmpty()) {
            NotificationEntity n = new NotificationEntity();
            n.setUser(users.get(0));
            n.setType(NotificationType.WARNING);
            n.setText("SubTeam '" + subTeam.getName() + "' has no lead");
            n.setDeliveryStatus(DeliveryStatus.PENDING);
            notificationRepository.save(n);
            warningNotificationId = n.getId();
            eventService.publishNotificationCreated(n.getId());
        }
        log.info("assignUsersToSubTeam subTeamId={} users={} hasLead={}", req.subTeamId(), req.userIds().size(), hasLead);
        return new AssignSubTeamResultLog(hasLead, warningNotificationId);
    }

    public record AssignSubTeamResultLog(boolean hasLead, Long warningNotificationId) {}

    @Transactional(readOnly = true)
    public AnalyzeTeamResponse analyzeTeam(Long teamId) {
        TeamEntity team = teamRepository.findById(teamId).orElseThrow(() -> new Exceptions.NotFoundException("Team not found"));
        Map<UserLevel, Long> grouped = team.getMembers().stream().collect(Collectors.groupingBy(UserEntity::getLevel, Collectors.counting()));
        long juniors = grouped.getOrDefault(UserLevel.JUNIOR, 0L);
        long middles = grouped.getOrDefault(UserLevel.MIDDLE, 0L);
        long leads = grouped.getOrDefault(UserLevel.LEAD, 0L);
        long total = juniors + middles + leads;
        boolean hasLead = leads > 0;

        List<String> issues = new ArrayList<>();
        if (!hasLead) issues.add("no lead");
        if (total == 0) issues.add("team is empty");
        if (total > 0 && juniors > 0 && (middles + leads) == 0) issues.add("only juniors, no senior staff");
        if (total >= 4 && leads == 0) issues.add("team of " + total + " has no lead");
        if (total >= 5 && (middles + leads) * 2 < juniors) issues.add("too many juniors relative to middles/leads");

        String recommendation = issues.isEmpty() ? "Composition looks balanced" : String.join("; ", issues);
        return new AnalyzeTeamResponse(hasLead, juniors, middles, leads, recommendation);
    }

    @Transactional
    public TaskEntity startTask(StartTaskRequest req) {
        TaskEntity task = getTask(req.taskId());
        if (task.getStatus() == TaskStatus.DONE) {
            throw new Exceptions.BusinessRuleException("Done task cannot be moved to IN_PROGRESS");
        }
        for (TaskStartPolicy policy : taskStartPolicies) {
            policy.validate(task);
        }
        task.setStatus(TaskStatus.IN_PROGRESS);
        eventService.publishTaskStarted(task.getId());
        log.info("startTask taskId={} -> IN_PROGRESS", task.getId());
        return taskRepository.save(task);
    }

    public CriticalTaskResponse findCriticalTasks(Long sprintId) {
        getSprint(sprintId);
        List<TaskEntity> tasks = taskRepository.findBySprintId(sprintId);
        Map<Long, Long> blockerCounts = blockerCountsForSprint(sprintId);
        List<Long> critical = tasks.stream()
                .filter(t -> t.getPriority() == TaskPriority.CRITICAL
                        || blockerCounts.getOrDefault(t.getId(), 0L) >= criticalBlockerThreshold)
                .map(TaskEntity::getId)
                .toList();
        return new CriticalTaskResponse(critical);
    }

    @Transactional
    public CriticalEscalationResponse escalateCriticalTasks(Long sprintId) {
        getSprint(sprintId);
        List<TaskEntity> tasks = taskRepository.findBySprintId(sprintId);
        Map<Long, Long> blockerCounts = blockerCountsForSprint(sprintId);
        List<Long> escalated = new ArrayList<>();
        for (TaskEntity t : tasks) {
            long blocks = blockerCounts.getOrDefault(t.getId(), 0L);
            if (blocks >= criticalBlockerThreshold && t.getPriority() != TaskPriority.CRITICAL) {
                t.setPriority(TaskPriority.CRITICAL);
                taskRepository.save(t);
                escalated.add(t.getId());
            }
        }
        log.info("escalateCriticalTasks sprintId={} escalatedCount={}", sprintId, escalated.size());
        return new CriticalEscalationResponse(escalated);
    }

    private Map<Long, Long> blockerCountsForSprint(Long sprintId) {
        return taskDependencyRepository.findBySprintId(sprintId).stream()
                .filter(d -> d.getBlockerTask() != null)
                .collect(Collectors.groupingBy(d -> d.getBlockerTask().getId(), Collectors.counting()));
    }

    @Transactional
    public Map<String, Object> completeSprint(CompleteSprintRequest req) {
        SprintEntity sprint = sprintRepository.findById(req.sprintId()).orElseThrow(() -> new Exceptions.NotFoundException("Sprint not found"));
        if (sprint.getStatus() == SprintStatus.DONE) {
            throw new Exceptions.BusinessRuleException("Sprint is already completed");
        }
        List<TaskEntity> unfinishedTasks = taskRepository.findBySprintIdAndStatusNot(sprint.getId(), TaskStatus.DONE);
        sprint.setStatus(SprintStatus.DONE);
        sprintRepository.save(sprint);

        List<Long> notificationIds = new ArrayList<>();
        for (TaskEntity t : unfinishedTasks) {
            UserEntity recipient = t.getAssignee();
            if (recipient == null) {
                continue;
            }
            NotificationEntity n = new NotificationEntity();
            n.setUser(recipient);
            n.setType(NotificationType.RISK);
            n.setText("Sprint completed with unfinished task: " + t.getTitle());
            n.setDeliveryStatus(DeliveryStatus.PENDING);
            notificationRepository.save(n);
            notificationIds.add(n.getId());
            eventService.publishNotificationCreated(n.getId());
        }
        if (!unfinishedTasks.isEmpty()) {
            eventService.publishRiskDetected("Sprint " + sprint.getId() + " closed with " + unfinishedTasks.size() + " unfinished tasks");
        }
        log.info("completeSprint sprintId={} unfinished={} notifications={}", sprint.getId(), unfinishedTasks.size(), notificationIds.size());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("result", "Sprint completed");
        result.put("sprintId", sprint.getId());
        result.put("unfinishedCount", unfinishedTasks.size());
        result.put("notificationIds", notificationIds);
        return result;
    }

    @Transactional
    public List<Long> bulkRebalance(BulkRebalanceRequest request) {
        SprintEntity sprint = sprintRepository.findById(request.sprintId())
                .orElseThrow(() -> new Exceptions.NotFoundException("Sprint not found"));
        if (sprint.getStatus() != SprintStatus.ACTIVE) {
            throw new Exceptions.BusinessRuleException("Only ACTIVE sprint can be rebalanced");
        }

        List<TaskEntity> tasks = taskRepository.findRebalanceCandidates(request.sprintId());
        List<UserEntity> users = collectProjectMembers(sprint);
        if (users.size() < 2) {
            throw new Exceptions.BusinessRuleException("Need at least 2 team members for rebalance");
        }
        if (tasks.isEmpty()) {
            return List.of();
        }
        List<TaskEntity> rebalanced = rebalanceStrategy.rebalance(tasks, users);
        for (TaskEntity t : rebalanced) {
            taskRepository.save(t);
        }
        eventService.publishRebalanceDone(request.sprintId());
        log.info("bulkRebalance sprintId={} tasks={} users={}", request.sprintId(), rebalanced.size(), users.size());
        return rebalanced.stream().map(TaskEntity::getId).toList();
    }

    private List<UserEntity> collectProjectMembers(SprintEntity sprint) {
        ProjectEntity project = sprint.getProject();
        if (project == null) {
            return List.of();
        }
        Set<UserEntity> uniq = new java.util.LinkedHashSet<>();
        teamRepository.findByProjectId(project.getId(), Pageable.unpaged()).forEach(team -> uniq.addAll(team.getMembers()));
        return new ArrayList<>(uniq);
    }

    @Transactional
    public void scheduleRiskNotifications() {
        List<TaskEntity> overdue = taskRepository.findOverdueTasks(Instant.now());
        int created = 0;
        for (TaskEntity task : overdue) {
            if (task.getAssignee() == null) {
                continue;
            }
            if (notificationRepository.existsByUserIdAndTextAndType(
                    task.getAssignee().getId(),
                    "Task is overdue: " + task.getTitle(),
                    NotificationType.RISK)) {
                continue;
            }
            NotificationEntity n = new NotificationEntity();
            n.setUser(task.getAssignee());
            n.setType(NotificationType.RISK);
            n.setText("Task is overdue: " + task.getTitle());
            n.setDeliveryStatus(DeliveryStatus.PENDING);
            notificationRepository.save(n);
            eventService.publishNotificationCreated(n.getId());
            created++;
        }
        if (created > 0) {
            log.info("scheduleRiskNotifications createdNotifications={}", created);
        }
    }

    private void validateProjectDuration(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new Exceptions.BusinessRuleException("Project endDate cannot be before startDate");
        }
        long durationDaysInclusive = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (durationDaysInclusive > 14) {
            throw new Exceptions.BusinessRuleException("Project duration cannot exceed 14 days");
        }
    }

    private void validateSprintDuration(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new Exceptions.BusinessRuleException("Sprint endDate cannot be before startDate");
        }
        long durationDaysInclusive = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (durationDaysInclusive < 1 || durationDaysInclusive > 3) {
            throw new Exceptions.BusinessRuleException("Sprint duration must be 1..3 days");
        }
    }
}

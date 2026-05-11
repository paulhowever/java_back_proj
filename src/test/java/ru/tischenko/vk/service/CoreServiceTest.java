package ru.tischenko.vk.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import ru.tischenko.vk.api.dto.Dtos.AssignSubTeamRequest;
import ru.tischenko.vk.api.dto.Dtos.BulkRebalanceRequest;
import ru.tischenko.vk.api.dto.Dtos.CompleteSprintRequest;
import ru.tischenko.vk.api.dto.Dtos.ProjectRequest;
import ru.tischenko.vk.api.dto.Dtos.SprintRequest;
import ru.tischenko.vk.api.dto.Dtos.StartTaskRequest;
import ru.tischenko.vk.domain.Enums.NotificationType;
import ru.tischenko.vk.domain.Enums.ProjectStatus;
import ru.tischenko.vk.domain.Enums.SprintStatus;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.Enums.UserLevel;
import ru.tischenko.vk.domain.NotificationEntity;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.domain.SprintEntity;
import ru.tischenko.vk.domain.SubTeamEntity;
import ru.tischenko.vk.domain.TaskDependencyEntity;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.domain.TeamEntity;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.repository.Repositories.*;
import ru.tischenko.vk.service.policy.TaskStartPolicy;
import ru.tischenko.vk.service.strategy.RebalanceStrategy;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class CoreServiceTest {
    @Mock UserRepository userRepository;
    @Mock ProjectRepository projectRepository;
    @Mock SprintRepository sprintRepository;
    @Mock TaskRepository taskRepository;
    @Mock TaskDependencyRepository taskDependencyRepository;
    @Mock TeamRepository teamRepository;
    @Mock SubTeamRepository subTeamRepository;
    @Mock NotificationRepository notificationRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock DomainEventService eventService;
    @Mock RebalanceStrategy rebalanceStrategy;
    @Mock TaskStartPolicy policy;

    private CoreService newService(List<TaskStartPolicy> policies) {
        CoreService svc = new CoreService(
                userRepository, projectRepository, sprintRepository, taskRepository,
                taskDependencyRepository, teamRepository, subTeamRepository, notificationRepository,
                passwordEncoder, eventService, policies, rebalanceStrategy
        );
        ReflectionTestUtils.setField(svc, "criticalBlockerThreshold", 2);
        return svc;
    }

    @Test
    void startTaskShouldCallPolicies() {
        CoreService coreService = newService(List.of(policy));
        TaskEntity task = mock(TaskEntity.class);
        when(task.getId()).thenReturn(1L);
        when(task.getStatus()).thenReturn(TaskStatus.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(task)).thenReturn(task);

        coreService.startTask(new StartTaskRequest(1L));
        verify(policy).validate(task);
    }

    @Test
    void bulkRebalanceShouldFailForNonActiveSprint() {
        CoreService coreService = newService(Collections.emptyList());
        SprintEntity sprint = new SprintEntity();
        sprint.setStatus(SprintStatus.PLANNED);
        when(sprintRepository.findById(2L)).thenReturn(Optional.of(sprint));

        assertThrows(Exceptions.BusinessRuleException.class,
                () -> coreService.bulkRebalance(new BulkRebalanceRequest(2L)));
    }

    @Test
    void createProjectShouldRejectLongDuration() {
        CoreService coreService = newService(Collections.emptyList());
        assertThrows(Exceptions.BusinessRuleException.class, () ->
                coreService.createProject(new ProjectRequest("P",
                        LocalDate.now(), LocalDate.now().plusDays(20), ProjectStatus.ACTIVE)));
    }

    @Test
    void createSprintShouldRejectFourDayDuration() {
        CoreService coreService = newService(Collections.emptyList());
        ProjectEntity project = new ProjectEntity();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));

        assertThrows(Exceptions.BusinessRuleException.class, () ->
                coreService.createSprint(new SprintRequest(
                        1L, "S",
                        LocalDate.of(2026, 5, 1),
                        LocalDate.of(2026, 5, 4),
                        SprintStatus.ACTIVE)));
    }

    @Test
    void createSprintShouldAcceptTwoDayDuration() {
        CoreService coreService = newService(Collections.emptyList());
        ProjectEntity project = new ProjectEntity();
        when(projectRepository.findById(1L)).thenReturn(Optional.of(project));
        when(sprintRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        SprintEntity result = coreService.createSprint(new SprintRequest(
                1L, "S",
                LocalDate.of(2026, 5, 1),
                LocalDate.of(2026, 5, 2),
                SprintStatus.ACTIVE));
        assertNotNull(result);
    }

    @Test
    void assignSubTeamShouldFailWhenNotAllUsersFound() {
        CoreService coreService = newService(Collections.emptyList());
        SubTeamEntity subTeam = new SubTeamEntity();
        when(subTeamRepository.findById(7L)).thenReturn(Optional.of(subTeam));
        when(userRepository.findAllById(List.of(1L, 2L))).thenReturn(List.of());

        assertThrows(Exceptions.NotFoundException.class, () ->
                coreService.assignUsersToSubTeam(new AssignSubTeamRequest(7L, List.of(1L, 2L))));
    }

    @Test
    void analyzeTeamShouldReportNoLead() {
        CoreService coreService = newService(Collections.emptyList());
        TeamEntity team = new TeamEntity();
        UserEntity junior = userOf(1L, UserLevel.JUNIOR);
        UserEntity middle = userOf(2L, UserLevel.MIDDLE);
        team.getMembers().addAll(Set.of(junior, middle));
        when(teamRepository.findById(10L)).thenReturn(Optional.of(team));

        var resp = coreService.analyzeTeam(10L);
        assertFalse(resp.hasLead());
        assertTrue(resp.recommendation().toLowerCase().contains("no lead"));
    }

    @Test
    void analyzeTeamShouldReportImbalanceForJuniorHeavy() {
        CoreService coreService = newService(Collections.emptyList());
        TeamEntity team = new TeamEntity();
        Set<UserEntity> members = new HashSet<>();
        for (long i = 1; i <= 6; i++) members.add(userOf(i, UserLevel.JUNIOR));
        members.add(userOf(99L, UserLevel.LEAD));
        team.getMembers().addAll(members);
        when(teamRepository.findById(11L)).thenReturn(Optional.of(team));

        var resp = coreService.analyzeTeam(11L);
        assertTrue(resp.hasLead());
        assertTrue(resp.recommendation().toLowerCase().contains("juniors"));
    }

    @Test
    void escalateCriticalTasksShouldRaisePriorityForTasksBlockingTwoOrMore() {
        CoreService coreService = newService(Collections.emptyList());
        SprintEntity sprint = new SprintEntity();
        when(sprintRepository.findById(1L)).thenReturn(Optional.of(sprint));

        TaskEntity blocker = new TaskEntity();
        ReflectionTestUtils.setField(blocker, "id", 100L);
        blocker.setPriority(TaskPriority.MEDIUM);

        TaskEntity other = new TaskEntity();
        ReflectionTestUtils.setField(other, "id", 200L);
        other.setPriority(TaskPriority.LOW);

        when(taskRepository.findBySprintId(1L)).thenReturn(List.of(blocker, other));

        TaskDependencyEntity d1 = makeDep(blocker);
        TaskDependencyEntity d2 = makeDep(blocker);
        TaskDependencyEntity d3 = makeDep(other);
        when(taskDependencyRepository.findBySprintId(1L)).thenReturn(List.of(d1, d2, d3));

        var resp = coreService.escalateCriticalTasks(1L);

        assertEquals(List.of(100L), resp.escalatedTaskIds());
        assertEquals(TaskPriority.CRITICAL, blocker.getPriority());
        assertEquals(TaskPriority.LOW, other.getPriority());
        verify(taskRepository).save(blocker);
        verify(taskRepository, never()).save(other);
    }

    @Test
    void completeSprintShouldCreateNotificationsForUnfinishedAssignedTasks() {
        CoreService coreService = newService(Collections.emptyList());
        SprintEntity sprint = new SprintEntity();
        sprint.setStatus(SprintStatus.ACTIVE);
        ReflectionTestUtils.setField(sprint, "id", 5L);
        when(sprintRepository.findById(5L)).thenReturn(Optional.of(sprint));

        UserEntity assignee = userOf(42L, UserLevel.MIDDLE);
        TaskEntity unfinished = new TaskEntity();
        unfinished.setTitle("t1");
        unfinished.setStatus(TaskStatus.IN_PROGRESS);
        unfinished.setAssignee(assignee);

        TaskEntity unassigned = new TaskEntity();
        unassigned.setTitle("t2");
        unassigned.setStatus(TaskStatus.TODO);

        when(taskRepository.findBySprintIdAndStatusNot(5L, TaskStatus.DONE))
                .thenReturn(List.of(unfinished, unassigned));
        when(notificationRepository.save(any())).thenAnswer(inv -> {
            NotificationEntity n = inv.getArgument(0);
            ReflectionTestUtils.setField(n, "id", 1L);
            return n;
        });

        Map<String, Object> result = coreService.completeSprint(new CompleteSprintRequest(5L));

        assertEquals(SprintStatus.DONE, sprint.getStatus());
        assertEquals(2, result.get("unfinishedCount"));
        // только один task имеет assignee → одна notification
        List<?> ids = (List<?>) result.get("notificationIds");
        assertEquals(1, ids.size());
        verify(eventService).publishRiskDetected(any());
    }

    @Test
    void completeSprintShouldRejectAlreadyCompleted() {
        CoreService coreService = newService(Collections.emptyList());
        SprintEntity sprint = new SprintEntity();
        sprint.setStatus(SprintStatus.DONE);
        when(sprintRepository.findById(9L)).thenReturn(Optional.of(sprint));

        assertThrows(Exceptions.BusinessRuleException.class,
                () -> coreService.completeSprint(new CompleteSprintRequest(9L)));
    }

    @Test
    void bulkRebalanceShouldUseProjectTeamMembersNotAllUsers() {
        CoreService coreService = newService(Collections.emptyList());
        ProjectEntity project = new ProjectEntity();
        ReflectionTestUtils.setField(project, "id", 50L);
        SprintEntity sprint = new SprintEntity();
        sprint.setProject(project);
        sprint.setStatus(SprintStatus.ACTIVE);
        when(sprintRepository.findById(7L)).thenReturn(Optional.of(sprint));

        UserEntity u1 = userOf(1L, UserLevel.LEAD);
        UserEntity u2 = userOf(2L, UserLevel.MIDDLE);
        TeamEntity team = new TeamEntity();
        team.getMembers().addAll(Set.of(u1, u2));
        when(teamRepository.findByProjectId(eqLong(50L), any()))
                .thenReturn(new org.springframework.data.domain.PageImpl<>(List.of(team)));

        TaskEntity task = new TaskEntity();
        ReflectionTestUtils.setField(task, "id", 300L);
        task.setEstimatedHours(2);
        when(taskRepository.findRebalanceCandidates(7L)).thenReturn(List.of(task));
        when(rebalanceStrategy.rebalance(any(), any())).thenAnswer(a -> a.getArgument(0));
        when(taskRepository.save(any())).thenAnswer(a -> a.getArgument(0));

        var ids = coreService.bulkRebalance(new BulkRebalanceRequest(7L));
        assertEquals(List.of(300L), ids);
        verify(userRepository, never()).findAll();
    }

    @Test
    void scheduleRiskNotificationsShouldSkipDuplicates() {
        CoreService coreService = newService(Collections.emptyList());
        UserEntity assignee = userOf(8L, UserLevel.MIDDLE);
        TaskEntity overdue = new TaskEntity();
        overdue.setTitle("late");
        overdue.setAssignee(assignee);

        when(taskRepository.findOverdueTasks(any())).thenReturn(List.of(overdue));
        when(notificationRepository.existsByUserIdAndTextAndType(
                eqLong(8L), any(), eqType(NotificationType.RISK))).thenReturn(true);

        coreService.scheduleRiskNotifications();
        verify(notificationRepository, never()).save(any());
    }

    private static UserEntity userOf(long id, UserLevel level) {
        UserEntity u = new UserEntity();
        ReflectionTestUtils.setField(u, "id", id);
        u.setLevel(level);
        u.setEmail("u" + id + "@local");
        return u;
    }

    private static TaskDependencyEntity makeDep(TaskEntity blocker) {
        TaskDependencyEntity d = new TaskDependencyEntity();
        d.setBlockerTask(blocker);
        return d;
    }

    private static Long eqLong(long v) { return org.mockito.ArgumentMatchers.eq(v); }
    private static NotificationType eqType(NotificationType t) { return org.mockito.ArgumentMatchers.eq(t); }
}

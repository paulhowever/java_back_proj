package ru.tischenko.vk.service.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.BulkRebalanceRequest;
import ru.tischenko.vk.api.dto.Dtos.CompleteSprintRequest;
import ru.tischenko.vk.domain.Enums.DeliveryStatus;
import ru.tischenko.vk.domain.Enums.NotificationType;
import ru.tischenko.vk.domain.Enums.SprintStatus;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.NotificationEntity;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.domain.SprintEntity;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.repository.NotificationRepository;
import ru.tischenko.vk.repository.SprintRepository;
import ru.tischenko.vk.repository.TaskRepository;
import ru.tischenko.vk.repository.TeamRepository;
import ru.tischenko.vk.service.DomainEventService;
import ru.tischenko.vk.service.Exceptions;
import ru.tischenko.vk.service.strategy.RebalanceStrategy;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class SprintOperationsService {
    private static final Logger log = LoggerFactory.getLogger(SprintOperationsService.class);

    private final SprintRepository sprintRepository;
    private final TaskRepository taskRepository;
    private final NotificationRepository notificationRepository;
    private final TeamRepository teamRepository;
    private final DomainEventService eventService;
    private final RebalanceStrategy rebalanceStrategy;

    public SprintOperationsService(SprintRepository sprintRepository,
                                   TaskRepository taskRepository,
                                   NotificationRepository notificationRepository,
                                   TeamRepository teamRepository,
                                   DomainEventService eventService,
                                   RebalanceStrategy rebalanceStrategy) {
        this.sprintRepository = sprintRepository;
        this.taskRepository = taskRepository;
        this.notificationRepository = notificationRepository;
        this.teamRepository = teamRepository;
        this.eventService = eventService;
        this.rebalanceStrategy = rebalanceStrategy;
    }

    @Transactional
    public Map<String, Object> completeSprint(CompleteSprintRequest req) {
        SprintEntity sprint = sprintRepository.findById(req.sprintId())
                .orElseThrow(() -> new Exceptions.NotFoundException("Sprint not found"));
        if (sprint.getStatus() == SprintStatus.DONE) {
            throw new Exceptions.BusinessRuleException("Sprint is already completed");
        }
        List<TaskEntity> unfinishedTasks = taskRepository.findBySprintIdAndStatusNot(sprint.getId(), TaskStatus.DONE);
        sprint.setStatus(SprintStatus.DONE);
        sprintRepository.save(sprint);

        List<Long> notificationIds = new ArrayList<>();
        int unassignedSkipped = 0;
        for (TaskEntity t : unfinishedTasks) {
            UserEntity recipient = t.getAssignee();
            if (recipient == null) {
                unassignedSkipped++;
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
        log.info("completeSprint sprintId={} unfinished={} notifications={} unassignedSkipped={}",
                sprint.getId(), unfinishedTasks.size(), notificationIds.size(), unassignedSkipped);

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
        taskRepository.saveAll(rebalanced);
        eventService.publishRebalanceDone(request.sprintId());
        log.info("bulkRebalance sprintId={} tasks={} users={}", request.sprintId(), rebalanced.size(), users.size());
        return rebalanced.stream().map(TaskEntity::getId).toList();
    }

    private List<UserEntity> collectProjectMembers(SprintEntity sprint) {
        ProjectEntity project = sprint.getProject();
        if (project == null) {
            return List.of();
        }
        Set<UserEntity> uniq = new LinkedHashSet<>();
        teamRepository.findByProjectId(project.getId(), Pageable.unpaged()).forEach(team -> uniq.addAll(team.getMembers()));
        return new ArrayList<>(uniq);
    }
}

package ru.tischenko.vk.service.ops;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.CriticalEscalationResponse;
import ru.tischenko.vk.api.dto.Dtos.CriticalTaskResponse;
import ru.tischenko.vk.api.dto.Dtos.StartTaskRequest;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.repository.TaskDependencyRepository;
import ru.tischenko.vk.repository.TaskRepository;
import ru.tischenko.vk.service.DomainEventService;
import ru.tischenko.vk.service.Exceptions;
import ru.tischenko.vk.service.SprintService;
import ru.tischenko.vk.service.TaskService;
import ru.tischenko.vk.service.policy.TaskStartPolicy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TaskOperationsService {
    private static final Logger log = LoggerFactory.getLogger(TaskOperationsService.class);

    private final TaskRepository taskRepository;
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskService taskService;
    private final SprintService sprintService;
    private final List<TaskStartPolicy> taskStartPolicies;
    private final DomainEventService eventService;

    @Value("${app.business.critical-blocker-threshold:2}")
    private int criticalBlockerThreshold;

    public TaskOperationsService(TaskRepository taskRepository,
                                 TaskDependencyRepository taskDependencyRepository,
                                 TaskService taskService,
                                 SprintService sprintService,
                                 List<TaskStartPolicy> taskStartPolicies,
                                 DomainEventService eventService) {
        this.taskRepository = taskRepository;
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskService = taskService;
        this.sprintService = sprintService;
        this.taskStartPolicies = taskStartPolicies;
        this.eventService = eventService;
    }

    @Transactional
    public TaskEntity startTask(StartTaskRequest req) {
        TaskEntity task = taskService.getTask(req.taskId());
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

    @Transactional(readOnly = true)
    public CriticalTaskResponse findCriticalTasks(Long sprintId) {
        sprintService.getSprint(sprintId);
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
        sprintService.getSprint(sprintId);
        List<TaskEntity> tasks = taskRepository.findBySprintId(sprintId);
        Map<Long, Long> blockerCounts = blockerCountsForSprint(sprintId);
        List<Long> escalated = new ArrayList<>();
        for (TaskEntity t : tasks) {
            long blocks = blockerCounts.getOrDefault(t.getId(), 0L);
            if (blocks >= criticalBlockerThreshold && t.getPriority() != TaskPriority.CRITICAL) {
                t.setPriority(TaskPriority.CRITICAL);
                // t is a managed entity from findBySprintId — Hibernate dirty-checking
                // flushes the priority change on commit, no explicit save needed.
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
}

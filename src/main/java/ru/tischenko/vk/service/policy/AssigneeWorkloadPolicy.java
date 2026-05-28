package ru.tischenko.vk.service.policy;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.repository.TaskRepository;
import ru.tischenko.vk.service.Exceptions;

import java.util.List;

@Component
public class AssigneeWorkloadPolicy implements TaskStartPolicy {
    private final TaskRepository taskRepository;
    private final long maxActiveTasks;

    public AssigneeWorkloadPolicy(TaskRepository taskRepository,
                                  @Value("${app.business.max-active-tasks-per-user:5}") long maxActiveTasks) {
        this.taskRepository = taskRepository;
        this.maxActiveTasks = maxActiveTasks;
    }

    @Override
    public void validate(TaskEntity task) {
        if (task.getAssignee() == null) {
            return;
        }
        long activeTasks = taskRepository.countByAssigneeIdAndStatusIn(
                task.getAssignee().getId(),
                List.of(TaskStatus.TODO, TaskStatus.IN_PROGRESS)
        );
        if (activeTasks >= maxActiveTasks) {
            throw new Exceptions.BusinessRuleException("Assignee is overloaded (active=" + activeTasks + ", limit=" + maxActiveTasks + ")");
        }
    }
}

package ru.tischenko.vk.service.policy;

import org.springframework.stereotype.Component;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.repository.TaskDependencyRepository;
import ru.tischenko.vk.service.Exceptions;

@Component
public class DependencyCompletionPolicy implements TaskStartPolicy {
    private final TaskDependencyRepository dependencyRepository;

    public DependencyCompletionPolicy(TaskDependencyRepository dependencyRepository) {
        this.dependencyRepository = dependencyRepository;
    }

    @Override
    public void validate(TaskEntity task) {
        boolean blocked = dependencyRepository.findByBlockedTaskId(task.getId()).stream()
                .anyMatch(d -> d.getBlockerTask().getStatus() != TaskStatus.DONE);
        if (blocked) {
            throw new Exceptions.BusinessRuleException("Dependencies are not done");
        }
    }
}

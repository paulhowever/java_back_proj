package ru.tischenko.vk.service.policy;

import ru.tischenko.vk.domain.TaskEntity;

public interface TaskStartPolicy {
    void validate(TaskEntity task);
}

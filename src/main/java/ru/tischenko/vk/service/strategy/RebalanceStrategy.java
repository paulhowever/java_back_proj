package ru.tischenko.vk.service.strategy;

import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.domain.UserEntity;

import java.util.List;

public interface RebalanceStrategy {
    List<TaskEntity> rebalance(List<TaskEntity> tasks, List<UserEntity> users);
}

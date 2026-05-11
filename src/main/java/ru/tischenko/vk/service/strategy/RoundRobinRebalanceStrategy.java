package ru.tischenko.vk.service.strategy;

import org.springframework.stereotype.Component;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.domain.UserEntity;

import java.util.Comparator;
import java.util.List;

@Component
public class RoundRobinRebalanceStrategy implements RebalanceStrategy {
    @Override
    public List<TaskEntity> rebalance(List<TaskEntity> tasks, List<UserEntity> users) {
        if (users.isEmpty()) {
            return tasks;
        }
        List<TaskEntity> sorted = tasks.stream()
                .sorted(Comparator.comparing(TaskEntity::getEstimatedHours).reversed())
                .toList();
        for (int i = 0; i < sorted.size(); i++) {
            sorted.get(i).setAssignee(users.get(i % users.size()));
        }
        return sorted;
    }
}

package ru.tischenko.vk.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.TaskDependencyRequest;
import ru.tischenko.vk.domain.TaskDependencyEntity;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.repository.TaskDependencyRepository;

@Service
public class TaskDependencyService {
    private final TaskDependencyRepository taskDependencyRepository;
    private final TaskService taskService;

    public TaskDependencyService(TaskDependencyRepository taskDependencyRepository, TaskService taskService) {
        this.taskDependencyRepository = taskDependencyRepository;
        this.taskService = taskService;
    }

    @Transactional
    public TaskDependencyEntity createTaskDependency(TaskDependencyRequest req) {
        if (req.blockerTaskId().equals(req.blockedTaskId())) {
            throw new Exceptions.BusinessRuleException("Task cannot depend on itself");
        }
        TaskEntity blocker = taskService.getTask(req.blockerTaskId());
        TaskEntity blocked = taskService.getTask(req.blockedTaskId());
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
        TaskDependencyEntity dep = taskDependencyRepository.findById(id)
                .orElseThrow(() -> new Exceptions.NotFoundException("Dependency not found"));
        taskDependencyRepository.delete(dep);
    }

    public Page<TaskDependencyEntity> listTaskDependencies(Pageable pageable) {
        return taskDependencyRepository.findAll(pageable);
    }
}

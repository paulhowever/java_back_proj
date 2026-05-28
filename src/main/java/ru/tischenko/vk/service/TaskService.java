package ru.tischenko.vk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.TaskRequest;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.repository.TaskRepository;

@Service
public class TaskService {
    private final TaskRepository taskRepository;
    private final SprintService sprintService;
    private final UserService userService;

    public TaskService(TaskRepository taskRepository, SprintService sprintService, UserService userService) {
        this.taskRepository = taskRepository;
        this.sprintService = sprintService;
        this.userService = userService;
    }

    @Transactional
    public TaskEntity createTask(TaskRequest req) {
        TaskEntity e = new TaskEntity();
        e.setTitle(req.title());
        e.setDescription(req.description());
        e.setStatus(req.status());
        e.setPriority(req.priority());
        e.setSprint(sprintService.getSprint(req.sprintId()));
        if (req.assigneeId() != null) {
            e.setAssignee(userService.getUser(req.assigneeId()));
        }
        e.setDeadline(req.deadline());
        e.setEstimatedHours(req.estimatedHours() == null ? 1 : req.estimatedHours());
        return taskRepository.save(e);
    }

    public TaskEntity getTask(Long id) {
        return taskRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Task not found"));
    }

    @Transactional
    public TaskEntity updateTask(Long id, TaskRequest req) {
        TaskEntity t = getTask(id);
        t.setTitle(req.title());
        t.setDescription(req.description());
        t.setStatus(req.status());
        t.setPriority(req.priority());
        t.setDeadline(req.deadline());
        if (req.estimatedHours() != null) {
            t.setEstimatedHours(req.estimatedHours());
        }
        if (req.assigneeId() != null) {
            t.setAssignee(userService.getUser(req.assigneeId()));
        }
        return taskRepository.save(t);
    }

    @Transactional
    public void deleteTask(Long id) {
        taskRepository.delete(getTask(id));
    }

    public Page<TaskEntity> listTasks(TaskStatus status, Long assigneeId, TaskPriority priority, Pageable pageable) {
        return taskRepository.search(status, assigneeId, priority, pageable);
    }
}

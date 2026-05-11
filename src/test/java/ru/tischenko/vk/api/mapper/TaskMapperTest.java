package ru.tischenko.vk.api.mapper;

import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;
import ru.tischenko.vk.api.mapper.ApiMappers.TaskMapper;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.SprintEntity;
import ru.tischenko.vk.domain.TaskEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TaskMapperTest {
    private final TaskMapper mapper = Mappers.getMapper(TaskMapper.class);

    @Test
    void shouldMapTask() {
        SprintEntity sprint = new SprintEntity();
        TaskEntity task = new TaskEntity();
        task.setSprint(sprint);
        task.setTitle("T");
        task.setPriority(TaskPriority.HIGH);
        task.setStatus(TaskStatus.TODO);

        var dto = mapper.toResponse(task);
        assertEquals("T", dto.title());
    }
}

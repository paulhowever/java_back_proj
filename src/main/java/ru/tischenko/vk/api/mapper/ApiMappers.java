package ru.tischenko.vk.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import ru.tischenko.vk.api.dto.Dtos.*;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.domain.UserEntity;

@Mapper(componentModel = "spring")
public interface ApiMappers {
    @Mapper(componentModel = "spring")
    interface UserMapper {
        UserResponse toResponse(UserEntity entity);
    }

    @Mapper(componentModel = "spring")
    interface ProjectMapper {
        ProjectResponse toResponse(ProjectEntity entity);
    }

    @Mapper(componentModel = "spring")
    interface TaskMapper {
        @Mapping(target = "sprintId", source = "sprint.id")
        @Mapping(target = "assigneeId", source = "assignee.id")
        TaskResponse toResponse(TaskEntity entity);
    }
}

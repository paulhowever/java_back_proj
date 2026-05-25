package ru.tischenko.vk.api.mapper;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import ru.tischenko.vk.api.dto.Dtos.*;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.domain.SprintEntity;
import ru.tischenko.vk.domain.SubTeamEntity;
import ru.tischenko.vk.domain.TaskDependencyEntity;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.domain.TeamEntity;
import ru.tischenko.vk.domain.UserEntity;

import java.util.List;
import java.util.Set;

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

    @Mapper(componentModel = "spring")
    interface TeamMapper {
        @Mapping(target = "projectId", source = "project.id")
        @Mapping(target = "memberIds", source = "members", qualifiedByName = "userIds")
        TeamResponse toResponse(TeamEntity entity);

        @Named("userIds")
        default List<Long> userIds(Set<UserEntity> users) {
            return users == null ? List.of() : users.stream().map(UserEntity::getId).toList();
        }
    }

    @Mapper(componentModel = "spring")
    interface SubTeamMapper {
        @Mapping(target = "teamId", source = "team.id")
        @Mapping(target = "memberIds", source = "members", qualifiedByName = "userIds")
        SubTeamResponse toResponse(SubTeamEntity entity);

        @Named("userIds")
        default List<Long> userIds(Set<UserEntity> users) {
            return users == null ? List.of() : users.stream().map(UserEntity::getId).toList();
        }
    }

    @Mapper(componentModel = "spring")
    interface SprintMapper {
        @Mapping(target = "projectId", source = "project.id")
        SprintResponse toResponse(SprintEntity entity);
    }

    @Mapper(componentModel = "spring")
    interface TaskDependencyMapper {
        @Mapping(target = "blockerTaskId", source = "blockerTask.id")
        @Mapping(target = "blockedTaskId", source = "blockedTask.id")
        TaskDependencyResponse toResponse(TaskDependencyEntity entity);
    }
}

package ru.tischenko.vk.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.tischenko.vk.api.dto.Dtos.ProjectResponse;
import ru.tischenko.vk.api.dto.Dtos.UserResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.ProjectMapper;
import ru.tischenko.vk.api.mapper.ApiMappers.UserMapper;
import ru.tischenko.vk.domain.Enums;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.service.ProjectService;
import ru.tischenko.vk.service.UserService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class ListEndpointFilterContractTest {

    @Test
    void listUsersShouldSupportRoleLevelEnabledFilters() {
        UserService userService = mock(UserService.class);
        UserMapper userMapper = mock(UserMapper.class);
        UserController controller = new UserController(userService, userMapper);

        UserEntity entity = new UserEntity();
        when(userService.listUsers(eq(Enums.Role.ADMIN), eq(Enums.UserLevel.LEAD), eq(true), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(entity), PageRequest.of(0, 10), 1));
        when(userMapper.toResponse(entity)).thenReturn(new UserResponse(1L, "a@a", Enums.Role.ADMIN, Enums.UserLevel.LEAD));

        var page = controller.listUsers(Enums.Role.ADMIN, Enums.UserLevel.LEAD, true, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void listProjectsShouldSupportDateAndNameFilters() {
        ProjectService projectService = mock(ProjectService.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectController controller = new ProjectController(projectService, projectMapper);

        ProjectEntity entity = new ProjectEntity();
        when(projectService.listProjects(eq(Enums.ProjectStatus.ACTIVE), eq(LocalDate.parse("2026-01-01")), eq(LocalDate.parse("2026-01-31")), eq("team"), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(entity), PageRequest.of(0, 10), 1));
        when(projectMapper.toResponse(entity)).thenReturn(new ProjectResponse(1L, "Team", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-14"), Enums.ProjectStatus.ACTIVE));

        var page = controller.listProjects(Enums.ProjectStatus.ACTIVE, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "team", PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }
}

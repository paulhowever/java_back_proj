package ru.tischenko.vk.api;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import ru.tischenko.vk.api.dto.Dtos.ProjectResponse;
import ru.tischenko.vk.api.dto.Dtos.UserResponse;
import ru.tischenko.vk.api.mapper.ApiMappers.ProjectMapper;
import ru.tischenko.vk.api.mapper.ApiMappers.TaskMapper;
import ru.tischenko.vk.api.mapper.ApiMappers.UserMapper;
import ru.tischenko.vk.domain.Enums;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.service.CoreService;
import ru.tischenko.vk.service.IdempotencyService;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class CoreControllerFilterContractTest {

    @Test
    void listUsersShouldSupportRoleLevelEnabledFilters() {
        CoreService service = mock(CoreService.class);
        UserMapper userMapper = mock(UserMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        TaskMapper taskMapper = mock(TaskMapper.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        CoreController controller = new CoreController(service, userMapper, projectMapper, taskMapper, idempotencyService);

        UserEntity entity = new UserEntity();
        when(service.listUsers(eq(Enums.Role.ADMIN), eq(Enums.UserLevel.LEAD), eq(true), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(entity), PageRequest.of(0, 10), 1));
        when(userMapper.toResponse(entity)).thenReturn(new UserResponse(1L, "a@a", Enums.Role.ADMIN, Enums.UserLevel.LEAD));

        var page = controller.listUsers(Enums.Role.ADMIN, Enums.UserLevel.LEAD, true, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }

    @Test
    void listProjectsShouldSupportDateAndNameFilters() {
        CoreService service = mock(CoreService.class);
        UserMapper userMapper = mock(UserMapper.class);
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        TaskMapper taskMapper = mock(TaskMapper.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        CoreController controller = new CoreController(service, userMapper, projectMapper, taskMapper, idempotencyService);

        ProjectEntity entity = new ProjectEntity();
        when(service.listProjects(eq(Enums.ProjectStatus.ACTIVE), eq(LocalDate.parse("2026-01-01")), eq(LocalDate.parse("2026-01-31")), eq("team"), any()))
                .thenReturn(new PageImpl<>(java.util.List.of(entity), PageRequest.of(0, 10), 1));
        when(projectMapper.toResponse(entity)).thenReturn(new ProjectResponse(1L, "Team", LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-14"), Enums.ProjectStatus.ACTIVE));

        var page = controller.listProjects(Enums.ProjectStatus.ACTIVE, LocalDate.parse("2026-01-01"), LocalDate.parse("2026-01-31"), "team", PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }
}

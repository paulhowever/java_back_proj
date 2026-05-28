package ru.tischenko.vk.api;

import org.junit.jupiter.api.Test;
import ru.tischenko.vk.api.dto.Dtos.BulkRebalanceRequest;
import ru.tischenko.vk.api.mapper.ApiMappers.ProjectMapper;
import ru.tischenko.vk.api.mapper.ApiMappers.TaskMapper;
import ru.tischenko.vk.api.mapper.ApiMappers.UserMapper;
import ru.tischenko.vk.domain.IdempotencyRecordEntity;
import ru.tischenko.vk.service.IdempotencyService;
import ru.tischenko.vk.service.ProjectService;
import ru.tischenko.vk.service.SprintService;
import ru.tischenko.vk.service.SubTeamService;
import ru.tischenko.vk.service.TaskDependencyService;
import ru.tischenko.vk.service.TaskService;
import ru.tischenko.vk.service.TeamService;
import ru.tischenko.vk.service.UserService;
import ru.tischenko.vk.service.ops.SprintOperationsService;
import ru.tischenko.vk.service.ops.TaskOperationsService;
import ru.tischenko.vk.service.ops.TeamOperationsService;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class IdempotencyContractTest {

    private static CoreController buildController(SprintOperationsService sprintOps, IdempotencyService idempotencyService) {
        return new CoreController(
                mock(UserService.class),
                mock(ProjectService.class),
                mock(SprintService.class),
                mock(TaskService.class),
                mock(TeamService.class),
                mock(SubTeamService.class),
                mock(TaskDependencyService.class),
                mock(TeamOperationsService.class),
                mock(TaskOperationsService.class),
                sprintOps,
                mock(UserMapper.class),
                mock(ProjectMapper.class),
                mock(TaskMapper.class),
                idempotencyService
        );
    }

    @Test
    void shouldReturnUnifiedReplayResponseWhenKeyAlreadyExists() {
        SprintOperationsService sprintOps = mock(SprintOperationsService.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        CoreController controller = buildController(sprintOps, idempotencyService);

        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey("key-1");
        record.setOperation("bulkRebalance");
        record.setResponseCode(200);
        record.setResponseBody("[1,2,3]");
        when(idempotencyService.reserveOrGetExisting("key-1", "bulkRebalance")).thenReturn(Optional.of(record));

        var response = controller.bulkRebalance(new BulkRebalanceRequest(1L), "key-1");

        assertTrue(response.replayed());
        assertEquals("key-1", response.idempotencyKey());
        assertEquals("[1,2,3]", response.payload());
        assertEquals("bulkRebalance", response.operation());
        verify(sprintOps, never()).bulkRebalance(any());
    }

    @Test
    void shouldReturnUnifiedFreshResponseWhenKeyIsNew() {
        SprintOperationsService sprintOps = mock(SprintOperationsService.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        CoreController controller = buildController(sprintOps, idempotencyService);

        when(idempotencyService.reserveOrGetExisting("key-2", "bulkRebalance")).thenReturn(Optional.empty());
        when(sprintOps.bulkRebalance(any())).thenReturn(List.of(5L, 6L));

        var response = controller.bulkRebalance(new BulkRebalanceRequest(2L), "key-2");

        assertFalse(response.replayed());
        assertEquals("key-2", response.idempotencyKey());
        assertEquals(List.of(5L, 6L), response.payload());
        assertEquals("bulkRebalance", response.operation());
        verify(idempotencyService).markCompleted(eq("key-2"), eq(200), eq("[5, 6]"));
    }

    @Test
    void shouldReturn409WhenExistingReservationIsStillProcessing() {
        SprintOperationsService sprintOps = mock(SprintOperationsService.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        CoreController controller = buildController(sprintOps, idempotencyService);

        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey("key-x");
        record.setOperation("bulkRebalance");
        record.setResponseCode(102);
        record.setResponseBody("PROCESSING");
        when(idempotencyService.reserveOrGetExisting("key-x", "bulkRebalance")).thenReturn(Optional.of(record));

        assertThrows(ru.tischenko.vk.service.Exceptions.ConflictException.class,
                () -> controller.bulkRebalance(new BulkRebalanceRequest(1L), "key-x"));
        verify(sprintOps, never()).bulkRebalance(any());
    }

    @Test
    void shouldReleaseReservationWhenBusinessOperationFails() {
        SprintOperationsService sprintOps = mock(SprintOperationsService.class);
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        CoreController controller = buildController(sprintOps, idempotencyService);

        when(idempotencyService.reserveOrGetExisting("key-3", "bulkRebalance")).thenReturn(Optional.empty());
        when(sprintOps.bulkRebalance(any())).thenThrow(new RuntimeException("boom"));

        assertThrows(RuntimeException.class, () -> controller.bulkRebalance(new BulkRebalanceRequest(3L), "key-3"));
        verify(idempotencyService).releaseReservation("key-3");
    }
}

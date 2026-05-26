package ru.tischenko.vk.service;

import org.junit.jupiter.api.Test;
import ru.tischenko.vk.domain.IdempotencyRecordEntity;

import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class IdempotentOperationExecutorTest {

    @Test
    void shouldReturnUnifiedReplayResponseWhenKeyAlreadyExists() {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        IdempotentOperationExecutor executor = new IdempotentOperationExecutor(idempotencyService);

        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey("key-1");
        record.setOperation("bulkRebalance");
        record.setResponseCode(200);
        record.setResponseBody("[1,2,3]");
        when(idempotencyService.reserveOrGetExisting("key-1", "bulkRebalance")).thenReturn(Optional.of(record));

        Supplier<List<Long>> work = mock(Supplier.class);
        var response = executor.execute("key-1", "bulkRebalance", work);

        assertTrue(response.replayed());
        assertEquals("key-1", response.idempotencyKey());
        assertEquals("[1,2,3]", response.payload());
        assertEquals("bulkRebalance", response.operation());
        verify(work, never()).get();
    }

    @Test
    void shouldReturnUnifiedFreshResponseWhenKeyIsNew() {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        IdempotentOperationExecutor executor = new IdempotentOperationExecutor(idempotencyService);

        when(idempotencyService.reserveOrGetExisting("key-2", "bulkRebalance")).thenReturn(Optional.empty());

        var response = executor.execute("key-2", "bulkRebalance", () -> List.of(5L, 6L));

        assertFalse(response.replayed());
        assertEquals("key-2", response.idempotencyKey());
        assertEquals(List.of(5L, 6L), response.payload());
        assertEquals("bulkRebalance", response.operation());
        verify(idempotencyService).markCompleted(eq("key-2"), eq(200), eq("[5, 6]"));
    }

    @Test
    void shouldReturnConflictWhenExistingReservationIsStillProcessing() {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        IdempotentOperationExecutor executor = new IdempotentOperationExecutor(idempotencyService);

        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey("key-x");
        record.setOperation("bulkRebalance");
        record.setResponseCode(IdempotencyService.RESPONSE_CODE_PROCESSING);
        record.setResponseBody("PROCESSING");
        when(idempotencyService.reserveOrGetExisting("key-x", "bulkRebalance")).thenReturn(Optional.of(record));

        Supplier<List<Long>> work = mock(Supplier.class);
        assertThrows(Exceptions.ConflictException.class,
                () -> executor.execute("key-x", "bulkRebalance", work));
        verify(work, never()).get();
    }

    @Test
    void shouldReleaseReservationWhenBusinessOperationFails() {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        IdempotentOperationExecutor executor = new IdempotentOperationExecutor(idempotencyService);

        when(idempotencyService.reserveOrGetExisting("key-3", "bulkRebalance")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () ->
                executor.execute("key-3", "bulkRebalance", () -> {
                    throw new RuntimeException("boom");
                }));
        verify(idempotencyService).releaseReservation("key-3");
    }

    @Test
    void shouldExecuteWorkWithoutIdempotencyWhenKeyIsNull() {
        IdempotencyService idempotencyService = mock(IdempotencyService.class);
        IdempotentOperationExecutor executor = new IdempotentOperationExecutor(idempotencyService);

        var response = executor.execute(null, "bulkRebalance", () -> List.of(1L));

        assertFalse(response.replayed());
        assertEquals("n/a", response.idempotencyKey());
        assertEquals(List.of(1L), response.payload());
        verifyNoInteractions(idempotencyService);
    }
}

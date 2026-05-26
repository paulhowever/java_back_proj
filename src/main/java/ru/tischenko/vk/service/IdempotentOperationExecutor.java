package ru.tischenko.vk.service;

import org.springframework.stereotype.Service;
import ru.tischenko.vk.api.dto.Dtos.IdempotentResultResponse;

import java.util.function.Supplier;

/**
 * Wraps a business call with the Idempotency-Key contract:
 *  - no key  -> just executes the work and returns a "fresh" envelope (key = "n/a").
 *  - new key -> reserves a record, executes the work, marks the record completed.
 *  - replayed key with finished record -> returns cached body, marks replayed=true.
 *  - replayed key still in-flight      -> 409.
 *  - work throws                       -> releases the reservation, rethrows.
 *
 * Lives in the service layer (not the controller) so HTTP code does not own
 * transactional/idempotency mechanics.
 */
@Service
public class IdempotentOperationExecutor {
    private final IdempotencyService idempotencyService;

    public IdempotentOperationExecutor(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    public <T> IdempotentResultResponse execute(String idempotencyKey, String operation, Supplier<T> work) {
        if (idempotencyKey == null) {
            T result = work.get();
            return new IdempotentResultResponse(operation, "n/a", false, result);
        }

        var existing = idempotencyService.reserveOrGetExisting(idempotencyKey, operation);
        if (existing.isPresent()) {
            if (existing.get().getResponseCode() == IdempotencyService.RESPONSE_CODE_PROCESSING) {
                throw new Exceptions.ConflictException("Idempotent operation is already in progress");
            }
            return new IdempotentResultResponse(
                    existing.get().getOperation(),
                    idempotencyKey,
                    true,
                    existing.get().getResponseBody()
            );
        }

        try {
            T result = work.get();
            idempotencyService.markCompleted(idempotencyKey, 200, String.valueOf(result));
            return new IdempotentResultResponse(operation, idempotencyKey, false, result);
        } catch (RuntimeException ex) {
            idempotencyService.releaseReservation(idempotencyKey);
            throw ex;
        }
    }
}

package ru.tischenko.vk.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.domain.IdempotencyRecordEntity;
import ru.tischenko.vk.repository.Repositories.IdempotencyRecordRepository;

import java.util.Optional;

@Service
public class IdempotencyService {
    /**
     * Marker stored in {@code response_code} while the operation is in-flight.
     * 102 mirrors the HTTP "Processing" semantics — convention only, never returned to clients.
     */
    public static final int RESPONSE_CODE_PROCESSING = 102;

    private final IdempotencyRecordRepository repository;

    public IdempotencyService(IdempotencyRecordRepository repository) {
        this.repository = repository;
    }

    public Optional<IdempotencyRecordEntity> find(String key) {
        return repository.findByIdempotencyKey(key);
    }

    /**
     * Try to reserve a record for the given idempotency key in a *separate* transaction.
     * - Returns Optional.empty()   -> reservation acquired, caller may proceed.
     * - Returns Optional.of(rec)   -> key already exists; caller must replay or report 409.
     *
     * REQUIRES_NEW isolates the unique-violation roll-back from the caller's transaction —
     * otherwise the outer Hibernate session would be marked rollback-only and commit would fail.
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public Optional<IdempotencyRecordEntity> reserveOrGetExisting(String key, String operation) {
        Optional<IdempotencyRecordEntity> existing = repository.findByIdempotencyKey(key);
        if (existing.isPresent()) {
            return existing;
        }
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey(key);
        record.setOperation(operation);
        record.setResponseCode(RESPONSE_CODE_PROCESSING);
        record.setResponseBody("PROCESSING");
        try {
            repository.saveAndFlush(record);
            return Optional.empty();
        } catch (DataIntegrityViolationException ex) {
            return repository.findByIdempotencyKey(key);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(String key, String operation, int code, String body) {
        IdempotencyRecordEntity record = new IdempotencyRecordEntity();
        record.setIdempotencyKey(key);
        record.setOperation(operation);
        record.setResponseCode(code);
        record.setResponseBody(body);
        repository.save(record);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void markCompleted(String key, int code, String body) {
        IdempotencyRecordEntity record = repository.findByIdempotencyKey(key)
                .orElseThrow(() -> new IllegalStateException("Idempotency reservation is missing"));
        record.setResponseCode(code);
        record.setResponseBody(body);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void releaseReservation(String key) {
        repository.findByIdempotencyKey(key).ifPresent(repository::delete);
    }
}

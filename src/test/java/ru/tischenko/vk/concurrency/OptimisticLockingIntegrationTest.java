package ru.tischenko.vk.concurrency;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import ru.tischenko.vk.domain.*;
import ru.tischenko.vk.domain.Enums.*;
import ru.tischenko.vk.repository.SprintRepository;
import ru.tischenko.vk.repository.TaskRepository;
import ru.tischenko.vk.repository.ProjectRepository;

import java.time.LocalDate;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class OptimisticLockingIntegrationTest {
    @Autowired TaskRepository taskRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired SprintRepository sprintRepository;
    @Autowired PlatformTransactionManager txManager;

    /**
     * Two threads load the same task row, synchronise on a CyclicBarrier so both
     * have an in-memory copy with the same @Version, and then race their commits.
     * One thread must win (commit version+1), the other must fail with an
     * optimistic-lock exception. This is the real race the production code is
     * expected to handle by surfacing a 409 to the client.
     */
    @Test
    void concurrentUpdatesMustFailOptimisticLocking() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(txManager);
        Long taskId = tx.execute(status -> {
            ProjectEntity p = new ProjectEntity();
            p.setName("P");
            p.setStartDate(LocalDate.now());
            p.setEndDate(LocalDate.now().plusDays(1));
            p.setStatus(ProjectStatus.ACTIVE);
            projectRepository.save(p);

            SprintEntity s = new SprintEntity();
            s.setProject(p);
            s.setName("S");
            s.setStartDate(LocalDate.now());
            s.setEndDate(LocalDate.now().plusDays(1));
            s.setStatus(SprintStatus.ACTIVE);
            sprintRepository.save(s);

            TaskEntity t = new TaskEntity();
            t.setSprint(s);
            t.setTitle("T");
            t.setStatus(TaskStatus.TODO);
            t.setPriority(TaskPriority.HIGH);
            return taskRepository.save(t).getId();
        });

        CyclicBarrier readBarrier = new CyclicBarrier(2);
        CountDownLatch done = new CountDownLatch(2);
        AtomicReference<Throwable> err1 = new AtomicReference<>();
        AtomicReference<Throwable> err2 = new AtomicReference<>();

        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            pool.submit(updateTaskWorker("from-1", taskId, tx, readBarrier, done, err1));
            pool.submit(updateTaskWorker("from-2", taskId, tx, readBarrier, done, err2));

            assertTrue(done.await(10, TimeUnit.SECONDS), "workers did not finish in time");
        } finally {
            pool.shutdownNow();
        }

        // exactly one thread must fail with optimistic-lock; the other must succeed.
        Throwable winnerFailure = pickWinnerFailure(err1, err2);
        assertNull(winnerFailure, "one thread should commit cleanly, but both failed: " + winnerFailure);
        Throwable loserFailure = err1.get() != null ? err1.get() : err2.get();
        assertNotNull(loserFailure, "expected one thread to lose the race");
        assertTrue(isCausedBy(loserFailure, ObjectOptimisticLockingFailureException.class),
                "loser failure must be optimistic-lock, was: " + loserFailure);
    }

    private Runnable updateTaskWorker(String newTitle,
                                      Long taskId,
                                      TransactionTemplate tx,
                                      CyclicBarrier readBarrier,
                                      CountDownLatch done,
                                      AtomicReference<Throwable> errSink) {
        return () -> {
            try {
                tx.execute(status -> {
                    TaskEntity t = taskRepository.findById(taskId).orElseThrow();
                    try {
                        // Wait until the sibling worker has also read the same version.
                        readBarrier.await(5, TimeUnit.SECONDS);
                    } catch (Exception waitErr) {
                        throw new RuntimeException(waitErr);
                    }
                    t.setTitle(newTitle);
                    taskRepository.saveAndFlush(t);
                    return null;
                });
            } catch (Throwable ex) {
                errSink.set(ex);
            } finally {
                done.countDown();
            }
        };
    }

    private static Throwable pickWinnerFailure(AtomicReference<Throwable> a, AtomicReference<Throwable> b) {
        return a.get() != null && b.get() != null ? a.get() : null;
    }

    private static boolean isCausedBy(Throwable t, Class<? extends Throwable> target) {
        for (Throwable cur = t; cur != null; cur = cur.getCause()) {
            if (target.isInstance(cur)) {
                return true;
            }
        }
        return false;
    }
}

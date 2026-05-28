package ru.tischenko.vk.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;
import ru.tischenko.vk.domain.*;
import ru.tischenko.vk.domain.Enums.*;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class OverdueTaskNativeQueryDataJpaTest {
    @Autowired TestEntityManager em;
    @Autowired TaskRepository taskRepository;

    @Test
    void nativeQueryShouldReturnOldestOverdueExcludingDoneAndHonourLimit() {
        ProjectEntity p = new ProjectEntity();
        p.setName("P");
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(2));
        p.setStatus(ProjectStatus.ACTIVE);
        em.persist(p);

        SprintEntity s = new SprintEntity();
        s.setProject(p);
        s.setName("S");
        s.setStartDate(LocalDate.now());
        s.setEndDate(LocalDate.now().plusDays(1));
        s.setStatus(SprintStatus.ACTIVE);
        em.persist(s);

        Instant now = Instant.now();
        Instant oldest = now.minus(10, ChronoUnit.HOURS);
        Instant middle = now.minus(5, ChronoUnit.HOURS);
        Instant youngest = now.minus(1, ChronoUnit.HOURS);
        Instant future = now.plus(2, ChronoUnit.HOURS);

        em.persist(taskFor(s, "old-todo", oldest, TaskStatus.TODO));
        em.persist(taskFor(s, "mid-progress", middle, TaskStatus.IN_PROGRESS));
        em.persist(taskFor(s, "young-todo", youngest, TaskStatus.TODO));
        em.persist(taskFor(s, "done-but-overdue", oldest, TaskStatus.DONE));      // excluded by status
        em.persist(taskFor(s, "no-deadline", null, TaskStatus.TODO));             // excluded: deadline NULL
        em.persist(taskFor(s, "future-deadline", future, TaskStatus.TODO));       // excluded: not yet overdue
        em.flush();

        List<TaskEntity> top2 = taskRepository.findOldestOverdueNative(now, 2);

        // Limit honoured.
        assertEquals(2, top2.size());
        // Ordered by deadline ascending: oldest first.
        assertEquals("old-todo", top2.get(0).getTitle());
        assertEquals("mid-progress", top2.get(1).getTitle());

        // Larger limit returns all 3 valid overdue, still excludes DONE/NULL/future.
        List<TaskEntity> top10 = taskRepository.findOldestOverdueNative(now, 10);
        assertEquals(3, top10.size());
        assertTrue(top10.stream().noneMatch(t -> t.getStatus() == TaskStatus.DONE));
        assertTrue(top10.stream().allMatch(t -> t.getDeadline() != null && t.getDeadline().isBefore(now)));
    }

    private static TaskEntity taskFor(SprintEntity sprint, String title, Instant deadline, TaskStatus status) {
        TaskEntity t = new TaskEntity();
        t.setSprint(sprint);
        t.setTitle(title);
        t.setStatus(status);
        t.setPriority(TaskPriority.MEDIUM);
        t.setDeadline(deadline);
        return t;
    }
}

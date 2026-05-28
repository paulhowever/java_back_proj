package ru.tischenko.vk.repository;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.context.ActiveProfiles;
import ru.tischenko.vk.domain.*;
import ru.tischenko.vk.domain.Enums.*;
import ru.tischenko.vk.repository.TaskRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;

@DataJpaTest
@ActiveProfiles("test")
@Tag("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class TaskRepositoryDataJpaTest {
    @Autowired TestEntityManager em;
    @Autowired TaskRepository taskRepository;

    @Test
    void shouldFilterTasksByStatus() {
        ProjectEntity p = new ProjectEntity();
        p.setName("P");
        p.setStartDate(LocalDate.now());
        p.setEndDate(LocalDate.now().plusDays(1));
        p.setStatus(ProjectStatus.ACTIVE);
        em.persist(p);

        SprintEntity s = new SprintEntity();
        s.setProject(p);
        s.setName("S");
        s.setStartDate(LocalDate.now());
        s.setEndDate(LocalDate.now().plusDays(1));
        s.setStatus(SprintStatus.ACTIVE);
        em.persist(s);

        TaskEntity t = new TaskEntity();
        t.setSprint(s);
        t.setTitle("T");
        t.setStatus(TaskStatus.TODO);
        t.setPriority(TaskPriority.HIGH);
        em.persist(t);

        em.flush();

        var page = taskRepository.search(TaskStatus.TODO, null, null, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements());
    }
}

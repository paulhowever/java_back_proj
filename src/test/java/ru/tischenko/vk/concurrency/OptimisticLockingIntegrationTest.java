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
import ru.tischenko.vk.repository.Repositories.SprintRepository;
import ru.tischenko.vk.repository.Repositories.TaskRepository;
import ru.tischenko.vk.repository.Repositories.ProjectRepository;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertThrows;

@SpringBootTest
@ActiveProfiles("test")
@Tag("integration")
class OptimisticLockingIntegrationTest {
    @Autowired TaskRepository taskRepository;
    @Autowired ProjectRepository projectRepository;
    @Autowired SprintRepository sprintRepository;
    @Autowired PlatformTransactionManager txManager;

    @Test
    void shouldThrowOptimisticLockException() {
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

        TaskEntity firstCopy = tx.execute(status -> taskRepository.findById(taskId).orElseThrow());
        TaskEntity secondCopy = tx.execute(status -> taskRepository.findById(taskId).orElseThrow());

        tx.execute(status -> {
            firstCopy.setTitle("first");
            taskRepository.saveAndFlush(firstCopy);
            return null;
        });

        assertThrows(ObjectOptimisticLockingFailureException.class, () ->
            tx.execute(status -> {
                secondCopy.setTitle("second");
                taskRepository.saveAndFlush(secondCopy);
                return null;
            })
        );
    }
}

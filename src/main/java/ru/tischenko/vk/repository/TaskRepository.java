package ru.tischenko.vk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tischenko.vk.domain.TaskEntity;
import ru.tischenko.vk.domain.Enums.TaskPriority;
import ru.tischenko.vk.domain.Enums.TaskStatus;

import java.time.Instant;
import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
    @Query("""
        select t from TaskEntity t
        where (:status is null or t.status = :status)
          and (:assigneeId is null or t.assignee.id = :assigneeId)
          and (:priority is null or t.priority = :priority)
        """)
    Page<TaskEntity> search(TaskStatus status, Long assigneeId, TaskPriority priority, Pageable pageable);

    long countByAssigneeIdAndStatusIn(Long assigneeId, List<TaskStatus> statuses);

    List<TaskEntity> findBySprintId(Long sprintId);

    @Query("""
        select t from TaskEntity t
        where t.sprint.id = :sprintId
          and t.status <> :status
        """)
    List<TaskEntity> findBySprintIdAndStatusNot(Long sprintId, TaskStatus status);

    @Query("""
        select t from TaskEntity t
        where t.sprint.id = :sprintId
          and t.status <> ru.tischenko.vk.domain.Enums.TaskStatus.DONE
        """)
    List<TaskEntity> findRebalanceCandidates(Long sprintId);

    @Query("""
        select t from TaskEntity t
        where t.deadline is not null
          and t.deadline < :now
          and t.status <> ru.tischenko.vk.domain.Enums.TaskStatus.DONE
        """)
    List<TaskEntity> findOverdueTasks(Instant now);

    // Native query: bounded batch for the @Scheduled risk-notification job so a
    // huge backlog of overdue rows does not load the whole table into one tx.
    @Query(value = """
        SELECT * FROM tasks
        WHERE deadline IS NOT NULL
          AND deadline < :cutoff
          AND status <> 'DONE'
        ORDER BY deadline ASC
        LIMIT :maxResults
        """, nativeQuery = true)
    List<TaskEntity> findOldestOverdueNative(Instant cutoff, int maxResults);
}

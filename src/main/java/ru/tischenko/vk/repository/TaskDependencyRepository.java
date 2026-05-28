package ru.tischenko.vk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tischenko.vk.domain.TaskDependencyEntity;

import java.util.List;

@Repository
public interface TaskDependencyRepository extends JpaRepository<TaskDependencyEntity, Long> {
    List<TaskDependencyEntity> findByBlockedTaskId(Long blockedTaskId);

    long countByBlockerTaskId(Long blockerTaskId);

    Page<TaskDependencyEntity> findAll(Pageable pageable);

    @Query("""
        select d from TaskDependencyEntity d
        where d.blockerTask.sprint.id = :sprintId
        """)
    List<TaskDependencyEntity> findBySprintId(Long sprintId);
}

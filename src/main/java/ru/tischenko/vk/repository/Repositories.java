package ru.tischenko.vk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tischenko.vk.domain.*;
import ru.tischenko.vk.domain.Enums.ProjectStatus;
import ru.tischenko.vk.domain.Enums.Role;
import ru.tischenko.vk.domain.Enums.TaskStatus;
import ru.tischenko.vk.domain.Enums.UserLevel;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public final class Repositories {
    private Repositories() {
    }

    @Repository
    public interface UserRepository extends JpaRepository<UserEntity, Long> {
        Optional<UserEntity> findByEmail(String email);

        @Query("""
            select u from UserEntity u
            where (:role is null or u.role = :role)
              and (:level is null or u.level = :level)
              and (:enabled is null or u.enabled = :enabled)
            """)
        Page<UserEntity> search(Role role, UserLevel level, Boolean enabled, Pageable pageable);
    }

    @Repository
    public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
        Page<ProjectEntity> findByStatus(ru.tischenko.vk.domain.Enums.ProjectStatus status, Pageable pageable);

        @Query("""
            select p from ProjectEntity p
            where (:status is null or p.status = :status)
              and (:startDateFrom is null or p.startDate >= :startDateFrom)
              and (:startDateTo is null or p.startDate <= :startDateTo)
              and (:nameContains is null or lower(p.name) like lower(concat('%', :nameContains, '%')))
            """)
        Page<ProjectEntity> search(ProjectStatus status, LocalDate startDateFrom, LocalDate startDateTo, String nameContains, Pageable pageable);
    }

    @Repository
    public interface SprintRepository extends JpaRepository<SprintEntity, Long> {
        List<SprintEntity> findByProjectId(Long projectId);
        Page<SprintEntity> findByProjectId(Long projectId, Pageable pageable);
    }

    @Repository
    public interface TeamRepository extends JpaRepository<TeamEntity, Long> {
        Page<TeamEntity> findByProjectId(Long projectId, Pageable pageable);
    }

    @Repository
    public interface SubTeamRepository extends JpaRepository<SubTeamEntity, Long> {
        List<SubTeamEntity> findByTeamId(Long teamId);
        Page<SubTeamEntity> findByTeamId(Long teamId, Pageable pageable);
    }

    @Repository
    public interface TaskRepository extends JpaRepository<TaskEntity, Long> {
        @Query("""
            select t from TaskEntity t
            where (:status is null or t.status = :status)
              and (:assigneeId is null or t.assignee.id = :assigneeId)
              and (:priority is null or t.priority = :priority)
            """)
        Page<TaskEntity> search(TaskStatus status, Long assigneeId, ru.tischenko.vk.domain.Enums.TaskPriority priority, Pageable pageable);

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
    }

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

    @Repository
    public interface NotificationRepository extends JpaRepository<NotificationEntity, Long> {
        boolean existsByUserIdAndTextAndType(Long userId, String text, ru.tischenko.vk.domain.Enums.NotificationType type);
    }

    @Repository
    public interface IdempotencyRecordRepository extends JpaRepository<IdempotencyRecordEntity, Long> {
        Optional<IdempotencyRecordEntity> findByIdempotencyKey(String key);
    }
}

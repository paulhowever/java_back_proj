package ru.tischenko.vk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.domain.Enums.ProjectStatus;

import java.time.LocalDate;

@Repository
public interface ProjectRepository extends JpaRepository<ProjectEntity, Long> {
    Page<ProjectEntity> findByStatus(ProjectStatus status, Pageable pageable);

    // nameContains is always non-null here (service normalises null/blank to ""):
    // PgJDBC binds plain Java null as bytea, which then breaks lower(?) on Postgres.
    @Query("""
        select p from ProjectEntity p
        where (:status is null or p.status = :status)
          and (:startDateFrom is null or p.startDate >= :startDateFrom)
          and (:startDateTo is null or p.startDate <= :startDateTo)
          and (:nameContains = '' or lower(p.name) like lower(concat('%', :nameContains, '%')))
        """)
    Page<ProjectEntity> search(ProjectStatus status, LocalDate startDateFrom, LocalDate startDateTo, String nameContains, Pageable pageable);
}

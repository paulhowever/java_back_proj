package ru.tischenko.vk.service;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.ProjectRequest;
import ru.tischenko.vk.domain.Enums.ProjectStatus;
import ru.tischenko.vk.domain.ProjectEntity;
import ru.tischenko.vk.repository.ProjectRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class ProjectService {
    private final ProjectRepository projectRepository;

    public ProjectService(ProjectRepository projectRepository) {
        this.projectRepository = projectRepository;
    }

    @Transactional
    @CacheEvict(value = "projects", allEntries = true)
    public ProjectEntity createProject(ProjectRequest req) {
        validateDuration(req.startDate(), req.endDate());
        ProjectEntity e = new ProjectEntity();
        e.setName(req.name());
        e.setStartDate(req.startDate());
        e.setEndDate(req.endDate());
        e.setStatus(req.status());
        return projectRepository.save(e);
    }

    @Transactional(readOnly = true)
    @Cacheable(value = "projects", key = "#id")
    public ProjectEntity getProject(Long id) {
        return projectRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Project not found"));
    }

    @Transactional
    @CacheEvict(value = "projects", key = "#id")
    public ProjectEntity updateProject(Long id, ProjectRequest req) {
        validateDuration(req.startDate(), req.endDate());
        ProjectEntity p = getProject(id);
        p.setName(req.name());
        p.setStartDate(req.startDate());
        p.setEndDate(req.endDate());
        p.setStatus(req.status());
        return projectRepository.save(p);
    }

    @Transactional
    @CacheEvict(value = "projects", key = "#id")
    public void deleteProject(Long id) {
        projectRepository.delete(getProject(id));
    }

    @Transactional(readOnly = true)
    public Page<ProjectEntity> listProjects(ProjectStatus status, LocalDate startDateFrom, LocalDate startDateTo, String nameContains, Pageable pageable) {
        String normalisedName = (nameContains == null || nameContains.isBlank()) ? "" : nameContains.trim();
        return projectRepository.search(status, startDateFrom, startDateTo, normalisedName, pageable);
    }

    private void validateDuration(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new Exceptions.BusinessRuleException("Project endDate cannot be before startDate");
        }
        long durationDaysInclusive = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (durationDaysInclusive > 14) {
            throw new Exceptions.BusinessRuleException("Project duration cannot exceed 14 days");
        }
    }
}

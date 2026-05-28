package ru.tischenko.vk.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.SprintRequest;
import ru.tischenko.vk.domain.SprintEntity;
import ru.tischenko.vk.repository.SprintRepository;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Service
public class SprintService {
    private final SprintRepository sprintRepository;
    private final ProjectService projectService;

    public SprintService(SprintRepository sprintRepository, ProjectService projectService) {
        this.sprintRepository = sprintRepository;
        this.projectService = projectService;
    }

    @Transactional
    public SprintEntity createSprint(SprintRequest req) {
        validateDuration(req.startDate(), req.endDate());
        SprintEntity s = new SprintEntity();
        s.setProject(projectService.getProject(req.projectId()));
        s.setName(req.name());
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        s.setStatus(req.status());
        return sprintRepository.save(s);
    }

    @Transactional(readOnly = true)
    public SprintEntity getSprint(Long id) {
        return sprintRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("Sprint not found"));
    }

    @Transactional
    public SprintEntity updateSprint(Long id, SprintRequest req) {
        validateDuration(req.startDate(), req.endDate());
        SprintEntity s = getSprint(id);
        s.setName(req.name());
        s.setStartDate(req.startDate());
        s.setEndDate(req.endDate());
        s.setStatus(req.status());
        return sprintRepository.save(s);
    }

    @Transactional
    public void deleteSprint(Long id) {
        sprintRepository.delete(getSprint(id));
    }

    @Transactional(readOnly = true)
    public Page<SprintEntity> listSprints(Long projectId, Pageable pageable) {
        return projectId == null ? sprintRepository.findAll(pageable) : sprintRepository.findByProjectId(projectId, pageable);
    }

    private void validateDuration(LocalDate startDate, LocalDate endDate) {
        if (endDate.isBefore(startDate)) {
            throw new Exceptions.BusinessRuleException("Sprint endDate cannot be before startDate");
        }
        long durationDaysInclusive = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        if (durationDaysInclusive < 1 || durationDaysInclusive > 3) {
            throw new Exceptions.BusinessRuleException("Sprint duration must be 1..3 days");
        }
    }
}

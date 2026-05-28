package ru.tischenko.vk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import ru.tischenko.vk.domain.SubTeamEntity;

import java.util.List;

@Repository
public interface SubTeamRepository extends JpaRepository<SubTeamEntity, Long> {
    List<SubTeamEntity> findByTeamId(Long teamId);
    Page<SubTeamEntity> findByTeamId(Long teamId, Pageable pageable);
}

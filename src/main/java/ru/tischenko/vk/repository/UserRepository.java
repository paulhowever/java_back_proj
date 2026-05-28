package ru.tischenko.vk.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.domain.Enums.Role;
import ru.tischenko.vk.domain.Enums.UserLevel;

import java.util.Optional;

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

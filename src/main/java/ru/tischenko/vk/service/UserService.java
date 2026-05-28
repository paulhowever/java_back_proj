package ru.tischenko.vk.service;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import ru.tischenko.vk.api.dto.Dtos.UserRequest;
import ru.tischenko.vk.domain.Enums.Role;
import ru.tischenko.vk.domain.Enums.UserLevel;
import ru.tischenko.vk.domain.UserEntity;
import ru.tischenko.vk.repository.UserRepository;

@Service
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public UserEntity createUser(UserRequest req) {
        UserEntity e = new UserEntity();
        e.setEmail(req.email());
        e.setPasswordHash(passwordEncoder.encode(req.password()));
        e.setRole(req.role());
        e.setLevel(req.level());
        try {
            return userRepository.save(e);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("Email already exists");
        }
    }

    @Transactional(readOnly = true)
    public UserEntity getUser(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new Exceptions.NotFoundException("User not found"));
    }

    @Transactional
    public UserEntity updateUser(Long id, UserRequest req) {
        UserEntity u = getUser(id);
        u.setEmail(req.email());
        u.setRole(req.role());
        u.setLevel(req.level());
        if (req.password() != null && !req.password().isBlank()) {
            u.setPasswordHash(passwordEncoder.encode(req.password()));
        }
        try {
            return userRepository.save(u);
        } catch (DataIntegrityViolationException ex) {
            throw new Exceptions.ConflictException("Email already exists");
        }
    }

    @Transactional
    public void deleteUser(Long id) {
        userRepository.delete(getUser(id));
    }

    @Transactional(readOnly = true)
    public Page<UserEntity> listUsers(Role role, UserLevel level, Boolean enabled, Pageable pageable) {
        return userRepository.search(role, level, enabled, pageable);
    }
}
